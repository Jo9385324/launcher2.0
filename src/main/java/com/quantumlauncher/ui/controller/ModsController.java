package com.quantumlauncher.ui.controller;

import com.quantumlauncher.content.service.ModManagementService;
import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.instances.service.InstanceManager;
import com.quantumlauncher.security.scanner.SecurityScanner;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Controller для управления модами
 */
@Component
public class ModsController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(ModsController.class);
    
    @Autowired
    private ModManagementService modService;
    
    @Autowired
    private InstanceManager instanceManager;
    
    @Autowired
    private SecurityScanner securityScanner;
    
    // UI Elements
    @FXML private ComboBox<Instance> instanceSelector;
    @FXML private ListView<Mod> modsListView;
    @FXML private VBox detailsPanel;
    @FXML private Label modNameLabel;
    @FXML private Label modVersionLabel;
    @FXML private Label modAuthorLabel;
    @FXML private Label modCategoryLabel;
    @FXML private Label modStatusLabel;
    @FXML private TextArea modDescriptionArea;
    @FXML private Button enableButton;
    @FXML private Button disableButton;
    @FXML private Button deleteButton;
    @FXML private Button addModButton;
    @FXML private Button scanButton;
    @FXML private ProgressIndicator scanProgress;
    @FXML private Label scanResultLabel;
    
    private ObservableList<Mod> mods = FXCollections.observableArrayList();
    private ObservableList<Instance> instances = FXCollections.observableArrayList();
    private Mod selectedMod;
    private Instance selectedInstance;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация ModsController");
        
        // Загрузка инстансов
        loadInstances();
        
        // Настройка ListView
        modsListView.setItems(mods);
        modsListView.setCellFactory(listView -> new ModCell());
        
        // Обработчик выбора инстанса
        instanceSelector.setOnAction(e -> loadModsForInstance());
        
        // Обработчик выбора мода
        modsListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showModDetails(newVal);
                }
            });
        
        // Кнопки
        enableButton.setOnAction(e -> toggleMod(true));
        disableButton.setOnAction(e -> toggleMod(false));
        deleteButton.setOnAction(e -> deleteMod());
        addModButton.setOnAction(e -> addMod());
        scanButton.setOnAction(e -> scanMod());
        
        detailsPanel.setVisible(false);
    }
    
    private void loadInstances() {
        List<Instance> allInstances = instanceManager.getAllInstances();
        instances.setAll(allInstances);
        instanceSelector.setItems(instances);
        if (!allInstances.isEmpty()) {
            instanceSelector.setValue(allInstances.get(0));
        }
    }
    
    private void loadModsForInstance() {
        selectedInstance = instanceSelector.getValue();
        if (selectedInstance == null) {
            mods.clear();
            return;
        }
        
        // Загрузка модов из БД
        // В реальной реализации - из ModRepository
        // Для демонстрации - пустой список
        mods.clear();
        
        log.info("Загружено модов для инстанса {}: {}", selectedInstance.getName(), mods.size());
    }
    
    private void showModDetails(Mod mod) {
        selectedMod = mod;
        detailsPanel.setVisible(true);
        
        modNameLabel.setText(mod.getName());
        modVersionLabel.setText("Версия: " + (mod.getVersion() != null ? mod.getVersion() : "N/A"));
        modAuthorLabel.setText("Автор: " + (mod.getAuthor() != null ? mod.getAuthor() : "Unknown"));
        modCategoryLabel.setText("Категория: " + (mod.getCategory() != null ? mod.getCategory() : "OTHER"));
        modDescriptionArea.setText(mod.getDescription() != null ? mod.getDescription() : "Описание недоступно");
        
        // Статус
        boolean enabled = mod.getEnabled() != null && mod.getEnabled();
        modStatusLabel.setText(enabled ? "Включён" : "Отключён");
        modStatusLabel.setTextFill(enabled ? Color.GREEN : Color.RED);
        
        // Состояние кнопок
        enableButton.setDisable(enabled);
        disableButton.setDisable(!enabled);
    }
    
    private void toggleMod(boolean enable) {
        if (selectedMod == null) return;
        
        try {
            modService.toggleMod(selectedMod.getId(), enable);
            selectedMod.setEnabled(enable);
            showModDetails(selectedMod);
            loadModsForInstance();
            
            log.info("Мод {} {}", selectedMod.getName(), enable ? "включён" : "отключён");
        } catch (Exception e) {
            log.error("Ошибка переключения мода", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void deleteMod() {
        if (selectedMod == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление мода");
        alert.setHeaderText("Удалить мод '" + selectedMod.getName() + "'?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                modService.uninstallMod(selectedMod.getId());
                loadModsForInstance();
                detailsPanel.setVisible(false);
                selectedMod = null;
            } catch (Exception e) {
                log.error("Ошибка удаления мода", e);
                showAlert("Ошибка", e.getMessage());
            }
        }
    }
    
    private void addMod() {
        // В реальной реализации - открытие диалога выбора файла
        showAlert("Добавление мода", "Выберите JAR файл мода для установки");
    }
    
    private void scanMod() {
        if (selectedMod == null) {
            showAlert("Сканирование", "Выберите мод для сканирования");
            return;
        }
        
        scanProgress.setVisible(true);
        scanResultLabel.setText("Сканирование...");
        
        new Thread(() -> {
            try {
                Path modPath = Paths.get(selectedMod.getFilePath());
                SecurityScanner.ScanResult result = securityScanner.scanModFile(modPath);
                
                Platform.runLater(() -> {
                    scanProgress.setVisible(false);
                    if (result.isSafe) {
                        scanResultLabel.setText("Безопасно");
                        scanResultLabel.setTextFill(Color.GREEN);
                    } else {
                        scanResultLabel.setText("Угрозы: " + result.threats.size());
                        scanResultLabel.setTextFill(Color.ORANGE);
                        
                        StringBuilder sb = new StringBuilder("Обнаруженные угрозы:\n");
                        for (var threat : result.threats) {
                            sb.append("- ").append(threat.description).append("\n");
                        }
                        showAlert("Результат сканирования", sb.toString());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    scanProgress.setVisible(false);
                    scanResultLabel.setText("Ошибка сканирования");
                    scanResultLabel.setTextFill(Color.RED);
                });
                log.error("Ошибка сканирования", e);
            }
        }).start();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Кастомная ячейка для ListView
     */
    private static class ModCell extends ListCell<Mod> {
        @Override
        protected void updateItem(Mod item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                
                // Иконка статуса
                Label statusIcon = new Label(item.getEnabled() ? "Вкл" : "Выкл");
                statusIcon.setStyle("-fx-font-size:12; -fx-background-color: " + 
                    (item.getEnabled() ? "#4CAF50" : "#F44336") + "; -fx-padding: 2 6;");
                
                // Информация
                VBox info = new VBox(2);
                Label name = new Label(item.getName());
                name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
                
                String version = item.getVersion() != null ? item.getVersion() : "";
                String category = item.getCategory() != null ? item.getCategory() : "OTHER";
                Label desc = new Label(version + " - " + category);
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(statusIcon, info);
                
                setGraphic(hbox);
            }
        }
    }
}
