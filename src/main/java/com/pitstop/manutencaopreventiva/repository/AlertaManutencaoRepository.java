package com.pitstop.manutencaopreventiva.repository;

import com.pitstop.manutencaopreventiva.domain.AlertaManutencao;
import com.pitstop.manutencaopreventiva.domain.StatusAlerta;
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

@Repository
public interface AlertaManutencaoRepository extends JpaRepository<AlertaManutencao, UUID> {

    /**
     * Lista alertas pendentes para envio (imediato ou agendado).
     */
    @Query("""
        SELECT a FROM AlertaManutencao a
        LEFT JOIN FETCH a.oficina o
        LEFT JOIN FETCH a.plano p
        LEFT JOIN FETCH a.veiculo v
        LEFT JOIN FETCH a.cliente c
        WHERE a.status = 'PENDENTE'
        AND (a.proximaTentativa IS NULL OR a.proximaTentativa <= :agora)
        AND (a.agendarPara IS NULL OR a.agendarPara <= :agora)
        ORDER BY a.createdAt
        """)
    List<AlertaManutencao> findPendentesParaEnvio(@Param("agora") LocalDateTime agora);

    /**
     * Lista alertas para retry (falharam mas podem tentar novamente).
     */
    @Query("""
        SELECT a FROM AlertaManutencao a
        WHERE a.status = 'PENDENTE'
        AND a.tentativas > 0
        AND a.tentativas < a.maxTentativas
        AND a.proximaTentativa <= :agora
        """)
    List<AlertaManutencao> findParaRetry(@Param("agora") LocalDateTime agora);

    /**
     * Busca alertas com filtros e paginação.
     */
    @Query(value = """
        SELECT a FROM AlertaManutencao a
        LEFT JOIN FETCH a.plano p
        LEFT JOIN FETCH a.veiculo v
        WHERE a.oficina.id = :oficinaId
        AND (:status IS NULL OR a.status = :status)
        AND (:planoId IS NULL OR a.plano.id = :planoId)
        ORDER BY a.createdAt DESC
        """,
        countQuery = """
        SELECT COUNT(a) FROM AlertaManutencao a
        WHERE a.oficina.id = :oficinaId
        AND (:status IS NULL OR a.status = :status)
        AND (:planoId IS NULL OR a.plano.id = :planoId)
        """)
    Page<AlertaManutencao> findByFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusAlerta status,
        @Param("planoId") UUID planoId,
        Pageable pageable
    );

    /**
     * Conta alertas pendentes (exclui alertas de planos inativos/finalizados).
     */
    @Query("""
        SELECT COUNT(a) FROM AlertaManutencao a
        WHERE a.oficina.id = :oficinaId
        AND a.status = 'PENDENTE'
        AND (a.plano IS NULL OR (a.plano.ativo = true AND a.plano.status = 'ATIVO'))
        """)
    long countPendentes(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta alertas enviados no período.
     */
    @Query("""
        SELECT COUNT(a) FROM AlertaManutencao a
        WHERE a.oficina.id = :oficinaId
        AND a.status = 'ENVIADO'
        AND a.enviadoEm BETWEEN :inicio AND :fim
        """)
    long countEnviadosNoPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    /**
     * Verifica se já existe alerta pendente para o plano.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM AlertaManutencao a
        WHERE a.plano.id = :planoId
        AND a.status = 'PENDENTE'
        AND a.tipoAlerta = :tipoAlerta
        """)
    boolean existsAlertaPendente(
        @Param("planoId") UUID planoId,
        @Param("tipoAlerta") com.pitstop.manutencaopreventiva.domain.TipoAlerta tipoAlerta
    );

    /**
     * Cancela todos os alertas pendentes de um plano.
     */
    @Modifying
    @Query("UPDATE AlertaManutencao a SET a.status = 'CANCELADO' WHERE a.plano.id = :planoId AND a.status = 'PENDENTE'")
    void cancelarAlertasPendentes(@Param("planoId") UUID planoId);

    /**
     * Lista alertas prontos para envio (alias para findPendentesParaEnvio).
     */
    default List<AlertaManutencao> findParaEnvio(LocalDateTime agora) {
        return findPendentesParaEnvio(agora);
    }

    /**
     * Conta alertas por oficina e status.
     */
    @Query("SELECT COUNT(a) FROM AlertaManutencao a WHERE a.oficina.id = :oficinaId AND a.status = :status")
    long countByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusAlerta status);
}
