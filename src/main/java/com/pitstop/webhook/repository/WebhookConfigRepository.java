package com.pitstop.webhook.repository;

import com.pitstop.webhook.domain.TipoEventoWebhook;
import com.pitstop.webhook.domain.WebhookConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para configurações de webhooks.
 *
 * @author PitStop Team
 */
@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {

    /**
     * Lista webhooks de uma oficina.
     */
    List<WebhookConfig> findByOficinaId(UUID oficinaId);

    /**
     * Lista webhooks de uma oficina com paginação.
     */
    Page<WebhookConfig> findByOficinaId(UUID oficinaId, Pageable pageable);

    /**
     * Lista webhooks ativos de uma oficina.
     */
    List<WebhookConfig> findByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Busca webhook por ID e oficina (segurança multi-tenant).
     */
    Optional<WebhookConfig> findByIdAndOficinaId(UUID id, UUID oficinaId);

    /**
     * Busca webhooks que devem ser disparados para um evento.
     */
    @Query("""
        SELECT w FROM WebhookConfig w
        JOIN w.eventos e
        WHERE w.oficinaId = :oficinaId
          AND w.ativo = true
          AND e = :evento
        """)
    List<WebhookConfig> findActiveByOficinaIdAndEvento(
        @Param("oficinaId") UUID oficinaId,
        @Param("evento") TipoEventoWebhook evento
    );

    /**
     * Busca todos webhooks ativos que escutam um evento (para broadcast).
     */
    @Query("""
        SELECT w FROM WebhookConfig w
        JOIN w.eventos e
        WHERE w.ativo = true
          AND e = :evento
        """)
    List<WebhookConfig> findAllActiveByEvento(@Param("evento") TipoEventoWebhook evento);

    /**
     * Conta webhooks ativos de uma oficina.
     */
    long countByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Conta total de webhooks de uma oficina.
     */
    long countByOficinaId(UUID oficinaId);

    /**
     * Lista webhooks com muitas falhas consecutivas (para alertas).
     */
    @Query("""
        SELECT w FROM WebhookConfig w
        WHERE w.oficinaId = :oficinaId
          AND w.falhasConsecutivas >= :minFalhas
        """)
    List<WebhookConfig> findByOficinaIdWithManyFailures(
        @Param("oficinaId") UUID oficinaId,
        @Param("minFalhas") int minFalhas
    );

    /**
     * Lista webhooks desativados automaticamente por falhas.
     */
    @Query("""
        SELECT w FROM WebhookConfig w
        WHERE w.oficinaId = :oficinaId
          AND w.ativo = false
          AND w.falhasConsecutivas >= 10
        """)
    List<WebhookConfig> findAutoDisabledByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Verifica se existe webhook com o nome na oficina.
     */
    boolean existsByOficinaIdAndNome(UUID oficinaId, String nome);

    /**
     * Verifica se existe webhook com a URL na oficina.
     */
    boolean existsByOficinaIdAndUrl(UUID oficinaId, String url);
}
