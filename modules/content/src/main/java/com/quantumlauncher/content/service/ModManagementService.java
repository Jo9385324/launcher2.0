package com.quantumlauncher.content.service;

import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.repository.ModRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Сервис управления модами
 */
@Service
public class ModManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(ModManagementService.class);
    
    @Autowired
    private ModRepository modRepository;
    
    /**
     * Получить все моды для указанного инстанса
     */
    public List<Mod> getModsForInstance(String instanceId) {
        return modRepository.findByInstanceId(instanceId);
    }
    
    /**
     * Получить мод по ID
     */
    public Optional<Mod> getModById(String id) {
        return modRepository.findById(id);
    }
    
    /**
     * Включить/отключить мод
     */
    @Transactional
    public void toggleMod(String modId, boolean enable) throws Exception {
        Optional<Mod> modOpt = modRepository.findById(modId);
        if (modOpt.isEmpty()) {
            throw new Exception("Мод не найден: " + modId);
        }
        
        Mod mod = modOpt.get();
        mod.setEnabled(enable);
        modRepository.save(mod);
        
        // Перемещаем файл мода в/из папки disabled
        if (mod.getFilePath() != null) {
            Path modPath = Paths.get(mod.getFilePath());
            Path disabledPath = modPath.resolveSibling("disabled");
            
            if (enable) {
                // Включаем мод - перемещаем из disabled обратно
                Path disabledFile = disabledPath.resolve(modPath.getFileName());
                if (Files.exists(disabledFile)) {
                    Files.move(disabledFile, modPath);
                }
            } else {
                // Отключаем мод - перемещаем в disabled
                if (Files.exists(modPath)) {
                    if (!Files.exists(disabledPath)) {
                        Files.createDirectory(disabledPath);
                    }
                    Path targetPath = disabledPath.resolve(modPath.getFileName());
                    Files.move(modPath, targetPath);
                }
            }
        }
        
        log.info("Мод {} {}", mod.getName(), enable ? "включён" : "отключён");
    }
    
    /**
     * Установить мод из файла
     */
    @Transactional
    public Mod installMod(String instanceId, File modFile) throws Exception {
        // Проверка файла
        if (!modFile.exists()) {
            throw new Exception("Файл мода не найден: " + modFile.getName());
        }
        
        if (!modFile.getName().endsWith(".jar")) {
            throw new Exception("Некорректный формат файла. Требуется JAR");
        }
        
        // Создание записи в БД
        Mod mod = new Mod();
        mod.setName(modFile.getName().replace(".jar", ""));
        mod.setInstanceId(instanceId);
        mod.setFilePath(modFile.getAbsolutePath());
        mod.setEnabled(true);
        mod.setSource("local");
        
        // Вычисление хеша файла
        // mod.setFileHash(computeFileHash(modFile));
        
        return modRepository.save(mod);
    }
    
    /**
     * Удалить мод
     */
    @Transactional
    public void uninstallMod(String modId) throws Exception {
        Optional<Mod> modOpt = modRepository.findById(modId);
        if (modOpt.isEmpty()) {
            throw new Exception("Мод не найден: " + modId);
        }
        
        Mod mod = modOpt.get();
        
        // Удаление файла мода
        if (mod.getFilePath() != null) {
            Path modPath = Paths.get(mod.getFilePath());
            if (Files.exists(modPath)) {
                Files.delete(modPath);
            }
        }
        
        // Удаление из БД
        modRepository.delete(mod);
        
        log.info("Мод {} удалён", mod.getName());
    }
    
    /**
     * Обновить информацию о моде
     */
    @Transactional
    public Mod updateMod(Mod mod) {
        return modRepository.save(mod);
    }
    
    /**
     * Поиск модов по названию
     */
    public List<Mod> searchMods(String query) {
        return modRepository.findByNameContainingIgnoreCase(query);
    }
    
    /**
     * Получить моды по категории
     */
    public List<Mod> getModsByCategory(String instanceId, String category) {
        return modRepository.findByInstanceIdAndCategory(instanceId, category);
    }
}
