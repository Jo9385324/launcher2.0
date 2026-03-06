package com.quantumlauncher.content.service;

import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.repository.ModRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Сервис управления модами - загрузка, установка, удаление
 */
@Service
public class ModManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(ModManagementService.class);
    
    @Autowired
    private ModRepository modRepository;
    
    /**
     * Типы модов
     */
    public enum ModType {
        COREMOD,
        FABRIC_MOD,
        FORGE_MOD,
        QUILT_MOD
    }
    
    /**
     * Информация о моде из файла
     */
    public static class ModInfo {
        public String name;
        public String version;
        public String description;
        public String author;
        public String mainClass;
        public List<String> dependencies = new ArrayList<>();
        public ModType type;
        public String modId;
    }
    
    /**
     * Установка мода из файла
     */
    public Mod installMod(String instanceId, Path modFile) throws Exception {
        log.info("Установка мода {} для инстанса {}", modFile, instanceId);
        
        // Чтение информации о моде
        ModInfo info = parseModFile(modFile);
        
        // Проверка наличия мода
        if (modRepository.existsByNameAndInstanceId(info.name, instanceId)) {
            throw new IllegalStateException("Мод уже установлен: " + info.name);
        }
        
        // Копирование файла в директорию модов
        Path modsDir = Paths.get("instances", instanceId, "mods");
        Files.createDirectories(modsDir);
        Path targetPath = modsDir.resolve(modFile.getFileName());
        Files.copy(modFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Создание записи в БД
        Mod mod = new Mod();
        mod.setName(info.name);
        mod.setVersion(info.version);
        mod.setDescription(info.description);
        mod.setAuthor(info.author);
        mod.setInstanceId(instanceId);
        mod.setFilePath(targetPath.toString());
        mod.setFileHash(calculateFileHash(targetPath));
        mod.setCategory(detectCategory(info.name));
        
        return modRepository.save(mod);
    }
    
    /**
     * Удаление мода
     */
    public void uninstallMod(String modId) throws Exception {
        Optional<Mod> modOpt = modRepository.findById(modId);
        if (modOpt.isEmpty()) {
            throw new IllegalArgumentException("Мод не найден: " + modId);
        }
        
        Mod mod = modOpt.get();
        
        // Удаление файла
        if (mod.getFilePath() != null) {
            Path filePath = Paths.get(mod.getFilePath());
            Files.deleteIfExists(filePath);
        }
        
        // Удаление из БД
        modRepository.deleteById(modId);
        log.info("Мод удалён: {}", mod.getName());
    }
    
    /**
     * Включение/выключение мода
     */
    public Mod toggleMod(String modId, boolean enabled) throws Exception {
        Optional<Mod> modOpt = modRepository.findById(modId);
        if (modOpt.isEmpty()) {
            throw new IllegalArgumentException("Мод не найден: " + modId);
        }
        
        Mod mod = modOpt.get();
        
        // Перемещение файла
        Path currentPath = Paths.get(mod.getFilePath());
        String fileName = currentPath.getFileName().toString();
        
        if (enabled) {
            // Включение - перемещение из _disabled в mods
            Path disabledDir = currentPath.getParent().resolve("_disabled");
            Path enabledPath = currentPath.getParent().getParent().resolve("mods").resolve(fileName);
            if (Files.exists(disabledDir.resolve(fileName))) {
                Files.move(disabledDir.resolve(fileName), enabledPath);
                mod.setFilePath(enabledPath.toString());
            }
        } else {
            // Выключение - перемещение в _disabled
            Path disabledDir = currentPath.getParent().resolve("_disabled");
            Files.createDirectories(disabledDir);
            Path disabledPath = disabledDir.resolve(fileName);
            Files.move(currentPath, disabledPath);
            mod.setFilePath(disabledPath.toString());
        }
        
        mod.setEnabled(enabled);
        return modRepository.save(mod);
    }
    
    /**
     * Парсинг информации о моде из JAR файла
     */
    public ModInfo parseModFile(Path modFile) throws Exception {
        ModInfo info = new ModInfo();
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(modFile))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                // Поиск fabric.mod.json
                if (name.equals("fabric.mod.json")) {
                    parseFabricModJson(zis, info);
                    info.type = ModType.FABRIC_MOD;
                }
                // Поиск mods.toml (Forge)
                else if (name.equals("META-INF/mods.toml")) {
                    parseForgeModsToml(zis, info);
                    info.type = ModType.FORGE_MOD;
                }
                // Поиск quilt.mod.json
                else if (name.equals("quilt.mod.json")) {
                    parseQuiltModJson(zis, info);
                    info.type = ModType.QUILT_MOD;
                }
                // Поиск mcmod.info (Forge старый формат)
                else if (name.equals("mcmod.info")) {
                    parseMcmodInfo(zis, info);
                    info.type = ModType.FORGE_MOD;
                }
            }
        }
        
        // Если не удалось определить тип - определяем по файлу
        if (info.name == null || info.name.isEmpty()) {
            info.name = modFile.getFileName().toString()
                .replace(".jar", "")
                .replace("-", " ");
        }
        
        return info;
    }
    
    private void parseFabricModJson(InputStream is, ModInfo info) {
        try {
            String content = new String(is.readAllBytes());
            // Простой парсинг JSON (упрощённо)
            info.name = extractJsonValue(content, "\"name\"");
            info.version = extractJsonValue(content, "\"version\"");
            info.description = extractJsonValue(content, "\"description\"");
            info.author = extractJsonValue(content, "\"authors\"");
            info.mainClass = extractJsonValue(content, "\"entrypoints\"");
            info.modId = extractJsonValue(content, "\"id\"");
        } catch (Exception e) {
            log.warn("Ошибка парсинга fabric.mod.json", e);
        }
    }
    
    private void parseForgeModsToml(InputStream is, ModInfo info) {
        try {
            String content = new String(is.readAllBytes());
            // TOML парсинг (упрощённо)
            for (String line : content.split("\n")) {
                if (line.startsWith("modId=")) {
                    info.modId = line.split("=")[1].replace("\"", "").trim();
                }
                if (line.startsWith("version=")) {
                    info.version = line.split("=")[1].replace("\"", "").trim();
                }
                if (line.startsWith("displayName=")) {
                    info.name = line.split("=")[1].replace("\"", "").trim();
                }
            }
            info.type = ModType.FORGE_MOD;
        } catch (Exception e) {
            log.warn("Ошибка парсинга mods.toml", e);
        }
    }
    
    private void parseMcmodInfo(InputStream is, ModInfo info) {
        try {
            String content = new String(is.readAllBytes());
            info.name = extractJsonValue(content, "\"name\"");
            info.version = extractJsonValue(content, "\"version\"");
            info.description = extractJsonValue(content, "\"description\"");
            info.author = extractJsonValue(content, "\"author\"");
        } catch (Exception e) {
            log.warn("Ошибка парсинга mcmod.info", e);
        }
    }
    
    private void parseQuiltModJson(InputStream is, ModInfo info) {
        parseFabricModJson(is, info); // Аналогичный формат
        info.type = ModType.QUILT_MOD;
    }
    
    private String extractJsonValue(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return null;
        
        int start = json.indexOf(":", index);
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        
        if (start == -1 || end == -1) return null;
        
        String value = json.substring(start + 1, end).trim();
        value = value.replace("\"", "").replace("[", "").replace("]", "");
        
        return value.isEmpty() ? null : value;
    }
    
    private String calculateFileHash(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = Files.readAllBytes(file);
        byte[] hash = md.digest(bytes);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private String detectCategory(String name) {
        String lower = name.toLowerCase();
        
        if (containsAny(lower, "jei", "rei", "emi", "recipe", "книга рецептов")) return "RECIPE";
        if (containsAny(lower, "minimap", "карта", "xaero")) return "MAP";
        if (containsAny(lower, "optifine", "shader", "шейдер")) return "GRAPHICS";
        if (containsAny(lower, "inventory", "container", "сундук")) return "UI";
        if (containsAny(lower, "world", "worldedit", "structure", " мир")) return "WORLD";
        if (containsAny(lower, "item", "tool", "предмет")) return "ITEMS";
        if (containsAny(lower, "tech", "машина", "механизм")) return "TECH";
        if (containsAny(lower, "magic", "магия", "spell")) return "MAGIC";
        
        return "OTHER";
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
