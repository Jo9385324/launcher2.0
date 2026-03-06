package com.quantumlauncher.security.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Сканер безопасности модов и файлов
 */
@Service
public class SecurityScanner {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityScanner.class);
    
    // Известные вредоносные паттерны
    private static final List<String> MALICIOUS_PATTERNS = Arrays.asList(
        "javax.script.ScriptEngineManager",
        "java.lang.Runtime.exec",
        "ProcessBuilder",
        "Desktop.getDesktop().open",
        "java.net.URLClassLoader",
        "javax.crypto.Cipher",
        "javafx.scene.web.WebEngine",
        "java.lang.Thread.stop",
        "System.exit",
        "Runtime.getRuntime().halt"
    );
    
    // Белый список безопасных модов
    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
        "minecraft", "forge", "fabric", "quilt", "liteloader", "optifine"
    ));
    
    /**
     * Результат сканирования
     */
    public static class ScanResult {
        public boolean isSafe;
        public List<Threat> threats = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public String fileHash;
        public long fileSize;
        
        public ScanResult() {
            this.isSafe = true;
        }
    }
    
    /**
     * Обнаруженная угроза
     */
    public static class Threat {
        public String type;
        public String description;
        public String severity; // LOW, MEDIUM, HIGH, CRITICAL
        public String location;
        
        public Threat(String type, String description, String severity, String location) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.location = location;
        }
    }
    
    /**
     * Сканирование файла мода
     */
    public ScanResult scanModFile(Path file) {
        log.info("Сканирование файла: {}", file);
        ScanResult result = new ScanResult();
        
        try {
            result.fileSize = Files.size(file);
            result.fileHash = calculateSHA256(file);
            
            // Проверка расширения
            String fileName = file.getFileName().toString().toLowerCase();
            if (!fileName.endsWith(".jar")) {
                result.warnings.add("Необычное расширение файла: " + fileName);
            }
            
            // Проверка размера
            if (result.fileSize < 1000) {
                result.threats.add(new Threat(
                    "SUSPICIOUS_SIZE",
                    "Файл слишком маленький",
                    "MEDIUM",
                    "Размер: " + result.fileSize + " байт"
                ));
                result.isSafe = false;
            }
            
            if (result.fileSize > 100_000_000) { // > 100MB
                result.warnings.add("Подозрительно большой файл: " + (result.fileSize / 1_000_000) + "MB");
            }
            
            // Анализ содержимого JAR
            analyzeJarContent(file, result);
            
            // Проверка хеша по базе известных вредоносных файлов
            checkHashDatabase(result);
            
        } catch (Exception e) {
            log.error("Ошибка сканирования файла {}", file, e);
            result.threats.add(new Threat(
                "SCAN_ERROR",
                "Ошибка при сканировании: " + e.getMessage(),
                "HIGH",
                file.toString()
            ));
            result.isSafe = false;
        }
        
        log.info("Сканирование завершено. Безопасно: {}, Угроз: {}, Предупреждений: {}", 
            result.isSafe, result.threats.size(), result.warnings.size());
        
        return result;
    }
    
    /**
     * Анализ содержимого JAR файла
     */
    private void analyzeJarContent(Path file, ScanResult result) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            Set<String> classFiles = new HashSet<>();
            
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                // Проверка на вредоносные паттерны
                if (name.endsWith(".class")) {
                    classFiles.add(name);
                    
                    // Читаем байты класса для анализа
                    if (entry.getSize() > 0 && entry.getSize() < 100_000) {
                        byte[] classData = zis.readAllBytes();
                        analyzeClassBytes(name, classData, result);
                    }
                }
                
                // Проверка на подозрительные файлы
                if (name.contains("hack") || name.contains("cheat") || 
                    name.contains("exploit") || name.contains("inject")) {
                    result.threats.add(new Threat(
                        "SUSPICIOUS_FILE",
                        "Подозрительный файл в архиве",
                        "HIGH",
                        name
                    ));
                    result.isSafe = false;
                }
                
                // Проверка на нативные библиотеки
                if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib")) {
                    result.warnings.add("Нативная библиотека: " + name);
                }
            }
            
            // Проверка на наличие MANIFEST.MF
            if (!classFiles.stream().anyMatch(f -> f.startsWith("META-INF/"))) {
                result.warnings.add("Отсутствует MANIFEST.MF");
            }
            
        } catch (Exception e) {
            log.warn("Ошибка анализа JAR содержимого", e);
        }
    }
    
    /**
     * Анализ байт-кода класса
     */
    private void analyzeClassBytes(String className, byte[] data, ScanResult result) {
        // Проверка на подозрительные последовательности байтов
        // Это упрощённая проверка - в реальном сканере используется дизассемблер
        
        String dataStr = new String(data);
        
        for (String pattern : MALICIOUS_PATTERNS) {
            if (dataStr.contains(pattern)) {
                result.threats.add(new Threat(
                    "DANGEROUS_CODE",
                    "Обнаружен потенциально опасный код: " + pattern,
                    "MEDIUM",
                    className
                ));
                result.isSafe = false;
            }
        }
        
        // Проверка на шифрование
        if (dataStr.contains("javax.crypto") && data.length > 10000) {
            result.warnings.add("Возможно обфуцированный или зашифрованный код: " + className);
        }
    }
    
    /**
     * Проверка хеша по базе
     */
    private void checkHashDatabase(ScanResult result) {
        // В реальной реализации здесь была бы проверка по базе данных известных хешей
        // Для демонстрации оставляем пустым
        
        // Пример: если хеш в чёрном списке - добавить угрозу
        // if (BLACKLIST_HASHES.contains(result.fileHash)) { ... }
    }
    
    /**
     * Проверка цифровой подписи
     */
    public boolean verifySignature(Path file) {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().startsWith("META-INF/") && 
                    entry.getName().endsWith(".RSA")) {
                    // Найдена подпись
                    log.info("Найдена цифровая подпись: {}", entry.getName());
                    return true;
                }
            }
            
            log.warn("Цифровая подпись не найдена: {}", file);
            return false;
            
        } catch (Exception e) {
            log.error("Ошибка проверки подписи", e);
            return false;
        }
    }
    
    /**
     * Вычисление SHA-256 хеша файла
     */
    private String calculateSHA256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file);
        byte[] hash = md.digest(fileBytes);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
