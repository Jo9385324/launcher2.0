package com.quantumlauncher.core.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Модель инстанса Minecraft
 */
@Entity
@Table(name = "instances")
public class Instance {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String version;
    
    private String modloader;
    
    @Column(nullable = false)
    private String path;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "last_played")
    private Long lastPlayed;
    
    private String icon;
    
    private String description;
    
    @Column(name = "java_path")
    private String javaPath;
    
    @Column(name = "jvm_args")
    private String jvmArgs;
    
    @Column(name = "min_ram")
    private Integer minRam;
    
    @Column(name = "max_ram")
    private Integer maxRam;
    
    @Column(name = "minecraft_account_id")
    private String minecraftAccountId;
    
    @Column(name = "modloader_version")
    private String modloaderVersion;
    
    // Constructors
    public Instance() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now().getEpochSecond();
    }
    
    public Instance(String name, String version, String modloader, String path) {
        this();
        this.name = name;
        this.version = version;
        this.modloader = modloader;
        this.path = path;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getModloader() { return modloader; }
    public void setModloader(String modloader) { this.modloader = modloader; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(Long lastPlayed) { this.lastPlayed = lastPlayed; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }
    
    public String getJvmArgs() { return jvmArgs; }
    public void setJvmArgs(String jvmArgs) { this.jvmArgs = jvmArgs; }
    
    public Integer getMinRam() { return minRam; }
    public void setMinRam(Integer minRam) { this.minRam = minRam; }
    
    public Integer getMaxRam() { return maxRam; }
    public void setMaxRam(Integer maxRam) { this.maxRam = maxRam; }
    
    public String getMinecraftAccountId() { return minecraftAccountId; }
    public void setMinecraftAccountId(String minecraftAccountId) { this.minecraftAccountId = minecraftAccountId; }
    
    public String getModloaderVersion() { return modloaderVersion; }
    public void setModloaderVersion(String modloaderVersion) { this.modloaderVersion = modloaderVersion; }
}
