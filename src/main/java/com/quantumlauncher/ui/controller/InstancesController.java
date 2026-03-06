package com.quantumlauncher.ui.controller;

import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.instances.service.InstanceManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Controller для управления инстансами
 */
@Component
public class InstancesController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(InstancesController.class);
    
    @Autowired
    private InstanceManager instanceManager;
    
    // UI Elements
    @FXML private ListView<Instance> instancesListView;
    @FXML private VBox detailsPanel;
    @FXML private Label instanceNameLabel;
    @FXML private Label instanceVersionLabel;
    @FXML private Label instanceModloaderLabel;
    @FXML private Label instanceStatusLabel;
    @FXML private Label lastPlayedLabel;
    @FXML private Slider ramSlider;
    @FXML private Label ramLabel;
    @FXML private Button launchButton;
    @FXML private Button stopButton;
    @FXML private Button deleteButton;
    @FXML private Button createButton;
    
    private ObservableList<Instance> instances = FXCollections.observableArrayList();
    private Instance selectedInstance;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация InstancesController");
        
        instancesListView.setItems(instances);
        instancesListView.setCellFactory(listView -> new InstanceCell());
        
        // Загрузка инстансов
        loadInstances();
        
        // Обработчик выбора
        instancesListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showInstanceDetails(newVal);
                }
            });
        
        // Slider для RAM
        ramSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int ram = newVal.intValue();
            ramLabel.setText(ram + " MB");
            if (selectedInstance != null) {
                selectedInstance.setMaxRam(ram);
            }
        });
        
        // Кнопки
        launchButton.setOnAction(e -> launchInstance());
        stopButton.setOnAction(e -> stopInstance());
        deleteButton.setOnAction(e -> deleteInstance());
        createButton.setOnAction(e -> showCreateDialog());
        
        // Начальное состояние
        detailsPanel.setVisible(false);
    }
    
    private void loadInstances() {
        List<Instance> allInstances = instanceManager.getAllInstances();
        instances.setAll(allInstances);
        log.info("Загружено инстансов: {}", allInstances.size());
    }
    
    private void showInstanceDetails(Instance instance) {
        selectedInstance = instance;
        detailsPanel.setVisible(true);
        
        instanceNameLabel.setText(instance.getName());
        instanceVersionLabel.setText("Minecraft " + instance.getVersion());
        instanceModloaderLabel.setText(instance.getModloader() != null ? 
            instance.getModloader() : "Vanilla");
        
        // Статус
        boolean isRunning = instanceManager.isRunning(instance.getId());
        instanceStatusLabel.setText(isRunning ? "🟢 Запущен" : "🔴 Остановлен");
        instanceStatusLabel.setTextFill(isRunning ? Color.GREEN : Color.RED);
        
        // RAM
        if (instance.getMaxRam() != null) {
            ramSlider.setValue(instance.getMaxRam());
            ramLabel.setText(instance.getMaxRam() + " MB");
        }
        
        // Последняя игра
        if (instance.getLastPlayed() != null) {
            lastPlayedLabel.setText("Последняя игра: " + 
                java.time.Instant.ofEpochSecond(instance.getLastPlayed())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime().toString());
        } else {
            lastPlayedLabel.setText("Ещё не играли");
        }
        
        // Состояние кнопок
        launchButton.setDisable(isRunning);
        stopButton.setDisable(!isRunning);
    }
    
    private void launchInstance() {
        if (selectedInstance == null) return;
        
        log.info("Запуск инстанса: {}", selectedInstance.getName());
        
        instanceManager.launchInstance(selectedInstance.getId())
            .thenAccept(process -> {
                Platform.runLater(() -> {
                    loadInstances();
                    instanceStatusLabel.setText("🟢 Запущен");
                    instanceStatusLabel.setTextFill(Color.GREEN);
                    launchButton.setDisable(true);
                    stopButton.setDisable(false);
                });
            })
            .exceptionally(ex -> {
                log.error("Ошибка запуска", ex);
                Platform.runLater(() -> {
                    showAlert("Ошибка запуска", ex.getMessage());
                });
                return null;
            });
    }
    
    private void stopInstance() {
        if (selectedInstance == null) return;
        
        log.info("Остановка инстанса: {}", selectedInstance.getName());
        instanceManager.stopInstance(selectedInstance.getId());
        
        instanceStatusLabel.setText("🔴 Остановлен");
        instanceStatusLabel.setTextFill(Color.RED);
        launchButton.setDisable(false);
        stopButton.setDisable(true);
    }
    
    private void deleteInstance() {
        if (selectedInstance == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление инстанса");
        alert.setHeaderText("Удалить инстанс '" + selectedInstance.getName() + "'?");
        alert.setContentText("Все файлы будут удалены безвозвратно.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            instanceManager.deleteInstance(selectedInstance.getId());
            loadInstances();
            detailsPanel.setVisible(false);
            selectedInstance = null;
        }
    }
    
    private void showCreateDialog() {
        Dialog<Instance> dialog = new Dialog<>();
        dialog.setTitle("Создание инстанса");
        dialog.setHeaderText("Создать новый инстанс Minecraft");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Название инстанса");
        
        ComboBox<String> versionCombo = new ComboBox<>();
        versionCombo.getItems().addAll("1.20.4", "1.20.3", "1.20.2", "1.19.4", "1.19.3", "1.18.2");
        versionCombo.setValue("1.20.4");
        
        ComboBox<String> modloaderCombo = new ComboBox<>();
        modloaderCombo.getItems().addAll("Vanilla", "Fabric", "Forge", "Quilt");
        modloaderCombo.setValue("Vanilla");
        
        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Версия:"), 0, 1);
        grid.add(versionCombo, 1, 1);
        grid.add(new Label("ModLoader:"), 0, 2);
        grid.add(modloaderCombo, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return instanceManager.createInstance(
                        nameField.getText(),
                        versionCombo.getValue(),
                        modloaderCombo.getValue(),
                        null
                    );
                } catch (Exception e) {
                    showAlert("Ошибка", e.getMessage());
                }
            }
            return null;
        });
        
        Optional<Instance> result = dialog.showAndWait();
        result.ifPresent(instance -> {
            loadInstances();
            instancesListView.getSelectionModel().select(instance);
        });
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Кастомная ячейка для ListView
     */
    private static class InstanceCell extends ListCell<Instance> {
        @Override
        protected void updateItem(Instance item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                
                // Иконка версии
                Label icon = new Label(getVersionIcon(item.getVersion()));
                icon.setStyle("-fx-font-size: 24;");
                
                // Информация
                VBox info = new VBox(2);
                Label name = new Label(item.getName());
                name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
                
                String version = item.getVersion();
                String modloader = item.getModloader() != null ? item.getModloader() : "Vanilla";
                Label desc = new Label("Minecraft " + version + " • " + modloader);
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(icon, info);
                
                setGraphic(hbox);
            }
        }
        
        private String getVersionIcon(String version) {
            if (version == null) return "🎮";
            if (version.startsWith("1.20")) return "🆕";
            if (version.startsWith("1.19")) return "🟢";
            if (version.startsWith("1.18")) return "🟡";
            return "🎮";
        }
    }
}
