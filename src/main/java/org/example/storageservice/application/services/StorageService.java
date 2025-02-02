package org.example.storageservice.application.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final AmazonS3 s3Client;

    @Autowired
    public StorageService(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Сохраняет файл в S3 и возвращает URL.
     */
    public String saveFile(MultipartFile file) {
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

            return s3Client.getUrl(bucketName, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении файла: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Сохраняет несколько файлов и возвращает список URL.
     */
    public List<String> saveFiles(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(saveFile(file));
        }
        return urls;
    }

    /**
     * Загружает файл из S3 и возвращает его содержимое в виде массива байтов.
     */
    public byte[] loadFile(String fileUrl) {
        String objectKey = extractObjectKey(fileUrl);
        try (S3Object s3Object = s3Client.getObject(bucketName, objectKey);
             InputStream inputStream = s3Object.getObjectContent()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла: " + fileUrl, e);
        }
    }

    /**
     * Удаляет файл из S3.
     */
    public void deleteFile(String fileUrl) {
        String objectKey = extractObjectKey(fileUrl);
        s3Client.deleteObject(bucketName, objectKey);
    }

    /**
     * Удаляет несколько файлов из S3.
     */
    public void deleteFiles(List<String> fileUrls) {
        for (String fileUrl : fileUrls) {
            deleteFile(fileUrl);
        }
    }

    /**
     * Генерирует уникальное имя файла для предотвращения конфликтов.
     */
    private String generateUniqueFileName(String originalFilename) {
        return UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("\\s+", "_");
    }

    /**
     * Извлекает ключ объекта из полного URL файла.
     */
    private String extractObjectKey(String fileUrl) {
        String bucketUrl = s3Client.getUrl(bucketName, "").toString();
        if (!fileUrl.startsWith(bucketUrl)) {
            throw new IllegalArgumentException("URL не принадлежит указанному bucket: " + fileUrl);
        }
        return fileUrl.substring(bucketUrl.length());
    }


    /**
     * Возвращает список URL всех файлов в указанной папке бакета S3.
     *
     * @param folderPath Путь к папке в бакете (например, "folder/subfolder/").
     *                   Если null или пусто, вернет файлы из корневой папки.
     * @return Список URL файлов.
     */
    public List<String> listFiles(String folderPath) {
        List<String> fileUrls = new ArrayList<>();

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(folderPath != null ? folderPath : ""); // Путь к папке

        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String fileUrl = s3Client.getUrl(bucketName, objectSummary.getKey()).toString();
                fileUrls.add(fileUrl);
            }

            // Устанавливаем продолжение списка
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return fileUrls;
    }
}