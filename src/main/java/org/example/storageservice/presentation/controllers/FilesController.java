package org.example.storageservice.presentation.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.storageservice.application.services.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@Tag(name = "Files API", description = "API для загрузки, удаления, скачивания файлов и получения списка файлов из S3")
public class FilesController {

    private final StorageService storageService;

    @Autowired
    public FilesController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(
            summary = "Загрузка одного файла",
            description = "Загружает один файл в S3 и возвращает его публичный URL"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно загружен",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Срок действия токена истек"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при загрузке файла", content = @Content)
    })
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @Parameter(description = "Файл для загрузки", required = true)
            @RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.saveFile(file);
        return ResponseEntity.ok(fileUrl);
    }

    @Operation(
            summary = "Пакетная загрузка файлов",
            description = "Загружает список файлов в S3 и возвращает список их публичных URL"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файлы успешно загружены",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Срок действия токена истек"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при загрузке файлов", content = @Content)
    })
    @PostMapping(value = "/batch-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadFiles(
            @Parameter(description = "Список файлов для загрузки", required = true)
            @RequestPart("photos") List<MultipartFile> files) {
        List<String> fileUrls = storageService.saveFiles(files);
        return ResponseEntity.ok(fileUrls);
    }

    @Operation(
            summary = "Удаление файла",
            description = "Удаляет файл из S3 по указанному URL"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно удалён",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Неверный URL файла", content = @Content),
            @ApiResponse(responseCode = "401", description = "Срок действия токена истек"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при удалении файла", content = @Content)
    })
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(
            @Parameter(description = "URL файла для удаления", required = true)
            @RequestParam("url") String fileUrl) {
        storageService.deleteFile(fileUrl);
        return ResponseEntity.ok("File deleted successfully.");
    }

    @Operation(
            summary = "Пакетное удаление файлов",
            description = "Удаляет несколько файлов из S3 по указанным URL"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файлы успешно удалены",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Неверные URL файлов", content = @Content),
            @ApiResponse(responseCode = "401", description = "Срок действия токена истек"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при удалении файлов", content = @Content)
    })
    @DeleteMapping("/batch-delete")
    public ResponseEntity<String> deleteFiles(
            @Parameter(description = "Список URL файлов для удаления", required = true)
            @RequestParam("urls") List<String> fileUrls) {
        storageService.deleteFiles(fileUrls);
        return ResponseEntity.ok("Files deleted successfully.");
    }

    @Operation(
            summary = "Скачивание файла",
            description = "Скачивает файл из S3 по указанному URL и возвращает его содержимое в виде массива байтов"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно загружен",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "400", description = "Неверный URL файла", content = @Content),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при скачивании файла", content = @Content)
    })
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "URL файла для скачивания", required = true)
            @RequestParam("url") String fileUrl) {
        byte[] fileContent = storageService.loadFile(fileUrl);
        return ResponseEntity.ok(fileContent);
    }

    @Operation(
            summary = "Получение списка файлов",
            description = "Возвращает список URL всех файлов в указанной папке S3"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список файлов получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "Неверный путь к папке", content = @Content),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера при получении списка файлов", content = @Content)
    })
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(
            @Parameter(description = "Путь к папке в бакете S3 (например, folder/subfolder/)", required = true)
            @RequestParam("folder") String folder) {
        List<String> fileUrls = storageService.listFiles(folder);
        return ResponseEntity.ok(fileUrls);
    }
}