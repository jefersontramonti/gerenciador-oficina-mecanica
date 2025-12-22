package com.pitstop.estoque.repository;

import com.pitstop.estoque.domain.LocalArmazenamento;
import com.pitstop.estoque.domain.TipoLocal;
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
 * Repository para gerenciamento de locais de armazenamento.
 * Fornece queries especializadas para navegação hierárquica e validação de ciclos.
 *
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Repository
public interface LocalArmazenamentoRepository extends JpaRepository<LocalArmazenamento, UUID> {

    /**
     * Busca local por ID com locaisFilhos carregados (evita LazyInitializationException) em uma oficina.
     * Sobrescreve o findById padrão do JPA.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID do local
     * @return Optional contendo o local se encontrado
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND l.id = :id")
    Optional<LocalArmazenamento> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca local por código (normalizado para uppercase) com locaisFilhos carregados em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código único do local
     * @return Optional contendo o local se encontrado
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND l.codigo = :codigo")
    Optional<LocalArmazenamento> findByOficinaIdAndCodigo(@Param("oficinaId") UUID oficinaId, @Param("codigo") String codigo);

    /**
     * Verifica se existe local com o código especificado em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código a verificar
     * @return true se existe
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalArmazenamento l WHERE l.oficina.id = :oficinaId AND l.codigo = :codigo")
    boolean existsByOficinaIdAndCodigo(@Param("oficinaId") UUID oficinaId, @Param("codigo") String codigo);

    /**
     * Verifica se existe local com o código, excluindo um ID específico em uma oficina.
     * Útil para validação em updates.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param codigo código a verificar
     * @param id ID a excluir da verificação
     * @return true se existe outro local com o mesmo código
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LocalArmazenamento l WHERE l.oficina.id = :oficinaId AND l.codigo = :codigo AND l.id <> :id")
    boolean existsByOficinaIdAndCodigoAndIdNot(@Param("oficinaId") UUID oficinaId, @Param("codigo") String codigo, @Param("id") UUID id);

    /**
     * Lista todos os locais ativos com locaisFilhos carregados (evita LazyInitializationException) em uma oficina.
     * Usa LEFT JOIN FETCH para carregar a coleção locaisFilhos na mesma query.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de locais ativos
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByOficinaIdAndAtivoTrue(@Param("oficinaId") UUID oficinaId);

    /**
     * Lista locais raiz (sem pai) que estão ativos em uma oficina.
     * Normalmente são depósitos ou vitrines principais.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de locais raiz
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND l.localizacaoPai IS NULL AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findLocaisRaizByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Lista locais filhos de um pai específico em uma oficina.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param paiId ID do local pai
     * @return lista de locais filhos
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND l.localizacaoPai.id = :paiId AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByOficinaIdAndLocalizacaoPaiId(@Param("oficinaId") UUID oficinaId, @Param("paiId") UUID paiId);

    /**
     * Lista locais por tipo em uma oficina.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo do local
     * @return lista de locais do tipo especificado
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND l.tipo = :tipo AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByOficinaIdAndTipoAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("tipo") TipoLocal tipo);

    /**
     * Conta quantas peças estão vinculadas a um local específico em uma oficina.
     * Usado para impedir exclusão de locais com peças.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param localId ID do local
     * @return quantidade de peças vinculadas
     */
    @Query("SELECT COUNT(p) FROM Peca p WHERE p.oficina.id = :oficinaId AND p.localArmazenamento.id = :localId AND p.ativo = true")
    long countPecasVinculadasByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("localId") UUID localId);

    /**
     * Verifica se criar uma relação pai-filho criaria um ciclo na hierarquia em uma oficina.
     * Usa CTE recursivo para detectar ciclos.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param localId ID do local que terá o pai alterado
     * @param novoPaiId ID do novo pai
     * @return true se criaria ciclo (NÃO permitir)
     */
    @Query(value = """
        WITH RECURSIVE hierarquia AS (
            SELECT id, localizacao_pai_id, 1 as nivel
            FROM local_armazenamento
            WHERE oficina_id = CAST(:oficinaId AS UUID) AND id = :localId

            UNION ALL

            SELECT l.id, l.localizacao_pai_id, h.nivel + 1
            FROM local_armazenamento l
            INNER JOIN hierarquia h ON l.id = h.localizacao_pai_id
            WHERE l.oficina_id = CAST(:oficinaId AS UUID) AND h.nivel < 10
        )
        SELECT CASE
            WHEN COUNT(*) > 0 THEN true
            ELSE false
        END
        FROM hierarquia
        WHERE localizacao_pai_id = :novoPaiId OR id = :novoPaiId
        """, nativeQuery = true)
    boolean verificaCicloHierarquiaByOficinaId(
            @Param("oficinaId") UUID oficinaId,
            @Param("localId") UUID localId,
            @Param("novoPaiId") UUID novoPaiId
    );

    /**
     * Retorna a árvore completa de locais a partir de um local raiz em uma oficina.
     * Busca recursiva de todos os descendentes.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param raizId ID do local raiz
     * @return lista de todos os descendentes
     */
    @Query(value = """
        WITH RECURSIVE arvore AS (
            SELECT id, codigo, tipo, descricao, localizacao_pai_id, 0 as nivel
            FROM local_armazenamento
            WHERE oficina_id = CAST(:oficinaId AS UUID) AND id = :raizId AND ativo = true

            UNION ALL

            SELECT l.id, l.codigo, l.tipo, l.descricao, l.localizacao_pai_id, a.nivel + 1
            FROM local_armazenamento l
            INNER JOIN arvore a ON l.localizacao_pai_id = a.id
            WHERE l.oficina_id = CAST(:oficinaId AS UUID) AND l.ativo = true AND a.nivel < 10
        )
        SELECT id FROM arvore ORDER BY nivel, descricao
        """, nativeQuery = true)
    List<UUID> findArvoreDescendentesByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("raizId") UUID raizId);

    /**
     * Busca locais por descrição (parcial, case insensitive) em uma oficina.
     * Usa LEFT JOIN FETCH para carregar locaisFilhos.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param descricao texto a buscar
     * @return lista de locais encontrados
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId AND LOWER(l.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')) AND l.ativo = true ORDER BY l.descricao")
    List<LocalArmazenamento> findByOficinaIdAndDescricaoContainingIgnoreCaseAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("descricao") String descricao);

    /**
     * Busca todos os ancestrais de um local (caminho até a raiz) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param localId ID do local
     * @return lista de IDs dos ancestrais (do mais próximo ao mais distante)
     */
    @Query(value = """
        WITH RECURSIVE ancestrais AS (
            SELECT id, localizacao_pai_id, 0 as nivel
            FROM local_armazenamento
            WHERE oficina_id = CAST(:oficinaId AS UUID) AND id = :localId

            UNION ALL

            SELECT l.id, l.localizacao_pai_id, a.nivel + 1
            FROM local_armazenamento l
            INNER JOIN ancestrais a ON l.id = a.localizacao_pai_id
            WHERE l.oficina_id = CAST(:oficinaId AS UUID) AND a.nivel < 10
        )
        SELECT id FROM ancestrais WHERE id != :localId ORDER BY nivel
        """, nativeQuery = true)
    List<UUID> findAncestoresByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("localId") UUID localId);

    /**
     * Busca todos os locais de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de locais
     */
    @Query("SELECT DISTINCT l FROM LocalArmazenamento l LEFT JOIN FETCH l.locaisFilhos WHERE l.oficina.id = :oficinaId")
    Page<LocalArmazenamento> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Alias para findLocaisRaizByOficinaId (compatibilidade).
     */
    default List<LocalArmazenamento> findLocaisRaiz(UUID oficinaId) {
        return findLocaisRaizByOficinaId(oficinaId);
    }

    /**
     * Alias para countPecasVinculadasByOficinaId (compatibilidade).
     */
    default long countPecasVinculadas(UUID oficinaId, UUID localId) {
        return countPecasVinculadasByOficinaId(oficinaId, localId);
    }

    /**
     * Alias para verificaCicloHierarquiaByOficinaId (compatibilidade).
     */
    default boolean verificaCicloHierarquia(UUID oficinaId, UUID localId, UUID novoPaiId) {
        return verificaCicloHierarquiaByOficinaId(oficinaId, localId, novoPaiId);
    }
}
