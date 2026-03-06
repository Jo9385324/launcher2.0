package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.Instance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с инстансами
 */
@Repository
public interface InstanceRepository extends JpaRepository<Instance, String> {
    
    List<Instance> findByNameContainingIgnoreCase(String name);
    
    List<Instance> findByVersion(String version);
    
    List<Instance> findByModloader(String modloader);
    
    boolean existsByName(String name);
}
