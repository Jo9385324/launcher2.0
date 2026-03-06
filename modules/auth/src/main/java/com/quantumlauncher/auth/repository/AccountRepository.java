package com.quantumlauncher.auth.repository;

import com.quantumlauncher.auth.model.MinecraftAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для аккаунтов Minecraft
 */
@Repository
public interface AccountRepository extends JpaRepository<MinecraftAccount, String> {
    
    Optional<MinecraftAccount> findByUuid(String uuid);
    
    Optional<MinecraftAccount> findByUsername(String username);
    
    Optional<MinecraftAccount> findByAccessToken(String accessToken);
    
    List<MinecraftAccount> findByActiveTrue();
    
    boolean existsByUuid(String uuid);
    
    boolean existsByUsername(String username);
}
