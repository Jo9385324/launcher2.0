package com.quantumlauncher.api.controller;

import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.repository.ModRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * API контроллер для управления модами
 */
@RestController
@RequestMapping("/api/mods")
public class ModController {
    
    @Autowired
    private ModRepository modRepository;
    
    @GetMapping
    public ResponseEntity<List<Mod>> getAllMods() {
        return ResponseEntity.ok(modRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Mod> getModById(@PathVariable String id) {
        return modRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/instance/{instanceId}")
    public ResponseEntity<List<Mod>> getModsByInstance(@PathVariable String instanceId) {
        List<Mod> mods = modRepository.findByInstanceId(instanceId);
        return ResponseEntity.ok(mods);
    }
    
    @GetMapping("/instance/{instanceId}/enabled")
    public ResponseEntity<List<Mod>> getEnabledModsByInstance(@PathVariable String instanceId) {
        List<Mod> mods = modRepository.findByInstanceIdAndEnabled(instanceId, true);
        return ResponseEntity.ok(mods);
    }
    
    @PostMapping
    public ResponseEntity<Mod> createMod(@RequestBody Mod mod) {
        Mod saved = modRepository.save(mod);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Mod> updateMod(@PathVariable String id, @RequestBody Mod modDetails) {
        return modRepository.findById(id)
                .map(mod -> {
                    mod.setName(modDetails.getName());
                    mod.setVersion(modDetails.getVersion());
                    mod.setCategory(modDetails.getCategory());
                    mod.setDescription(modDetails.getDescription());
                    mod.setEnabled(modDetails.isEnabled());
                    return ResponseEntity.ok(modRepository.save(mod));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMod(@PathVariable String id) {
        if (!modRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        modRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Mod>> searchMods(@RequestParam String query) {
        List<Mod> mods = modRepository.findByNameContainingIgnoreCase(query);
        return ResponseEntity.ok(mods);
    }
}
