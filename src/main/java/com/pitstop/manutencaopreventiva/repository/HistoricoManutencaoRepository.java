package com.pitstop.manutencaopreventiva.repository;

import com.pitstop.manutencaopreventiva.domain.HistoricoManutencaoPreventiva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricoManutencaoRepository extends JpaRepository<HistoricoManutencaoPreventiva, UUID> {

    /**
     * Lista histórico de um plano.
     */
    @Query("SELECT h FROM HistoricoManutencaoPreventiva h WHERE h.plano.id = :planoId ORDER BY h.dataExecucao DESC")
    List<HistoricoManutencaoPreventiva> findByPlanoId(@Param("planoId") UUID planoId);

    /**
     * Lista histórico de um veículo.
     */
    @Query("""
        SELECT h FROM HistoricoManutencaoPreventiva h
        LEFT JOIN FETCH h.plano
        WHERE h.veiculo.id = :veiculoId
        ORDER BY h.dataExecucao DESC
        """)
    List<HistoricoManutencaoPreventiva> findByVeiculoId(@Param("veiculoId") UUID veiculoId);

    /**
     * Busca histórico com filtros e paginação.
     */
    @Query(value = """
        SELECT h FROM HistoricoManutencaoPreventiva h
        LEFT JOIN FETCH h.veiculo v
        LEFT JOIN FETCH h.plano p
        WHERE h.oficina.id = :oficinaId
        AND (:veiculoId IS NULL OR h.veiculo.id = :veiculoId)
        AND (:planoId IS NULL OR h.plano.id = :planoId)
        AND (:dataInicio IS NULL OR h.dataExecucao >= :dataInicio)
        AND (:dataFim IS NULL OR h.dataExecucao <= :dataFim)
        ORDER BY h.dataExecucao DESC
        """,
        countQuery = """
        SELECT COUNT(h) FROM HistoricoManutencaoPreventiva h
        WHERE h.oficina.id = :oficinaId
        AND (:veiculoId IS NULL OR h.veiculo.id = :veiculoId)
        AND (:planoId IS NULL OR h.plano.id = :planoId)
        AND (:dataInicio IS NULL OR h.dataExecucao >= :dataInicio)
        AND (:dataFim IS NULL OR h.dataExecucao <= :dataFim)
        """)
    Page<HistoricoManutencaoPreventiva> findByFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("veiculoId") UUID veiculoId,
        @Param("planoId") UUID planoId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Conta manutenções realizadas no período.
     */
    @Query("""
        SELECT COUNT(h) FROM HistoricoManutencaoPreventiva h
        WHERE h.oficina.id = :oficinaId
        AND h.dataExecucao BETWEEN :dataInicio AND :dataFim
        """)
    long countByPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Conta manutenções por tipo no período.
     */
    @Query("""
        SELECT h.tipoManutencao, COUNT(h) FROM HistoricoManutencaoPreventiva h
        WHERE h.oficina.id = :oficinaId
        AND h.dataExecucao BETWEEN :dataInicio AND :dataFim
        GROUP BY h.tipoManutencao
        ORDER BY COUNT(h) DESC
        """)
    List<Object[]> countByTipoManutencao(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Última execução de um plano.
     */
    @Query("SELECT h FROM HistoricoManutencaoPreventiva h WHERE h.plano.id = :planoId ORDER BY h.dataExecucao DESC LIMIT 1")
    HistoricoManutencaoPreventiva findUltimaExecucao(@Param("planoId") UUID planoId);
}
