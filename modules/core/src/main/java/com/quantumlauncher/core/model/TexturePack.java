package com.quantumlauncher.core.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Модель текстур-пака
 */
@Entity
@Table(name = "texture_packs")
public class TexturePack {
    
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
    
    @Column(name = "resolution")
    private String resolution; // 16x, 32x, 64x, 128x, 256x, 512x
    
    @Column(name = "pack_format")
    private String packFormat; // 1-15 для разных версий
    
    // Constructors
    public TexturePack() {
        this.id = UUID.randomUUID().toString();
    }
    
    public TexturePack(String name, String version, String source) {
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
    
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    
    public String getPackFormat() { return packFormat; }
    public void setPackFormat(String packFormat) { this.packFormat = packFormat; }
}
