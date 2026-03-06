package com.quantumlauncher.instances.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Сервис для установки и управления версиями Minecraft
 */
@Service
public class MinecraftInstaller {
    
    private static final Logger log = LoggerFactory.getLogger(MinecraftInstaller.class);
    
    // Mojang URLs
    private static final String VERSIONS_JSON_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String ASSETS_BASE_URL = "https://resources.download.minecraft.net";
    private static final String LIBRARIES_BASE_URL = "https://libraries.minecraft.net";
    
    // Директории
    private static final String VERSIONS_DIR = "versions";
    private static final String ASSETS_DIR = "assets";
    private static final String LIBRARIES_DIR = "libraries";
    private static final String NATIVES_DIR = "natives";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    
    public MinecraftInstaller() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Получение списка доступных версий
     */
    public CompletableFuture<List<VersionInfo>> getAvailableVersions() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(VERSIONS_JSON_URL)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка получения версий: " + response.code());
                    }
                    
                    String body = response.body().string();
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    JsonArray versions = json.getAsJsonArray("versions");
                    
                    List<VersionInfo> result = new ArrayList<>();
                    for (JsonElement v : versions) {
                        JsonObject version = v.getAsJsonObject();
                        VersionInfo info = new VersionInfo();
                        info.id = version.get("id").getAsString();
                        info.type = version.get("type").getAsString();
                        info.url = version.get("url").getAsString();
                        info.releaseTime = version.get("releaseTime").getAsString();
                        result.add(info);
                    }
                    
                    return result;
                }
            } catch (Exception e) {
                log.error("Ошибка получения списка версий", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Получение списка только релизных версий
     */
    public CompletableFuture<List<String>> getReleaseVersions() {
        return getAvailableVersions().thenApply(versions -> 
                versions.stream()
                        .filter(v -> "release".equals(v.type))
                        .map(v -> v.id)
                        .collect(Collectors.toList())
        );
    }
    
    /**
     * Установка версии Minecraft
     */
    public CompletableFuture<Void> installVersion(String version, ProgressCallback callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Начало установки версии: {}", version);
                
                // 1. Получение URL манифеста версии
                String manifestUrl = getVersionManifestUrl(version);
                
                // 2. Скачивание манифеста
                callback.onProgress("Скачивание манифеста версии...", 0);
                JsonObject manifest = downloadVersionManifest(manifestUrl);
                
                // 3. Создание директорий
                Path versionDir = getVersionDir(version);
                Files.createDirectories(versionDir);
                
                // 4. Сохранение манифеста
                callback.onProgress("Сохранение манифеста...", 10);
                saveVersionManifest(versionDir, manifest, version);
                
                // 5. Скачивание client.jar
                callback.onProgress("Скачивание client.jar...", 20);
                downloadClientJar(version, manifest);
                
                // 6. Скачивание libraries
                callback.onProgress("Скачивание библиотеки...", 30);
                downloadLibraries(manifest, callback);
                
                // 7. Скачивание assets
                callback.onProgress("Скачивание ресурсов...", 70);
                downloadAssets(manifest, callback);
                
                // 8. Извлечение нативов
                callback.onProgress("Извлечение нативов...", 90);
                extractNatives(version, manifest);
                
                callback.onProgress("Готово!", 100);
                log.info("Версия {} успешно установлена", version);
                
            } catch (Exception e) {
                log.error("Ошибка установки версии {}", version, e);
                throw new RuntimeException("Ошибка установки версии: " + e.getMessage(), e);
            }
        }, executor);
    }
    
    /**
     * Получение URL манифеста версии
     */
    private String getVersionManifestUrl(String version) throws IOException {
        Request request = new Request.Builder()
                .url(VERSIONS_JSON_URL)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body().string();
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray versions = json.getAsJsonArray("versions");
            
            for (JsonElement v : versions) {
                JsonObject ver = v.getAsJsonObject();
                if (version.equals(ver.get("id").getAsString())) {
                    return ver.get("url").getAsString();
                }
            }
            
            throw new IOException("Версия " + version + " не найдена");
        }
    }
    
    /**
     * Скачивание манифеста версии
     */
    private JsonObject downloadVersionManifest(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка скачивания манифеста: " + response.code());
            }
            
            String body = response.body().string();
            return JsonParser.parseString(body).getAsJsonObject();
        }
    }
    
    /**
     * Сохранение манифеста версии
     */
    private void saveVersionManifest(Path versionDir, JsonObject manifest, String version) throws IOException {
        Path jsonPath = versionDir.resolve(version + ".json");
        String jsonContent = gson.toJson(manifest);
        Files.writeString(jsonPath, jsonContent);
    }
    
    /**
     * Скачивание client.jar
     */
    private void downloadClientJar(String version, JsonObject manifest) throws IOException {
        Path jarPath = getVersionJarPath(version);
        
        // Проверяем, существует ли уже файл
        if (Files.exists(jarPath)) {
            log.info("client.jar уже существует, пропускаем");
            return;
        }
        
        JsonObject downloads = manifest.getAsJsonObject("downloads");
        if (downloads == null || !downloads.has("client")) {
            throw new IOException("Нет client.jar в манифесте");
        }
        
        String url = downloads.getAsJsonObject("client").get("url").getAsString();
        downloadFile(url, jarPath, "client.jar");
    }
    
    /**
     * Скачивание библиотек
     */
    private void downloadLibraries(JsonObject manifest, ProgressCallback callback) throws IOException {
        JsonArray libraries = manifest.getAsJsonArray("libraries");
        int total = libraries.size();
        int current = 0;
        
        for (JsonElement libElem : libraries) {
            current++;
            JsonObject lib = libElem.getAsJsonObject();
            
            // Пропускаем, если нет правила для этой ОС
            if (!isLibraryAllowed(lib)) {
                continue;
            }
            
            try {
                downloadLibrary(lib);
            } catch (Exception e) {
                log.warn("Не удалось скачать библиотеку: {}", e.getMessage());
            }
            
            int progress = 30 + (current * 40 / total);
            callback.onProgress("Скачивание библиотек: " + current + "/" + total, progress);
        }
    }
    
    /**
     * Проверка, разрешена ли библиотека для текущей ОС
     */
    private boolean isLibraryAllowed(JsonObject lib) {
        if (!lib.has("rules") || lib.get("rules").isJsonNull()) {
            return true;
        }
        
        JsonArray rules = lib.getAsJsonArray("rules");
        boolean allowed = false;
        String os = System.getProperty("os.name").toLowerCase();
        
        for (JsonElement ruleElem : rules) {
            JsonObject rule = ruleElem.getAsJsonObject();
            String action = rule.get("action").getAsString();
            
            if ("allow".equals(action)) {
                if (!rule.has("os")) {
                    allowed = true;
                } else {
                    JsonObject ruleOs = rule.getAsJsonObject("os");
                    if (ruleOs.has("name")) {
                        String ruleOsName = ruleOs.get("name").getAsString().toLowerCase();
                        if ((ruleOsName.contains("windows") && os.contains("windows")) ||
                            (ruleOsName.contains("linux") && os.contains("linux")) ||
                            (ruleOsName.contains("os x") && os.contains("mac"))) {
                            allowed = true;
                        }
                    }
                }
            } else if ("disallow".equals(action)) {
                if (rule.has("os")) {
                    JsonObject ruleOs = rule.getAsJsonObject("os");
                    if (ruleOs.has("name")) {
                        String ruleOsName = ruleOs.get("name").getAsString().toLowerCase();
                        if ((ruleOsName.contains("windows") && os.contains("windows")) ||
                            (ruleOsName.contains("linux") && os.contains("linux")) ||
                            (ruleOsName.contains("os x") && os.contains("mac"))) {
                            allowed = false;
                        }
                    }
                }
            }
        }
        
        return allowed;
    }
    
    /**
     * Скачивание одной библиотеки
     */
    private void downloadLibrary(JsonObject lib) throws IOException {
        if (!lib.has("downloads") || lib.get("downloads").isJsonNull()) {
            return;
        }
        
        JsonObject downloads = lib.getAsJsonObject("downloads");
        if (!downloads.has("artifact") || downloads.get("artifact").isJsonNull()) {
            return;
        }
        
        JsonObject artifact = downloads.getAsJsonObject("artifact");
        String url = artifact.get("url").getAsString();
        String path = artifact.get("path").getAsString();
        
        Path libPath = Paths.get(LIBRARIES_DIR, path);
        
        // Проверяем, существует ли уже файл с правильным SHA1
        if (Files.exists(libPath)) {
            if (artifact.has("sha1")) {
                try {
                    String existingSha1 = calculateSHA1(libPath);
                    if (existingSha1.equalsIgnoreCase(artifact.get("sha1").getAsString())) {
                        return;
                    }
                } catch (Exception e) {
                    log.warn("Ошибка проверки SHA1: {}", e.getMessage());
                }
            }
        }
        
        // Создаём директории
        Files.createDirectories(libPath.getParent());
        
        // Скачиваем
        downloadFile(url, libPath, "library");
    }
    
    /**
     * Скачивание assets
     */
    private void downloadAssets(JsonObject manifest, ProgressCallback callback) throws IOException {
        JsonObject assetIndexObj = manifest.getAsJsonObject("assetIndex");
        String assetIndex = assetIndexObj.get("id").getAsString();
        
        Path indexPath = Paths.get(ASSETS_DIR, "indexes", assetIndex + ".json");
        
        // Скачиваем индекс если не существует
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath.getParent());
            String url = assetIndexObj.get("url").getAsString();
            downloadFile(url, indexPath, "asset index");
        }
        
        // Читаем индекс
        String indexContent = Files.readString(indexPath);
        JsonObject indexJson = JsonParser.parseString(indexContent).getAsJsonObject();
        JsonObject objects = indexJson.getAsJsonObject("objects");
        
        int total = objects.size();
        int current = 0;
        
        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            current++;
            String key = entry.getKey();
            JsonObject obj = entry.getValue().getAsJsonObject();
            
            String hash = obj.get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            
            Path assetPath = Paths.get(ASSETS_DIR, "objects", prefix, hash);
            
            if (!Files.exists(assetPath)) {
                String url = ASSETS_BASE_URL + "/" + prefix + "/" + hash;
                try {
                    Files.createDirectories(assetPath.getParent());
                    downloadFile(url, assetPath, "asset " + key);
                } catch (Exception e) {
                    log.warn("Не удалось скачать asset {}: {}", key, e.getMessage());
                }
            }
            
            int progress = 70 + (current * 20 / total);
            callback.onProgress("Скачивание ресурсов: " + current + "/" + total, progress);
        }
    }
    
    /**
     * Извлечение нативов
     */
    private void extractNatives(String version, JsonObject manifest) throws IOException {
        Path nativesPath = getNativesDir(version);
        Files.createDirectories(nativesPath);
        
        // Очищаем старые нативы
        try (var files = Files.list(nativesPath)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch");
        String osKey = getOsKey(os);
        
        JsonArray libraries = manifest.getAsJsonArray("libraries");
        
        for (JsonElement libElem : libraries) {
            JsonObject lib = libElem.getAsJsonObject();
            
            if (!lib.has("natives") || lib.get("natives").isJsonNull()) {
                continue;
            }
            
            JsonObject natives = lib.getAsJsonObject("natives");
            if (!natives.has(osKey)) {
                continue;
            }
            
            // Скачиваем и извлекаем нативную библиотеку
            try {
                extractNativeLibrary(lib, natives.get(osKey).getAsString(), nativesPath);
            } catch (Exception e) {
                log.warn("Не удалось извлечь нативы: {}", e.getMessage());
            }
        }
    }
        
    /**
     * Извлечение нативной библиотеки из JAR
     */
    private void extractNativeLibrary(JsonObject lib, String classifier, Path nativesPath) throws IOException {
        if (!lib.has("downloads") || lib.get("downloads").isJsonNull()) {
            return;
        }
        
        JsonObject downloads = lib.getAsJsonObject("downloads");
        if (!downloads.has("classifiers") || downloads.get("classifiers").isJsonNull()) {
            return;
        }
        
        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
        if (!classifiers.has(classifier) || classifiers.get(classifier).isJsonNull()) {
            return;
        }
        
        JsonObject classifierArtifact = classifiers.getAsJsonObject(classifier);
        String url = classifierArtifact.get("url").getAsString();
        String path = classifierArtifact.get("path").getAsString();
        
        Path libPath = Paths.get(LIBRARIES_DIR, path);
        
        if (!Files.exists(libPath)) {
            downloadFile(url, libPath, "native");
        }
        
        // Извлекаем из ZIP/JAR
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(libPath.toFile())) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // Файлы нативов
                if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib")) {
                    Path destPath = nativesPath.resolve(Paths.get(name).getFileName().toString());
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
    
    /**
     * Получение ключа ОС для правил
     */
    private String getOsKey(String os) {
        if (os.contains("windows")) return "windows";
        if (os.contains("linux")) return "linux";
        if (os.contains("mac")) return "osx";
        return os;
    }
    
    /**
     * Скачивание файла
     */
    private void downloadFile(String url, Path destPath, String description) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ошибка скачивания " + description + ": " + response.code());
            }
            
            try (InputStream is = response.body().byteStream()) {
                Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            log.debug("Скачан: {}", description);
        }
    }
    
    /**
     * Вычисление SHA1 хеша файла
     */
    private String calculateSHA1(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Получение директории версии
     */
    public Path getVersionDir(String version) {
        return Paths.get(VERSIONS_DIR, version);
    }
    
    /**
     * Получение пути к client.jar
     */
    public Path getVersionJarPath(String version) {
        return getVersionDir(version).resolve(version + ".jar");
    }
    
    /**
     * Получение директории нативов
     */
    public Path getNativesDir(String version) {
        return getVersionDir(version).resolve(NATIVES_DIR).resolve(getOsKey(System.getProperty("os.name").toLowerCase()));
    }
    
    /**
     * Получение директории assets
     */
    public Path getAssetsDir() {
        return Paths.get(ASSETS_DIR);
    }
    
    /**
     * Получение директории libraries
     */
    public Path getLibrariesDir() {
        return Paths.get(LIBRARIES_DIR);
    }
    
    /**
     * Проверка, установлена ли версия
     */
    public boolean isVersionInstalled(String version) {
        return Files.exists(getVersionJarPath(version));
    }
    
    /**
     * Получение полного classpath для версии
     */
    public List<Path> getClasspath(String version) throws IOException {
        List<Path> classpath = new ArrayList<>();
        
        Path jarPath = getVersionJarPath(version);
        if (Files.exists(jarPath)) {
            classpath.add(jarPath);
        }
        
        // Читаем манифест
        Path manifestPath = getVersionDir(version).resolve(version + ".json");
        if (!Files.exists(manifestPath)) {
            throw new IOException("Манифест версии не найден");
        }
        
        String content = Files.readString(manifestPath);
        JsonObject manifest = JsonParser.parseString(content).getAsJsonObject();
        
        // Добавляем libraries
        if (manifest.has("libraries")) {
            JsonArray libraries = manifest.getAsJsonArray("libraries");
            for (JsonElement libElem : libraries) {
                JsonObject lib = libElem.getAsJsonObject();
                
                if (!isLibraryAllowed(lib)) continue;
                if (!lib.has("downloads") || lib.get("downloads").isJsonNull()) continue;
                
                JsonObject downloads = lib.getAsJsonObject("downloads");
                if (!downloads.has("artifact") || downloads.get("artifact").isJsonNull()) continue;
                
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                String path = artifact.get("path").getAsString();
                
                Path libPath = Paths.get(LIBRARIES_DIR, path);
                if (Files.exists(libPath)) {
                    classpath.add(libPath);
                }
            }
        }
        
        return classpath;
    }
    
    /**
     * Получение assetIndex для версии
     */
    public String getAssetIndex(String version) throws IOException {
        Path manifestPath = getVersionDir(version).resolve(version + ".json");
        if (!Files.exists(manifestPath)) {
            throw new IOException("Манифест версии не найден");
        }
        
        String content = Files.readString(manifestPath);
        JsonObject manifest = JsonParser.parseString(content).getAsJsonObject();
        return manifest.getAsJsonObject("assetIndex").get("id").getAsString();
    }
    
    /**
     * Информация о версии
     */
    public static class VersionInfo {
        public String id;
        public String type;
        public String url;
        public String releaseTime;
    }
    
    /**
     * Интерфейс для callback прогресса
     */
    public interface ProgressCallback {
        void onProgress(String message, int percent);
    }
}
