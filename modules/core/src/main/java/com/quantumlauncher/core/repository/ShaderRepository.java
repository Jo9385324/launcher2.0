package com.quantumlauncher.core.repository;

import com.quantumlauncher.core.model.Shader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с шейдерами
 */
@Repository
public interface ShaderRepository extends JpaRepository<Shader, String> {

    List<Shader> findByInstanceId(String instanceId);

    List<Shader> findByNameContainingIgnoreCase(String name);

    List<Shader> findByCategory(String category);

    List<Shader> findByInstanceIdAndEnabled(String instanceId, boolean enabled);

    List<Shader> findByInstanceIdAndShaderType(String instanceId, String shaderType);

    boolean existsByNameAndInstanceId(String name, String instanceId);
}
