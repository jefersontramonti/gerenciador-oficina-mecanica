package com.pitstop.estoque.repository;

import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.UnidadeMedida;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para gerenciamento de peças do estoque.
 * Fornece queries otimizadas para consultas comuns de estoque.
 *
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Repository
public interface PecaRepository extends JpaRepository<Peca, UUID> {

    /**
     * Busca peça por código (SKU) em uma oficina específica.
     * Considera apenas peças ativas devido ao @Where da entidade.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código único da peça
     * @return Optional contendo a peça se encontrada
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.codigo = :codigo AND p.ativo = true")
    Optional<Peca> findByOficinaIdAndCodigoAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("codigo") String codigo);

    /**
     * Busca peça por código (incluindo inativas) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código único da peça
     * @return Optional contendo a peça se encontrada
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.codigo = :codigo")
    Optional<Peca> findByOficinaIdAndCodigo(@Param("oficinaId") UUID oficinaId, @Param("codigo") String codigo);

    /**
     * Verifica se existe peça com o código especificado em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código a verificar
     * @return true se existe
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Peca p WHERE p.oficina.id = :oficinaId AND p.codigo = :codigo AND p.ativo = true")
    boolean existsByOficinaIdAndCodigoAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("codigo") String codigo);

    /**
     * Busca peça por ID com lock pessimista para update em uma oficina.
     * Usado em operações críticas de movimentação de estoque para evitar condições de corrida.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID da peça
     * @return Optional contendo a peça com lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.id = :id")
    Optional<Peca> findByOficinaIdAndIdForUpdate(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Lista peças com estoque baixo (quantidade atual <= quantidade mínima) em uma oficina.
     * Útil para alertas de reposição.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de peças com estoque baixo
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.quantidadeAtual <= p.quantidadeMinima ORDER BY p.quantidadeAtual ASC")
    Page<Peca> findEstoqueBaixoByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Lista peças com estoque zerado em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de peças sem estoque
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.quantidadeAtual = 0 ORDER BY p.descricao")
    Page<Peca> findEstoqueZeradoByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Busca peças por marca em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param marca marca das peças
     * @param pageable paginação
     * @return página de peças da marca
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND LOWER(p.marca) LIKE LOWER(CONCAT('%', :marca, '%')) AND p.ativo = true")
    Page<Peca> findByOficinaIdAndMarcaContainingIgnoreCaseAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("marca") String marca, Pageable pageable);

    /**
     * Busca peças por descrição (busca parcial, case insensitive) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param descricao texto a buscar na descrição
     * @param pageable paginação
     * @return página de peças encontradas
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND LOWER(p.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')) AND p.ativo = true")
    Page<Peca> findByOficinaIdAndDescricaoContainingIgnoreCaseAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("descricao") String descricao, Pageable pageable);

    /**
     * Busca peças por unidade de medida em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param unidadeMedida unidade de medida
     * @param pageable paginação
     * @return página de peças
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.unidadeMedida = :unidadeMedida AND p.ativo = true")
    Page<Peca> findByOficinaIdAndUnidadeMedidaAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("unidadeMedida") UnidadeMedida unidadeMedida, Pageable pageable);

    /**
     * Busca peças ativas ordenadas por descrição em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de peças ativas
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true ORDER BY p.descricao ASC")
    Page<Peca> findByOficinaIdAndAtivoTrueOrderByDescricaoAsc(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Calcula o valor total do inventário (soma de quantidadeAtual * valorCusto) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return valor total em estoque
     */
    @Query("SELECT COALESCE(SUM(p.quantidadeAtual * p.valorCusto), 0) FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true")
    BigDecimal calcularValorTotalInventarioByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta quantas peças têm estoque baixo em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de peças com estoque baixo
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.quantidadeAtual <= p.quantidadeMinima")
    long countEstoqueBaixoByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta quantas peças têm estoque zerado em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de peças sem estoque
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.quantidadeAtual = 0")
    long countEstoqueZeradoByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Lista todas as marcas distintas de peças ativas em uma oficina.
     * Útil para filtros dinâmicos no frontend.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de marcas únicas
     */
    @Query("SELECT DISTINCT p.marca FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.marca IS NOT NULL ORDER BY p.marca")
    List<String> findDistinctMarcasByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca peças filtradas por múltiplos critérios em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código da peça (opcional)
     * @param descricao texto na descrição (opcional)
     * @param marca marca (opcional)
     * @param unidadeMedida unidade de medida (opcional)
     * @param ativo filtro por status ativo (opcional)
     * @param estoqueBaixo filtro por estoque baixo (opcional)
     * @param pageable paginação
     * @return página de peças filtradas
     */
    @Query("""
            SELECT p FROM Peca p
            WHERE p.oficina.id = :oficinaId
            AND (:ativo IS NULL OR p.ativo = :ativo)
            AND (COALESCE(:codigo, '') = '' OR p.codigo LIKE CONCAT('%', :codigo, '%'))
            AND (COALESCE(:descricao, '') = '' OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')))
            AND (COALESCE(:marca, '') = '' OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :marca, '%')))
            AND (:unidadeMedida IS NULL OR p.unidadeMedida = :unidadeMedida)
            AND (:estoqueBaixo IS NULL OR (:estoqueBaixo = true AND p.quantidadeAtual <= p.quantidadeMinima) OR (:estoqueBaixo = false))
            ORDER BY p.descricao
            """)
    Page<Peca> findByFilters(
            @Param("oficinaId") UUID oficinaId,
            @Param("codigo") String codigo,
            @Param("descricao") String descricao,
            @Param("marca") String marca,
            @Param("unidadeMedida") UnidadeMedida unidadeMedida,
            @Param("ativo") Boolean ativo,
            @Param("estoqueBaixo") Boolean estoqueBaixo,
            Pageable pageable
    );

