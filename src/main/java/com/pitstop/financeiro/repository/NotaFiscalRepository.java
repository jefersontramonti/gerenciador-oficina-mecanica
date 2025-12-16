package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.NotaFiscal;
import com.pitstop.financeiro.domain.StatusNotaFiscal;
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
 * Repository para acesso aos dados de NotaFiscal.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
@Repository
public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, UUID> {

    /**
     * Busca notas fiscais por ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return lista de notas fiscais
     */
    List<NotaFiscal> findByOrdemServicoId(UUID ordemServicoId);

    /**
     * Busca nota fiscal por número e série.
     *
     * @param numero número da nota
     * @param serie série da nota
     * @return nota fiscal se encontrada
     */
    Optional<NotaFiscal> findByNumeroAndSerie(Long numero, Integer serie);

    /**
     * Busca nota fiscal por chave de acesso.
     *
     * @param chaveAcesso chave de acesso da NFe
     * @return nota fiscal se encontrada
     */
    Optional<NotaFiscal> findByChaveAcesso(String chaveAcesso);

    /**
     * Busca notas fiscais por status.
     *
     * @param status status da nota
     * @param pageable paginação
     * @return página de notas fiscais
     */
    Page<NotaFiscal> findByStatus(StatusNotaFiscal status, Pageable pageable);

    /**
     * Busca todas as notas fiscais com paginação.
     *
     * @param pageable paginação
     * @return página de notas fiscais
     */
    Page<NotaFiscal> findAll(Pageable pageable);

    /**
     * Verifica se existe nota fiscal para uma OS.
     *
     * @param ordemServicoId ID da OS
     * @return true se existe
     */
    boolean existsByOrdemServicoId(UUID ordemServicoId);

    /**
     * Busca o próximo número disponível para uma série.
     *
     * @param serie série da nota
     * @return próximo número disponível
     */
    @Query("SELECT COALESCE(MAX(nf.numero), 0) + 1 FROM NotaFiscal nf WHERE nf.serie = :serie")
    Long findProximoNumero(@Param("serie") Integer serie);
}
