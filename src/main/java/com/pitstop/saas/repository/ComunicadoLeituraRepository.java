package com.pitstop.saas.repository;

import com.pitstop.saas.domain.ComunicadoLeitura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComunicadoLeituraRepository extends JpaRepository<ComunicadoLeitura, UUID> {

    // Busca leitura específica
    Optional<ComunicadoLeitura> findByComunicadoIdAndOficinaId(UUID comunicadoId, UUID oficinaId);

    // Busca leituras de um comunicado
    Page<ComunicadoLeitura> findByComunicadoId(UUID comunicadoId, Pageable pageable);

    List<ComunicadoLeitura> findByComunicadoId(UUID comunicadoId);

    // Busca comunicados de uma oficina
    Page<ComunicadoLeitura> findByOficinaId(UUID oficinaId, Pageable pageable);

    // Busca comunicados não lidos de uma oficina
    @Query("""
        SELECT cl FROM ComunicadoLeitura cl
        WHERE cl.oficina.id = :oficinaId
        AND cl.visualizado = false
        ORDER BY cl.createdAt DESC
    """)
    List<ComunicadoLeitura> findNaoLidosByOficinaId(@Param("oficinaId") UUID oficinaId);

    // Contadores
    long countByComunicadoIdAndVisualizadoTrue(UUID comunicadoId);

    long countByComunicadoIdAndConfirmadoTrue(UUID comunicadoId);

    long countByOficinaIdAndVisualizadoFalse(UUID oficinaId);

    // Verifica se existe leitura
    boolean existsByComunicadoIdAndOficinaId(UUID comunicadoId, UUID oficinaId);

    // Busca com detalhes para exibição
    @Query("""
        SELECT cl FROM ComunicadoLeitura cl
        JOIN FETCH cl.comunicado c
        WHERE cl.oficina.id = :oficinaId
        AND c.status = 'ENVIADO'
        ORDER BY c.dataEnvio DESC
    """)
    Page<ComunicadoLeitura> findComunicadosEnviadosParaOficina(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    // Busca comunicados pendentes de confirmação
    @Query("""
        SELECT cl FROM ComunicadoLeitura cl
        JOIN FETCH cl.comunicado c
        WHERE cl.oficina.id = :oficinaId
        AND c.requerConfirmacao = true
        AND cl.confirmado = false
        ORDER BY c.dataEnvio DESC
    """)
    List<ComunicadoLeitura> findPendentesConfirmacao(@Param("oficinaId") UUID oficinaId);

    // Busca comunicados para exibir no login
    @Query("""
        SELECT cl FROM ComunicadoLeitura cl
        JOIN FETCH cl.comunicado c
        WHERE cl.oficina.id = :oficinaId
        AND c.exibirNoLogin = true
        AND cl.visualizado = false
        ORDER BY c.prioridade DESC, c.dataEnvio DESC
    """)
    List<ComunicadoLeitura> findParaExibirNoLogin(@Param("oficinaId") UUID oficinaId);
}
