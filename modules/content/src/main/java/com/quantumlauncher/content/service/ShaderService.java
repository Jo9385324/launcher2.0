package com.quantumlauncher.content.service;

import com.quantumlauncher.core.model.Shader;
import com.quantumlauncher.core.repository.ShaderRepository;
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
 * Сервис управления шейдерами
 */
@Service
public class ShaderService {
    
    private static final Logger log = LoggerFactory.getLogger(ShaderService.class);
    
    @Autowired
    private ShaderRepository shaderRepository;
    
    /**
     * Получить все шейдеры для указанного инстанса
     */
    public List<Shader> getShadersForInstance(String instanceId) {
        return shaderRepository.findByInstanceId(instanceId);
    }
    
    /**
     * Получить шейдер по ID
     */
    public Optional<Shader> getShaderById(String id) {
        return shaderRepository.findById(id);
    }
    
    /**
     * Включить/отключить шейдер
     */
    @Transactional
    public void toggleShader(String shaderId, boolean enable) throws Exception {
        Optional<Shader> shaderOpt = shaderRepository.findById(shaderId);
        if (shaderOpt.isEmpty()) {
            throw new Exception("Шейдер не найден: " + shaderId);
        }
        
        Shader shader = shaderOpt.get();
        shader.setEnabled(enable);
        shaderRepository.save(shader);
        
        log.info("Шейдер {} {}", shader.getName(), enable ? "включён" : "отключён");
    }
    
    /**
     * Установить шейдер из файла
     */
    @Transactional
    public Shader installShader(String instanceId, File shaderFile) throws Exception {
        if (!shaderFile.exists()) {
            throw new Exception("Файл шейдера не найден: " + shaderFile.getName());
        }
        
        String fileName = shaderFile.getName();
        if (!fileName.endsWith(".zip") && !fileName.endsWith(".jar")) {
            throw new Exception("Некорректный формат файла. Требуется ZIP или JAR");
        }
        
        Shader shader = new Shader();
        shader.setName(fileName.replace(".zip", "").replace(".jar", ""));
        shader.setInstanceId(instanceId);
        shader.setFilePath(shaderFile.getAbsolutePath());
        shader.setEnabled(true);
        shader.setSource("local");
        
        return shaderRepository.save(shader);
    }
    
    /**
     * Удалить шейдер
     */
    @Transactional
    public void uninstallShader(String shaderId) throws Exception {
        Optional<Shader> shaderOpt = shaderRepository.findById(shaderId);
        if (shaderOpt.isEmpty()) {
            throw new Exception("Шейдер не найден: " + shaderId);
        }
        
        Shader shader = shaderOpt.get();
        
        if (shader.getFilePath() != null) {
            Path shaderPath = Paths.get(shader.getFilePath());
            if (Files.exists(shaderPath)) {
                Files.delete(shaderPath);
            }
        }
        
        shaderRepository.delete(shader);
        log.info("Шейдер {} удалён", shader.getName());
    }
    
    /**
     * Обновить информацию о шейдере
     */
    @Transactional
    public Shader updateShader(Shader shader) {
        return shaderRepository.save(shader);
    }
    
    /**
     * Поиск шейдеров по названию
     */
    public List<Shader> searchShaders(String query) {
        return shaderRepository.findByNameContainingIgnoreCase(query);
    }
    
    /**
     * Получить шейдеры по типу
     */
    public List<Shader> getShadersByType(String instanceId, String shaderType) {
        return shaderRepository.findByInstanceIdAndShaderType(instanceId, shaderType);
    }
}
