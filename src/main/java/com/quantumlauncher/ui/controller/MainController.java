package com.quantumlauncher.ui.controller;

import com.quantumlauncher.monitoring.service.PerformanceMonitor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main Controller - Главный контроллер приложения
 */
@Component
public class MainController implements javafx.fxml.Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private ApplicationContext springContext;

    @Autowired
    private PerformanceMonitor performanceMonitor;

    // Navigation Menu
    @FXML
    private Button btnInstances;
    @FXML
    private Button btnMods;
    @FXML
    private Button btnShaders;
    @FXML
    private Button btnTexturePacks;
    @FXML
    private Button btnMaps;
    @FXML
    private Button btnForks;
    @FXML
    private Button btnSkins;
    @FXML
    private Button btnAccounts;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnAI;

    // Content Area
    @FXML
    private StackPane contentArea;

    // Profile Section
    @FXML
    private ImageView profileAvatar;
    @FXML
    private Label profileName;
    @FXML
    private Label profileStatus;

    // System Info
    @FXML
    private Label fpsLabel;
    @FXML
    private Label ramLabel;
    @FXML
    private Label cpuLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Инициализация MainController");

        setupNavigation();
        setupProfileSection();
        setupSystemMonitor();

        // По умолчанию показываем Instances
        showInstances();
    }

    private void setupNavigation() {
        btnInstances.setOnAction(e -> showInstances());
        btnMods.setOnAction(e -> showMods());
        btnShaders.setOnAction(e -> showShaders());
        btnTexturePacks.setOnAction(e -> showTexturePacks());
        btnMaps.setOnAction(e -> showMaps());
        btnForks.setOnAction(e -> showForks());
        btnSkins.setOnAction(e -> showSkins());
        btnAccounts.setOnAction(e -> showAccounts());
        btnSettings.setOnAction(e -> showSettings());
        btnAI.setOnAction(e -> showAI());
    }

    private void setupProfileSection() {
        profileName.setText("Игрок");
        profileStatus.setText("Оффлайн");

        try {
            Image defaultAvatar = new Image(
                    getClass().getResourceAsStream("/images/default-avatar.png")
            );
            profileAvatar.setImage(defaultAvatar);
        } catch (Exception e) {
            log.warn("Аватар не найден, используем заглушку");
        }
    }

    private void setupSystemMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PerformanceMonitor.MetricSnapshot metrics
                            = performanceMonitor.getCurrentMetrics();

                    Platform.runLater(() -> {
                        ramLabel.setText("RAM: "
                                + String.format("%.0f%%", metrics.memoryUsagePercent));

                        int currentFps = performanceMonitor.getCurrentFps();
                        fpsLabel.setText("FPS: " + (currentFps > 0 ? currentFps : "--"));

                        cpuLabel.setText("CPU: "
                                + String.format("%.1f", metrics.systemLoad));
                    });

                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    log.error("Ошибка мониторинга", e);
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    // Navigation Methods
    @FXML
    public void showInstances() {
        log.info("Переход: Instances");
        loadFXML("/fxml/instances.fxml");
    }

    @FXML
    public void showMods() {
        log.info("Переход: Mods");
        loadFXML("/fxml/mods.fxml");
    }

    @FXML
    public void showShaders() {
        log.info("Переход: Shaders");
        loadFXML("/fxml/shaders.fxml");
    }

    @FXML
    public void showTexturePacks() {
        log.info("Переход: TexturePacks");
        loadFXML("/fxml/texturepacks.fxml");
    }

    @FXML
    public void showMaps() {
        log.info("Переход: Maps");
        loadFXML("/fxml/maps.fxml");
    }

    @FXML
    public void showForks() {
        log.info("Переход: Forks");
        loadFXML("/fxml/forks.fxml");
    }

    @FXML
    public void showSkins() {
        log.info("Переход: Skins");
        loadFXML("/fxml/skins.fxml");
    }

    @FXML
    public void showAccounts() {
        log.info("Переход: Accounts");
        loadFXML("/fxml/accounts.fxml");
    }

    @FXML
    public void showSettings() {
        log.info("Переход: Settings");
        loadFXML("/fxml/settings.fxml");
    }

    @FXML
    public void showAI() {
        log.info("Переход: AI Assistant");
        loadFXML("/fxml/ai.fxml");
    }

    /**
     * Загрузка FXML в contentArea с использованием Spring
     */
    private void loadFXML(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            // Использование Spring ControllerFactory
            if (springContext != null) {
                loader.setControllerFactory(springContext::getBean);
            }

            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            log.info("Загружен FXML: {} с контроллером: {}",
                    fxmlPath, loader.getController().getClass().getSimpleName());

        } catch (Exception e) {
            log.error("Ошибка загрузки FXML: {}", fxmlPath, e);
            showPlaceholder("Ошибка загрузки: " + e.getMessage());
        }
    }

    /**
     * Показать заглушку
     */
    private void showPlaceholder(String message) {
        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));

        Label title = new Label("🚧 " + message);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #888;");

        container.getChildren().add(title);
        contentArea.getChildren().add(container);
    }
}
