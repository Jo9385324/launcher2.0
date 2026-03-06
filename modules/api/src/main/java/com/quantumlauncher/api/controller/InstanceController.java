package com.quantumlauncher.api.controller;

import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.core.repository.InstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * API контроллер для управления инстансами
 */
@RestController
@RequestMapping("/api/instances")
public class InstanceController {
    
    @Autowired
    private InstanceRepository instanceRepository;
    
    /**
     * Получить все инстансы
     */
    @GetMapping
    public ResponseEntity<List<Instance>> getAllInstances() {
        List<Instance> instances = instanceRepository.findAll();
        return ResponseEntity.ok(instances);
    }
    
    /**
     * Получить инстанс по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Instance> getInstanceById(@PathVariable String id) {
        Optional<Instance> instance = instanceRepository.findById(id);
        return instance.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Создать новый инстанс
     */
    @PostMapping
    public ResponseEntity<Instance> createInstance(@RequestBody Instance instance) {
        Instance saved = instanceRepository.save(instance);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * Обновить инстанс
     */
    @PutMapping("/{id}")
    public ResponseEntity<Instance> updateInstance(
            @PathVariable String id,
            @RequestBody Instance instanceDetails) {
        
        Optional<Instance> instanceOpt = instanceRepository.findById(id);
        if (instanceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Instance instance = instanceOpt.get();
        instance.setName(instanceDetails.getName());
        instance.setVersion(instanceDetails.getVersion());
        instance.setModloader(instanceDetails.getModloader());
        instance.setDescription(instanceDetails.getDescription());
        instance.setJvmArgs(instanceDetails.getJvmArgs());
        instance.setMinRam(instanceDetails.getMinRam());
        instance.setMaxRam(instanceDetails.getMaxRam());
        
        Instance updated = instanceRepository.save(instance);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Удалить инстанс
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable String id) {
        if (!instanceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        instanceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Поиск инстансов по названию
     */
    @GetMapping("/search")
    public ResponseEntity<List<Instance>> searchInstances(@RequestParam String query) {
        List<Instance> instances = instanceRepository.findByNameContainingIgnoreCase(query);
        return ResponseEntity.ok(instances);
    }
}
