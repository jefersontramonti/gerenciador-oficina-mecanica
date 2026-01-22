package com.pitstop.webhook.repository;

import com.pitstop.webhook.domain.StatusWebhookLog;
import com.pitstop.webhook.domain.TipoEventoWebhook;
import com.pitstop.webhook.domain.WebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository para logs de webhooks.
 *
 * @author PitStop Team
 */
@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    /**
     * Lista logs de uma oficina com paginação.
     */
    Page<WebhookLog> findByOficinaIdOrderByCreatedAtDesc(UUID oficinaId, Pageable pageable);

    /**
     * Lista logs de um webhook específico.
     */
    Page<WebhookLog> findByWebhookConfigIdOrderByCreatedAtDesc(UUID webhookConfigId, Pageable pageable);

    /**
     * Lista logs de uma oficina filtrado por status.
     */
    Page<WebhookLog> findByOficinaIdAndStatusOrderByCreatedAtDesc(
        UUID oficinaId,
        StatusWebhookLog status,
        Pageable pageable
    );

    /**
     * Lista logs de uma oficina filtrado por evento.
     */
    Page<WebhookLog> findByOficinaIdAndEventoOrderByCreatedAtDesc(
        UUID oficinaId,
        TipoEventoWebhook evento,
        Pageable pageable
    );

    /**
     * Busca logs pendentes de retry.
     */
    @Query("""
        SELECT l FROM WebhookLog l
        WHERE l.status = 'AGUARDANDO_RETRY'
          AND l.proximaTentativa <= :agora
        ORDER BY l.proximaTentativa ASC
        """)
    List<WebhookLog> findPendingRetries(@Param("agora") LocalDateTime agora);

    /**
     * Busca logs pendentes de retry de uma oficina.
     */
    @Query("""
        SELECT l FROM WebhookLog l
        WHERE l.oficinaId = :oficinaId
          AND l.status = 'AGUARDANDO_RETRY'
          AND l.proximaTentativa <= :agora
        ORDER BY l.proximaTentativa ASC
        """)
    List<WebhookLog> findPendingRetriesByOficinaId(
        @Param("oficinaId") UUID oficinaId,
        @Param("agora") LocalDateTime agora
    );

    /**
     * Conta logs por status para uma oficina.
     */
    long countByOficinaIdAndStatus(UUID oficinaId, StatusWebhookLog status);

    /**
     * Conta logs de sucesso nas últimas 24h para uma oficina.
     */
    @Query("""
        SELECT COUNT(l) FROM WebhookLog l
        WHERE l.oficinaId = :oficinaId
          AND l.status = 'SUCESSO'
          AND l.createdAt >= :desde
        """)
    long countSuccessSince(@Param("oficinaId") UUID oficinaId, @Param("desde") LocalDateTime desde);

    /**
     * Conta logs de falha nas últimas 24h para uma oficina.
     */
    @Query("""
        SELECT COUNT(l) FROM WebhookLog l
        WHERE l.oficinaId = :oficinaId
          AND l.status IN ('FALHA', 'ESGOTADO')
          AND l.createdAt >= :desde
        """)
    long countFailuresSince(@Param("oficinaId") UUID oficinaId, @Param("desde") LocalDateTime desde);

    /**
     * Tempo médio de resposta nas últimas 24h.
     */
    @Query("""
        SELECT AVG(l.tempoRespostaMs) FROM WebhookLog l
        WHERE l.oficinaId = :oficinaId
          AND l.status = 'SUCESSO'
          AND l.createdAt >= :desde
        """)
    Double avgResponseTimeSince(@Param("oficinaId") UUID oficinaId, @Param("desde") LocalDateTime desde);

    /**
     * Lista últimos logs de um webhook (para preview).
     */
    List<WebhookLog> findTop10ByWebhookConfigIdOrderByCreatedAtDesc(UUID webhookConfigId);

    /**
     * Remove logs antigos (limpeza).
     */
    @Modifying
    @Query("""
        DELETE FROM WebhookLog l
        WHERE l.createdAt < :antes
        """)
    int deleteOlderThan(@Param("antes") LocalDateTime antes);

    /**
     * Remove logs antigos de uma oficina.
     */
    @Modifying
    @Query("""
        DELETE FROM WebhookLog l
        WHERE l.oficinaId = :oficinaId
          AND l.createdAt < :antes
        """)
    int deleteByOficinaIdOlderThan(
        @Param("oficinaId") UUID oficinaId,
        @Param("antes") LocalDateTime antes
    );
}
