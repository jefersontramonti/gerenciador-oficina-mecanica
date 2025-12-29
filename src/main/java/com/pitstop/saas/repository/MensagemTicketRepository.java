package com.pitstop.saas.repository;

import com.pitstop.saas.domain.MensagemTicket;
import com.pitstop.saas.domain.TipoAutorMensagem;
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
public interface MensagemTicketRepository extends JpaRepository<MensagemTicket, UUID> {

    // Buscar mensagens por ticket
    List<MensagemTicket> findByTicketIdOrderByCriadoEmAsc(UUID ticketId);

    // Buscar mensagens visíveis (não internas) por ticket
    List<MensagemTicket> findByTicketIdAndIsInternoFalseOrderByCriadoEmAsc(UUID ticketId);

    // Buscar apenas notas internas
    List<MensagemTicket> findByTicketIdAndIsInternoTrueOrderByCriadoEmAsc(UUID ticketId);

    // Paginado
    Page<MensagemTicket> findByTicketId(UUID ticketId, Pageable pageable);

    // Primeira mensagem do suporte (para calcular tempo de resposta)
    @Query("""
        SELECT m FROM MensagemTicket m
        WHERE m.ticket.id = :ticketId
          AND m.autorTipo = 'SUPORTE'
          AND m.isInterno = false
        ORDER BY m.criadoEm ASC
        LIMIT 1
    """)
    Optional<MensagemTicket> findPrimeiraRespostaSuporte(@Param("ticketId") UUID ticketId);

    // Última mensagem de um ticket
    @Query("""
        SELECT m FROM MensagemTicket m
        WHERE m.ticket.id = :ticketId
        ORDER BY m.criadoEm DESC
        LIMIT 1
    """)
    Optional<MensagemTicket> findUltimaMensagem(@Param("ticketId") UUID ticketId);

    // Contar mensagens por tipo de autor
    long countByTicketIdAndAutorTipo(UUID ticketId, TipoAutorMensagem autorTipo);

    // Contar mensagens por ticket
    long countByTicketId(UUID ticketId);

    // Mensagens por período (para métricas)
    @Query("""
        SELECT COUNT(m) FROM MensagemTicket m
        WHERE m.criadoEm >= :dataInicio
          AND m.autorTipo = :autorTipo
    """)
    long countByAutorTipoDesde(
        @Param("autorTipo") TipoAutorMensagem autorTipo,
        @Param("dataInicio") LocalDateTime dataInicio
    );
}
