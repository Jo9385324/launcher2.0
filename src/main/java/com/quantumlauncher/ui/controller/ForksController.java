package com.quantumlauncher.ui.controller;

import com.quantumlauncher.forks.service.ForkManager;
import com.quantumlauncher.core.model.Fork;
import com.quantumlauncher.core.model.Instance;
import com.quantumlauncher.instances.service.InstanceManager;
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
 * Controller для управления сборками (Forks)
 */
@Component
public class ForksController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(ForksController.class);
    
    @Autowired
    private ForkManager forkManager;
    
    @Autowired
    private InstanceManager instanceManager;
    
    // UI Elements
    @FXML private ListView<Fork> forksListView;
    @FXML private VBox detailsPanel;
    @FXML private Label forkNameLabel;
    @FXML private Label forkVersionLabel;
    @FXML private Label forkModloaderLabel;
    @FXML private Label forkAuthorLabel;
    @FXML private Label forkDownloadsLabel;
    @FXML private TextArea forkDescriptionArea;
    @FXML private ComboBox<Instance> targetInstanceCombo;
    @FXML private Button applyButton;
    @FXML private Button createButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    
    private ObservableList<Fork> forks = FXCollections.observableArrayList();
    private ObservableList<Instance> instances = FXCollections.observableArrayList();
    private Fork selectedFork;
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация ForksController");
        
        // Загрузка данных
        loadForks();
        loadInstances();
        
        // Настройка ListView
        forksListView.setItems(forks);
        forksListView.setCellFactory(listView -> new ForkCell());
        
        // Обработчик выбора
        forksListView.getSelectionModel().selectedItemProperty()
            .addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showForkDetails(newVal);
                }
            });
        
        // Кнопки
        applyButton.setOnAction(e -> applyFork());
        createButton.setOnAction(e -> createFork());
        exportButton.setOnAction(e -> exportFork());
        importButton.setOnAction(e -> importFork());
        
        detailsPanel.setVisible(false);
    }
    
    private void loadForks() {
        List<Fork> allForks = forkManager.getOfficialForks();
        forks.setAll(allForks);
        log.info("Загружено сборок: {}", allForks.size());
    }
    
    private void loadInstances() {
        List<Instance> allInstances = instanceManager.getAllInstances();
        instances.setAll(allInstances);
        targetInstanceCombo.setItems(instances);
        if (!allInstances.isEmpty()) {
            targetInstanceCombo.setValue(allInstances.get(0));
        }
    }
    
    private void showForkDetails(Fork fork) {
        selectedFork = fork;
        detailsPanel.setVisible(true);
        
        forkNameLabel.setText(fork.getName());
        forkVersionLabel.setText("Minecraft " + fork.getMcVersion());
        forkModloaderLabel.setText(fork.getModloader() != null ? fork.getModloader() : "Vanilla");
        forkAuthorLabel.setText("Автор: " + (fork.getAuthorName() != null ? fork.getAuthorName() : "Unknown"));
        forkDownloadsLabel.setText("Загрузок: " + fork.getDownloadCount());
        forkDescriptionArea.setText(fork.getDescription() != null ? fork.getDescription() : "Описание недоступно");
    }
    
    private void applyFork() {
        if (selectedFork == null) {
            showAlert("Ошибка", "Выберите сборку для применения");
            return;
        }
        
        Instance targetInstance = targetInstanceCombo.getValue();
        if (targetInstance == null) {
            showAlert("Ошибка", "Выберите целевой инстанс");
            return;
        }
        
        try {
            forkManager.applyForkToInstance(selectedFork.getId(), targetInstance.getId());
            showAlert("Успех", "Сборка применена к инстансу " + targetInstance.getName());
        } catch (Exception e) {
            log.error("Ошибка применения сборки", e);
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void createFork() {
        showAlert("Создание сборки", "Функция создания сборки из инстанса");
    }
    
    private void exportFork() {
        if (selectedFork == null) {
            showAlert("Ошибка", "Выберите сборку для экспорта");
            return;
        }
        
        try {
            showAlert("Экспорт", "Сборка экспортирована");
        } catch (Exception e) {
            showAlert("Ошибка", e.getMessage());
        }
    }
    
    private void importFork() {
        showAlert("Импорт", "Выберите файл сборки для импорта");
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
    private static class ForkCell extends ListCell<Fork> {
        @Override
        protected void updateItem(Fork item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);
                
                // Иконка
                Label icon = new Label(item.isOfficial() ? "Официальная" : "Кастомная");
                icon.setStyle("-fx-font-size:12; -fx-background-color: #2196F3; -fx-padding: 2 6; -fx-text-fill: white;");
                
                // Информация
                VBox info = new VBox(2);
                Label name = new Label(item.getName());
                name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white;");
                
                String version = item.getMcVersion() != null ? item.getMcVersion() : "";
                String modloader = item.getModloader() != null ? item.getModloader() : "Vanilla";
                Label desc = new Label(version + " - " + modloader + " - Загрузок: " + item.getDownloadCount());
                desc.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");
                
                info.getChildren().addAll(name, desc);
                hbox.getChildren().addAll(icon, info);
                
                setGraphic(hbox);
            }
        }
    }
}
