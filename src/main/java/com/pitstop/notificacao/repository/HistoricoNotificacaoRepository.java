package com.pitstop.notificacao.repository;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
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
 * Repository para historico de notificacoes.
 *
 * @author PitStop Team
 */
@Repository
public interface HistoricoNotificacaoRepository extends JpaRepository<HistoricoNotificacao, UUID> {

    // ===== BUSCAS POR OFICINA =====

    /**
     * Lista historico de uma oficina com paginacao.
     *
     * @param oficinaId ID da oficina
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    Page<HistoricoNotificacao> findByOficinaIdOrderByCreatedAtDesc(UUID oficinaId, Pageable pageable);

    /**
     * Lista historico de uma oficina por status.
     *
     * @param oficinaId ID da oficina
     * @param status Status desejado
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    Page<HistoricoNotificacao> findByOficinaIdAndStatusOrderByCreatedAtDesc(
        UUID oficinaId,
        StatusNotificacao status,
        Pageable pageable
    );

    /**
     * Lista historico de uma oficina por evento.
     *
     * @param oficinaId ID da oficina
     * @param evento Evento
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    Page<HistoricoNotificacao> findByOficinaIdAndEventoOrderByCreatedAtDesc(
        UUID oficinaId,
        EventoNotificacao evento,
        Pageable pageable
    );

    /**
     * Lista historico de uma oficina por canal.
     *
     * @param oficinaId ID da oficina
     * @param tipoNotificacao Canal
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    Page<HistoricoNotificacao> findByOficinaIdAndTipoNotificacaoOrderByCreatedAtDesc(
        UUID oficinaId,
        TipoNotificacao tipoNotificacao,
        Pageable pageable
    );

    // ===== BUSCA COM FILTROS COMBINADOS =====

    /**
     * Lista historico com filtros combinados (todos opcionais).
     *
     * @param oficinaId ID da oficina
     * @param tipoNotificacao Canal (opcional)
     * @param status Status (opcional)
     * @param evento Evento (opcional)
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Query("""
        SELECT h FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND (:tipoNotificacao IS NULL OR h.tipoNotificacao = :tipoNotificacao)
          AND (:status IS NULL OR h.status = :status)
          AND (:evento IS NULL OR h.evento = :evento)
        ORDER BY h.createdAt DESC
        """)
    Page<HistoricoNotificacao> findWithFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("tipoNotificacao") TipoNotificacao tipoNotificacao,
        @Param("status") StatusNotificacao status,
        @Param("evento") EventoNotificacao evento,
        Pageable pageable
    );

    // ===== BUSCAS POR ORDEM DE SERVICO =====

    /**
     * Lista todas as notificacoes de uma OS com validação de tenant.
     * IMPORTANTE: Sempre usar este método para garantir isolamento multi-tenant.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return Lista de notificacoes
     */
    @Query("""
        SELECT h FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.ordemServicoId = :ordemServicoId
        ORDER BY h.createdAt DESC
        """)
    List<HistoricoNotificacao> findByOficinaIdAndOrdemServicoId(
        @Param("oficinaId") UUID oficinaId,
        @Param("ordemServicoId") UUID ordemServicoId
    );

    // ===== BUSCAS POR CLIENTE =====

    /**
     * Lista todas as notificacoes enviadas para um cliente com validação de tenant.
     * IMPORTANTE: Sempre usar este método para garantir isolamento multi-tenant.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param clienteId ID do cliente
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Query("""
        SELECT h FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.clienteId = :clienteId
        ORDER BY h.createdAt DESC
        """)
    Page<HistoricoNotificacao> findByOficinaIdAndClienteId(
        @Param("oficinaId") UUID oficinaId,
        @Param("clienteId") UUID clienteId,
        Pageable pageable
    );

    // ===== BUSCAS POR DESTINATARIO =====

    /**
     * Lista historico por destinatario (email ou telefone) com validação de tenant.
     * IMPORTANTE: Sempre usar este método para garantir isolamento multi-tenant.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param destinatario Email ou telefone
     * @param pageable Paginacao
     * @return Pagina de historico
     */
    @Query("""
        SELECT h FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.destinatario = :destinatario
        ORDER BY h.createdAt DESC
        """)
    Page<HistoricoNotificacao> findByOficinaIdAndDestinatario(
        @Param("oficinaId") UUID oficinaId,
        @Param("destinatario") String destinatario,
        Pageable pageable
    );

    // ===== PENDENTES E REENVIO =====

    /**
     * Lista notificacoes pendentes de envio.
     *
     * @param oficinaId ID da oficina
     * @return Lista de pendentes
     */
    List<HistoricoNotificacao> findByOficinaIdAndStatusOrderByCreatedAtAsc(
        UUID oficinaId,
        StatusNotificacao status
    );

