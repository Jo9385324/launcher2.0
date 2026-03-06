package com.quantumlauncher.core.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Модель скина
 */
@Entity
@Table(name = "skins")
public class Skin {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    private String type; // SKIN, CAPE
    
    private String path;
    
    private String url;
    
    @Column(name = "player_uuid")
    private String playerUuid;
    
    private boolean active;
    
    @Column(name = "is_premium")
    private boolean isPremium;
    
    private String author;
    
    // Constructors
    public Skin() {
        this.id = UUID.randomUUID().toString();
    }
    
    public Skin(String name, String type, String path) {
        this();
        this.name = name;
        this.type = type;
        this.path = path;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
    
    public boolean isActive() { return active; }
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public boolean isPremium() { return isPremium; }
    public boolean getPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}
