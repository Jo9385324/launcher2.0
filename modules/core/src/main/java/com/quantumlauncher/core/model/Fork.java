package com.quantumlauncher.core.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Модель кастомной сборки (Fork)
 */
@Entity
@Table(name = "forks")
public class Fork {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "author_id")
    private String authorId;
    
    @Column(name = "author_name")
    private String authorName;
    
    private String version;
    
    @Column(columnDefinition = "TEXT")
    private String content; // JSON с списком модов
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "updated_at")
    private Long updatedAt;
    
    private String description;
    
    private String image;
    
    @Column(name = "download_count")
    private int downloadCount;
    
    private boolean official;
    
    private String mcVersion;
    
    private String modloader;
    
    // Constructors
    public Fork() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now().getEpochSecond();
    }
    
    public Fork(String name, String authorName, String mcVersion, String modloader) {
        this();
        this.name = name;
        this.authorName = authorName;
        this.mcVersion = mcVersion;
        this.modloader = modloader;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
    
    public boolean isOfficial() { return official; }
    public void setOfficial(boolean official) { this.official = official; }
    
    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }
    
    public String getModloader() { return modloader; }
    public void setModloader(String modloader) { this.modloader = modloader; }
}
