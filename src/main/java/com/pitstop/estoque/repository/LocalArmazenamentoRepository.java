package com.pitstop.estoque.repository;

import com.pitstop.estoque.domain.LocalArmazenamento;
import com.pitstop.estoque.domain.TipoLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para gerenciamento de locais de armazenamento.
 * Fornece queries especializadas para navegação hierárquica e validação de ciclos.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Repository
public interface LocalArmazenamentoRepository extends JpaRepository<LocalArmazenamento, UUID> {

    /**
     * Busca local por ID com locaisFilhos carregados (evita LazyInitializationException).
     * Sobrescreve o findById padrão do JPA.
     *
     * @param id ID do local
     * @return Optional contendo o local se encontrado
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.id = :id")
    Optional<LocalArmazenamento> findById(@Param("id") UUID id);

    /**
     * Busca local por código (normalizado para uppercase) com locaisFilhos carregados.
     *
     * @param codigo código único do local
     * @return Optional contendo o local se encontrado
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.codigo = :codigo")
    Optional<LocalArmazenamento> findByCodigo(@Param("codigo") String codigo);

    /**
     * Verifica se existe local com o código especificado.
     *
     * @param codigo código a verificar
     * @return true se existe
     */
    boolean existsByCodigo(String codigo);

    /**
     * Verifica se existe local com o código, excluindo um ID específico.
     * Útil para validação em updates.
     *
     * @param codigo código a verificar
     * @param id ID a excluir da verificação
     * @return true se existe outro local com o mesmo código
     */
    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    /**
     * Lista todos os locais ativos com locaisFilhos carregados (evita LazyInitializationException).
     * Usa LEFT JOIN FETCH para carregar a coleção locaisFilhos na mesma query.
     *
     * @return lista de locais ativos
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByAtivoTrue();

    /**
     * Lista locais raiz (sem pai) que estão ativos.
     * Normalmente são depósitos ou vitrines principais.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @return lista de locais raiz
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.localizacaoPai IS NULL AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findLocaisRaiz();

    /**
     * Lista locais filhos de um pai específico.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param paiId ID do local pai
     * @return lista de locais filhos
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.localizacaoPai.id = :paiId AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByLocalizacaoPaiId(@Param("paiId") UUID paiId);

    /**
     * Lista locais por tipo.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param tipo tipo do local
     * @return lista de locais do tipo especificado
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.tipo = :tipo AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByTipoAndAtivoTrue(@Param("tipo") TipoLocal tipo);

    /**
     * Conta quantas peças estão vinculadas a um local específico.
     * Usado para impedir exclusão de locais com peças.
     *
     * @param localId ID do local
     * @return quantidade de peças vinculadas
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.localArmazenamento.id = :localId AND p.ativo = true")
    long countPecasVinculadas(@Param("localId") UUID localId);

    /**
     * Verifica se criar uma relação pai-filho criaria um ciclo na hierarquia.
     * Usa CTE recursivo para detectar ciclos.
     *
     * Lógica: Percorre a hierarquia a partir de localId subindo até a raiz.
     * Se encontrar novoPaiId no caminho, significa que criar a relação geraria um ciclo.
     *
     * Exemplo de ciclo:
     * - Local A (id=1) tem pai B (id=2)
     * - Local B (id=2) tem pai C (id=3)
     * - Se tentarmos fazer C ter pai A, criamos ciclo: A -> B -> C -> A
     *
     * @param localId ID do local que terá o pai alterado
     * @param novoPaiId ID do novo pai
     * @return true se criaria ciclo (NÃO permitir)
     */
    @Query(value = """
        WITH RECURSIVE hierarquia AS (
            -- Base case: começa do local atual
            SELECT id, localizacao_pai_id, 1 as nivel
            FROM local_armazenamento
            WHERE id = :localId

            UNION ALL

            -- Recursive case: sobe na hierarquia através do pai
            SELECT l.id, l.localizacao_pai_id, h.nivel + 1
            FROM local_armazenamento l
            INNER JOIN hierarquia h ON l.id = h.localizacao_pai_id
            WHERE h.nivel < 10  -- Limite de profundidade para evitar loop infinito
        )
        SELECT CASE
            WHEN COUNT(*) > 0 THEN true
            ELSE false
        END
        FROM hierarquia
        WHERE localizacao_pai_id = :novoPaiId OR id = :novoPaiId
        """, nativeQuery = true)
    boolean verificaCicloHierarquia(
            @Param("localId") UUID localId,
            @Param("novoPaiId") UUID novoPaiId
    );

    /**
     * Retorna a árvore completa de locais a partir de um local raiz.
     * Busca recursiva de todos os descendentes.
     *
     * @param raizId ID do local raiz
     * @return lista de todos os descendentes
     */
    @Query(value = """
        WITH RECURSIVE arvore AS (
            -- Base case: local raiz
            SELECT id, codigo, tipo, descricao, localizacao_pai_id, 0 as nivel
            FROM local_armazenamento
            WHERE id = :raizId AND ativo = true

            UNION ALL

            -- Recursive case: filhos
            SELECT l.id, l.codigo, l.tipo, l.descricao, l.localizacao_pai_id, a.nivel + 1
            FROM local_armazenamento l
            INNER JOIN arvore a ON l.localizacao_pai_id = a.id
            WHERE l.ativo = true AND a.nivel < 10
        )
        SELECT id FROM arvore ORDER BY nivel, descricao
        """, nativeQuery = true)
    List<UUID> findArvoreDescendentes(@Param("raizId") UUID raizId);

    /**
     * Busca locais por descrição (parcial, case insensitive).
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param descricao texto a buscar
     * @return lista de locais encontrados
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE LOWER(l.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')) AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByDescricaoContainingIgnoreCaseAndAtivoTrue(@Param("descricao") String descricao);

    /**
     * Busca todos os ancestrais de um local (caminho até a raiz).
     *
     * @param localId ID do local
     * @return lista de IDs dos ancestrais (do mais próximo ao mais distante)
     */
    @Query(value = """
        WITH RECURSIVE ancestrais AS (
            -- Base case: local atual
            SELECT id, localizacao_pai_id, 0 as nivel
            FROM local_armazenamento
            WHERE id = :localId

            UNION ALL

            -- Recursive case: sobe para o pai
            SELECT l.id, l.localizacao_pai_id, a.nivel + 1
            FROM local_armazenamento l
            INNER JOIN ancestrais a ON l.id = a.localizacao_pai_id
            WHERE a.nivel < 10
        )
        SELECT id FROM ancestrais WHERE id != :localId ORDER BY nivel
        """, nativeQuery = true)
    List<UUID> findAncestores(@Param("localId") UUID localId);
}
