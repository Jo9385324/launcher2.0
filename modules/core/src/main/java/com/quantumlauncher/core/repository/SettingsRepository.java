package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с настройками
 */
@Repository
public interface SettingsRepository extends JpaRepository<AppSettings, String> {
    
    Optional<AppSettings> findById(String id);
}
