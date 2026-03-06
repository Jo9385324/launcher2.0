package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с картами
 */
@Repository
public interface MapRepository extends JpaRepository<Map, String> {

    List<Map> findByInstanceId(String instanceId);

    List<Map> findByNameContainingIgnoreCase(String name);

    List<Map> findByCategory(String category);

    List<Map> findByInstanceIdAndEnabled(String instanceId, boolean enabled);

    List<Map> findByInstanceIdAndMapType(String instanceId, String mapType);

    List<Map> findByGameVersion(String gameVersion);

    boolean existsByNameAndInstanceId(String name, String instanceId);
}
