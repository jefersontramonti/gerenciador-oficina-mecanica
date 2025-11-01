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
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /**
     * Busca cliente por CPF/CNPJ (apenas ativos, devido ao @Where global).
     *
     * @param cpfCnpj CPF ou CNPJ formatado (ex: "123.456.789-00")
     * @return Optional contendo o cliente se encontrado
     */
    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    /**
     * Verifica se existe cliente com CPF/CNPJ específico.
     * Ignora o filtro @Where para validar unicidade incluindo inativos.
     *
     * @param cpfCnpj CPF ou CNPJ formatado
     * @return true se existe algum cliente (ativo ou inativo) com este documento
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cliente c WHERE c.cpfCnpj = :cpfCnpj")
    boolean existsByCpfCnpj(@Param("cpfCnpj") String cpfCnpj);

    /**
     * Verifica se existe outro cliente (diferente do ID fornecido) com mesmo CPF/CNPJ.
     * Útil para validação em operações de UPDATE.
     *
     * @param cpfCnpj CPF ou CNPJ formatado
     * @param id ID do cliente atual (para exclusão da busca)
     * @return true se existe outro cliente com este documento
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cliente c WHERE c.cpfCnpj = :cpfCnpj AND c.id <> :id")
    boolean existsByCpfCnpjAndIdNot(@Param("cpfCnpj") String cpfCnpj, @Param("id") UUID id);

    /**
     * Busca clientes por tipo (Pessoa Física ou Jurídica) com paginação.
     *
     * @param tipo tipo do cliente
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes do tipo especificado
     */
    Page<Cliente> findByTipo(TipoCliente tipo, Pageable pageable);

    /**
     * Busca clientes cujo nome contém o termo fornecido (case-insensitive).
     *
     * @param nome termo de busca
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes encontrados
     */
    Page<Cliente> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /**
     * Busca clientes por cidade (case-insensitive).
     *
     * @param cidade nome da cidade
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes da cidade especificada
     */
    @Query("SELECT c FROM Cliente c WHERE LOWER(c.endereco.cidade) = LOWER(:cidade)")
    Page<Cliente> findByCidade(@Param("cidade") String cidade, Pageable pageable);

    /**
     * Busca clientes por estado (UF).
     *
     * @param estado sigla do estado (ex: "SP")
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes do estado especificado
     */
    @Query("SELECT c FROM Cliente c WHERE c.endereco.estado = :estado")
    Page<Cliente> findByEstado(@Param("estado") String estado, Pageable pageable);

    /**
     * Busca avançada com múltiplos filtros opcionais.
     * Retorna clientes que atendem a TODOS os critérios fornecidos.
     *
     * <p>Nota: Usa Native Query com CAST para evitar erro de serialização BYTEA do PostgreSQL
     * com enum TipoCliente. Hibernate estava serializando o enum como binário em vez de VARCHAR.</p>
     *
     * @param nome parte do nome (null para ignorar)
     * @param tipoStr tipo de cliente como string (null para ignorar)
     * @param cidade cidade (null para ignorar)
     * @param estado UF (null para ignorar)
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes filtrados
     */
    @Query(value = """
        SELECT * FROM clientes
        WHERE ativo = true
        AND (:nome IS NULL OR LOWER(nome) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
        AND (:tipoStr IS NULL OR tipo = CAST(:tipoStr AS TEXT))
        AND (:cidade IS NULL OR LOWER(cidade) = LOWER(CAST(:cidade AS TEXT)))
        AND (:estado IS NULL OR estado = CAST(:estado AS TEXT))
        """,
        countQuery = """
        SELECT COUNT(*) FROM clientes
        WHERE ativo = true
        AND (:nome IS NULL OR LOWER(nome) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
        AND (:tipoStr IS NULL OR tipo = CAST(:tipoStr AS TEXT))
        AND (:cidade IS NULL OR LOWER(cidade) = LOWER(CAST(:cidade AS TEXT)))
        AND (:estado IS NULL OR estado = CAST(:estado AS TEXT))
        """,
        nativeQuery = true)
    Page<Cliente> findByFiltros(
        @Param("nome") String nome,
        @Param("tipoStr") String tipoStr,
        @Param("cidade") String cidade,
        @Param("estado") String estado,
        Pageable pageable
    );

    /**
     * Busca todos os clientes incluindo inativos (ignora @Where).
     *
     * @param pageable configuração de paginação e ordenação
     * @return página de todos os clientes (ativos e inativos)
     */
    @Query("SELECT c FROM Cliente c")
    Page<Cliente> findAllIncludingInactive(Pageable pageable);

    /**
     * Busca apenas clientes inativos (soft deleted).
     *
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes inativos
     */
    @Query("SELECT c FROM Cliente c WHERE c.ativo = false")
    Page<Cliente> findInactiveClientes(Pageable pageable);

    /**
     * Busca cliente por ID incluindo inativos (ignora @Where clause).
     * Usa Native Query para bypassar completamente o filtro Hibernate.
     *
     * @param id identificador do cliente
     * @return Optional contendo o cliente (ativo ou inativo)
     */
    @Query(value = "SELECT * FROM clientes WHERE id = :id", nativeQuery = true)
    Optional<Cliente> findByIdIncludingInactive(@Param("id") UUID id);

    /**
     * Conta total de clientes ativos por tipo.
     *
     * @param tipo tipo do cliente
     * @return quantidade de clientes ativos do tipo especificado
     */
    long countByTipo(TipoCliente tipo);

    /**
     * Retorna lista de todos os estados (UF) presentes nos endereços de clientes ativos.
     * Útil para filtros dinâmicos na UI.
     *
     * @return lista de siglas de estados (ex: ["SP", "RJ", "MG"])
     */
    @Query("SELECT DISTINCT c.endereco.estado FROM Cliente c WHERE c.endereco.estado IS NOT NULL ORDER BY c.endereco.estado")
    List<String> findDistinctEstados();

    /**
     * Retorna lista de todas as cidades presentes nos endereços de clientes ativos.
     * Útil para filtros dinâmicos na UI.
     *
     * @return lista de nomes de cidades ordenadas alfabeticamente
     */
    @Query("SELECT DISTINCT c.endereco.cidade FROM Cliente c WHERE c.endereco.cidade IS NOT NULL ORDER BY c.endereco.cidade")
    List<String> findDistinctCidades();
}
