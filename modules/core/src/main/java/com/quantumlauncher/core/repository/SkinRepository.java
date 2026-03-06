package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.Skin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы со скинами
 */
@Repository
public interface SkinRepository extends JpaRepository<Skin, String> {
    
    List<Skin> findByNameContainingIgnoreCase(String name);
    
    List<Skin> findByType(String type);
    
    List<Skin> findByPlayerUuid(String playerUuid);
    
    Optional<Skin> findByPlayerUuidAndActive(String playerUuid, boolean active);
    
    Optional<Skin> findByUrl(String url);
}
