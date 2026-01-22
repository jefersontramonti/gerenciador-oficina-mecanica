package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.Assinatura;
import com.pitstop.financeiro.domain.StatusAssinatura;
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
 * Repositório para assinaturas de clientes.
 */
@Repository
public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    /**
     * Busca assinaturas por oficina.
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.cliente JOIN FETCH a.plano WHERE a.oficina.id = :oficinaId ORDER BY a.createdAt DESC")
    Page<Assinatura> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Busca assinatura por ID e oficina.
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.cliente JOIN FETCH a.plano WHERE a.id = :id AND a.oficina.id = :oficinaId")
    Optional<Assinatura> findByIdAndOficinaId(@Param("id") UUID id, @Param("oficinaId") UUID oficinaId);

    /**
     * Busca assinaturas por cliente.
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.plano WHERE a.cliente.id = :clienteId ORDER BY a.createdAt DESC")
    List<Assinatura> findByClienteId(@Param("clienteId") UUID clienteId);

    /**
     * Busca assinatura ativa do cliente.
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.plano WHERE a.cliente.id = :clienteId AND a.status = 'ATIVA'")
    Optional<Assinatura> findAtivaByClienteId(@Param("clienteId") UUID clienteId);

    /**
     * Busca assinaturas por status.
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.cliente JOIN FETCH a.plano WHERE a.oficina.id = :oficinaId AND a.status = :status")
    List<Assinatura> findByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusAssinatura status);

    /**
     * Busca assinaturas com vencimento próximo (para gerar faturas).
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.cliente JOIN FETCH a.plano WHERE a.oficina.id = :oficinaId AND a.status IN ('ATIVA', 'INADIMPLENTE') AND a.dataProximoVencimento <= :dataLimite")
    List<Assinatura> findParaGerarFatura(@Param("oficinaId") UUID oficinaId, @Param("dataLimite") LocalDate dataLimite);

    /**
     * Busca assinaturas ativas para cobrança.
     */
    @Query("SELECT a FROM Assinatura a JOIN FETCH a.cliente WHERE a.status IN ('ATIVA', 'INADIMPLENTE') AND a.dataProximoVencimento <= :data")
    List<Assinatura> findParaCobranca(@Param("data") LocalDate data);

    /**
     * Conta assinaturas por status na oficina.
     */
    @Query("SELECT COUNT(a) FROM Assinatura a WHERE a.oficina.id = :oficinaId AND a.status = :status")
    long countByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusAssinatura status);

    /**
     * Soma valor mensal de assinaturas ativas.
     */
    @Query("SELECT COALESCE(SUM(a.valorAtual), 0) FROM Assinatura a WHERE a.oficina.id = :oficinaId AND a.status = 'ATIVA'")
    BigDecimal sumReceitaRecorrenteByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Verifica se cliente já tem assinatura ativa.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Assinatura a WHERE a.cliente.id = :clienteId AND a.status IN ('ATIVA', 'PAUSADA')")
    boolean existsAssinaturaAtivaByClienteId(@Param("clienteId") UUID clienteId);

    /**
     * Busca assinaturas por filtros.
     */
    @Query("""
        SELECT a FROM Assinatura a
        JOIN FETCH a.cliente c
        JOIN FETCH a.plano p
        WHERE a.oficina.id = :oficinaId
        AND (:status IS NULL OR a.status = :status)
        AND (:planoId IS NULL OR p.id = :planoId)
        AND (COALESCE(:busca, '') = '' OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
        ORDER BY a.createdAt DESC
        """)
    Page<Assinatura> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusAssinatura status,
        @Param("planoId") UUID planoId,
        @Param("busca") String busca,
        Pageable pageable
    );
}
