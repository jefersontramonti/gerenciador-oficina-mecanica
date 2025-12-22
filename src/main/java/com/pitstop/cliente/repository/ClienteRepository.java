package com.pitstop.cliente.repository;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.TipoCliente;
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
 * Repositório JPA para operações de persistência da entidade {@link Cliente}.
 *
 * <p>Devido ao {@code @Where(clause = "ativo = true")} na entidade, todos os métodos
 * padrão retornam apenas clientes ativos. Para buscar clientes inativos, use consultas
 * customizadas com {@code @Query} explícitas.</p>
 *
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Busca cliente por CPF/CNPJ em uma oficina específica (apenas ativos, devido ao @Where global).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param cpfCnpj CPF ou CNPJ formatado (ex: "123.456.789-00")
     * @return Optional contendo o cliente se encontrado
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.cpfCnpj = :cpfCnpj")
    Optional<Cliente> findByOficinaIdAndCpfCnpj(@Param("oficinaId") UUID oficinaId, @Param("cpfCnpj") String cpfCnpj);

    /**
     * Verifica se existe cliente com CPF/CNPJ específico em uma oficina.
     * Ignora o filtro @Where para validar unicidade incluindo inativos.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param cpfCnpj CPF ou CNPJ formatado
     * @return true se existe algum cliente (ativo ou inativo) com este documento
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.cpfCnpj = :cpfCnpj")
    boolean existsByOficinaIdAndCpfCnpj(@Param("oficinaId") UUID oficinaId, @Param("cpfCnpj") String cpfCnpj);

    /**
     * Verifica se existe outro cliente (diferente do ID fornecido) com mesmo CPF/CNPJ em uma oficina.
     * Útil para validação em operações de UPDATE.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param cpfCnpj CPF ou CNPJ formatado
     * @param id ID do cliente atual (para exclusão da busca)
     * @return true se existe outro cliente com este documento
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.cpfCnpj = :cpfCnpj AND c.id <> :id")
    boolean existsByOficinaIdAndCpfCnpjAndIdNot(@Param("oficinaId") UUID oficinaId, @Param("cpfCnpj") String cpfCnpj, @Param("id") UUID id);

    /**
     * Busca clientes por tipo (Pessoa Física ou Jurídica) em uma oficina específica.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo do cliente
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes do tipo especificado
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.tipo = :tipo")
    Page<Cliente> findByOficinaIdAndTipo(@Param("oficinaId") UUID oficinaId, @Param("tipo") TipoCliente tipo, Pageable pageable);

    /**
     * Busca clientes cujo nome contém o termo fornecido (case-insensitive) em uma oficina específica.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param nome termo de busca
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes encontrados
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    Page<Cliente> findByOficinaIdAndNomeContainingIgnoreCase(@Param("oficinaId") UUID oficinaId, @Param("nome") String nome, Pageable pageable);

    /**
     * Busca clientes por cidade (case-insensitive) em uma oficina específica.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param cidade nome da cidade
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes da cidade especificada
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND LOWER(c.endereco.cidade) = LOWER(:cidade)")
    Page<Cliente> findByOficinaIdAndCidade(@Param("oficinaId") UUID oficinaId, @Param("cidade") String cidade, Pageable pageable);

    /**
     * Busca clientes por estado (UF) em uma oficina específica.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param estado sigla do estado (ex: "SP")
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes do estado especificado
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.endereco.estado = :estado")
    Page<Cliente> findByOficinaIdAndEstado(@Param("oficinaId") UUID oficinaId, @Param("estado") String estado, Pageable pageable);

    /**
     * Busca avançada com múltiplos filtros opcionais em uma oficina específica.
     * Retorna clientes que atendem a TODOS os critérios fornecidos.
     *
     * <p>Nota: Usa Native Query com CAST para evitar erro de serialização BYTEA do PostgreSQL
     * com enum TipoCliente. Hibernate estava serializando o enum como binário em vez de VARCHAR.</p>
     *
     * @param oficinaId ID da oficina (tenant)
     * @param nome parte do nome (null para ignorar)
     * @param tipoStr tipo de cliente como string (null para ignorar)
     * @param ativo status ativo/inativo (null para trazer todos)
     * @param cidade cidade (null para ignorar)
     * @param estado UF (null para ignorar)
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes filtrados
     */
    @Query(value = """
        SELECT * FROM clientes
        WHERE oficina_id = CAST(:oficinaId AS UUID)
        AND (:ativo IS NULL OR ativo = :ativo)
        AND (:nome IS NULL OR LOWER(nome) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
        AND (:tipoStr IS NULL OR tipo = CAST(:tipoStr AS TEXT))
        AND (:cidade IS NULL OR LOWER(cidade) = LOWER(CAST(:cidade AS TEXT)))
        AND (:estado IS NULL OR estado = CAST(:estado AS TEXT))
        """,
        countQuery = """
        SELECT COUNT(*) FROM clientes
        WHERE oficina_id = CAST(:oficinaId AS UUID)
        AND (:ativo IS NULL OR ativo = :ativo)
        AND (:nome IS NULL OR LOWER(nome) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
        AND (:tipoStr IS NULL OR tipo = CAST(:tipoStr AS TEXT))
        AND (:cidade IS NULL OR LOWER(cidade) = LOWER(CAST(:cidade AS TEXT)))
        AND (:estado IS NULL OR estado = CAST(:estado AS TEXT))
        """,
        nativeQuery = true)
    Page<Cliente> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("nome") String nome,
        @Param("tipoStr") String tipoStr,
        @Param("ativo") Boolean ativo,
        @Param("cidade") String cidade,
        @Param("estado") String estado,
        Pageable pageable
    );

    /**
     * Busca todos os clientes de uma oficina incluindo inativos (ignora @Where).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable configuração de paginação e ordenação
     * @return página de todos os clientes (ativos e inativos)
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId")
    Page<Cliente> findAllByOficinaIdIncludingInactive(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Busca apenas clientes inativos (soft deleted) de uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes inativos
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.ativo = false")
    Page<Cliente> findInactiveClientesByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Busca cliente por ID de uma oficina incluindo inativos (ignora @Where clause).
     * Usa Native Query para bypassar completamente o filtro Hibernate.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id identificador do cliente
     * @return Optional contendo o cliente (ativo ou inativo)
     */
    @Query(value = "SELECT * FROM clientes WHERE oficina_id = CAST(:oficinaId AS UUID) AND id = :id", nativeQuery = true)
    Optional<Cliente> findByOficinaIdAndIdIncludingInactive(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Conta total de clientes ativos em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de clientes ativos (devido ao @Where clause, já filtra ativos)
     */
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.oficina.id = :oficinaId")
    long countByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta total de clientes ativos por tipo em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param tipo tipo do cliente
     * @return quantidade de clientes ativos do tipo especificado
     */
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.tipo = :tipo")
    long countByOficinaIdAndTipo(@Param("oficinaId") UUID oficinaId, @Param("tipo") TipoCliente tipo);

    /**
     * Retorna lista de todos os estados (UF) presentes nos endereços de clientes ativos de uma oficina.
     * Útil para filtros dinâmicos na UI.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de siglas de estados (ex: ["SP", "RJ", "MG"])
     */
    @Query("SELECT DISTINCT c.endereco.estado FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.endereco.estado IS NOT NULL ORDER BY c.endereco.estado")
    List<String> findDistinctEstadosByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Retorna lista de todas as cidades presentes nos endereços de clientes ativos de uma oficina.
     * Útil para filtros dinâmicos na UI.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de nomes de cidades ordenadas alfabeticamente
     */
    @Query("SELECT DISTINCT c.endereco.cidade FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.endereco.cidade IS NOT NULL ORDER BY c.endereco.cidade")
    List<String> findDistinctCidadesByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca cliente por ID em uma oficina (apenas ativos, devido ao @Where global).
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID do cliente
     * @return Optional contendo o cliente se encontrado
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId AND c.id = :id")
    Optional<Cliente> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca todos os clientes ativos de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes ativos
     */
    @Query("SELECT c FROM Cliente c WHERE c.oficina.id = :oficinaId")
    Page<Cliente> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Alias para findDistinctEstadosByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de estados únicos
     */
    default List<String> findDistinctEstados(UUID oficinaId) {
        return findDistinctEstadosByOficinaId(oficinaId);
    }

    /**
     * Alias para findDistinctCidadesByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de cidades únicas
     */
    default List<String> findDistinctCidades(UUID oficinaId) {
        return findDistinctCidadesByOficinaId(oficinaId);
    }
}