    /**
     * Lista notificacoes que falharam e podem ser retentadas.
     *
     * @param oficinaId ID da oficina
     * @param maxTentativas Maximo de tentativas
     * @return Lista para reenvio
     */
    @Query("""
        SELECT h FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.status = 'FALHA'
          AND h.tentativas < :maxTentativas
        ORDER BY h.createdAt ASC
        """)
    List<HistoricoNotificacao> findParaReenvio(
        @Param("oficinaId") UUID oficinaId,
        @Param("maxTentativas") int maxTentativas
    );

    /**
     * Lista notificacoes agendadas prontas para envio.
     *
     * @param agora Data/hora atual
     * @return Lista de agendadas
     */
    @Query("""
        SELECT h FROM HistoricoNotificacao h
        WHERE h.status = 'AGENDADO'
          AND h.dataAgendada <= :agora
        ORDER BY h.dataAgendada ASC
        """)
    List<HistoricoNotificacao> findAgendadasProntasParaEnvio(@Param("agora") LocalDateTime agora);

    // ===== METRICAS =====

    /**
     * Conta notificacoes por status em um periodo.
     *
     * @param oficinaId ID da oficina
     * @param status Status
     * @param inicio Data inicio
     * @param fim Data fim
     * @return Quantidade
     */
    @Query("""
        SELECT COUNT(h) FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.status = :status
          AND h.createdAt BETWEEN :inicio AND :fim
        """)
    long countByOficinaAndStatusAndPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusNotificacao status,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    /**
     * Conta notificacoes por canal em um periodo.
     *
     * @param oficinaId ID da oficina
     * @param tipoNotificacao Canal
     * @param inicio Data inicio
     * @param fim Data fim
     * @return Quantidade
     */
    @Query("""
        SELECT COUNT(h) FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.tipoNotificacao = :tipoNotificacao
          AND h.createdAt BETWEEN :inicio AND :fim
        """)
    long countByOficinaAndCanalAndPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("tipoNotificacao") TipoNotificacao tipoNotificacao,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    /**
     * Conta notificacoes por evento em um periodo.
     *
     * @param oficinaId ID da oficina
     * @param evento Evento
     * @param inicio Data inicio
     * @param fim Data fim
     * @return Quantidade
     */
    @Query("""
        SELECT COUNT(h) FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.evento = :evento
          AND h.createdAt BETWEEN :inicio AND :fim
        """)
    long countByOficinaAndEventoAndPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("evento") EventoNotificacao evento,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    /**
     * Conta total de notificacoes de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return Quantidade total
     */
    long countByOficinaId(UUID oficinaId);

    /**
     * Conta notificacoes enviadas com sucesso de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return Quantidade enviadas
     */
    @Query("""
        SELECT COUNT(h) FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.status IN ('ENVIADO', 'ENTREGUE', 'LIDO')
        """)
    long countEnviadasByOficina(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta notificacoes que falharam de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return Quantidade falhadas
     */
    long countByOficinaIdAndStatus(UUID oficinaId, StatusNotificacao status);

    // ===== BUSCA POR ID EXTERNO =====

    /**
     * Busca notificacao pelo ID externo (para callbacks de APIs externas).
     * NOTA: Este método é usado para callbacks de APIs externas (Twilio, Evolution API)
     * que não possuem contexto de tenant. O idExterno é único globalmente.
     *
     * @param idExterno Message ID da API
     * @return Notificacao se encontrada
     */
    java.util.Optional<HistoricoNotificacao> findByIdExterno(String idExterno);

    /**
     * Busca notificacao pelo ID externo com validação de tenant.
     * Use este método quando houver contexto de tenant disponível.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param idExterno Message ID da API
     * @return Notificacao se encontrada
     */
    @Query("SELECT h FROM HistoricoNotificacao h WHERE h.oficinaId = :oficinaId AND h.idExterno = :idExterno")
    java.util.Optional<HistoricoNotificacao> findByOficinaIdAndIdExterno(
        @Param("oficinaId") UUID oficinaId,
        @Param("idExterno") String idExterno
    );

    /**
     * Busca notificacao por ID com validação de tenant.
     * IMPORTANTE: Sempre usar este método ao invés de findById() para garantir isolamento multi-tenant.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID da notificacao
     * @return Notificacao se encontrada
     */
    @Query("SELECT h FROM HistoricoNotificacao h WHERE h.oficinaId = :oficinaId AND h.id = :id")
    java.util.Optional<HistoricoNotificacao> findByOficinaIdAndId(
        @Param("oficinaId") UUID oficinaId,
        @Param("id") UUID id
    );

    // ===== LIMPEZA =====

    /**
     * Remove historico antigo para uma oficina.
     *
     * @param oficinaId ID da oficina
     * @param dataLimite Data limite (remove antes desta data)
     * @return Quantidade removida
     */
    @Modifying
    @Query("""
        DELETE FROM HistoricoNotificacao h
        WHERE h.oficinaId = :oficinaId
          AND h.createdAt < :dataLimite
        """)
    int deleteHistoricoAntigo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataLimite") LocalDateTime dataLimite
    );
}
