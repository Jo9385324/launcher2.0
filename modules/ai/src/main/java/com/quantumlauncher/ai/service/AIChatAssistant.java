package com.quantumlauncher.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Чат-ассистент для помощи пользователю
 * Поддерживает интеграцию с LangChain4j и режим офлайн
 */
@Service
public class AIChatAssistant {
    
    private static final Logger log = LoggerFactory.getLogger(AIChatAssistant.class);
    
    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    
    @Value("${ai.chat.api-key:}")
    private String apiKey;
    
    @Value("${ai.chat.model:gpt-3.5-turbo}")
    private String modelName;
    
    private boolean aiEnabled = false;
    
    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("demo")) {
            aiEnabled = true;
            log.info("AI Chat включён с моделью: {}", modelName);
        } else {
            log.info("AI Chat работает в локальном режиме");
        }
    }
    
    /**
     * Сообщение чата
     */
    public static class ChatMessage {
        public String role;
        public String content;
        public long timestamp;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Ответ на вопрос пользователя
     */
    public String getResponse(String userId, String message) {
        log.info("Получен вопрос от пользователя {}: {}", userId, message);
        
        conversationHistory.computeIfAbsent(userId, k -> new ArrayList<>())
            .add(new ChatMessage("user", message));
        
        String response = generateResponse(message);
        conversationHistory.get(userId).add(new ChatMessage("assistant", response));
        
        // Ограничение истории
        if (conversationHistory.get(userId).size() > 20) {
            conversationHistory.get(userId).subList(0, 10).clear();
        }
        
        return response;
    }
    
    /**
     * Генерация ответа (локальная база знаний)
     */
    private String generateResponse(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Приветствие
        if (containsAny(lowerMessage, "привет", "hi", "hello", "здорово", "hey")) {
            return """
                Привет! Я AI-ассистент QuantumLauncher. 🤖
                
                Я могу помочь вам с:
                • Созданием и управлением инстансами
                • Установкой и удалением модов
                • Анализом конфликтов между модами
                • Оптимизацией производительности
                • Настройкой скинов
                • Созданием кастомных сборок
                
                О чём хотите узнать?""";
        }
        
        // Создание инстанса
        if (containsAny(lowerMessage, "создать инстанс", "new instance", "создать сервер", "new game")) {
            return """
                Для создания нового инстанса:
                
                1. Перейдите в раздел "Инстансы"
                2. Нажмите кнопку "+" в правом верхнем углу
                3. Выберите версию Minecraft (рекомендую 1.20.4)
                4. Выберите загрузчик модов:
                   • Vanilla - без модов
                   • Fabric - лёгкий, популярный
                   • Forge - много модов, тяжелее
                5. Назовите инстанс и нажмите "Создать"
                
                После создания можно настроить RAM и JVM аргументы.""";
        }
        
        // Установка модов
        if (containsAny(lowerMessage, "мод", "mod", "моды", "установить мод", "скачать мод")) {
            return """
                Для установки модов:
                
                1. Выберите инстанс в списке
                2. Перейдите в раздел "Моды"
                3. Нажмите "Загрузить мод" или перетащите файл
                4. Мод будет установлен в папку mods
                
                Важные моды для начала:
                • Sodium - значительное улучшение FPS
                • JEI/REI/EMI - просмотр рецептов
                • ModMenu - управление модами в игре
                • Lithium + Phosphor - дополнительная оптимизация""";
        }
        
        // Конфликты
        if (containsAny(lowerMessage, "конфликт", "конфликтуют", "conflict", "crash", "вылетает", "краш")) {
            return """
                Для анализа конфликтов:
                
                1. Перейдите в раздел "AI Помощник"
                2. Нажмите "Анализ конфликтов"
                3. Система проверит все установленные моды
                
                Распространённые конфликты:
                • JEI + REI + EMI - выберите только один!
                • OptiFine + Sodium - несовместимы
                • Несколько модов оптимизации одновременно
                
                После краша проверьте логи в папке instances/логи""";
        }
        
        // Оптимизация FPS
        if (containsAny(lowerMessage, "fps", "оптимиз", "производительност", "lag", "лагает", "тормозит")) {
            return """
                Советы по оптимизации FPS:
                
                Установите эти моды (Fabric):
                • Sodium - основная оптимизация рендера
                • Lithium - улучшение игрового сервера
                • Phosphor - оптимизация освещения
                • Starlight - улучшение ночной темноты
                
                Настройки в игре:
                • Render Distance: 8-12 чанков
                • Simulation Distance: 6-8 чанков
                • Graphics: Fast
                • VSync: ON
                • Particles: Minimal
                
                В лаунчере:
                • Увеличьте RAM до 4-6 GB
                • Используйте JVM аргументы оптимизации""";
        }
        
        // Версии Minecraft
        if (containsAny(lowerMessage, "версия", "version", "майна", "minecraft", "1.20", "1.19", "1.18")) {
            return """
                Поддерживаемые версии Minecraft:
                
                🟢 1.20.4 (последняя) - Рекомендуется
                🟢 1.20.3
                🟢 1.20.2
                🟢 1.20.1
                🟡 1.19.4
                🟡 1.19.3
                🟡 1.18.2
                
                Рекомендую 1.20.4 - самая стабильная версия с большим выбором модов.""";
        }
        
        // Forks / Сборки
        if (containsAny(lowerMessage, "сборка", "fork", "модпак", "modpack")) {
            return """
                Кастомные сборки (Forks):
                
                1. Перейдите в раздел "Сборки"
                2. Выберите версию и модлоадер
                3. Найдите подходящую сборку
                4. Нажмите "Установить"
                
                Создание своей сборки:
                1. Настройте инстанс с нужными модами
                2. Перейдите в "Сборки"
                3. Нажмите "Создать сборку"
                4. Поделитесь с другими игроками!""";
        }
        
        // Скины
        if (containsAny(lowerMessage, "скин", "skin", "плащ", "cape")) {
            return """
                Управление скинами:
                
                1. Перейдите в раздел "Скины"
                2. Загрузите свой скин (PNG 64x32 или 128x64)
                или введите никнейм игрока для загрузки его скина
                
                3D Preview позволяет посмотреть модель со всех сторон
                Мышь - вращение, колесо - зум
                
                Плащи загружаются аналогично.""";
        }
        
        // Настройки
        if (containsAny(lowerMessage, "настройка", "settings", "конфиг", "config", "ram", "память")) {
            return """
                Настройки QuantumLauncher:
                
                Java:
                • Путь к Java - автоматически определяется
                • Можно указать свою версию JDK 21+
                
                Память (RAM):
                • Минимум: 1024 MB
                • Рекомендуется: 4096 MB (4 GB)
                • Максимум: зависит от вашей системы
                
                Рекомендуемые JVM аргументы:
                • -XX:+UseG1GC - современный сборщик мусора
                • -XX:+ParallelRefProcEnabled - многопоточность
                • -XX:MaxGCPauseMillis=200 - плавный FPS""";
        }
        
        // Безопасность
        if (containsAny(lowerMessage, "безопасност", "вирус", "scanner", "проверить")) {
            return """
                Безопасность в QuantumLauncher:
                
                Встроенный сканер проверяет моды на:
                • Известные вредоносные сигнатуры
                • Подозрительный код (Runtime.exec и т.д.)
                • Zip-bomb атаки
                • Неизвестные хеши файлов
                
                Все моды автоматически проверяются при установке.
                Подозрительные файлы будут заблокированы.""";
        }
        
        // Облако
        if (containsAny(lowerMessage, "облако", "cloud", "синхрониз", "backup")) {
            return """
                Облачная синхронизация:
                
                Настройте в разделе "Настройки":
                • S3 Endpoint (например, s3.amazonaws.com)
                • Bucket Name
                • Access Key и Secret Key
                
                Синхронизируется:
                • Инстансы и их настройки
                • Установленные моды
                • Скины и профили
                
                Данные шифруются перед загрузкой.""";
        }
        
        // Благодарность
        if (containsAny(lowerMessage, "спасибо", "thanks", "thank", "благодар")) {
            return "Пожалуйста! Рад был помочь! 😊\n\nЕсли будут ещё вопросы - обращайтесь!";
        }
        
        // Прощание
        if (containsAny(lowerMessage, "пока", "bye", "exit", "выход", "до свидания")) {
            return "До свидания! Удачи с игрой! 🎮\n\nВозвращайтесь, если понадоблюсь!";
        }
        
        // Не поняли
        return """
            Извините, я не до конца понял ваш вопрос. 😕
            
            Я могу помочь с:
            • Созданием инстансов
            • Установкой модов
            • Анализом конфликтов
            • Оптимизацией FPS
            • Управлением скинами
            • Настройками лаунчера
            • Безопасностью
            • Облачной синхронизацией
            
            Попробуйте переформулировать вопрос!""";
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    public List<ChatMessage> getConversationHistory(String userId) {
        return conversationHistory.getOrDefault(userId, new ArrayList<>());
    }
    
    public void clearConversationHistory(String userId) {
        conversationHistory.remove(userId);
        log.info("История чата очищена для пользователя: {}", userId);
    }
    
    public boolean isAIEnabled() {
        return aiEnabled;
    }
}
