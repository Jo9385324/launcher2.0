package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.Fork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы со сборками (Forks)
 */
@Repository
public interface ForkRepository extends JpaRepository<Fork, String> {
    
    List<Fork> findByNameContainingIgnoreCase(String name);
    
    List<Fork> findByMcVersion(String mcVersion);
    
    List<Fork> findByModloader(String modloader);
    
    List<Fork> findByOfficial(boolean official);
    
    List<Fork> findByAuthorName(String authorName);
}
