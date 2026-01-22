package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.StatusConciliacao;
import com.pitstop.financeiro.domain.TipoTransacaoBancaria;
import com.pitstop.financeiro.domain.TransacaoExtrato;
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
 * Repositório para transações do extrato.
 */
@Repository
public interface TransacaoExtratoRepository extends JpaRepository<TransacaoExtrato, UUID> {

    /**
     * Busca transações por extrato.
     */
    @Query("SELECT t FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId ORDER BY t.dataTransacao DESC")
    List<TransacaoExtrato> findByExtratoId(@Param("extratoId") UUID extratoId);

    /**
     * Busca transações por extrato com paginação.
     */
    @Query("SELECT t FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId ORDER BY t.dataTransacao DESC")
    Page<TransacaoExtrato> findByExtratoId(@Param("extratoId") UUID extratoId, Pageable pageable);

    /**
     * Busca transação por ID e oficina.
     */
    @Query("SELECT t FROM TransacaoExtrato t WHERE t.id = :id AND t.oficina.id = :oficinaId")
    Optional<TransacaoExtrato> findByIdAndOficinaId(@Param("id") UUID id, @Param("oficinaId") UUID oficinaId);

    /**
     * Busca transações não conciliadas por extrato.
     */
    @Query("SELECT t FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId AND t.status = 'NAO_CONCILIADA' ORDER BY t.dataTransacao DESC")
    List<TransacaoExtrato> findNaoConciliadasByExtratoId(@Param("extratoId") UUID extratoId);

    /**
     * Busca transações por status.
     */
    @Query("SELECT t FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId AND t.status = :status ORDER BY t.dataTransacao DESC")
    List<TransacaoExtrato> findByExtratoIdAndStatus(@Param("extratoId") UUID extratoId, @Param("status") StatusConciliacao status);

    /**
     * Conta transações por status no extrato.
     */
    @Query("SELECT COUNT(t) FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId AND t.status = :status")
    long countByExtratoIdAndStatus(@Param("extratoId") UUID extratoId, @Param("status") StatusConciliacao status);

    /**
     * Busca transações por valor aproximado para matching.
     */
    @Query("""
        SELECT t FROM TransacaoExtrato t
        WHERE t.oficina.id = :oficinaId
        AND t.status = 'NAO_CONCILIADA'
        AND t.tipo = :tipo
        AND t.valor BETWEEN :valorMin AND :valorMax
        AND t.dataTransacao BETWEEN :dataInicio AND :dataFim
        ORDER BY ABS(t.valor - :valorRef)
        """)
    List<TransacaoExtrato> findParaMatching(
        @Param("oficinaId") UUID oficinaId,
        @Param("tipo") TipoTransacaoBancaria tipo,
        @Param("valorMin") BigDecimal valorMin,
        @Param("valorMax") BigDecimal valorMax,
        @Param("valorRef") BigDecimal valorRef,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Verifica se já existe transação com mesmo identificador no extrato.
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId AND t.identificadorBanco = :identificador")
    boolean existsByExtratoIdAndIdentificadorBanco(@Param("extratoId") UUID extratoId, @Param("identificador") String identificador);

    /**
     * Soma valores de créditos não conciliados.
     */
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId AND t.tipo = 'CREDITO' AND t.status = 'NAO_CONCILIADA'")
    BigDecimal sumCreditosNaoConciliados(@Param("extratoId") UUID extratoId);

    /**
     * Soma valores de débitos não conciliados.
     */
    @Query("SELECT COALESCE(SUM(t.valor), 0) FROM TransacaoExtrato t WHERE t.extrato.id = :extratoId AND t.tipo = 'DEBITO' AND t.status = 'NAO_CONCILIADA'")
    BigDecimal sumDebitosNaoConciliados(@Param("extratoId") UUID extratoId);
}
