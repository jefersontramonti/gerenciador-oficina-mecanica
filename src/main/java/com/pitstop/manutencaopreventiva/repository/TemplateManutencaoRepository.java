package com.pitstop.manutencaopreventiva.repository;

import com.pitstop.manutencaopreventiva.domain.TemplateManutencao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateManutencaoRepository extends JpaRepository<TemplateManutencao, UUID> {

    /**
     * Lista templates globais (oficina_id IS NULL) ativos.
     */
    @Query("SELECT t FROM TemplateManutencao t WHERE t.oficina IS NULL AND t.ativo = true ORDER BY t.nome")
    List<TemplateManutencao> findTemplatesGlobais();

    /**
     * Lista templates de uma oficina específica ativos.
     */
    @Query("SELECT t FROM TemplateManutencao t WHERE t.oficina.id = :oficinaId AND t.ativo = true ORDER BY t.nome")
    List<TemplateManutencao> findByOficinaIdAndAtivoTrue(@Param("oficinaId") UUID oficinaId);

    /**
     * Lista templates disponíveis para uma oficina (globais + da oficina).
     */
    @Query("SELECT t FROM TemplateManutencao t WHERE (t.oficina IS NULL OR t.oficina.id = :oficinaId) AND t.ativo = true ORDER BY t.nome")
    List<TemplateManutencao> findDisponiveisParaOficina(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca com filtros e paginação.
     */
    @Query("""
        SELECT t FROM TemplateManutencao t
        WHERE (t.oficina IS NULL OR t.oficina.id = :oficinaId)
        AND t.ativo = true
        AND (:tipoManutencao IS NULL OR t.tipoManutencao = :tipoManutencao)
        AND (:busca IS NULL OR :busca = '' OR LOWER(t.nome) LIKE CONCAT('%', LOWER(:busca), '%'))
        ORDER BY t.nome
        """)
    Page<TemplateManutencao> findByFilters(
        @Param("oficinaId") UUID oficinaId,
        @Param("tipoManutencao") String tipoManutencao,
        @Param("busca") String busca,
        Pageable pageable
    );

    /**
     * Lista tipos de manutenção distintos.
     */
    @Query("SELECT DISTINCT t.tipoManutencao FROM TemplateManutencao t WHERE (t.oficina IS NULL OR t.oficina.id = :oficinaId) AND t.ativo = true ORDER BY t.tipoManutencao")
    List<String> findTiposManutencao(@Param("oficinaId") UUID oficinaId);
}
