package com.quantumlauncher.content.service;

import com.quantumlauncher.core.model.Map;
import com.quantumlauncher.core.repository.MapRepository;
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
 * Сервис управления картами
 */
@Service
public class MapService {
    
    private static final Logger log = LoggerFactory.getLogger(MapService.class);
    
    @Autowired
    private MapRepository mapRepository;
    
    /**
     * Получить все карты для указанного инстанса
     */
    public List<Map> getMapsForInstance(String instanceId) {
        return mapRepository.findByInstanceId(instanceId);
    }
    
    /**
     * Получить карту по ID
     */
    public Optional<Map> getMapById(String id) {
        return mapRepository.findById(id);
    }
    
    /**
     * Включить/отключить карту
     */
    @Transactional
    public void toggleMap(String mapId, boolean enable) throws Exception {
        Optional<Map> mapOpt = mapRepository.findById(mapId);
        if (mapOpt.isEmpty()) {
            throw new Exception("Карта не найдена: " + mapId);
        }
        
        Map map = mapOpt.get();
        map.setEnabled(enable);
        mapRepository.save(map);
        
        log.info("Карта {} {}", map.getName(), enable ? "включена" : "отключена");
    }
    
    /**
     * Установить карту из файла
     */
    @Transactional
    public Map installMap(String instanceId, File mapFile) throws Exception {
        if (!mapFile.exists()) {
            throw new Exception("Файл карты не найден: " + mapFile.getName());
        }
        
        String fileName = mapFile.getName();
        if (!fileName.endsWith(".zip") && !fileName.endsWith(".jar")) {
            throw new Exception("Некорректный формат файла. Требуется ZIP или JAR");
        }
        
        Map map = new Map();
        map.setName(fileName.replace(".zip", "").replace(".jar", ""));
        map.setInstanceId(instanceId);
        map.setFilePath(mapFile.getAbsolutePath());
        map.setEnabled(true);
        map.setSource("local");
        
        return mapRepository.save(map);
    }
    
    /**
     * Удалить карту
     */
    @Transactional
    public void uninstallMap(String mapId) throws Exception {
        Optional<Map> mapOpt = mapRepository.findById(mapId);
        if (mapOpt.isEmpty()) {
            throw new Exception("Карта не найдена: " + mapId);
        }
        
        Map map = mapOpt.get();
        
        if (map.getFilePath() != null) {
            Path mapPath = Paths.get(map.getFilePath());
            if (Files.exists(mapPath)) {
                Files.delete(mapPath);
            }
        }
        
        mapRepository.delete(map);
        log.info("Карта {} удалена", map.getName());
    }
    
    /**
     * Обновить информацию о карте
     */
    @Transactional
    public Map updateMap(Map map) {
        return mapRepository.save(map);
    }
    
    /**
     * Поиск карт по названию
     */
    public List<Map> searchMaps(String query) {
        return mapRepository.findByNameContainingIgnoreCase(query);
    }
    
    /**
     * Получить карты по типу
     */
    public List<Map> getMapsByType(String instanceId, String mapType) {
        return mapRepository.findByInstanceIdAndMapType(instanceId, mapType);
    }
    
    /**
     * Получить карты по версии игры
     */
    public List<Map> getMapsByGameVersion(String gameVersion) {
        return mapRepository.findByGameVersion(gameVersion);
    }
}
