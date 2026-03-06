package com.quantumlauncher.core.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Модель карты
 */
@Entity
@Table(name = "maps")
public class Map {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String version;
    
    private String source;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "download_url")
    private String downloadUrl;
    
    @Column(name = "instance_id")
    private String instanceId;
    
    private String category;
    
    private String description;
    
    private String author;
    
    @Column(name = "file_hash")
    private String fileHash;
    
    private Boolean enabled = true;
    
    @Column(name = "map_type")
    private String mapType; // ADVENTURE, SURVIVAL, CREATIVE, PVP, MINIGAME
    
    @Column(name = "game_version")
    private String gameVersion;
    
    @Column(name = "player_count")
    private int playerCount; // рекомендуемое количество игроков
    
    // Constructors
    public Map() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Map(String name, String version, String source) {
        this();
        this.name = name;
        this.version = version;
        this.source = source;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    
    public boolean isEnabled() { return enabled != null ? enabled : false; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getMapType() { return mapType; }
    public void setMapType(String mapType) { this.mapType = mapType; }
    
    public String getGameVersion() { return gameVersion; }
    public void setGameVersion(String gameVersion) { this.gameVersion = gameVersion; }
    
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
}
