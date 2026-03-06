package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.Mod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с модами
 */
@Repository
public interface ModRepository extends JpaRepository<Mod, String> {

    List<Mod> findByInstanceId(String instanceId);

    List<Mod> findByNameContainingIgnoreCase(String name);

    List<Mod> findByCategory(String category);

    List<Mod> findByInstanceIdAndEnabled(String instanceId, boolean enabled);

    List<Mod> findByInstanceIdAndCategory(String instanceId, String category);

    boolean existsByNameAndInstanceId(String name, String instanceId);
}
