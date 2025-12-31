package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.PagamentoOnline;
import com.pitstop.financeiro.domain.StatusPagamentoOnline;
import com.pitstop.financeiro.domain.TipoGateway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoOnlineRepository extends JpaRepository<PagamentoOnline, UUID> {

    /**
     * Busca pagamentos online de uma oficina.
     */
    Page<PagamentoOnline> findByOficinaIdOrderByCreatedAtDesc(UUID oficinaId, Pageable pageable);

    /**
     * Busca pagamentos online de uma ordem de serviço.
     */
    List<PagamentoOnline> findByOrdemServicoIdOrderByCreatedAtDesc(UUID ordemServicoId);

    /**
     * Busca pelo ID da preferência (checkout).
     */
    Optional<PagamentoOnline> findByPreferenceId(String preferenceId);

    /**
     * Busca pelo ID externo do pagamento no gateway.
     */
    Optional<PagamentoOnline> findByIdExterno(String idExterno);

    /**
     * Busca pelo ID da cobrança.
     */
    Optional<PagamentoOnline> findByIdCobranca(String idCobranca);

    /**
     * Busca pagamentos pendentes.
     */
    List<PagamentoOnline> findByStatusAndOficinaId(StatusPagamentoOnline status, UUID oficinaId);

    /**
     * Busca pagamentos pendentes que estão prestes a expirar.
     */
    @Query("""
        SELECT p FROM PagamentoOnline p
        WHERE p.status = 'PENDENTE'
          AND p.dataExpiracao IS NOT NULL
          AND p.dataExpiracao < :dataLimite
        """)
    List<PagamentoOnline> findPendentesExpirando(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca o último pagamento online de uma OS.
     */
    Optional<PagamentoOnline> findFirstByOrdemServicoIdOrderByCreatedAtDesc(UUID ordemServicoId);

    /**
     * Busca pagamentos aprovados por período.
     */
    @Query("""
        SELECT p FROM PagamentoOnline p
        WHERE p.oficina.id = :oficinaId
          AND p.status = 'APROVADO'
          AND p.dataAprovacao BETWEEN :inicio AND :fim
        ORDER BY p.dataAprovacao DESC
        """)
    List<PagamentoOnline> findAprovadosPorPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    /**
     * Conta pagamentos por status.
     */
    long countByOficinaIdAndStatus(UUID oficinaId, StatusPagamentoOnline status);

    /**
     * Soma valores de pagamentos aprovados por período.
     */
    @Query("""
        SELECT COALESCE(SUM(p.valor), 0) FROM PagamentoOnline p
        WHERE p.oficina.id = :oficinaId
          AND p.status = 'APROVADO'
          AND p.dataAprovacao BETWEEN :inicio AND :fim
        """)
    java.math.BigDecimal somarAprovadosPorPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    // ========== Métodos com validação de tenant ==========

    /**
     * Busca pagamento por ID com validação de tenant.
     * IMPORTANTE: Sempre usar este método ao invés de findById() para garantir isolamento multi-tenant.
     */
    @Query("SELECT p FROM PagamentoOnline p WHERE p.oficina.id = :oficinaId AND p.id = :id")
    Optional<PagamentoOnline> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca pagamentos de uma OS com validação de tenant.
     */
    @Query("""
        SELECT p FROM PagamentoOnline p
        WHERE p.oficina.id = :oficinaId
          AND p.ordemServicoId = :ordemServicoId
        ORDER BY p.createdAt DESC
        """)
    List<PagamentoOnline> findByOficinaIdAndOrdemServicoId(
        @Param("oficinaId") UUID oficinaId,
        @Param("ordemServicoId") UUID ordemServicoId
    );

    /**
     * Busca pelo ID da preferência com validação de tenant.
     */
    @Query("SELECT p FROM PagamentoOnline p WHERE p.oficina.id = :oficinaId AND p.preferenceId = :preferenceId")
    Optional<PagamentoOnline> findByOficinaIdAndPreferenceId(
        @Param("oficinaId") UUID oficinaId,
        @Param("preferenceId") String preferenceId
    );

    /**
     * Busca pelo ID externo com validação de tenant.
     */
    @Query("SELECT p FROM PagamentoOnline p WHERE p.oficina.id = :oficinaId AND p.idExterno = :idExterno")
    Optional<PagamentoOnline> findByOficinaIdAndIdExterno(
        @Param("oficinaId") UUID oficinaId,
        @Param("idExterno") String idExterno
    );
}
