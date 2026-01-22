package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.FaturaAssinatura;
import com.pitstop.financeiro.domain.StatusFaturaAssinatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para faturas de assinatura.
 */
@Repository
public interface FaturaAssinaturaRepository extends JpaRepository<FaturaAssinatura, UUID> {

    /**
     * Busca faturas por oficina.
     */
    @Query("SELECT f FROM FaturaAssinatura f JOIN FETCH f.assinatura a JOIN FETCH a.cliente WHERE f.oficina.id = :oficinaId ORDER BY f.dataVencimento DESC")
    Page<FaturaAssinatura> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Busca fatura por ID e oficina.
     */
    @Query("SELECT f FROM FaturaAssinatura f JOIN FETCH f.assinatura a JOIN FETCH a.cliente JOIN FETCH a.plano WHERE f.id = :id AND f.oficina.id = :oficinaId")
    Optional<FaturaAssinatura> findByIdAndOficinaId(@Param("id") UUID id, @Param("oficinaId") UUID oficinaId);

    /**
     * Busca faturas por assinatura.
     */
    @Query("SELECT f FROM FaturaAssinatura f WHERE f.assinatura.id = :assinaturaId ORDER BY f.mesReferencia DESC")
    List<FaturaAssinatura> findByAssinaturaId(@Param("assinaturaId") UUID assinaturaId);

    /**
     * Busca faturas por status.
     */
    @Query("SELECT f FROM FaturaAssinatura f JOIN FETCH f.assinatura a JOIN FETCH a.cliente WHERE f.oficina.id = :oficinaId AND f.status = :status ORDER BY f.dataVencimento")
    List<FaturaAssinatura> findByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusFaturaAssinatura status);

    /**
     * Busca faturas vencidas (para marcar e notificar).
     */
    @Query("SELECT f FROM FaturaAssinatura f JOIN FETCH f.assinatura a JOIN FETCH a.cliente WHERE f.status = 'PENDENTE' AND f.dataVencimento < :data")
    List<FaturaAssinatura> findVencidas(@Param("data") LocalDate data);

    /**
     * Verifica se existe fatura para o mês.
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FaturaAssinatura f WHERE f.assinatura.id = :assinaturaId AND f.mesReferencia = :mesReferencia")
    boolean existsByAssinaturaIdAndMesReferencia(@Param("assinaturaId") UUID assinaturaId, @Param("mesReferencia") LocalDate mesReferencia);

    /**
     * Conta faturas por status.
     */
    @Query("SELECT COUNT(f) FROM FaturaAssinatura f WHERE f.oficina.id = :oficinaId AND f.status = :status")
    long countByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusFaturaAssinatura status);

    /**
     * Soma valor de faturas pendentes.
     */
    @Query("SELECT COALESCE(SUM(f.valor), 0) FROM FaturaAssinatura f WHERE f.oficina.id = :oficinaId AND f.status IN ('PENDENTE', 'VENCIDA')")
    BigDecimal sumPendentesByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Soma valor de faturas pagas no período.
     */
    @Query("SELECT COALESCE(SUM(f.valor), 0) FROM FaturaAssinatura f WHERE f.oficina.id = :oficinaId AND f.status = 'PAGA' AND CAST(f.dataPagamento AS localdate) BETWEEN :inicio AND :fim")
    BigDecimal sumPagasByOficinaIdAndPeriodo(@Param("oficinaId") UUID oficinaId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /**
     * Busca por ID do pagamento no gateway.
     */
    @Query("SELECT f FROM FaturaAssinatura f WHERE f.gatewayPaymentId = :paymentId")
    Optional<FaturaAssinatura> findByGatewayPaymentId(@Param("paymentId") String paymentId);

    /**
     * Busca próximo número de fatura.
     */
    @Query(value = "SELECT nextval('seq_fatura_assinatura_numero')", nativeQuery = true)
    Long getNextNumeroFatura();

    /**
     * Busca faturas por filtros.
     */
    @Query("""
        SELECT f FROM FaturaAssinatura f
        JOIN FETCH f.assinatura a
        JOIN FETCH a.cliente c
        WHERE f.oficina.id = :oficinaId
        AND (:status IS NULL OR f.status = :status)
        AND (:assinaturaId IS NULL OR a.id = :assinaturaId)
        AND (:mesReferencia IS NULL OR f.mesReferencia = :mesReferencia)
        ORDER BY f.dataVencimento DESC
        """)
    Page<FaturaAssinatura> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusFaturaAssinatura status,
        @Param("assinaturaId") UUID assinaturaId,
        @Param("mesReferencia") LocalDate mesReferencia,
        Pageable pageable
    );
}
