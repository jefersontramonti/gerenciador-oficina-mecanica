package com.pitstop.ordemservico.repository;

import com.pitstop.ordemservico.domain.HistoricoStatusOS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório para histórico de status de Ordens de Serviço.
 */
@Repository
public interface HistoricoStatusOSRepository extends JpaRepository<HistoricoStatusOS, UUID> {

    /**
     * Busca histórico de uma OS específica, ordenado por data de alteração.
     */
    @Query("SELECT h FROM HistoricoStatusOS h " +
           "WHERE h.oficina.id = :oficinaId " +
           "AND h.ordemServico.id = :ordemServicoId " +
           "ORDER BY h.dataAlteracao ASC")
    List<HistoricoStatusOS> findByOrdemServicoId(
            @Param("oficinaId") UUID oficinaId,
            @Param("ordemServicoId") UUID ordemServicoId
    );

    /**
     * Busca histórico de uma OS pelo número da OS.
     */
    @Query("SELECT h FROM HistoricoStatusOS h " +
           "JOIN h.ordemServico os " +
           "WHERE h.oficina.id = :oficinaId " +
           "AND os.numero = :numero " +
           "ORDER BY h.dataAlteracao ASC")
    List<HistoricoStatusOS> findByOrdemServicoNumero(
            @Param("oficinaId") UUID oficinaId,
            @Param("numero") Long numero
    );

    /**
     * Conta total de mudanças de status de uma OS.
     */
    @Query("SELECT COUNT(h) FROM HistoricoStatusOS h " +
           "WHERE h.oficina.id = :oficinaId " +
           "AND h.ordemServico.id = :ordemServicoId")
    long countByOrdemServicoId(
            @Param("oficinaId") UUID oficinaId,
            @Param("ordemServicoId") UUID ordemServicoId
    );
}
