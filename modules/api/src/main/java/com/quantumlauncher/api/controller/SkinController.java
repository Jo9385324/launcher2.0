package com.quantumlauncher.api.controller;

import com.quantumlauncher.core.model.Skin;
import com.quantumlauncher.core.repository.SkinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * API контроллер для управления скинами
 */
@RestController
@RequestMapping("/api/skins")
public class SkinController {
    
    @Autowired
    private SkinRepository skinRepository;
    
    @GetMapping
    public ResponseEntity<List<Skin>> getAllSkins() {
        return ResponseEntity.ok(skinRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Skin> getSkinById(@PathVariable String id) {
        return skinRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/player/{playerUuid}")
    public ResponseEntity<List<Skin>> getSkinsByPlayer(@PathVariable String playerUuid) {
        return ResponseEntity.ok(skinRepository.findByPlayerUuid(playerUuid));
    }
    
    @GetMapping("/player/{playerUuid}/active")
    public ResponseEntity<Skin> getActiveSkin(@PathVariable String playerUuid) {
        return skinRepository.findByPlayerUuidAndActive(playerUuid, true)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Skin>> getSkinsByType(@PathVariable String type) {
        return ResponseEntity.ok(skinRepository.findByType(type));
    }
    
    @PostMapping
    public ResponseEntity<Skin> createSkin(@RequestBody Skin skin) {
        // Деактивировать все другие скины этого игрока
        if (skin.isActive()) {
            skinRepository.findByPlayerUuid(skin.getPlayerUuid())
                    .forEach(s -> {
                        s.setActive(false);
                        skinRepository.save(s);
                    });
        }
        Skin saved = skinRepository.save(skin);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Skin> updateSkin(@PathVariable String id, @RequestBody Skin skinDetails) {
        return skinRepository.findById(id)
                .map(skin -> {
                    skin.setName(skinDetails.getName());
                    skin.setType(skinDetails.getType());
                    skin.setUrl(skinDetails.getUrl());
                    skin.setActive(skinDetails.isActive());
                    return ResponseEntity.ok(skinRepository.save(skin));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/activate")
    public ResponseEntity<Skin> activateSkin(@PathVariable String id) {
        return skinRepository.findById(id)
                .map(skin -> {
                    // Деактивировать все остальные скины игрока
                    skinRepository.findByPlayerUuid(skin.getPlayerUuid())
                            .forEach(s -> {
                                s.setActive(false);
                                skinRepository.save(s);
                            });
                    skin.setActive(true);
                    return ResponseEntity.ok(skinRepository.save(skin));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkin(@PathVariable String id) {
        if (!skinRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        skinRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
