package com.quantumlauncher.core.model;

import jakarta.persistence.*;

/**
 * Модель настроек приложения
 */
@Entity
@Table(name = "settings")
public class AppSettings {
    
    @Id
    private String id;
    
    private String javaPath;
    
    @Column(name = "auto_detect_java")
    private boolean autoDetectJava;
    
    @Column(name = "default_ram")
    private int defaultRam;
    
    @Column(name = "max_ram")
    private int maxRam;
    
    @Column(name = "jvm_args", columnDefinition = "TEXT")
    private String jvmArgs;
    
    @Column(name = "s3_endpoint")
    private String s3Endpoint;
    
    @Column(name = "s3_bucket")
    private String s3Bucket;
    
    @Column(name = "s3_access_key")
    private String s3AccessKey;
    
    @Column(name = "s3_secret_key")
    private String s3SecretKey;
    
    @Column(name = "cloud_sync_enabled")
    private boolean cloudSyncEnabled;
    
    @Column(name = "show_fps")
    private boolean showFps;
    
    @Column(name = "show_ram")
    private boolean showRam;
    
    private String theme;
    
    private String language;
    
    // Constructors
    public AppSettings() {
        this.id = "default";
        this.autoDetectJava = true;
        this.defaultRam = 2048;
        this.maxRam = 4096;
        this.jvmArgs = "-Xmx2G";
        this.theme = "Dark";
        this.language = "Русский";
        this.showFps = true;
        this.showRam = true;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }
    
    public boolean getAutoDetectJava() { return autoDetectJava; }
    public void setAutoDetectJava(boolean autoDetectJava) { this.autoDetectJava = autoDetectJava; }
    
    public int getDefaultRam() { return defaultRam; }
    public void setDefaultRam(int defaultRam) { this.defaultRam = defaultRam; }
    
    public int getMaxRam() { return maxRam; }
    public void setMaxRam(int maxRam) { this.maxRam = maxRam; }
    
    public String getJvmArgs() { return jvmArgs; }
    public void setJvmArgs(String jvmArgs) { this.jvmArgs = jvmArgs; }
    
    public String getS3Endpoint() { return s3Endpoint; }
    public void setS3Endpoint(String s3Endpoint) { this.s3Endpoint = s3Endpoint; }
    
    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }
    
    public String getS3AccessKey() { return s3AccessKey; }
    public void setS3AccessKey(String s3AccessKey) { this.s3AccessKey = s3AccessKey; }
    
    public String getS3SecretKey() { return s3SecretKey; }
    public void setS3SecretKey(String s3SecretKey) { this.s3SecretKey = s3SecretKey; }
    
    public boolean getCloudSyncEnabled() { return cloudSyncEnabled; }
    public void setCloudSyncEnabled(boolean cloudSyncEnabled) { this.cloudSyncEnabled = cloudSyncEnabled; }
    
    public boolean getShowFps() { return showFps; }
    public void setShowFps(boolean showFps) { this.showFps = showFps; }
    
    public boolean getShowRam() { return showRam; }
    public void setShowRam(boolean showRam) { this.showRam = showRam; }
    
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
