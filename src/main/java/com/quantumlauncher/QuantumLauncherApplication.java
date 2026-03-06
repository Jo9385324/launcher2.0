package com.quantumlauncher;

import com.quantumlauncher.ui.QuantumLauncherApp;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * QuantumLauncher - Spring Boot Application
 *
 * Основной класс приложения с интеграцией Spring Boot и JavaFX
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.quantumlauncher",
    "com.quantumlauncher.core",
    "com.quantumlauncher.core.config",
    "com.quantumlauncher.core.repository",
    "com.quantumlauncher.api",
    "com.quantumlauncher.ai",
    "com.quantumlauncher.ai.service",
    "com.quantumlauncher.content",
    "com.quantumlauncher.content.service",
    "com.quantumlauncher.security",
    "com.quantumlauncher.security.scanner",
    "com.quantumlauncher.cloud",
    "com.quantumlauncher.cloud.service",
    "com.quantumlauncher.instances",
    "com.quantumlauncher.instances.service",
    "com.quantumlauncher.forks",
    "com.quantumlauncher.forks.service",
    "com.quantumlauncher.skins",
    "com.quantumlauncher.skins.service",
    "com.quantumlauncher.monitoring",
    "com.quantumlauncher.monitoring.service",
    "com.quantumlauncher.auth",
    "com.quantumlauncher.auth.repository",
    "com.quantumlauncher.auth.service",
    "com.quantumlauncher.ui",
    "com.quantumlauncher.ui.controller"
})
@EntityScan(basePackages = {
    "com.quantumlauncher.core.model",
    "com.quantumlauncher.auth.model"
})
@EnableJpaRepositories(basePackages = {
    "com.quantumlauncher.core.repository",
    "com.quantumlauncher.auth.repository"
})
@EnableAsync
@EnableScheduling
public class QuantumLauncherApplication {

    private static final Logger log = LoggerFactory.getLogger(QuantumLauncherApplication.class);

    public static void main(String[] args) {
        // Запуск Spring Boot
        ConfigurableApplicationContext context = SpringApplication.run(
                QuantumLauncherApplication.class, args
        );

        log.info("Spring контекст инициализирован");

        // Передача контекста в JavaFX приложение
        QuantumLauncherApp.setSpringContext(context);

        // Запуск JavaFX
        try {
            Application.launch(QuantumLauncherApp.class, args);
        } catch (Exception e) {
            log.error("Ошибка запуска JavaFX", e);
        }
    }
}
