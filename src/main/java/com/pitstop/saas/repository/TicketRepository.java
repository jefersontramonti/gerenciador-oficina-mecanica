package com.pitstop.saas.repository;

import com.pitstop.saas.domain.PrioridadeTicket;
import com.pitstop.saas.domain.StatusTicket;
import com.pitstop.saas.domain.Ticket;
import com.pitstop.saas.domain.TipoTicket;
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
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByNumero(String numero);

    // Busca com filtros
    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.oficina o
        WHERE (:oficinaId IS NULL OR t.oficina.id = :oficinaId)
          AND (:status IS NULL OR t.status = :status)
          AND (:tipo IS NULL OR t.tipo = :tipo)
          AND (:prioridade IS NULL OR t.prioridade = :prioridade)
          AND (:atribuidoA IS NULL OR t.atribuidoA.id = :atribuidoA)
          AND (:busca IS NULL OR
               LOWER(t.numero) LIKE LOWER(CONCAT('%', :busca, '%')) OR
               LOWER(t.assunto) LIKE LOWER(CONCAT('%', :busca, '%')) OR
               LOWER(t.usuarioNome) LIKE LOWER(CONCAT('%', :busca, '%')))
        ORDER BY
            CASE t.prioridade
                WHEN 'URGENTE' THEN 1
                WHEN 'ALTA' THEN 2
                WHEN 'MEDIA' THEN 3
                WHEN 'BAIXA' THEN 4
            END,
            t.aberturaEm DESC
    """)
    Page<Ticket> findAllWithFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusTicket status,
        @Param("tipo") TipoTicket tipo,
        @Param("prioridade") PrioridadeTicket prioridade,
        @Param("atribuidoA") UUID atribuidoA,
        @Param("busca") String busca,
        Pageable pageable
    );

    // Contagens por status
    long countByStatus(StatusTicket status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status NOT IN ('RESOLVIDO', 'FECHADO')")
    long countAbertos();

    // Tickets da oficina
    Page<Ticket> findByOficinaId(UUID oficinaId, Pageable pageable);
    List<Ticket> findByOficinaIdAndStatusNotIn(UUID oficinaId, List<StatusTicket> statusList);

    // Tickets atribuídos a um usuário
    Page<Ticket> findByAtribuidoAId(UUID usuarioId, Pageable pageable);
    List<Ticket> findByAtribuidoAIdAndStatusNotIn(UUID usuarioId, List<StatusTicket> statusList);

    // Tickets não atribuídos
    @Query("SELECT t FROM Ticket t WHERE t.atribuidoA IS NULL AND t.status NOT IN ('RESOLVIDO', 'FECHADO')")
    List<Ticket> findNaoAtribuidos();

    // Tickets com SLA vencido
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.respostaInicialEm IS NULL
          AND t.status NOT IN ('RESOLVIDO', 'FECHADO')
          AND FUNCTION('timestampadd', MINUTE, t.slaMinutos, t.aberturaEm) < CURRENT_TIMESTAMP
    """)
    List<Ticket> findComSlaVencido();

    // Tickets por período
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.aberturaEm BETWEEN :inicio AND :fim
    """)
    List<Ticket> findByPeriodo(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    // Métricas
    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.aberturaEm >= :dataInicio
    """)
    long countAbertosDesdeDe(@Param("dataInicio") LocalDateTime dataInicio);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.resolvidoEm >= :dataInicio
    """)
    long countResolvidosDesdeDe(@Param("dataInicio") LocalDateTime dataInicio);

    @Query("""
        SELECT AVG(t.tempoRespostaMinutos) FROM Ticket t
        WHERE t.tempoRespostaMinutos IS NOT NULL
          AND t.aberturaEm >= :dataInicio
    """)
    Double avgTempoRespostaDesdeDe(@Param("dataInicio") LocalDateTime dataInicio);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.tempoRespostaMinutos IS NOT NULL
          AND t.tempoRespostaMinutos <= t.slaMinutos
          AND t.aberturaEm >= :dataInicio
    """)
    long countDentroSla(@Param("dataInicio") LocalDateTime dataInicio);

    @Query("""
        SELECT COUNT(t) FROM Ticket t
        WHERE t.tempoRespostaMinutos IS NOT NULL
          AND t.aberturaEm >= :dataInicio
    """)
    long countComResposta(@Param("dataInicio") LocalDateTime dataInicio);

    // Próximo número de ticket
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero, 11) AS INTEGER)), 0) + 1 FROM tickets WHERE numero LIKE :prefix", nativeQuery = true)
    Integer findNextNumeroByPrefix(@Param("prefix") String prefix);

    // Busca com fetch de mensagens
    @Query("""
        SELECT DISTINCT t FROM Ticket t
        LEFT JOIN FETCH t.mensagens m
        LEFT JOIN FETCH t.oficina
        LEFT JOIN FETCH t.atribuidoA
        WHERE t.id = :id
    """)
    Optional<Ticket> findByIdWithMensagens(@Param("id") UUID id);
}
