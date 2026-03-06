package com.quantumlauncher.ui.controller;

import com.quantumlauncher.skins.service.SkinManager;
import com.quantumlauncher.core.model.Skin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Controller для управления скинами
 */
@Component
public class SkinsController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(SkinsController.class);
    
    @Autowired
    private SkinManager skinManager;
    
    // UI Elements
    @FXML private ListView<Skin> skinsListView;
    @FXML private VBox previewPanel;
    @FXML private Label skinNameLabel;
    @FXML private Label skinTypeLabel;
    @FXML private Label skinStatusLabel;
    @FXML private TextField usernameField;
    @FXML private Button loadByUsernameButton;
    @FXML private Button uploadSkinButton;
    @FXML private Button uploadCapeButton;
    @FXML private Button activateButton;
    @FXML private Button deleteButton;
    
    private ObservableList<Skin> skins = FXCollections.observableArrayList();
    private Skin selectedSkin;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация SkinsController");
        
        // Загрузка скинов
        loadSkins();
        
        // Настройка ListView
        skinsListView.setItems(skins);
        skinsListView.setCellFactory(listView -> new SkinCell());
        
        // Обработчик выбора
        skinsListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showSkinDetails(newVal);
                }
            });
        
        // Кнопки
        loadByUsernameButton.setOnAction(e -> loadSkinByUsername());
        uploadSkinButton.setOnAction(e -> uploadSkin());
        uploadCapeButton.setOnAction(e -> uploadCape());
        activateButton.setOnAction(e -> activateSkin());
        deleteButton.setOnAction(e -> deleteSkin());
        
        previewPanel.setVisible(false);
    }
    
    private void loadSkins() {
        // Загрузка всех скинов - для демонстрации пустой список
        skins.clear();
        log.info("Загружено скинов: {}", skins.size());
    }
    
    private void showSkinDetails(Skin skin) {
        selectedSkin = skin;
        previewPanel.setVisible(true);
        
        skinNameLabel.setText(skin.getName());
        skinTypeLabel.setText("Тип: " + (skin.getType() != null ? skin.getType() : "SKIN"));
        
        boolean active = skin.getActive();
        skinStatusLabel.setText(active ? "Активен" : "Неактивен");
        
        activateButton.setDisable(active);
    }
    
    private void loadSkinByUsername() {
        String username = usernameField.getText();
        if (username == null || username.isEmpty()) {
            showAlert("Ошибка", "Введите никнейм игрока");
            return;
        }
        
        try {
            Skin skin = skinManager.loadSkinByUsername(username);
            skins.add(skin);
            showAlert("Успех", "Скин загружен для " + username);
            usernameField.clear();
        } catch (Exception e) {
            log.error("Ошибка загрузки скина", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void uploadSkin() {
        showAlert("Загрузка скина", "Выберите PNG файл скина");
    }
    
    private void uploadCape() {
        showAlert("Загрузка плаща", "Выберите PNG файл плаща");
    }
    
    private void activateSkin() {
        if (selectedSkin == null) {
            showAlert("Ошибка", "Выберите скин для активации");
            return;
        }
        
        try {
            skinManager.activateSkin(selectedSkin.getId());
            loadSkins();
            showAlert("Успех", "Скин активирован");
        } catch (Exception e) {
            log.error("Ошибка активации скина", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void deleteSkin() {
        if (selectedSkin == null) {
            showAlert("Ошибка", "Выберите скин для удаления");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление скина");
        alert.setHeaderText("Удалить скин '" + selectedSkin.getName() + "'?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            skinManager.deleteSkin(selectedSkin.getId());
            loadSkins();
            previewPanel.setVisible(false);
            selectedSkin = null;
        }
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
    private static class SkinCell extends ListCell<Skin> {
        @Override
        protected void updateItem(Skin item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                
                // Статус
                String status = item.getActive() ? "Активен" : "Неактивен";
                Label statusLabel = new Label(status);
                statusLabel.setStyle("-fx-font-size:12; -fx-background-color: " + 
                    (item.getActive() ? "#4CAF50" : "#757575") + "; -fx-padding: 2 6; -fx-text-fill: white;");
                
                // Информация
                VBox info = new VBox(2);
                Label name = new Label(item.getName());
                name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
                
                String type = item.getType() != null ? item.getType() : "SKIN";
                String premium = item.getPremium() ? "Премиум" : "Кастомный";
                Label desc = new Label(type + " - " + premium);
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(statusLabel, info);
                
                setGraphic(hbox);
            }
        }
    }
}
