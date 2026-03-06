package com.quantumlauncher.skins.service;

import com.quantumlauncher.core.model.Skin;
import com.quantumlauncher.core.repository.SkinRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Сервис управления скинами и плащами
 */
@Service
public class SkinManager {
    
    private static final Logger log = LoggerFactory.getLogger(SkinManager.class);
    private static final MediaType PNG = MediaType.parse("image/png");
    
    @Autowired
    private SkinRepository skinRepository;
    
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build();
    
    /**
     * Загрузка скина по никнейму игрока
     */
    public Skin loadSkinByUsername(String username) throws IOException {
        log.info("Загрузка скина для игрока: {}", username);
        
        // Использование Mojang API для получения UUID
        String uuid = getPlayerUUID(username);
        if (uuid == null) {
            throw new IOException("Игрок не найден: " + username);
        }
        
        // Получение скина
        String skinUrl = getPlayerSkin(uuid);
        
        if (skinUrl == null) {
            throw new IOException("Скин не найден для игрока: " + username);
        }
        
        // Скачивание скина
        Path skinPath = downloadSkin(skinUrl, username);
        
        // Сохранение в БД
        Skin skin = new Skin();
        skin.setName(username);
        skin.setType("SKIN");
        skin.setPath(skinPath.toString());
        skin.setUrl(skinUrl);
        skin.setPlayerUuid(uuid);
        skin.setPremium(true);
        skin.setActive(true);
        
        return skinRepository.save(skin);
    }
    
    /**
     * Загрузка кастомного скина из файла
     */
    public Skin uploadCustomSkin(Path skinFile, String name) throws IOException {
        log.info("Загрузка кастомного скина: {}", name);
        
        // Проверка формата
        String fileName = skinFile.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".png")) {
            throw new IllegalArgumentException("Скин должен быть в формате PNG");
        }
        
        // Проверка размера изображения (должен быть 64x32 или 128x64)
        // В реальной реализации - проверка через BufferedImage
        
        // Копирование в папку скинов
        Path skinsDir = Paths.get("data", "skins");
        Files.createDirectories(skinsDir);
        
        Path targetPath = skinsDir.resolve(name + ".png");
        Files.copy(skinFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Сохранение в БД
        Skin skin = new Skin();
        skin.setName(name);
        skin.setType("SKIN");
        skin.setPath(targetPath.toString());
        skin.setPremium(false);
        skin.setActive(true);
        
        return skinRepository.save(skin);
    }
    
    /**
     * Загрузка плаща
     */
    public Skin uploadCape(Path capeFile, String name) throws IOException {
        log.info("Загрузка плаща: {}", name);
        
        Path capesDir = Paths.get("data", "capes");
        Files.createDirectories(capesDir);
        
        Path targetPath = capesDir.resolve(name + ".png");
        Files.copy(capeFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        Skin cape = new Skin();
        cape.setName(name);
        cape.setType("CAPE");
        cape.setPath(targetPath.toString());
        cape.setActive(true);
        
        return skinRepository.save(cape);
    }
    
    /**
     * Активация скина
     */
    public Skin activateSkin(String skinId) {
        Optional<Skin> skinOpt = skinRepository.findById(skinId);
        if (skinOpt.isEmpty()) {
            throw new IllegalArgumentException("Скин не найден");
        }
        
        Skin skin = skinOpt.get();
        
        // Деактивация других скинов того же типа
        List<Skin> playerSkins = skinRepository.findByPlayerUuid(skin.getPlayerUuid());
        for (Skin s : playerSkins) {
            if (s.getType().equals(skin.getType())) {
                s.setActive(false);
                skinRepository.save(s);
            }
        }
        
        skin.setActive(true);
        return skinRepository.save(skin);
    }
    
    /**
     * Получение UUID игрока по никнейму
     */
    private String getPlayerUUID(String username) {
        try {
            Request request = new Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/" + username)
                .get()
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                // Простой парсинг JSON
                int idStart = json.indexOf("\"id\":\"") + 5;
                int idEnd = json.indexOf("\"", idStart);
                if (idStart > 4 && idEnd > idStart) {
                    return json.substring(idStart, idEnd);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка получения UUID", e);
        }
        return null;
    }
    
    /**
     * Получение URL скина игрока
     */
    private String getPlayerSkin(String uuid) {
        try {
            Request request = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)
                .get()
                .build();
            
            Response response = httpClient.newCall(request).execute();
            
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                
                // Поиск URL скина в JSON
                int texturesStart = json.indexOf("\"textures\":{");
                if (texturesStart >= 0) {
                    int skinStart = json.indexOf("\"SKIN\":{", texturesStart);
                    if (skinStart >= 0) {
                        int urlStart = json.indexOf("\"url\":\"", skinStart) + 7;
                        int urlEnd = json.indexOf("\"", urlStart);
                        if (urlStart > 6 && urlEnd > urlStart) {
                            return json.substring(urlStart, urlEnd);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка получения скина", e);
        }
        return null;
    }
    
    /**
     * Скачивание скина
     */
    private Path downloadSkin(String url, String username) throws IOException {
        Path skinsDir = Paths.get("data", "skins");
        Files.createDirectories(skinsDir);
        
        Path skinPath = skinsDir.resolve(username + ".png");
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        Response response = httpClient.newCall(request).execute();
        
        if (response.isSuccessful() && response.body() != null) {
            try (InputStream is = response.body().byteStream()) {
                Files.copy(is, skinPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            throw new IOException("Не удалось скачать скин");
        }
        
        return skinPath;
    }
    
    /**
     * Получение всех скинов игрока
     */
    public List<Skin> getPlayerSkins(String playerUuid) {
        return skinRepository.findByPlayerUuid(playerUuid);
    }
    
    /**
     * Получение активного скина
     */
    public Optional<Skin> getActiveSkin(String playerUuid) {
        return skinRepository.findByPlayerUuidAndActive(playerUuid, true);
    }
    
    /**
     * Удаление скина
     */
    public void deleteSkin(String skinId) {
        Optional<Skin> skinOpt = skinRepository.findById(skinId);
        if (skinOpt.isPresent()) {
            Skin skin = skinOpt.get();
            
            // Удаление файла
            if (skin.getPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(skin.getPath()));
                } catch (IOException e) {
                    log.warn("Не удалось удалить файл скина", e);
                }
            }
            
            skinRepository.delete(skin);
        }
    }
}
