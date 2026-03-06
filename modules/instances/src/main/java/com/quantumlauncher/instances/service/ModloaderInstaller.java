package com.quantumlauncher.instances.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Сервис для установки модлоадеров (Fabric, Forge, Quilt)
 */
@Service
public class ModloaderInstaller {
    
    private static final Logger log = LoggerFactory.getLogger(ModloaderInstaller.class);
    
    // Fabric URLs
    private static final String FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions";
    private static final String FABRIC_INSTALLER_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar";
    
    // Forge URLs
    private static final String FORGE_MAVEN_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge";
    
    // Quilt URLs
    private static final String QUILT_META_URL = "https://meta.quiltmc.org/v3/versions";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final MinecraftInstaller installer;
    
    public ModloaderInstaller(MinecraftInstaller installer) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool();
        this.installer = installer;
    }
    
    // ==================== FABRIC ====================
    
    /**
     * Получение доступных версий Fabric для версии Minecraft
     */
    public CompletableFuture<List<ModloaderVersion>> getAvailableFabricVersions(String mcVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(FABRIC_META_URL + "/loader/" + mcVersion)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка получения версий Fabric: " + response.code());
                    }
                    
                    String body = response.body().string();
                    JsonArray json = JsonParser.parseString(body).getAsJsonArray();
                    
                    List<ModloaderVersion> versions = new ArrayList<>();
                    for (var v : json) {
                        JsonObject obj = v.getAsJsonObject();
                        ModloaderVersion version = new ModloaderVersion();
                        version.loader = "fabric";
                        version.version = obj.get("loader").getAsString();
                        version.mcVersion = obj.get("gameVersion").getAsString();
                        version.url = obj.has("url") ? obj.get("url").getAsString() : null;
                        version.stable = obj.has("stable") && obj.get("stable").getAsBoolean();
                        versions.add(version);
                    }
                    
                    return versions;
                }
            } catch (Exception e) {
                log.error("Ошибка получения версий Fabric", e);
                return Collections.emptyList();
            }
        }, executor);
    }
    
    /**
     * Получение последней стабильной версии Fabric
     */
    public CompletableFuture<String> getLatestFabricVersion(String mcVersion) {
        return getAvailableFabricVersions(mcVersion).thenApply(versions -> 
                versions.stream()
                        .filter(ModloaderVersion::isStable)
                        .findFirst()
                        .map(v -> v.version)
                        .orElse(null)
        );
    }
    
    /**
     * Установка Fabric
     */
    public CompletableFuture<Void> installFabric(String mcVersion, String loaderVersion) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Установка Fabric {} для Minecraft {}", loaderVersion, mcVersion);
                
                // Убеждаемся что Minecraft установлен
                if (!installer.isVersionInstalled(mcVersion)) {
                    throw new IOException("Minecraft " + mcVersion + " не установлен");
                }
                
                // Скачиваем инсталлер Fabric
                String installerUrl = String.format(FABRIC_INSTALLER_URL, loaderVersion, loaderVersion);
                Path installerPath = Paths.get("libraries/net/fabricmc/fabric-installer/" + 
                        loaderVersion + "/fabric-installer-" + loaderVersion + ".jar");
                
                Files.createDirectories(installerPath.getParent());
                
                if (!Files.exists(installerPath)) {
                    downloadFile(installerUrl, installerPath, "Fabric installer");
                }
                
                // Запускаем инсталлер
                String javaPath = findJava();
                List<String> command = new ArrayList<>();
                command.add(javaPath);
                command.add("-jar");
                command.add(installerPath.toString());
                command.add("client");
                command.add("-dir");
                command.add(installer.getVersionDir(mcVersion).toString());
                command.add("-mcversion");
                command.add(mcVersion);
                command.add("-loader");
                command.add(loaderVersion);
                command.add("-natives");
                command.add(installer.getNativesDir(mcVersion).toString());
                command.add("-side");
                command.add("client");
                
                log.info("Запуск Fabric инсталлера: {}", String.join(" ", command));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                // Логируем вывод
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Fabric Installer] {}", line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Fabric installer завершился с кодом " + exitCode);
                }
                
                // Создаём meta JSON
                createFabricMetaJson(mcVersion, loaderVersion);
                
                log.info("Fabric {} успешно установлен", loaderVersion);
                
            } catch (Exception e) {
                log.error("Ошибка установки Fabric", e);
                throw new RuntimeException("Ошибка установки Fabric: " + e.getMessage(), e);
            }
        }, executor);
    }
    
    /**
     * Создание fabric-meta.json
     */
    private void createFabricMetaJson(String mcVersion, String loaderVersion) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("version", loaderVersion);
        json.addProperty("id", "fabric-loader-" + loaderVersion);
        
        JsonObject launcherMeta = new JsonObject();
        launcherMeta.addProperty("version", loaderVersion);
        launcherMeta.addProperty("name", "Fabric Loader");
        
        JsonObject libraries = new JsonObject();
        JsonObject common = new JsonObject();
        JsonArray commonArr = new JsonArray();
        
        JsonObject loaderLib = new JsonObject();
        loaderLib.addProperty("name", "net.fabricmc:fabric-loader:" + loaderVersion);
        commonArr.add(loaderLib);
        
        common.add("libraries", commonArr);
        libraries.add("common", common);
        
        JsonObject client = new JsonObject();
        client.add("libraries", new JsonArray());
        client.addProperty("mainClass", "net.fabricmc.loader.impl.launch.knot.KnotClient");
        
        launcherMeta.add("libraries", libraries);
        launcherMeta.addProperty("mainClass", "net.fabricmc.loader.impl.launch.knot.KnotClient");
        
        json.add("launcherMeta", launcherMeta);
        
        Path metaPath = installer.getVersionDir(mcVersion).resolve("fabric-loader-meta.json");
        Files.writeString(metaPath, json.toString());
    }
    
    // ==================== FORGE ====================
    
    /**
     * Получение доступных версий Forge для версии Minecraft
     */
    public CompletableFuture<List<ModloaderVersion>> getAvailableForgeVersions(String mcVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Forge 1.17+ использует новый формат
                String url;
                if (compareVersions(mcVersion, "1.17") >= 0) {
                    // Новый Forge (1.17+)
                    url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.json";
                } else {
                    // Старый Forge (1.16.5 и ранее)
                    url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.json";
                }
                
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка получения версий Forge: " + response.code());
                    }
                    
                    String body = response.body().string();
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    List<ModloaderVersion> versions = new ArrayList<>();
                    
                    // Парсим версии из json
                    if (json.has("versions")) {
                        JsonArray versionsArray = json.getAsJsonArray("versions");
                        for (var v : versionsArray) {
                            String version = v.getAsString();
                            if (version.contains(mcVersion)) {
                                ModloaderVersion modVer = new ModloaderVersion();
                                modVer.loader = "forge";
                                modVer.version = version;
                                modVer.mcVersion = mcVersion;
                                modVer.url = FORGE_MAVEN_URL + "/" + version + "/forge-" + version + "-universal.jar";
                                versions.add(modVer);
                            }
                        }
                    }
                    
                    return versions;
                }
            } catch (Exception e) {
                log.error("Ошибка получения версий Forge", e);
                return Collections.emptyList();
            }
        }, executor);
    }
    
    /**
     * Получение последней версии Forge
     */
    public CompletableFuture<String> getLatestForgeVersion(String mcVersion) {
        return getAvailableForgeVersions(mcVersion).thenApply(versions -> 
                versions.stream()
                        .findFirst()
                        .map(v -> v.version)
                        .orElse(null)
        );
    }
    
    /**
     * Установка Forge
     */
    public CompletableFuture<Void> installForge(String mcVersion, String forgeVersion) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Установка Forge {} для Minecraft {}", forgeVersion, mcVersion);
                
                // Убеждаемся что Minecraft установлен
                if (!installer.isVersionInstalled(mcVersion)) {
                    throw new IOException("Minecraft " + mcVersion + " не установлен");
                }
                
                // Скачиваем инсталлер Forge
                String installerUrl = getForgeInstallerUrl(mcVersion, forgeVersion);
                Path installerPath = Paths.get("libraries/net/minecraftforge/forge/" + 
                        forgeVersion + "/forge-" + forgeVersion + "-installer.jar");
                
                Files.createDirectories(installerPath.getParent());
                
                if (!Files.exists(installerPath)) {
                    downloadFile(installerUrl, installerPath, "Forge installer");
                }
                
                // Запускаем инсталлер
                String javaPath = findJava();
                List<String> command = new ArrayList<>();
                command.add(javaPath);
                command.add("-jar");
                command.add(installerPath.toString());
                command.add("--installServer");
                command.add(installer.getVersionDir(mcVersion).toString());
                
                log.info("Запуск Forge инсталлера: {}", String.join(" ", command));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                // Логируем вывод
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Forge Installer] {}", line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Forge installer завершился с кодом " + exitCode);
                }
                
                log.info("Forge {} успешно установлен", forgeVersion);
                
            } catch (Exception e) {
                log.error("Ошибка установки Forge", e);
                throw new RuntimeException("Ошибка установки Forge: " + e.getMessage(), e);
            }
        }, executor);
    }
    
    /**
     * Получение URL инсталлера Forge
     */
    private String getForgeInstallerUrl(String mcVersion, String forgeVersion) {
        // Forge 1.17+ имеет другой путь
        if (compareVersions(mcVersion, "1.17") >= 0) {
            return String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar", 
                    forgeVersion, forgeVersion);
        }
        // Старый Forge
        return String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", 
                forgeVersion, forgeVersion);
    }
    
    // ==================== QUILT ====================
    
    /**
     * Получение доступных версий Quilt для версии Minecraft
     */
    public CompletableFuture<List<ModloaderVersion>> getAvailableQuiltVersions(String mcVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(QUILT_META_URL + "/loader/" + mcVersion)
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ошибка получения версий Quilt: " + response.code());
                    }
                    
                    String body = response.body().string();
                    JsonArray json = JsonParser.parseString(body).getAsJsonArray();
                    
                    List<ModloaderVersion> versions = new ArrayList<>();
                    for (var v : json) {
                        JsonObject obj = v.getAsJsonObject();
                        ModloaderVersion version = new ModloaderVersion();
                        version.loader = "quilt";
                        version.version = obj.get("loader").getAsString();
                        version.mcVersion = obj.get("gameVersion").getAsString();
                        version.stable = obj.has("stable") && obj.get("stable").getAsBoolean();
                        versions.add(version);
                    }
                    
                    return versions;
                }
            } catch (Exception e) {
                log.error("Ошибка получения версий Quilt", e);
                return Collections.emptyList();
            }
        }, executor);
    }
    
    /**
     * Установка Quilt (аналогично Fabric)
     */
    public CompletableFuture<Void> installQuilt(String mcVersion, String quiltVersion) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Установка Quilt {} для Minecraft {}", quiltVersion, mcVersion);
                
                // Аналогично Fabric, используется похожий процесс
                // Скачиваем инсталлер Quilt
                String installerUrl = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/" + 
                        quiltVersion + "/quilt-installer-" + quiltVersion + ".jar";
                
                Path installerPath = Paths.get("libraries/org/quiltmc/quilt-installer/" + 
                        quiltVersion + "/quilt-installer-" + quiltVersion + ".jar");
                
                Files.createDirectories(installerPath.getParent());
                
                if (!Files.exists(installerPath)) {
                    downloadFile(installerUrl, installerPath, "Quilt installer");
                }
                
                // Запускаем инсталлер
                String javaPath = findJava();
                List<String> command = new ArrayList<>();
                command.add(javaPath);
                command.add("-jar");
                command.add(installerPath.toString());
                command.add("client");
                command.add("-dir");
                command.add(installer.getVersionDir(mcVersion).toString());
                command.add("-mcversion");
                command.add(mcVersion);
                command.add("-loader");
                command.add(quiltVersion);
                command.add("-side");
                command.add("client");
                
                log.info("Запуск Quilt инсталлера: {}", String.join(" ", command));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Quilt Installer] {}", line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Quilt installer завершился с кодом " + exitCode);
                }
                
                log.info("Quilt {} успешно установлен", quiltVersion);
                
            } catch (Exception e) {
                log.error("Ошибка установки Quilt", e);
                throw new RuntimeException("Ошибка установки Quilt: " + e.getMessage(), e);
            }
        }, executor);
    }
    
    // ==================== ОБЩИЕ МЕТОДЫ ====================
    
    /**
     * Проверка, установлен ли модлоадер
     */
    public boolean isInstalled(String mcVersion, String modloader) {
        Path versionDir = installer.getVersionDir(mcVersion);
        
        if (!Files.exists(versionDir)) {
            return false;
        }
        
        String versionId = mcVersion + "-" + modloader.toLowerCase();
        Path jarPath = versionDir.resolve(versionId + ".jar");
        
        // Проверяем разные варианты
        if (Files.exists(jarPath)) {
            return true;
        }
        
        // Для Fabric есть meta файл
        if ("fabric".equalsIgnoreCase(modloader)) {
            return Files.exists(versionDir.resolve("fabric-loader-meta.json"));
        }
        
        // Для Forge есть universal jar
        if ("forge".equalsIgnoreCase(modloader)) {
            return Files.exists(versionDir.resolve(mcVersion + "-forge.jar"));
        }
        
        return false;
    }
    
    /**
     * Получение версии для запуска (mcVersion + modloader)
     */
    public String getVersionId(String mcVersion, String modloader) {
        return mcVersion + "-" + modloader.toLowerCase();
    }
    
    /**
     * Получение main class для модлоадера
     */
    public String getMainClass(String mcVersion, String modloader) {
        if ("fabric".equalsIgnoreCase(modloader)) {
            return "net.fabricmc.loader.impl.launch.knot.KnotClient";
        }
        if ("forge".equalsIgnoreCase(modloader)) {
            // Forge 1.17+ использует другой main class
            if (compareVersions(mcVersion, "1.17") >= 0) {
                return "net.minecraftforge.bootstrap.Bootstrap";
            }
            return "net.minecraft.launchwrapper.Launch";
        }
        if ("quilt".equalsIgnoreCase(modloader)) {
            return "org.quiltmc.loader.impl.launch.knot.KnotClient";
        }
        // Vanilla
        return "net.minecraft.client.main.Main";
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
     * Поиск Java
     */
    private String findJava() {
        String javaHome = System.getProperty("java.home");
        Path javaPath = Paths.get(javaHome, "bin", "java.exe");
        
        if (Files.exists(javaPath)) {
            return javaPath.toString();
        }
        
        javaPath = Paths.get(javaHome, "bin", "java");
        return javaPath.toString();
    }
    
    /**
     * Сравнение версий
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (p1 != p2) {
                return p1 - p2;
            }
        }
        return 0;
    }
    
    /**
     * Класс для представления версии модлоадера
     */
    public static class ModloaderVersion {
        public String loader;
        public String version;
        public String mcVersion;
        public String url;
        public boolean stable;
        
        public String getLoader() { return loader; }
        public String getVersion() { return version; }
        public String getMcVersion() { return mcVersion; }
        public String getUrl() { return url; }
        public boolean isStable() { return stable; }
    }
}
