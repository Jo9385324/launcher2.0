package com.quantumlauncher.forks.service;

import com.quantumlauncher.core.model.Fork;
import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.repository.ForkRepository;
import com.quantumlauncher.core.repository.InstanceRepository;
import com.quantumlauncher.core.repository.ModRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Сервис управления кастомными сборками (Forks)
 */
@Service
public class ForkManager {
    
    private static final Logger log = LoggerFactory.getLogger(ForkManager.class);
    private final Gson gson = new Gson();
    
    @Autowired
    private ForkRepository forkRepository;
    
    @Autowired
    private InstanceRepository instanceRepository;
    
    @Autowired
    private ModRepository modRepository;
    
    /**
     * Структура сборки (JSON)
     */
    public static class ForkContent {
        public String name;
        public String mcVersion;
        public String modloader;
        public List<ModInfo> mods = new ArrayList<>();
        
        public static class ModInfo {
            public String name;
            public String version;
            public String downloadUrl;
            public String hash;
        }
    }
    
    /**
     * Создание сборки из инстанса
     */
    public Fork createForkFromInstance(String instanceId, String forkName, String description) {
        log.info("Создание сборки {} из инстанса {}", forkName, instanceId);
        
        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        if (instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Инстанс не найден: " + instanceId);
        }
        
        Instance instance = instanceOpt.get();
        
        // Создание сборки
        Fork fork = new Fork();
        fork.setName(forkName);
        fork.setDescription(description);
        fork.setMcVersion(instance.getVersion());
        fork.setModloader(instance.getModloader());
        fork.setAuthorName("Local User");
        
        // Сбор списка модов
        List<Mod> instanceMods = modRepository.findByInstanceId(instanceId);
        ForkContent content = new ForkContent();
        content.name = forkName;
        content.mcVersion = instance.getVersion();
        content.modloader = instance.getModloader();
        
        for (Mod mod : instanceMods) {
            ForkContent.ModInfo modInfo = new ForkContent.ModInfo();
            modInfo.name = mod.getName();
            modInfo.version = mod.getVersion();
            modInfo.downloadUrl = mod.getDownloadUrl();
            modInfo.hash = mod.getFileHash();
            content.mods.add(modInfo);
        }
        
        fork.setContent(gson.toJson(content));
        
        return forkRepository.save(fork);
    }
    
    /**
     * Применение сборки к инстансу
     */
    public void applyForkToInstance(String forkId, String instanceId) {
        log.info("Применение сборки {} к инстансу {}", forkId, instanceId);
        
        Optional<Fork> forkOpt = forkRepository.findById(forkId);
        Optional<Instance> instanceOpt = instanceRepository.findById(instanceId);
        
        if (forkOpt.isEmpty() || instanceOpt.isEmpty()) {
            throw new IllegalArgumentException("Сборка или инстанс не найдены");
        }
        
        Fork fork = forkOpt.get();
        Instance instance = instanceOpt.get();
        
        // Увеличение счётчика загрузок
        fork.setDownloadCount(fork.getDownloadCount() + 1);
        fork.setUpdatedAt(Instant.now().getEpochSecond());
        forkRepository.save(fork);
        
        // Парсинг контента
        Type listType = new TypeToken<ForkContent>() {}.getType();
        ForkContent content = gson.fromJson(fork.getContent(), listType);
        
        // Очистка текущих модов
        List<Mod> currentMods = modRepository.findByInstanceId(instanceId);
        for (Mod mod : currentMods) {
            modRepository.delete(mod);
        }
        
        // Установка новых модов (упрощённо - только создание записей в БД)
        for (ForkContent.ModInfo modInfo : content.mods) {
            Mod mod = new Mod();
            mod.setName(modInfo.name);
            mod.setVersion(modInfo.version);
            mod.setDownloadUrl(modInfo.downloadUrl);
            mod.setFileHash(modInfo.hash);
            mod.setInstanceId(instanceId);
            mod.setEnabled(true);
            modRepository.save(mod);
        }
        
        log.info("Сборка применена. Установлено {} модов", content.mods.size());
    }
    
    /**
     * Экспорт сборки в файл
     */
    public Path exportFork(String forkId, Path targetDir) throws IOException {
        Optional<Fork> forkOpt = forkRepository.findById(forkId);
        if (forkOpt.isEmpty()) {
            throw new IllegalArgumentException("Сборка не найдена");
        }
        
        Fork fork = forkOpt.get();
        
        Files.createDirectories(targetDir);
        Path exportFile = targetDir.resolve(fork.getName() + ".qforge");
        
        try (FileWriter writer = new FileWriter(exportFile.toFile())) {
            gson.toJson(fork, writer);
        }
        
        return exportFile;
    }
    
    /**
     * Импорт сборки из файла
     */
    public Fork importFork(Path filePath) throws IOException {
        log.info("Импорт сборки из: {}", filePath);
        
        try (FileReader reader = new FileReader(filePath.toFile())) {
            Fork fork = gson.fromJson(reader, Fork.class);
            fork.setId(UUID.randomUUID().toString());
            fork.setOfficial(false);
            
            return forkRepository.save(fork);
        }
    }
    
    /**
     * Получение списка официальных сборок
     */
    public List<Fork> getOfficialForks() {
        return forkRepository.findByOfficial(true);
    }
    
    /**
     * Поиск сборок по версии
     */
    public List<Fork> getForksByVersion(String mcVersion) {
        return forkRepository.findByMcVersion(mcVersion);
    }
    
    /**
     * Поиск сборок по модлоадеру
     */
    public List<Fork> getForksByModloader(String modloader) {
        return forkRepository.findByModloader(modloader);
    }
}
