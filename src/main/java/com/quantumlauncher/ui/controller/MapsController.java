package com.quantumlauncher.ui.controller;

import com.quantumlauncher.content.service.MapService;
import com.quantumlauncher.core.model.Map;
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
 * Controller для управления картами
 */
@Component
public class MapsController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(MapsController.class);
    
    @Autowired
    private MapService mapService;
    
    @Autowired
    private InstanceManager instanceManager;
    
    // UI Elements
    @FXML private ComboBox<Instance> instanceSelector;
    @FXML private ListView<Map> mapsListView;
    @FXML private VBox detailsPanel;
    @FXML private Label mapNameLabel;
    @FXML private Label mapVersionLabel;
    @FXML private Label mapAuthorLabel;
    @FXML private Label mapTypeLabel;
    @FXML private Label mapPlayersLabel;
    @FXML private Label mapStatusLabel;
    @FXML private TextArea mapDescriptionArea;
    @FXML private Button enableButton;
    @FXML private Button disableButton;
    @FXML private Button deleteButton;
    @FXML private Button addMapButton;
    
    private ObservableList<Map> maps = FXCollections.observableArrayList();
    private ObservableList<Instance> instances = FXCollections.observableArrayList();
    private Map selectedMap;
    private Instance selectedInstance;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация MapsController");
        
        loadInstances();
        
        mapsListView.setItems(maps);
        mapsListView.setCellFactory(listView -> new MapCell());
        
        instanceSelector.setOnAction(e -> loadMapsForInstance());
        
        mapsListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showMapDetails(newVal);
                }
            });
        
        enableButton.setOnAction(e -> toggleMap(true));
        disableButton.setOnAction(e -> toggleMap(false));
        deleteButton.setOnAction(e -> deleteMap());
        addMapButton.setOnAction(e -> addMap());
        
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
    
    private void loadMapsForInstance() {
        selectedInstance = instanceSelector.getValue();
        if (selectedInstance == null) {
            maps.clear();
            return;
        }
        
        List<Map> loadedMaps = mapService.getMapsForInstance(selectedInstance.getId());
        maps.setAll(loadedMaps);
        
        log.info("Загружено карт для инстанса {}: {}", selectedInstance.getName(), maps.size());
    }
    
    private void showMapDetails(Map map) {
        selectedMap = map;
        detailsPanel.setVisible(true);
        
        mapNameLabel.setText(map.getName());
        mapVersionLabel.setText("Версия: " + (map.getVersion() != null ? map.getVersion() : "N/A"));
        mapAuthorLabel.setText("Автор: " + (map.getAuthor() != null ? map.getAuthor() : "Unknown"));
        mapTypeLabel.setText("Тип: " + (map.getMapType() != null ? map.getMapType() : "N/A"));
        mapPlayersLabel.setText("Игроки: " + map.getPlayerCount());
        mapDescriptionArea.setText(map.getDescription() != null ? map.getDescription() : "Описание недоступно");
        
        boolean enabled = map.getEnabled() != null && map.getEnabled();
        mapStatusLabel.setText(enabled ? "Включена" : "Отключена");
        mapStatusLabel.setTextFill(enabled ? Color.GREEN : Color.RED);
        
        enableButton.setDisable(enabled);
        disableButton.setDisable(!enabled);
    }
    
    private void toggleMap(boolean enable) {
        if (selectedMap == null) return;
        
        try {
            mapService.toggleMap(selectedMap.getId(), enable);
            selectedMap.setEnabled(enable);
            showMapDetails(selectedMap);
            loadMapsForInstance();
            
            log.info("Карта {} {}", selectedMap.getName(), enable ? "включена" : "отключена");
        } catch (Exception e) {
            log.error("Ошибка переключения карты", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void deleteMap() {
        if (selectedMap == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление карты");
        alert.setHeaderText("Удалить карту '" + selectedMap.getName() + "'?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                mapService.uninstallMap(selectedMap.getId());
                loadMapsForInstance();
                detailsPanel.setVisible(false);
                selectedMap = null;
            } catch (Exception e) {
                log.error("Ошибка удаления карты", e);
                showAlert("Ошибка", e.getMessage());
            }
        }
    }
    
    private void addMap() {
        showAlert("Добавление карты", "Выберите ZIP или JAR файл карты для установки");
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
    private static class MapCell extends ListCell<Map> {
        @Override
        protected void updateItem(Map item, boolean empty) {
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
                
                String type = item.getMapType() != null ? item.getMapType() : "N/A";
                String players = item.getPlayerCount() + " игроков";
                Label desc = new Label(type + " - " + players);
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(statusIcon, info);
                
                setGraphic(hbox);
            }
        }
    }
}
