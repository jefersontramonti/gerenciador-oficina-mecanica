package com.pitstop.manutencaopreventiva.repository;

import com.pitstop.manutencaopreventiva.domain.AgendamentoManutencao;
import com.pitstop.manutencaopreventiva.domain.StatusAgendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgendamentoManutencaoRepository extends JpaRepository<AgendamentoManutencao, UUID> {

    /**
     * Busca agendamento pelo token de confirmação.
     */
    @Query("""
        SELECT a FROM AgendamentoManutencao a
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        LEFT JOIN FETCH a.oficina o
        WHERE a.tokenConfirmacao = :token
        """)
    Optional<AgendamentoManutencao> findByTokenConfirmacao(@Param("token") String token);

    /**
     * Lista agendamentos entre datas (para calendário).
     */
    @Query("""
        SELECT a FROM AgendamentoManutencao a
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        WHERE a.oficina.id = :oficinaId
        AND a.dataAgendamento BETWEEN :dataInicio AND :dataFim
        AND a.status NOT IN ('CANCELADO')
        ORDER BY a.dataAgendamento, a.horaAgendamento
        """)
    List<AgendamentoManutencao> findByOficinaIdAndDataAgendamentoBetween(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Lista agendamentos do dia.
     */
    @Query("""
        SELECT a FROM AgendamentoManutencao a
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        WHERE a.oficina.id = :oficinaId
        AND a.dataAgendamento = :data
        AND a.status IN ('AGENDADO', 'CONFIRMADO')
        ORDER BY a.horaAgendamento
        """)
    List<AgendamentoManutencao> findAgendamentosDoDia(
        @Param("oficinaId") UUID oficinaId,
        @Param("data") LocalDate data
    );

    /**
     * Lista próximos agendamentos.
     */
    @Query("""
        SELECT a FROM AgendamentoManutencao a
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        WHERE a.oficina.id = :oficinaId
        AND a.dataAgendamento >= :hoje
        AND a.status IN ('AGENDADO', 'CONFIRMADO')
        ORDER BY a.dataAgendamento, a.horaAgendamento
        """)
    List<AgendamentoManutencao> findProximosAgendamentos(
        @Param("oficinaId") UUID oficinaId,
        @Param("hoje") LocalDate hoje,
        Pageable pageable
    );

    /**
     * Lista agendamentos que precisam de lembrete (hoje, ainda não enviado).
     */
    @Query("""
        SELECT a FROM AgendamentoManutencao a
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        LEFT JOIN FETCH a.oficina o
        WHERE a.dataAgendamento = :data
        AND a.status IN ('AGENDADO', 'CONFIRMADO')
        AND a.lembreteEnviado = false
        """)
    List<AgendamentoManutencao> findParaEnviarLembrete(@Param("data") LocalDate data);

    /**
     * Busca com filtros e paginação.
     */
    @Query(value = """
        SELECT a FROM AgendamentoManutencao a
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        WHERE a.oficina.id = :oficinaId
        AND (:veiculoId IS NULL OR a.veiculo.id = :veiculoId)
        AND (:clienteId IS NULL OR a.cliente.id = :clienteId)
        AND (:status IS NULL OR a.status = :status)
        AND (:dataInicio IS NULL OR a.dataAgendamento >= :dataInicio)
        AND (:dataFim IS NULL OR a.dataAgendamento <= :dataFim)
        ORDER BY a.dataAgendamento DESC, a.horaAgendamento DESC
        """,
        countQuery = """
        SELECT COUNT(a) FROM AgendamentoManutencao a
        WHERE a.oficina.id = :oficinaId
        AND (:veiculoId IS NULL OR a.veiculo.id = :veiculoId)
        AND (:clienteId IS NULL OR a.cliente.id = :clienteId)
        AND (:status IS NULL OR a.status = :status)
        AND (:dataInicio IS NULL OR a.dataAgendamento >= :dataInicio)
        AND (:dataFim IS NULL OR a.dataAgendamento <= :dataFim)
        """)
    Page<AgendamentoManutencao> findByFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("veiculoId") UUID veiculoId,
        @Param("clienteId") UUID clienteId,
        @Param("status") StatusAgendamento status,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Conta agendamentos do dia.
     */
    @Query("""
        SELECT COUNT(a) FROM AgendamentoManutencao a
        WHERE a.oficina.id = :oficinaId
        AND a.dataAgendamento = :data
        AND a.status IN ('AGENDADO', 'CONFIRMADO')
        """)
    long countAgendamentosDoDia(@Param("oficinaId") UUID oficinaId, @Param("data") LocalDate data);

    /**
     * Conta agendamentos por status.
     */
    @Query("""
        SELECT a.status, COUNT(a) FROM AgendamentoManutencao a
        WHERE a.oficina.id = :oficinaId
        AND a.dataAgendamento BETWEEN :dataInicio AND :dataFim
        GROUP BY a.status
        """)
    List<Object[]> countByStatusNoPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim
    );

    /**
     * Verifica conflito de horário.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM AgendamentoManutencao a
        WHERE a.oficina.id = :oficinaId
        AND a.dataAgendamento = :data
        AND a.horaAgendamento = :hora
        AND a.status IN ('AGENDADO', 'CONFIRMADO')
        AND (:excluirId IS NULL OR a.id != :excluirId)
        """)
    boolean existsConflitoHorario(
        @Param("oficinaId") UUID oficinaId,
        @Param("data") LocalDate data,
        @Param("hora") java.time.LocalTime hora,
        @Param("excluirId") UUID excluirId
    );
}
