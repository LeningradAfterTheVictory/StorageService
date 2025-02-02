package org.example.storageservice.presentation.controllers;


import org.example.storageservice.application.services.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FilesController {

    private final StorageService storageService;

    @Autowired
    public FilesController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Загрузка одного файла.
     *
     * @param file файл для загрузки
     * @return URL загруженного файла
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.saveFile(file);
        return ResponseEntity.ok(fileUrl);
    }

    /**
     * Загрузка нескольких файлов.
     *
     * @param files список файлов для загрузки
     * @return список URL загруженных файлов
     */
    @PostMapping("/batch-upload")
    public ResponseEntity<List<String>> uploadFiles(@RequestParam("photos") List<MultipartFile> files) {
        List<String> fileUrls = storageService.saveFiles(files);
        return ResponseEntity.ok(fileUrls);
    }

    /**
     * Удаление файла по URL.
     *
     * @param fileUrl URL файла для удаления
     * @return сообщение об успешном удалении
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("url") String fileUrl) {
        storageService.deleteFile(fileUrl);
        return ResponseEntity.ok("File deleted successfully.");
    }

    /**
     * Удаление нескольких файлов.
     *
     * @param fileUrls список URL файлов для удаления
     * @return сообщение об успешном удалении
     */
    @DeleteMapping("/batch-delete")
    public ResponseEntity<String> deleteFiles(@RequestParam("urls") List<String> fileUrls) {
        storageService.deleteFiles(fileUrls);
        return ResponseEntity.ok("Files deleted successfully.");
    }

    /**
     * Загрузка файла по URL.
     *
     * @param fileUrl URL файла
     * @return содержимое файла
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("url") String fileUrl) {
        byte[] fileContent = storageService.loadFile(fileUrl);
        return ResponseEntity.ok(fileContent);
    }

    /**
     * Получение списка всех файлов в папке.
     *
     * @return список URL всех файлов
     */
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(@RequestParam("folder") String folder) {
        List<String> fileUrls = storageService.listFiles(folder);
        return ResponseEntity.ok(fileUrls);
    }
}