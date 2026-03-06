package com.quantumlauncher.auth.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Модель аккаунта Minecraft
 */
@Entity
@Table(name = "minecraft_accounts")
public class MinecraftAccount {
    
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String uuid;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(name = "access_token", length = 2048)
    private String accessToken;
    
    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;
    
    @Column(name = "refresh_token_expiry")
    private Long refreshTokenExpiry;
    
    @Column(name = "xbl_token", length = 2048)
    private String xblToken;
    
    @Column(name = "xsts_token", length = 2048)
    private String xstsToken;
    
    @Column(name = "skin_url")
    private String skinUrl;
    
    @Column(name = "cape_url")
    private String capeUrl;
    
    @Column(name = "created_at")
    private Long createdAt;
    
    @Column(name = "last_used")
    private Long lastUsed;
    
    @Column(nullable = false)
    private boolean active = true;
    
    // Constructors
    public MinecraftAccount() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = java.time.Instant.now().getEpochSecond();
    }
    
    public MinecraftAccount(String uuid, String username) {
        this();
        this.uuid = uuid;
        this.username = username;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public Long getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public void setRefreshTokenExpiry(Long refreshTokenExpiry) { this.refreshTokenExpiry = refreshTokenExpiry; }
    
    public String getXblToken() { return xblToken; }
    public void setXblToken(String xblToken) { this.xblToken = xblToken; }
    
    public String getXstsToken() { return xstsToken; }
    public void setXstsToken(String xstsToken) { this.xstsToken = xstsToken; }
    
    public String getSkinUrl() { return skinUrl; }
    public void setSkinUrl(String skinUrl) { this.skinUrl = skinUrl; }
    
    public String getCapeUrl() { return capeUrl; }
    public void setCapeUrl(String capeUrl) { this.capeUrl = capeUrl; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getLastUsed() { return lastUsed; }
    public void setLastUsed(Long lastUsed) { this.lastUsed = lastUsed; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public boolean isRefreshTokenExpired() {
        if (refreshTokenExpiry == null) return true;
        return System.currentTimeMillis() >= refreshTokenExpiry;
    }
}
