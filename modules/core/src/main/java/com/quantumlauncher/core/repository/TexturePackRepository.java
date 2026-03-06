package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.TexturePack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с текстур-паками
 */
@Repository
public interface TexturePackRepository extends JpaRepository<TexturePack, String> {

    List<TexturePack> findByInstanceId(String instanceId);

    List<TexturePack> findByNameContainingIgnoreCase(String name);

    List<TexturePack> findByCategory(String category);

    List<TexturePack> findByInstanceIdAndEnabled(String instanceId, boolean enabled);

    List<TexturePack> findByInstanceIdAndResolution(String instanceId, String resolution);

    boolean existsByNameAndInstanceId(String name, String instanceId);
}
