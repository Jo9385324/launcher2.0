package com.quantumlauncher.ui.controller;

import com.quantumlauncher.ai.service.AIChatAssistant;
import com.quantumlauncher.ai.service.ConflictAnalysisService;
import com.quantumlauncher.core.model.Mod;
import com.quantumlauncher.core.repository.ModRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Controller для AI модуля
 */
@Component
public class AIController implements javafx.fxml.Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(AIController.class);
    
    @Autowired
    private AIChatAssistant chatAssistant;
    
    @Autowired
    private ConflictAnalysisService conflictAnalysisService;
    
    @Autowired
    private ModRepository modRepository;
    
    // Chat Elements
    @FXML private ListView<HBox> chatMessagesView;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    
    // Analysis Elements
    @FXML private ComboBox<String> instanceSelector;
    @FXML private TextArea analysisResult;
    @FXML private Button analyzeButton;
    @FXML private ProgressIndicator analysisProgress;
    
    // Tab control
    @FXML private TabPane aiTabPane;
    @FXML private Tab chatTab;
    @FXML private Tab analysisTab;
    
    private ObservableList<HBox> messages = FXCollections.observableArrayList();
    private String currentUserId = "local_user";
    
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        log.info("Инициализация AIController");
        
        // Настройка чата
        chatMessagesView.setItems(messages);
        
        // Обработчик отправки сообщения
        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnAction(e -> sendMessage());
        
        // Обработчик анализа
        analyzeButton.setOnAction(e -> analyzeConflicts());
        
        // Загрузка инстансов в селектор
        loadInstanceSelector();
        
        // Добавление приветственного сообщения
        addMessage("assistant", "Привет! Я AI-ассистент QuantumLauncher. Чем могу помочь?");
    }
    
    private void loadInstanceSelector() {
        // Загрузка списка инстансов
        List<String> instanceIds = modRepository.findAll()
            .stream()
            .map(Mod::getInstanceId)
            .distinct()
            .toList();
        
        instanceSelector.getItems().clear();
        instanceSelector.getItems().addAll(instanceIds);
        
        if (!instanceIds.isEmpty()) {
            instanceSelector.setValue(instanceIds.get(0));
        }
    }
    
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) return;
        
        // Добавление сообщения пользователя
        addMessage("user", message);
        messageInput.clear();
        
        // Получение ответа от AI
        new Thread(() -> {
            try {
                String response = chatAssistant.getResponse(currentUserId, message);
                
                Platform.runLater(() -> {
                    addMessage("assistant", response);
                });
            } catch (Exception e) {
                log.error("Ошибка получения ответа от AI", e);
                Platform.runLater(() -> {
                    addMessage("assistant", "Извините, произошла ошибка: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void addMessage(String sender, String text) {
        HBox messageBox = new HBox(10);
        messageBox.setAlignment(sender.equals("user") ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(10));
        
        // Стиль в зависимости от отправителя
        String bgColor = sender.equals("user") ? "#00d4ff" : "#1a2332";
        String textColor = sender.equals("user") ? "#000000" : "#ffffff";
        String align = sender.equals("user") ? "CENTER_RIGHT" : "CENTER_LEFT";
        
        Text textNode = new Text(text);
        textNode.setStyle("-fx-font-size: 14; -fx-fill: " + textColor + ";");
        textNode.wrappingWidthProperty().bind(chatMessagesView.widthProperty().subtract(60));
        
        VBox bubble = new VBox(textNode);
        bubble.setStyle("""
            -fx-background-color: %s;
            -fx-background-radius: 15;
            -fx-padding: 10 15;
            -fx-max-width: 400;
            """.formatted(bgColor));
        
        messageBox.getChildren().add(bubble);
        messages.add(messageBox);
        
        // Прокрутка к последнему сообщению
        Platform.runLater(() -> {
            chatMessagesView.scrollTo(messages.size() - 1);
        });
    }
    
    private void analyzeConflicts() {
        String instanceId = instanceSelector.getValue();
        
        if (instanceId == null || instanceId.isEmpty()) {
            showAlert("Ошибка", "Выберите инстанс для анализа");
            return;
        }
        
        log.info("Запуск анализа конфликтов для инстанса: {}", instanceId);
        
        // Показ индикатора загрузки
        analysisProgress.setVisible(true);
        analyzeButton.setDisable(true);
        analysisResult.setText("Анализ конфликтов...\n");
        
        // Запуск анализа в отдельном потоке
        new Thread(() -> {
            try {
                List<Mod> mods = modRepository.findByInstanceId(instanceId);
                
                ConflictAnalysisService.ConflictResult result = 
                    conflictAnalysisService.analyzeConflicts(mods);
                
                Platform.runLater(() -> {
                    displayAnalysisResult(result);
                });
            } catch (Exception e) {
                log.error("Ошибка анализа", e);
                Platform.runLater(() -> {
                    analysisResult.setText("Ошибка анализа: " + e.getMessage());
                    analysisProgress.setVisible(false);
                    analyzeButton.setDisable(false);
                });
            }
        }).start();
    }
    
    private void displayAnalysisResult(ConflictAnalysisService.ConflictResult result) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("═══════════════════════════════════════\n");
        sb.append("         РЕЗУЛЬТАТ АНАЛИЗА              \n");
        sb.append("═══════════════════════════════════════\n\n");
        
        // Статус
        if (result.hasConflicts) {
            sb.append("⚠️  ОБНАРУЖЕНЫ КОНФЛИКТЫ\n\n");
        } else {
            sb.append("✅  КОНФЛИКТОВ НЕ ОБНАРУЖЕНО\n\n");
        }
        
        // Конфликты
        if (!result.conflicts.isEmpty()) {
            sb.append("📋 НАЙДЕННЫЕ КОНФЛИКТЫ:\n");
            sb.append("─────────────────────────────────────\n");
            
            for (ConflictAnalysisService.Conflict conflict : result.conflicts) {
                sb.append(String.format("• %s ↔ %s\n", conflict.mod1, conflict.mod2));
                sb.append(String.format("  Причина: %s\n", conflict.reason));
                sb.append(String.format("  Тип: %s\n\n", conflict.type));
            }
        }
        
        // Рекомендации
        if (!result.recommendations.isEmpty()) {
            sb.append("💡 РЕКОМЕНДАЦИИ:\n");
            sb.append("─────────────────────────────────────\n");
            
            for (String rec : result.recommendations) {
                sb.append("• ").append(rec).append("\n");
            }
        }
        
        sb.append("\n═══════════════════════════════════════");
        
        analysisResult.setText(sb.toString());
        
        // Скрытие индикатора
        analysisProgress.setVisible(false);
        analyzeButton.setDisable(false);
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
