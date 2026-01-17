package com.pitstop.manutencaopreventiva.repository;

import com.pitstop.manutencaopreventiva.domain.ConfiguracaoManutencaoPreventiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfiguracaoManutencaoRepository extends JpaRepository<ConfiguracaoManutencaoPreventiva, UUID> {

    /**
     * Busca configuração de uma oficina.
     */
    @Query("SELECT c FROM ConfiguracaoManutencaoPreventiva c WHERE c.oficina.id = :oficinaId")
    Optional<ConfiguracaoManutencaoPreventiva> findByOficinaId(@Param("oficinaId") UUID oficinaId);
}
