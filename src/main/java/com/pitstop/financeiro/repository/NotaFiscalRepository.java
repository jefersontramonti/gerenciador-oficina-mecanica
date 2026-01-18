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
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-23
 */
@Repository
public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, UUID> {

    /**
     * Busca notas fiscais por ordem de serviço em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return lista de notas fiscais
     */
    @Query("SELECT nf FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.ordemServicoId = :ordemServicoId")
    List<NotaFiscal> findByOficinaIdAndOrdemServicoId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca nota fiscal por número e série em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param numero número da nota
     * @param serie série da nota
     * @return nota fiscal se encontrada
     */
    @Query("SELECT nf FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.numero = :numero AND nf.serie = :serie")
    Optional<NotaFiscal> findByOficinaIdAndNumeroAndSerie(@Param("oficinaId") UUID oficinaId, @Param("numero") Long numero, @Param("serie") Integer serie);

    /**
     * Busca nota fiscal por chave de acesso em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param chaveAcesso chave de acesso da NFe
     * @return nota fiscal se encontrada
     */
    @Query("SELECT nf FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.chaveAcesso = :chaveAcesso")
    Optional<NotaFiscal> findByOficinaIdAndChaveAcesso(@Param("oficinaId") UUID oficinaId, @Param("chaveAcesso") String chaveAcesso);

    /**
     * Busca notas fiscais por status em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param status status da nota
     * @param pageable paginação
     * @return página de notas fiscais
     */
    @Query("SELECT nf FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.status = :status")
    Page<NotaFiscal> findByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusNotaFiscal status, Pageable pageable);

    /**
     * Busca todas as notas fiscais com paginação em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de notas fiscais
     */
    @Query("SELECT nf FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId")
    Page<NotaFiscal> findAllByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Verifica se existe nota fiscal para uma OS em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da OS
     * @return true se existe
     */
    @Query("SELECT CASE WHEN COUNT(nf) > 0 THEN true ELSE false END FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.ordemServicoId = :ordemServicoId")
    boolean existsByOficinaIdAndOrdemServicoId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca o próximo número disponível para uma série em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param serie série da nota
     * @return próximo número disponível
     */
    @Query("SELECT COALESCE(MAX(nf.numero), 0) + 1 FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.serie = :serie")
    Long findProximoNumeroByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("serie") Integer serie);

    /**
     * Busca nota fiscal por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID da nota fiscal
     * @return Optional contendo a nota fiscal se encontrada
     */
    @Query("SELECT nf FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.id = :id")
    Optional<NotaFiscal> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Alias para findAllByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de notas fiscais
     */
    default Page<NotaFiscal> findByOficinaId(UUID oficinaId, Pageable pageable) {
        return findAllByOficinaId(oficinaId, pageable);
    }

    /**
     * Alias para findProximoNumeroByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param serie série da nota
     * @return próximo número disponível
     */
    default Long findProximoNumero(UUID oficinaId, Integer serie) {
        return findProximoNumeroByOficinaId(oficinaId, serie);
    }

    // ==================== MÉTODOS PARA DASHBOARD ====================

    /**
     * Conta notas fiscais por status em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param status status da nota
     * @return quantidade de notas com o status
     */
    @Query("SELECT COUNT(nf) FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.status = :status")
    long countByOficinaIdAndStatus(@Param("oficinaId") UUID oficinaId, @Param("status") StatusNotaFiscal status);

    /**
     * Conta notas fiscais emitidas no mês atual em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de notas emitidas no mês
     */
    @Query("""
        SELECT COUNT(nf) FROM NotaFiscal nf
        WHERE nf.oficina.id = :oficinaId
        AND nf.status = 'EMITIDA'
        AND MONTH(nf.dataEmissao) = MONTH(CURRENT_DATE)
        AND YEAR(nf.dataEmissao) = YEAR(CURRENT_DATE)
        """)
    long countEmitidasNoMes(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta notas fiscais canceladas no mês atual em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de notas canceladas no mês
     */
    @Query("""
        SELECT COUNT(nf) FROM NotaFiscal nf
        WHERE nf.oficina.id = :oficinaId
        AND nf.status = 'CANCELADA'
        AND MONTH(nf.updatedAt) = MONTH(CURRENT_DATE)
        AND YEAR(nf.updatedAt) = YEAR(CURRENT_DATE)
        """)
    long countCanceladasNoMes(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta notas fiscais em rascunho (pendentes de emissão) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de rascunhos
     */
    @Query("SELECT COUNT(nf) FROM NotaFiscal nf WHERE nf.oficina.id = :oficinaId AND nf.status = 'RASCUNHO'")
    long countRascunhos(@Param("oficinaId") UUID oficinaId);
}
