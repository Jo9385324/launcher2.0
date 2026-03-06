package com.quantumlauncher.cloud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Сервис облачного хранилища (S3)
 */
@Service
public class CloudStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudStorageService.class);

    @Value("${cloud.s3.bucket:quantumlauncher}")
    private String bucketName;

    @Value("${cloud.s3.region:us-east-1}")
    private String region;

    @Value("${cloud.s3.access-key:}")
    private String accessKey;

    @Value("${cloud.s3.secret-key:}")
    private String secretKey;

    @Value("${cloud.s3.endpoint:}")
    private String customEndpoint;

    private S3Client s3Client;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @PostConstruct
    public void init() {
        log.info("Инициализация S3 клиента. Bucket: {}, Region: {}", bucketName, region);

        // Проверяем наличие credentials
        if (accessKey == null || accessKey.isEmpty()) {
            log.warn("AWS credentials не настроены. Облачная синхронизация недоступна.");
            log.warn("Для настройки добавьте в application.properties: cloud.s3.access-key и cloud.s3.secret-key");
            s3Client = null;
            return;
        }

        try {
            var builder = S3Client.builder()
                    .region(Region.of(region));

            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            ));

            if (customEndpoint != null && !customEndpoint.isEmpty()) {
                builder.endpointOverride(java.net.URI.create(customEndpoint));
            }

            s3Client = builder.build();

            // Проверка доступности бакета
            try {
                s3Client.headBucket(HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Бакет {} доступен", bucketName);
            } catch (NoSuchBucketException e) {
                log.warn("Бакет {} не существует, будет создан при первой загрузке", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка инициализации S3 клиента: {}", e.getMessage());
            s3Client = null;
        }
    }

    /**
     * Проверка, доступен ли S3
     */
    public boolean isEnabled() {
        return s3Client != null;
    }

    /**
     * Загрузка файла в облако
     */
    public String uploadFile(String key, Path filePath) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("Облачное хранилище не настроено. Добавьте AWS credentials.");
        }

        log.info("Загрузка файла в облако: {}", key);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(Files.probeContentType(filePath))
                .build();

        s3Client.putObject(request, RequestBody.fromFile(filePath));

        String url = String.format("s3://%s/%s", bucketName, key);
        log.info("Файл загружен: {}", url);

        return url;
    }

    /**
     * Асинхронная загрузка файла
     */
    public CompletableFuture<String> uploadFileAsync(String key, Path filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return uploadFile(key, filePath);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка загрузки файла", e);
            }
        }, executor);
    }

    /**
     * Загрузка файла из облака
     */
    public Path downloadFile(String key, Path targetDir) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("Облачное хранилище не настроено");
        }

        log.info("Загрузка файла из облака: {}", key);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        Path targetPath = targetDir.resolve(key.substring(key.lastIndexOf("/") + 1));

        s3Client.getObject(request, targetPath);

        log.info("Файл загружен: {}", targetPath);
        return targetPath;
    }

    /**
     * Удаление файла из облака
     */
    public void deleteFile(String key) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("Облачное хранилище не настроено");
        }

        log.info("Удаление файла из облака: {}", key);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
        log.info("Файл удалён: {}", key);
    }

    /**
     * Получение списка файлов
     */
    public List<String> listFiles(String prefix) {
        if (!isEnabled()) {
            log.warn("Облачное хранилище не настроено");
            return List.of();
        }

        log.info("Получение списка файлов с префиксом: {}", prefix);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        List<String> files = new ArrayList<>();
        for (S3Object obj : response.contents()) {
            files.add(obj.key());
        }

        return files;
    }

    /**
     * Проверка существования файла
     */
    public boolean fileExists(String key) {
        if (!isEnabled()) {
            return false;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Получение публичной ссылки на файл
     */
    public String getPublicUrl(String key) {
        // Для приватных бакетов потребуется генерация преsigned URL
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, key);
    }

    /**
     * Синхронизация локальной папки с облаком
     */
    public void syncFolder(String prefix, Path localFolder) throws Exception {
        log.info("Синхронизация папки {} с облаком", localFolder);

        // Загрузка новых/изменённых файлов
        Files.walk(localFolder)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    String relativePath = localFolder.relativize(file).toString();
                    String key = prefix + "/" + relativePath.replace("\\", "/");

                    try {
                        if (!fileExists(key)) {
                            uploadFile(key, file);
                        } else {
                            // Проверка хеша - в реальной реализации
                            log.info("Файл уже существует: {}", key);
                        }
                    } catch (Exception e) {
                        log.error("Ошибка синхронизации файла {}", file, e);
                    }
                });

        log.info("Синхронизация завершена");
    }

    /**
     * Закрытие клиента
     */
    public void shutdown() {
        if (s3Client != null) {
            s3Client.close();
        }
        executor.shutdown();
    }
}
