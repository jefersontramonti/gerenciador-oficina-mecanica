package com.pitstop.saas.repository;

import com.pitstop.saas.domain.Comunicado;
import com.pitstop.saas.domain.StatusComunicado;
import com.pitstop.saas.domain.TipoComunicado;
import com.pitstop.saas.domain.PrioridadeComunicado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComunicadoRepository extends JpaRepository<Comunicado, UUID> {

    // Busca por status
    Page<Comunicado> findByStatus(StatusComunicado status, Pageable pageable);

    List<Comunicado> findByStatus(StatusComunicado status);

    // Busca por tipo
    Page<Comunicado> findByTipo(TipoComunicado tipo, Pageable pageable);

    // Busca comunicados agendados para envio
    @Query("SELECT c FROM Comunicado c WHERE c.status = 'AGENDADO' AND c.dataAgendamento <= :now")
    List<Comunicado> findAgendadosParaEnvio(@Param("now") OffsetDateTime now);

    // Busca com filtros (usando native query para evitar problema de tipo com resumo)
    @Query(value = """
        SELECT * FROM comunicados c
        WHERE (:status IS NULL OR c.status = CAST(:status AS VARCHAR))
        AND (:tipo IS NULL OR c.tipo = CAST(:tipo AS VARCHAR))
        AND (:prioridade IS NULL OR c.prioridade = CAST(:prioridade AS VARCHAR))
        AND (:busca IS NULL OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS VARCHAR), '%'))
             OR LOWER(CAST(c.resumo AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:busca AS VARCHAR), '%')))
        ORDER BY c.created_at DESC
    """,
    countQuery = """
        SELECT COUNT(*) FROM comunicados c
        WHERE (:status IS NULL OR c.status = CAST(:status AS VARCHAR))
        AND (:tipo IS NULL OR c.tipo = CAST(:tipo AS VARCHAR))
        AND (:prioridade IS NULL OR c.prioridade = CAST(:prioridade AS VARCHAR))
        AND (:busca IS NULL OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', CAST(:busca AS VARCHAR), '%'))
             OR LOWER(CAST(c.resumo AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:busca AS VARCHAR), '%')))
    """,
    nativeQuery = true)
    Page<Comunicado> findWithFilters(
        @Param("status") String status,
        @Param("tipo") String tipo,
        @Param("prioridade") String prioridade,
        @Param("busca") String busca,
        Pageable pageable
    );

    // Contadores por status
    long countByStatus(StatusComunicado status);

    // Estatísticas
    @Query("""
        SELECT COUNT(c) FROM Comunicado c
        WHERE c.status = 'ENVIADO'
        AND c.dataEnvio >= :desde
    """)
    long countEnviadosDesde(@Param("desde") OffsetDateTime desde);

    @Query("""
        SELECT COALESCE(SUM(c.totalDestinatarios), 0) FROM Comunicado c
        WHERE c.status = 'ENVIADO'
        AND c.dataEnvio >= :desde
    """)
    long countDestinatariosDesde(@Param("desde") OffsetDateTime desde);

    @Query("""
        SELECT COALESCE(SUM(c.totalVisualizacoes), 0) FROM Comunicado c
        WHERE c.status = 'ENVIADO'
        AND c.dataEnvio >= :desde
    """)
    long countVisualizacoesDesde(@Param("desde") OffsetDateTime desde);

    // Busca por autor
    Page<Comunicado> findByAutorId(UUID autorId, Pageable pageable);

    // Comunicados recentes enviados
    @Query("SELECT c FROM Comunicado c WHERE c.status = 'ENVIADO' ORDER BY c.dataEnvio DESC")
    Page<Comunicado> findEnviadosRecentes(Pageable pageable);
}
