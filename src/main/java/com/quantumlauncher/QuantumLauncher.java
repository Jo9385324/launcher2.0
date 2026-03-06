package com.quantumlauncher;

import com.quantumlauncher.ui.QuantumLauncherApp;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * QuantumLauncher - Главная точка входа
 * 
 * Запускает JavaFX UI и Spring Boot контекст
 */
public class QuantumLauncher {
    
    private static final Logger log = LoggerFactory.getLogger(QuantumLauncher.class);
    private static ConfigurableApplicationContext springContext;
    
    public static void main(String[] args) {
        log.info("Запуск QuantumLauncher...");
        
        try {
            // Запуск Spring Boot
            springContext = SpringApplication.run(QuantumLauncherApplication.class, args);
            
            // Запуск JavaFX
            Application.launch(QuantumLauncherApp.class, args);
            
        } catch (Exception e) {
            log.error("Критическая ошибка запуска", e);
            System.exit(1);
        }
    }
    
    public static <T> T getBean(Class<T> beanClass) {
        return springContext.getBean(beanClass);
    }
}
