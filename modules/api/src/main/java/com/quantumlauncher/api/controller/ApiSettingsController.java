package com.quantumlauncher.api.controller;

import com.quantumlauncher.core.model.AppSettings;
import com.quantumlauncher.core.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API контроллер для управления настройками
 */
@RestController
@RequestMapping("/api/settings")
public class ApiSettingsController {
    
    @Autowired
    private SettingsRepository settingsRepository;
    
    @GetMapping
    public ResponseEntity<List<AppSettings>> getAllSettings() {
        return ResponseEntity.ok(settingsRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AppSettings> getSetting(@PathVariable String id) {
        return settingsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<AppSettings> createOrUpdateSetting(@RequestBody AppSettings setting) {
        AppSettings saved = settingsRepository.save(setting);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AppSettings> updateSetting(
            @PathVariable String id,
            @RequestBody AppSettings settingDetails) {
        
        return settingsRepository.findById(id)
                .map(setting -> {
                    setting.setJavaPath(settingDetails.getJavaPath());
                    setting.setAutoDetectJava(settingDetails.getAutoDetectJava());
                    setting.setDefaultRam(settingDetails.getDefaultRam());
                    setting.setMaxRam(settingDetails.getMaxRam());
                    setting.setJvmArgs(settingDetails.getJvmArgs());
                    setting.setS3Endpoint(settingDetails.getS3Endpoint());
                    setting.setS3Bucket(settingDetails.getS3Bucket());
                    setting.setS3AccessKey(settingDetails.getS3AccessKey());
                    setting.setS3SecretKey(settingDetails.getS3SecretKey());
                    setting.setCloudSyncEnabled(settingDetails.getCloudSyncEnabled());
                    setting.setShowFps(settingDetails.getShowFps());
                    setting.setShowRam(settingDetails.getShowRam());
                    setting.setTheme(settingDetails.getTheme());
                    setting.setLanguage(settingDetails.getLanguage());
                    return ResponseEntity.ok(settingsRepository.save(setting));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSetting(@PathVariable String id) {
        if (!settingsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        settingsRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
