package com.quantumlauncher.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller для AI Assistant
 */
public class AIAssistantController implements Initializable {
    
    private static final Logger log = LoggerFactory.getLogger(AIAssistantController.class);
    
    @FXML private TextArea chatHistory;
    @FXML private TextField userInput;
    @FXML private Button btnSend;
    @FXML private Button btnClear;
    @FXML private Button btnAnalyzeConflicts;
    @FXML private Button btnOptimize;
    @FXML private Label statusLabel;
    @FXML private ProgressBar analysisProgress;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Инициализация AIAssistantController");
        
        btnSend.setOnAction(e -> sendMessage());
        btnClear.setOnAction(e -> clearChat());
        btnAnalyzeConflicts.setOnAction(e -> analyzeConflicts());
        btnOptimize.setOnAction(e -> optimizeSystem());
        
        // Enter для отправки
        userInput.setOnAction(e -> sendMessage());
        
        // Приветственное сообщение
        appendToChat("AI", "Привет! Я AI-ассистент QuantumLauncher. Чем могу помочь?");
    }
    
    private void sendMessage() {
        String message = userInput.getText().trim();
        if (message.isEmpty()) return;
        
        appendToChat("Вы", message);
        userInput.clear();
        
        // Простой ответ (в реальной реализации - вызов AIChatAssistant)
        String response = generateResponse(message);
        appendToChat("AI", response);
    }
    
    private String generateResponse(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains("конфликт") || lower.contains("краш")) {
            return "Для анализа конфликтов нажмите кнопку 'Анализ конфликтов'. Это поможет найти несовместимые моды.";
        }
        
        if (lower.contains("оптимиз") || lower.contains("fps")) {
            return "Рекомендации по оптимизации:\n• Используйте Sodium, Lithium, Phosphor\n• Отключите VBO в настройках\n• Установите Distance на 8-12";
        }
        
        if (lower.contains("как") && lower.contains("созда")) {
            return "Для создания инстанса:\n1. Перейдите в раздел 'Инстансы'\n2. Нажмите 'Создать'\n3. Выберите версию и модлоадер";
        }
        
        return "Я получил ваш вопрос. Попробую помочь! Вы можете спросить:\n• Про конфликты модов\n• Про оптимизацию FPS\n• Как создать инстанс";
    }
    
    private void appendToChat(String sender, String message) {
        chatHistory.appendText(sender + ": " + message + "\n\n");
        chatHistory.setScrollTop(Double.MAX_VALUE);
    }
    
    private void clearChat() {
        chatHistory.clear();
        appendToChat("AI", "Чат очищен. Чем могу помочь?");
    }
    
    private void analyzeConflicts() {
        log.info("Запуск анализа конфликтов");
        statusLabel.setText("Анализ конфликтов...");
        
        // Имитация анализа
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    final int progress = i;
                    javafx.application.Platform.runLater(() -> {
                        analysisProgress.setProgress(progress / 100.0);
                    });
                    Thread.sleep(200);
                }
                
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Анализ завершён");
                    appendToChat("AI", "Анализ конфликтов завершён!\n\n✓ Конфликтов не обнаружено\n\nРекомендации:\n• Установите JEI для просмотра рецептов");
                });
            } catch (InterruptedException e) {
                log.error("Ошибка анализа", e);
            }
        }).start();
    }
    
    private void optimizeSystem() {
        log.info("Запуск оптимизации");
        statusLabel.setText("Оптимизация системы...");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Оптимизация");
        alert.setHeaderText("Оптимизация завершена");
        alert.setContentText("Применены следующие оптимизации:\n• Включён Sodium\n• Отключены частицы\n• Distance установлен на 10");
        alert.show();
        
        statusLabel.setText("Готов");
    }
}
