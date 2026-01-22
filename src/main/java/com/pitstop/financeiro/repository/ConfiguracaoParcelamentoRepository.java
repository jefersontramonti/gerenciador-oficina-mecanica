package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.ConfiguracaoParcelamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository para configurações de parcelamento.
 */
@Repository
public interface ConfiguracaoParcelamentoRepository extends JpaRepository<ConfiguracaoParcelamento, UUID> {

    /**
     * Busca configuração de parcelamento de uma oficina.
     */
    Optional<ConfiguracaoParcelamento> findByOficinaId(UUID oficinaId);

    /**
     * Verifica se existe configuração para uma oficina.
     */
    boolean existsByOficinaId(UUID oficinaId);
}
