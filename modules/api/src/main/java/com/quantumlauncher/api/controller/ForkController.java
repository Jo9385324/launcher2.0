package com.quantumlauncher.api.controller;

import com.quantumlauncher.core.model.Fork;
import com.quantumlauncher.core.repository.ForkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * API контроллер для управления сборками (Forks)
 */
@RestController
@RequestMapping("/api/forks")
public class ForkController {
    
    @Autowired
    private ForkRepository forkRepository;
    
    @GetMapping
    public ResponseEntity<List<Fork>> getAllForks() {
        return ResponseEntity.ok(forkRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Fork> getForkById(@PathVariable String id) {
        return forkRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/official")
    public ResponseEntity<List<Fork>> getOfficialForks() {
        return ResponseEntity.ok(forkRepository.findByOfficial(true));
    }
    
    @GetMapping("/version/{mcVersion}")
    public ResponseEntity<List<Fork>> getForksByVersion(@PathVariable String mcVersion) {
        return ResponseEntity.ok(forkRepository.findByMcVersion(mcVersion));
    }
    
    @GetMapping("/modloader/{modloader}")
    public ResponseEntity<List<Fork>> getForksByModloader(@PathVariable String modloader) {
        return ResponseEntity.ok(forkRepository.findByModloader(modloader));
    }
    
    @PostMapping
    public ResponseEntity<Fork> createFork(@RequestBody Fork fork) {
        Fork saved = forkRepository.save(fork);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Fork> updateFork(@PathVariable String id, @RequestBody Fork forkDetails) {
        return forkRepository.findById(id)
                .map(fork -> {
                    fork.setName(forkDetails.getName());
                    fork.setDescription(forkDetails.getDescription());
                    fork.setContent(forkDetails.getContent());
                    fork.setMcVersion(forkDetails.getMcVersion());
                    fork.setModloader(forkDetails.getModloader());
                    return ResponseEntity.ok(forkRepository.save(fork));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFork(@PathVariable String id) {
        if (!forkRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        forkRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/download")
    public ResponseEntity<Fork> incrementDownloadCount(@PathVariable String id) {
        return forkRepository.findById(id)
                .map(fork -> {
                    fork.setDownloadCount(fork.getDownloadCount() + 1);
                    return ResponseEntity.ok(forkRepository.save(fork));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
