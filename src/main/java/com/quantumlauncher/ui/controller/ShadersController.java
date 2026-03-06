package com.quantumlauncher.ui.controller;

import com.quantumlauncher.content.service.ShaderService;
import com.quantumlauncher.core.model.Shader;
import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.instances.service.InstanceManager;
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

import java.util.List;
import java.util.Optional;

/**
 * Controller для управления шейдерами
 */
@Component
public class ShadersController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(ShadersController.class);
    
    @Autowired
    private ShaderService shaderService;
    
    @Autowired
    private InstanceManager instanceManager;
    
    // UI Elements
    @FXML private ComboBox<Instance> instanceSelector;
    @FXML private ListView<Shader> shadersListView;
    @FXML private VBox detailsPanel;
    @FXML private Label shaderNameLabel;
    @FXML private Label shaderVersionLabel;
    @FXML private Label shaderAuthorLabel;
    @FXML private Label shaderTypeLabel;
    @FXML private Label shaderStatusLabel;
    @FXML private TextArea shaderDescriptionArea;
    @FXML private Button enableButton;
    @FXML private Button disableButton;
    @FXML private Button deleteButton;
    @FXML private Button addShaderButton;
    
    private ObservableList<Shader> shaders = FXCollections.observableArrayList();
    private ObservableList<Instance> instances = FXCollections.observableArrayList();
    private Shader selectedShader;
    private Instance selectedInstance;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация ShadersController");
        
        loadInstances();
        
        shadersListView.setItems(shaders);
        shadersListView.setCellFactory(listView -> new ShaderCell());
        
        instanceSelector.setOnAction(e -> loadShadersForInstance());
        
        shadersListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showShaderDetails(newVal);
                }
            });
        
        enableButton.setOnAction(e -> toggleShader(true));
        disableButton.setOnAction(e -> toggleShader(false));
        deleteButton.setOnAction(e -> deleteShader());
        addShaderButton.setOnAction(e -> addShader());
        
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
    
    private void loadShadersForInstance() {
        selectedInstance = instanceSelector.getValue();
        if (selectedInstance == null) {
            shaders.clear();
            return;
        }
        
        List<Shader> loadedShaders = shaderService.getShadersForInstance(selectedInstance.getId());
        shaders.setAll(loadedShaders);
        
        log.info("Загружено шейдеров для инстанса {}: {}", selectedInstance.getName(), shaders.size());
    }
    
    private void showShaderDetails(Shader shader) {
        selectedShader = shader;
        detailsPanel.setVisible(true);
        
        shaderNameLabel.setText(shader.getName());
        shaderVersionLabel.setText("Версия: " + (shader.getVersion() != null ? shader.getVersion() : "N/A"));
        shaderAuthorLabel.setText("Автор: " + (shader.getAuthor() != null ? shader.getAuthor() : "Unknown"));
        shaderTypeLabel.setText("Тип: " + (shader.getShaderType() != null ? shader.getShaderType() : "N/A"));
        shaderDescriptionArea.setText(shader.getDescription() != null ? shader.getDescription() : "Описание недоступно");
        
        boolean enabled = shader.getEnabled() != null && shader.getEnabled();
        shaderStatusLabel.setText(enabled ? "Включён" : "Отключён");
        shaderStatusLabel.setTextFill(enabled ? Color.GREEN : Color.RED);
        
        enableButton.setDisable(enabled);
        disableButton.setDisable(!enabled);
    }
    
    private void toggleShader(boolean enable) {
        if (selectedShader == null) return;
        
        try {
            shaderService.toggleShader(selectedShader.getId(), enable);
            selectedShader.setEnabled(enable);
            showShaderDetails(selectedShader);
            loadShadersForInstance();
            
            log.info("Шейдер {} {}", selectedShader.getName(), enable ? "включён" : "отключён");
        } catch (Exception e) {
            log.error("Ошибка переключения шейдера", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void deleteShader() {
        if (selectedShader == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление шейдера");
        alert.setHeaderText("Удалить шейдер '" + selectedShader.getName() + "'?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                shaderService.uninstallShader(selectedShader.getId());
                loadShadersForInstance();
                detailsPanel.setVisible(false);
                selectedShader = null;
            } catch (Exception e) {
                log.error("Ошибка удаления шейдера", e);
                showAlert("Ошибка", e.getMessage());
            }
        }
    }
    
    private void addShader() {
        showAlert("Добавление шейдера", "Выберите ZIP или JAR файл шейдера для установки");
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
    private static class ShaderCell extends ListCell<Shader> {
        @Override
        protected void updateItem(Shader item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                
                Label statusIcon = new Label(item.getEnabled() ? "Вкл" : "Выкл");
                statusIcon.setStyle("-fx-font-size:12; -fx-background-color: " + 
                    (item.getEnabled() ? "#4CAF50" : "#F44336") + "; -fx-padding: 2 6;");
                
                VBox info = new VBox(2);
                Label name = new Label(item.getName());
                name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
                
                String type = item.getShaderType() != null ? item.getShaderType() : "N/A";
                String version = item.getVersion() != null ? item.getVersion() : "";
                Label desc = new Label(type + " - " + version);
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(statusIcon, info);
                
                setGraphic(hbox);
            }
        }
    }
}
