package com.pitstop.ia.repository;

import com.pitstop.ia.domain.ConfiguracaoIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para ConfiguracaoIA.
 */
@Repository
public interface ConfiguracaoIARepository extends JpaRepository<ConfiguracaoIA, UUID> {

    /**
     * Busca configuração por oficina.
     */
    Optional<ConfiguracaoIA> findByOficinaId(UUID oficinaId);

    /**
     * Verifica se existe configuração para a oficina.
     */
    boolean existsByOficinaId(UUID oficinaId);

    /**
     * Busca configuração habilitada por oficina.
     */
    @Query("SELECT c FROM ConfiguracaoIA c WHERE c.oficina.id = :oficinaId AND c.iaHabilitada = true")
    Optional<ConfiguracaoIA> findByOficinaIdAndIaHabilitadaTrue(@Param("oficinaId") UUID oficinaId);
}
