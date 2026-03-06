package com.quantumlauncher.ui.controller;

import com.quantumlauncher.content.service.TexturePackService;
import com.quantumlauncher.core.model.TexturePack;
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
 * Controller для управления текстур-паками
 */
@Component
public class TexturePacksController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(TexturePacksController.class);
    
    @Autowired
    private TexturePackService texturePackService;
    
    @Autowired
    private InstanceManager instanceManager;
    
    // UI Elements
    @FXML private ComboBox<Instance> instanceSelector;
    @FXML private ListView<TexturePack> texturePacksListView;
    @FXML private VBox detailsPanel;
    @FXML private Label texturePackNameLabel;
    @FXML private Label texturePackVersionLabel;
    @FXML private Label texturePackAuthorLabel;
    @FXML private Label texturePackResolutionLabel;
    @FXML private Label texturePackStatusLabel;
    @FXML private TextArea texturePackDescriptionArea;
    @FXML private Button enableButton;
    @FXML private Button disableButton;
    @FXML private Button deleteButton;
    @FXML private Button addTexturePackButton;
    
    private ObservableList<TexturePack> texturePacks = FXCollections.observableArrayList();
    private ObservableList<Instance> instances = FXCollections.observableArrayList();
    private TexturePack selectedTexturePack;
    private Instance selectedInstance;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация TexturePacksController");
        
        loadInstances();
        
        texturePacksListView.setItems(texturePacks);
        texturePacksListView.setCellFactory(listView -> new TexturePackCell());
        
        instanceSelector.setOnAction(e -> loadTexturePacksForInstance());
        
        texturePacksListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showTexturePackDetails(newVal);
                }
            });
        
        enableButton.setOnAction(e -> toggleTexturePack(true));
        disableButton.setOnAction(e -> toggleTexturePack(false));
        deleteButton.setOnAction(e -> deleteTexturePack());
        addTexturePackButton.setOnAction(e -> addTexturePack());
        
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
    
    private void loadTexturePacksForInstance() {
        selectedInstance = instanceSelector.getValue();
        if (selectedInstance == null) {
            texturePacks.clear();
            return;
        }
        
        List<TexturePack> loadedTexturePacks = texturePackService.getTexturePacksForInstance(selectedInstance.getId());
        texturePacks.setAll(loadedTexturePacks);
        
        log.info("Загружено текстур-паков для инстанса {}: {}", selectedInstance.getName(), texturePacks.size());
    }
    
    private void showTexturePackDetails(TexturePack texturePack) {
        selectedTexturePack = texturePack;
        detailsPanel.setVisible(true);
        
        texturePackNameLabel.setText(texturePack.getName());
        texturePackVersionLabel.setText("Версия: " + (texturePack.getVersion() != null ? texturePack.getVersion() : "N/A"));
        texturePackAuthorLabel.setText("Автор: " + (texturePack.getAuthor() != null ? texturePack.getAuthor() : "Unknown"));
        texturePackResolutionLabel.setText("Разрешение: " + (texturePack.getResolution() != null ? texturePack.getResolution() : "N/A"));
        texturePackDescriptionArea.setText(texturePack.getDescription() != null ? texturePack.getDescription() : "Описание недоступно");
        
        boolean enabled = texturePack.getEnabled() != null && texturePack.getEnabled();
        texturePackStatusLabel.setText(enabled ? "Включён" : "Отключён");
        texturePackStatusLabel.setTextFill(enabled ? Color.GREEN : Color.RED);
        
        enableButton.setDisable(enabled);
        disableButton.setDisable(!enabled);
    }
    
    private void toggleTexturePack(boolean enable) {
        if (selectedTexturePack == null) return;
        
        try {
            texturePackService.toggleTexturePack(selectedTexturePack.getId(), enable);
            selectedTexturePack.setEnabled(enable);
            showTexturePackDetails(selectedTexturePack);
            loadTexturePacksForInstance();
            
            log.info("Текстур-пак {} {}", selectedTexturePack.getName(), enable ? "включён" : "отключён");
        } catch (Exception e) {
            log.error("Ошибка переключения текстур-пака", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void deleteTexturePack() {
        if (selectedTexturePack == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление текстур-пака");
        alert.setHeaderText("Удалить текстур-пак '" + selectedTexturePack.getName() + "'?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                texturePackService.uninstallTexturePack(selectedTexturePack.getId());
                loadTexturePacksForInstance();
                detailsPanel.setVisible(false);
                selectedTexturePack = null;
            } catch (Exception e) {
                log.error("Ошибка удаления текстур-пака", e);
                showAlert("Ошибка", e.getMessage());
            }
        }
    }
    
    private void addTexturePack() {
        showAlert("Добавление текстур-пака", "Выберите ZIP файл текстур-пака для установки");
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
    private static class TexturePackCell extends ListCell<TexturePack> {
        @Override
        protected void updateItem(TexturePack item, boolean empty) {
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
                
                String resolution = item.getResolution() != null ? item.getResolution() : "N/A";
                String version = item.getVersion() != null ? item.getVersion() : "";
                Label desc = new Label(resolution + " - " + version);
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(statusIcon, info);
                
                setGraphic(hbox);
            }
        }
    }
}
