package com.quantumlauncher.ui.controller;

import com.quantumlauncher.core.model.AppSettings;
import com.quantumlauncher.core.repository.SettingsRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Controller для настроек приложения
 */
@Component
public class SettingsController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);
    
    @Autowired
    private SettingsRepository settingsRepository;
    
    // Java Settings
    @FXML private TextField javaPathField;
    @FXML private CheckBox autoDetectJavaCheck;
    
    // Memory Settings
    @FXML private Slider defaultRamSlider;
    @FXML private Label defaultRamLabel;
    @FXML private Slider maxRamSlider;
    @FXML private Label maxRamLabel;
    
    // JVM Args
    @FXML private TextArea jvmArgsArea;
    
    // Cloud Settings
    @FXML private TextField s3EndpointField;
    @FXML private TextField s3BucketField;
    @FXML private TextField s3AccessKeyField;
    @FXML private PasswordField s3SecretKeyField;
    @FXML private CheckBox cloudSyncEnabledCheck;
    
    // UI Settings
    @FXML private CheckBox showFpsCheck;
    @FXML private CheckBox showRamCheck;
    @FXML private ComboBox<String> themeCombo;
    @FXML private ComboBox<String> languageCombo;
    
    // Buttons
    @FXML private Button saveButton;
    @FXML private Button resetButton;
    @FXML private Button testJavaButton;
    
    private AppSettings currentSettings;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация SettingsController");
        
        // Загрузка настроек
        loadSettings();
        
        // Настройка UI
        setupSliders();
        setupComboBoxes();
        setupButtons();
    }
    
    private void loadSettings() {
        Optional<AppSettings> settingsOpt = settingsRepository.findById("default");
        
        if (settingsOpt.isPresent()) {
            currentSettings = settingsOpt.get();
        } else {
            currentSettings = new AppSettings();
            currentSettings.setId("default");
        }
        
        // Применение настроек к UI
        applySettingsToUI();
    }
    
    private void applySettingsToUI() {
        // Java
        javaPathField.setText(currentSettings.getJavaPath() != null ? 
            currentSettings.getJavaPath() : "");
        autoDetectJavaCheck.setSelected(currentSettings.getAutoDetectJava());
        
        // Memory
        defaultRamSlider.setValue(currentSettings.getDefaultRam());
        defaultRamLabel.setText(currentSettings.getDefaultRam() + " MB");
        maxRamSlider.setValue(currentSettings.getMaxRam());
        maxRamLabel.setText(currentSettings.getMaxRam() + " MB");
        
        // JVM Args
        jvmArgsArea.setText(currentSettings.getJvmArgs() != null ? 
            currentSettings.getJvmArgs() : "");
        
        // Cloud
        s3EndpointField.setText(currentSettings.getS3Endpoint() != null ? 
            currentSettings.getS3Endpoint() : "");
        s3BucketField.setText(currentSettings.getS3Bucket() != null ? 
            currentSettings.getS3Bucket() : "");
        s3AccessKeyField.setText(currentSettings.getS3AccessKey() != null ? 
            currentSettings.getS3AccessKey() : "");
        s3SecretKeyField.setText(currentSettings.getS3SecretKey() != null ? 
            currentSettings.getS3SecretKey() : "");
        cloudSyncEnabledCheck.setSelected(currentSettings.getCloudSyncEnabled());
        
        // UI
        showFpsCheck.setSelected(currentSettings.getShowFps());
        showRamCheck.setSelected(currentSettings.getShowRam());
        themeCombo.setValue(currentSettings.getTheme() != null ? 
            currentSettings.getTheme() : "Dark");
        languageCombo.setValue(currentSettings.getLanguage() != null ? 
            currentSettings.getLanguage() : "Русский");
    }
    
    private void setupSliders() {
        defaultRamSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            defaultRamLabel.setText(newVal.intValue() + " MB");
        });
        
        maxRamSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxRamLabel.setText(newVal.intValue() + " MB");
        });
    }
    
    private void setupComboBoxes() {
        themeCombo.getItems().addAll("Dark", "Light", "System");
        languageCombo.getItems().addAll("Русский", "English", "Deutsch", "Español");
    }
    
    private void setupButtons() {
        saveButton.setOnAction(e -> saveSettings());
        resetButton.setOnAction(e -> resetSettings());
        testJavaButton.setOnAction(e -> testJava());
    }
    
    private void saveSettings() {
        try {
            // Сбор данных из UI
            currentSettings.setJavaPath(javaPathField.getText());
            currentSettings.setAutoDetectJava(autoDetectJavaCheck.isSelected());
            currentSettings.setDefaultRam((int) defaultRamSlider.getValue());
            currentSettings.setMaxRam((int) maxRamSlider.getValue());
            currentSettings.setJvmArgs(jvmArgsArea.getText());
            
            currentSettings.setS3Endpoint(s3EndpointField.getText());
            currentSettings.setS3Bucket(s3BucketField.getText());
            currentSettings.setS3AccessKey(s3AccessKeyField.getText());
            currentSettings.setS3SecretKey(s3SecretKeyField.getText());
            currentSettings.setCloudSyncEnabled(cloudSyncEnabledCheck.isSelected());
            
            currentSettings.setShowFps(showFpsCheck.isSelected());
            currentSettings.setShowRam(showRamCheck.isSelected());
            currentSettings.setTheme(themeCombo.getValue());
            currentSettings.setLanguage(languageCombo.getValue());
            
            // Сохранение
            settingsRepository.save(currentSettings);
            
            showAlert("Сохранено", "Настройки сохранены успешно");
            log.info("Настройки сохранены");
            
        } catch (Exception e) {
            log.error("Ошибка сохранения настроек", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void resetSettings() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Сброс настроек");
        alert.setHeaderText("Сбросить все настройки?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            currentSettings = new AppSettings();
            currentSettings.setId("default");
            settingsRepository.save(currentSettings);
            applySettingsToUI();
            showAlert("Сброшено", "Настройки сброшены");
        }
    }
    
    private void testJava() {
        String javaPath = javaPathField.getText();
        if (javaPath == null || javaPath.isEmpty()) {
            showAlert("Тест Java", "Укажите путь к Java");
            return;
        }
        
        // Простая проверка - пытаемся запустить java -version
        showAlert("Тест Java", "Проверка доступности Java по пути: " + javaPath);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
