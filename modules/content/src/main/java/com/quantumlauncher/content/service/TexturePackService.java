package com.quantumlauncher.content.service;

import com.quantumlauncher.core.model.TexturePack;
import com.quantumlauncher.core.repository.TexturePackRepository;
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
 * Сервис управления текстур-паками
 */
@Service
public class TexturePackService {
    
    private static final Logger log = LoggerFactory.getLogger(TexturePackService.class);
    
    @Autowired
    private TexturePackRepository texturePackRepository;
    
    /**
     * Получить все текстур-паки для указанного инстанса
     */
    public List<TexturePack> getTexturePacksForInstance(String instanceId) {
        return texturePackRepository.findByInstanceId(instanceId);
    }
    
    /**
     * Получить текстур-пак по ID
     */
    public Optional<TexturePack> getTexturePackById(String id) {
        return texturePackRepository.findById(id);
    }
    
    /**
     * Включить/отключить текстур-пак
     */
    @Transactional
    public void toggleTexturePack(String texturePackId, boolean enable) throws Exception {
        Optional<TexturePack> texturePackOpt = texturePackRepository.findById(texturePackId);
        if (texturePackOpt.isEmpty()) {
            throw new Exception("Текстур-пак не найден: " + texturePackId);
        }
        
        TexturePack texturePack = texturePackOpt.get();
        texturePack.setEnabled(enable);
        texturePackRepository.save(texturePack);
        
        log.info("Текстур-пак {} {}", texturePack.getName(), enable ? "включён" : "отключён");
    }
    
    /**
     * Установить текстур-пак из файла
     */
    @Transactional
    public TexturePack installTexturePack(String instanceId, File texturePackFile) throws Exception {
        if (!texturePackFile.exists()) {
            throw new Exception("Файл текстур-пака не найден: " + texturePackFile.getName());
        }
        
        String fileName = texturePackFile.getName();
        if (!fileName.endsWith(".zip")) {
            throw new Exception("Некорректный формат файла. Требуется ZIP");
        }
        
        TexturePack texturePack = new TexturePack();
        texturePack.setName(fileName.replace(".zip", ""));
        texturePack.setInstanceId(instanceId);
        texturePack.setFilePath(texturePackFile.getAbsolutePath());
        texturePack.setEnabled(true);
        texturePack.setSource("local");
        
        return texturePackRepository.save(texturePack);
    }
    
    /**
     * Удалить текстур-пак
     */
    @Transactional
    public void uninstallTexturePack(String texturePackId) throws Exception {
        Optional<TexturePack> texturePackOpt = texturePackRepository.findById(texturePackId);
        if (texturePackOpt.isEmpty()) {
            throw new Exception("Текстур-пак не найден: " + texturePackId);
        }
        
        TexturePack texturePack = texturePackOpt.get();
        
        if (texturePack.getFilePath() != null) {
            Path texturePackPath = Paths.get(texturePack.getFilePath());
            if (Files.exists(texturePackPath)) {
                Files.delete(texturePackPath);
            }
        }
        
        texturePackRepository.delete(texturePack);
        log.info("Текстур-пак {} удалён", texturePack.getName());
    }
    
    /**
     * Обновить информацию о текстур-паке
     */
    @Transactional
    public TexturePack updateTexturePack(TexturePack texturePack) {
        return texturePackRepository.save(texturePack);
    }
    
    /**
     * Поиск текстур-паков по названию
     */
    public List<TexturePack> searchTexturePacks(String query) {
        return texturePackRepository.findByNameContainingIgnoreCase(query);
    }
    
    /**
     * Получить текстур-паки по разрешению
     */
    public List<TexturePack> getTexturePacksByResolution(String instanceId, String resolution) {
        return texturePackRepository.findByInstanceIdAndResolution(instanceId, resolution);
    }
}
