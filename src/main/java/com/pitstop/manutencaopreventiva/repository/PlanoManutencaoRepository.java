package com.pitstop.manutencaopreventiva.repository;

import com.pitstop.manutencaopreventiva.domain.PlanoManutencaoPreventiva;
import com.pitstop.manutencaopreventiva.domain.StatusPlanoManutencao;
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
public interface PlanoManutencaoRepository extends JpaRepository<PlanoManutencaoPreventiva, UUID> {

    /**
     * Lista planos ativos de uma oficina.
     */
    @Query("SELECT p FROM PlanoManutencaoPreventiva p WHERE p.oficina.id = :oficinaId AND p.ativo = true ORDER BY p.proximaPrevisaoData")
    List<PlanoManutencaoPreventiva> findByOficinaIdAndAtivoTrue(@Param("oficinaId") UUID oficinaId);

    /**
     * Lista planos de um veículo.
     */
    @Query("""
        SELECT p FROM PlanoManutencaoPreventiva p
        LEFT JOIN FETCH p.veiculo
        WHERE p.veiculo.id = :veiculoId AND p.ativo = true
        ORDER BY p.proximaPrevisaoData
        """)
    List<PlanoManutencaoPreventiva> findByVeiculoIdAndAtivoTrue(@Param("veiculoId") UUID veiculoId);

    /**
     * Busca planos vencidos (próxima data antes de hoje).
     */
    @Query("""
        SELECT p FROM PlanoManutencaoPreventiva p
        LEFT JOIN FETCH p.veiculo
        WHERE p.oficina.id = :oficinaId
        AND p.status = 'ATIVO'
        AND p.ativo = true
        AND p.proximaPrevisaoData < :hoje
        """)
    List<PlanoManutencaoPreventiva> findPlanosVencidosPorData(
        @Param("oficinaId") UUID oficinaId,
        @Param("hoje") LocalDate hoje
    );

    /**
     * Busca planos próximos de vencer por data.
     */
    @Query("""
        SELECT p FROM PlanoManutencaoPreventiva p
        LEFT JOIN FETCH p.veiculo v
        LEFT JOIN FETCH p.oficina
        WHERE p.oficina.id = :oficinaId
        AND p.status = 'ATIVO'
        AND p.ativo = true
        AND p.proximaPrevisaoData BETWEEN :hoje AND :dataLimite
        """)
    List<PlanoManutencaoPreventiva> findPlanosProximosAVencerPorData(
        @Param("oficinaId") UUID oficinaId,
        @Param("hoje") LocalDate hoje,
        @Param("dataLimite") LocalDate dataLimite
    );

    /**
     * Busca planos para alertar (considerando antecedência).
     */
    @Query("""
        SELECT p FROM PlanoManutencaoPreventiva p
        LEFT JOIN FETCH p.veiculo v
        LEFT JOIN FETCH p.oficina o
        WHERE p.status = 'ATIVO'
        AND p.ativo = true
        AND (
            (p.proximaPrevisaoData IS NOT NULL AND p.proximaPrevisaoData <= :dataAlerta)
            OR (p.proximaPrevisaoKm IS NOT NULL AND v.quilometragem >= (p.proximaPrevisaoKm - p.antecedenciaKm))
        )
        AND (p.ultimoAlertaEnviadoEm IS NULL OR p.ultimoAlertaEnviadoEm < :dataUltimoAlerta)
        """)
    List<PlanoManutencaoPreventiva> findPlanosParaAlertar(
        @Param("dataAlerta") LocalDate dataAlerta,
        @Param("dataUltimoAlerta") java.time.LocalDateTime dataUltimoAlerta
    );

    /**
     * Busca com filtros e paginação.
     */
    @Query(value = """
        SELECT p FROM PlanoManutencaoPreventiva p
        LEFT JOIN FETCH p.veiculo v
        WHERE p.oficina.id = :oficinaId
        AND p.ativo = true
        AND (:veiculoId IS NULL OR p.veiculo.id = :veiculoId)
        AND (:status IS NULL OR p.status = :status)
        AND (:tipoManutencao IS NULL OR p.tipoManutencao = :tipoManutencao)
        AND (:busca IS NULL OR :busca = '' OR
            LOWER(p.nome) LIKE CONCAT('%', LOWER(:busca), '%')
            OR LOWER(v.placa) LIKE CONCAT('%', LOWER(:busca), '%')
            OR LOWER(v.marca) LIKE CONCAT('%', LOWER(:busca), '%')
            OR LOWER(v.modelo) LIKE CONCAT('%', LOWER(:busca), '%'))
        ORDER BY p.proximaPrevisaoData NULLS LAST
        """,
        countQuery = """
        SELECT COUNT(p) FROM PlanoManutencaoPreventiva p
        LEFT JOIN p.veiculo v
        WHERE p.oficina.id = :oficinaId
        AND p.ativo = true
        AND (:veiculoId IS NULL OR p.veiculo.id = :veiculoId)
        AND (:status IS NULL OR p.status = :status)
        AND (:tipoManutencao IS NULL OR p.tipoManutencao = :tipoManutencao)
        AND (:busca IS NULL OR :busca = '' OR
            LOWER(p.nome) LIKE CONCAT('%', LOWER(:busca), '%')
            OR LOWER(v.placa) LIKE CONCAT('%', LOWER(:busca), '%')
            OR LOWER(v.marca) LIKE CONCAT('%', LOWER(:busca), '%')
            OR LOWER(v.modelo) LIKE CONCAT('%', LOWER(:busca), '%'))
        """)
    Page<PlanoManutencaoPreventiva> findByFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("veiculoId") UUID veiculoId,
        @Param("status") StatusPlanoManutencao status,
        @Param("tipoManutencao") String tipoManutencao,
        @Param("busca") String busca,
        Pageable pageable
    );

    /**
     * Conta planos por status.
     */
    @Query("SELECT p.status, COUNT(p) FROM PlanoManutencaoPreventiva p WHERE p.oficina.id = :oficinaId AND p.ativo = true GROUP BY p.status")
    List<Object[]> countByStatus(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta planos ativos.
     */
    @Query("SELECT COUNT(p) FROM PlanoManutencaoPreventiva p WHERE p.oficina.id = :oficinaId AND p.status = 'ATIVO' AND p.ativo = true")
    long countAtivos(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta planos vencidos.
     */
    @Query("""
        SELECT COUNT(p) FROM PlanoManutencaoPreventiva p
        WHERE p.oficina.id = :oficinaId
        AND p.status = 'ATIVO'
        AND p.ativo = true
        AND p.proximaPrevisaoData < :hoje
        """)
    long countVencidos(@Param("oficinaId") UUID oficinaId, @Param("hoje") LocalDate hoje);
}
