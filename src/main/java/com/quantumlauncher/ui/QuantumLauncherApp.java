package com.quantumlauncher.ui;

import com.quantumlauncher.ui.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * QuantumLauncher - JavaFX Application Main Class
 */
public class QuantumLauncherApp extends Application {
    
    private static final Logger log = LoggerFactory.getLogger(QuantumLauncherApp.class);
    private static Stage primaryStage;
    private static ApplicationContext springContext;
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void setSpringContext(ApplicationContext context) {
        springContext = context;
    }

    @Override
    public void init() throws Exception {
        super.init();
        log.info("QuantumLauncher инициализация...");
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        
        try {
            // Загрузка главного представления с Spring ControllerFactory
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml")
            );
            
            // Установка фабрики контроллеров для Spring
            if (springContext != null) {
                loader.setControllerFactory(springContext::getBean);
                log.info("Spring ControllerFactory активирован");
            }
            
            Parent root = loader.load();
            
            // Получение контроллера для инициализации
            MainController mainController = loader.getController();
            if (mainController != null) {
                log.info("MainController инициализирован через Spring");
            }
            
            // Настройка сцены
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                getClass().getResource("/css/theme.css").toExternalForm()
            );
            
            // Настройка окна
            stage.setTitle("QuantumLauncher");
            stage.setScene(scene);
            stage.setMinWidth(1024);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            
            // Обработчик закрытия
            stage.setOnCloseRequest(e -> {
                log.info("Запрос на закрытие приложения");
                Platform.exit();
            });
            
            // Установка иконки
            try {
                stage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/images/icon.png"))
                );
            } catch (Exception e) {
                log.warn("Не удалось загрузить иконку приложения");
            }
            
            stage.show();
            log.info("QuantumLauncher запущен успешно");
            
        } catch (Exception e) {
            log.error("Ошибка загрузки интерфейса", e);
            Platform.exit();
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("QuantumLauncher завершение работы...");
        super.stop();
    }
}