    /**
     * Lista peças sem localização física definida em uma oficina.
     * Útil para identificar itens que precisam ser organizados no estoque.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de peças sem localização
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.localArmazenamento IS NULL ORDER BY p.descricao")
    Page<Peca> findPecasSemLocalizacaoByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Conta quantas peças estão sem localização em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de peças sem localização
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.localArmazenamento IS NULL")
    long countPecasSemLocalizacaoByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca peça por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID da peça
     * @return Optional contendo a peça se encontrada
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.id = :id AND p.ativo = true")
    Optional<Peca> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca todas as peças de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de peças
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true")
    Page<Peca> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Alias para findDistinctMarcasByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de marcas únicas
     */
    default List<String> findDistinctMarcas(UUID oficinaId) {
        return findDistinctMarcasByOficinaId(oficinaId);
    }

    /**
     * Lista todas as aplicações distintas de peças ativas em uma oficina.
     * Útil para filtros dinâmicos no frontend.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de aplicações únicas
     */
    @Query("SELECT DISTINCT p.aplicacao FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true AND p.aplicacao IS NOT NULL ORDER BY p.aplicacao")
    List<String> findDistinctAplicacoesByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Alias para findDistinctAplicacoesByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de aplicações únicas
     */
    default List<String> findDistinctAplicacoes(UUID oficinaId) {
        return findDistinctAplicacoesByOficinaId(oficinaId);
    }

    /**
     * Busca peças ativas sem ordenação em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de peças ativas
     */
    @Query("SELECT p FROM Peca p WHERE p.oficina.id = :oficinaId AND p.ativo = true")
    Page<Peca> findByOficinaIdAndAtivoTrue(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Alias para findEstoqueBaixoByOficinaId (compatibilidade).
     */
    default Page<Peca> findEstoqueBaixo(UUID oficinaId, Pageable pageable) {
        return findEstoqueBaixoByOficinaId(oficinaId, pageable);
    }

    /**
     * Alias para findEstoqueZeradoByOficinaId (compatibilidade).
     */
    default Page<Peca> findEstoqueZerado(UUID oficinaId, Pageable pageable) {
        return findEstoqueZeradoByOficinaId(oficinaId, pageable);
    }

    /**
     * Alias para calcularValorTotalInventarioByOficinaId (compatibilidade).
     */
    default BigDecimal calcularValorTotalInventario(UUID oficinaId) {
        return calcularValorTotalInventarioByOficinaId(oficinaId);
    }

    /**
     * Alias para countEstoqueBaixoByOficinaId (compatibilidade).
     */
    default long countEstoqueBaixo(UUID oficinaId) {
        return countEstoqueBaixoByOficinaId(oficinaId);
    }

    /**
     * Alias para countEstoqueZeradoByOficinaId (compatibilidade).
     */
    default long countEstoqueZerado(UUID oficinaId) {
        return countEstoqueZeradoByOficinaId(oficinaId);
    }

    /**
     * Alias para findPecasSemLocalizacaoByOficinaId (compatibilidade).
     */
    default Page<Peca> findPecasSemLocalizacao(UUID oficinaId, Pageable pageable) {
        return findPecasSemLocalizacaoByOficinaId(oficinaId, pageable);
    }

    /**
     * Alias para countPecasSemLocalizacaoByOficinaId (compatibilidade).
     */
    default long countPecasSemLocalizacao(UUID oficinaId) {
        return countPecasSemLocalizacaoByOficinaId(oficinaId);
    }
}
