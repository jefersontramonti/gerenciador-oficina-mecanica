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
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Repository
public interface PecaRepository extends JpaRepository<Peca, UUID> {

    /**
     * Busca peça por código (SKU).
     * Considera apenas peças ativas devido ao @Where da entidade.
     *
     * @param codigo código único da peça
     * @return Optional contendo a peça se encontrada
     */
    Optional<Peca> findByCodigoAndAtivoTrue(String codigo);

    /**
     * Busca peça por código (incluindo inativas).
     *
     * @param codigo código único da peça
     * @return Optional contendo a peça se encontrada
     */
    Optional<Peca> findByCodigo(String codigo);

    /**
     * Verifica se existe peça com o código especificado.
     *
     * @param codigo código a verificar
     * @return true se existe
     */
    boolean existsByCodigoAndAtivoTrue(String codigo);

    /**
     * Busca peça por ID com lock pessimista para update.
     * Usado em operações críticas de movimentação de estoque para evitar condições de corrida.
     *
     * @param id ID da peça
     * @return Optional contendo a peça com lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Peca p WHERE p.id = :id")
    Optional<Peca> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Lista peças com estoque baixo (quantidade atual <= quantidade mínima).
     * Útil para alertas de reposição.
     *
     * @param pageable paginação
     * @return página de peças com estoque baixo
     */
    @Query("SELECT p FROM Peca p WHERE p.ativo = true AND p.quantidadeAtual <= p.quantidadeMinima ORDER BY p.quantidadeAtual ASC")
    Page<Peca> findEstoqueBaixo(Pageable pageable);

    /**
     * Lista peças com estoque zerado.
     *
     * @param pageable paginação
     * @return página de peças sem estoque
     */
    @Query("SELECT p FROM Peca p WHERE p.ativo = true AND p.quantidadeAtual = 0 ORDER BY p.descricao")
    Page<Peca> findEstoqueZerado(Pageable pageable);

    /**
     * Busca peças por marca.
     *
     * @param marca marca das peças
     * @param pageable paginação
     * @return página de peças da marca
     */
    Page<Peca> findByMarcaContainingIgnoreCaseAndAtivoTrue(String marca, Pageable pageable);

    /**
     * Busca peças por descrição (busca parcial, case insensitive).
     *
     * @param descricao texto a buscar na descrição
     * @param pageable paginação
     * @return página de peças encontradas
     */
    Page<Peca> findByDescricaoContainingIgnoreCaseAndAtivoTrue(String descricao, Pageable pageable);

    /**
     * Busca peças por unidade de medida.
     *
     * @param unidadeMedida unidade de medida
     * @param pageable paginação
     * @return página de peças
     */
    Page<Peca> findByUnidadeMedidaAndAtivoTrue(UnidadeMedida unidadeMedida, Pageable pageable);

    /**
     * Busca peças ativas ordenadas por descrição.
     *
     * @param pageable paginação
     * @return página de peças ativas
     */
    Page<Peca> findByAtivoTrueOrderByDescricaoAsc(Pageable pageable);

    /**
     * Calcula o valor total do inventário (soma de quantidadeAtual * valorCusto).
     *
     * @return valor total em estoque
     */
    @Query("SELECT COALESCE(SUM(p.quantidadeAtual * p.valorCusto), 0) FROM Peca p WHERE p.ativo = true")
    BigDecimal calcularValorTotalInventario();

    /**
     * Conta quantas peças têm estoque baixo.
     *
     * @return quantidade de peças com estoque baixo
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.ativo = true AND p.quantidadeAtual <= p.quantidadeMinima")
    long countEstoqueBaixo();

    /**
     * Conta quantas peças têm estoque zerado.
     *
     * @return quantidade de peças sem estoque
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.ativo = true AND p.quantidadeAtual = 0")
    long countEstoqueZerado();

    /**
     * Lista todas as marcas distintas de peças ativas.
     * Útil para filtros dinâmicos no frontend.
     *
     * @return lista de marcas únicas
     */
    @Query("SELECT DISTINCT p.marca FROM Peca p WHERE p.ativo = true AND p.marca IS NOT NULL ORDER BY p.marca")
    List<String> findDistinctMarcas();

    /**
     * Busca peças filtradas por múltiplos critérios.
     *
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
            WHERE (:ativo IS NULL OR p.ativo = :ativo)
            AND (COALESCE(:codigo, '') = '' OR p.codigo LIKE CONCAT('%', :codigo, '%'))
            AND (COALESCE(:descricao, '') = '' OR LOWER(p.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')))
            AND (COALESCE(:marca, '') = '' OR LOWER(p.marca) LIKE LOWER(CONCAT('%', :marca, '%')))
            AND (:unidadeMedida IS NULL OR p.unidadeMedida = :unidadeMedida)
            AND (:estoqueBaixo IS NULL OR (:estoqueBaixo = true AND p.quantidadeAtual <= p.quantidadeMinima) OR (:estoqueBaixo = false))
            ORDER BY p.descricao
            """)
    Page<Peca> findByFilters(
            @Param("codigo") String codigo,
            @Param("descricao") String descricao,
            @Param("marca") String marca,
            @Param("unidadeMedida") UnidadeMedida unidadeMedida,
            @Param("ativo") Boolean ativo,
            @Param("estoqueBaixo") Boolean estoqueBaixo,
            Pageable pageable
    );

    /**
     * Lista peças sem localização física definida.
     * Útil para identificar itens que precisam ser organizados no estoque.
     *
     * @param pageable paginação
     * @return página de peças sem localização
     */
    @Query("SELECT p FROM Peca p WHERE p.ativo = true AND p.localArmazenamento IS NULL ORDER BY p.descricao")
    Page<Peca> findPecasSemLocalizacao(Pageable pageable);

    /**
     * Conta quantas peças estão sem localização.
     *
     * @return quantidade de peças sem localização
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.ativo = true AND p.localArmazenamento IS NULL")
    long countPecasSemLocalizacao();
}
