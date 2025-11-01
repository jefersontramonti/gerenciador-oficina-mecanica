package com.pitstop.veiculo.repository;

import com.pitstop.veiculo.domain.Veiculo;
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
 * Repositório JPA para operações de persistência da entidade {@link Veiculo}.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    /**
     * Busca veículo por placa (normalizada, sem hífen).
     *
     * @param placa placa do veículo (7 caracteres uppercase)
     * @return Optional contendo o veículo se encontrado
     */
    Optional<Veiculo> findByPlaca(String placa);

    /**
     * Verifica se existe veículo com placa específica.
     *
     * @param placa placa do veículo
     * @return true se existe veículo com esta placa
     */
    boolean existsByPlaca(String placa);

    /**
     * Verifica se existe outro veículo (diferente do ID fornecido) com mesma placa.
     * Útil para validação em operações de UPDATE.
     *
     * @param placa placa do veículo
     * @param id ID do veículo atual (para exclusão da busca)
     * @return true se existe outro veículo com esta placa
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Veiculo v WHERE v.placa = :placa AND v.id <> :id")
    boolean existsByPlacaAndIdNot(@Param("placa") String placa, @Param("id") UUID id);

    /**
     * Busca todos os veículos de um cliente específico.
     *
     * @param clienteId ID do cliente
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos do cliente
     */
    Page<Veiculo> findByClienteId(UUID clienteId, Pageable pageable);

    /**
     * Conta total de veículos de um cliente.
     *
     * @param clienteId ID do cliente
     * @return quantidade de veículos
     */
    long countByClienteId(UUID clienteId);

    /**
     * Busca veículos por marca (case-insensitive).
     *
     * @param marca nome da marca
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos da marca especificada
     */
    Page<Veiculo> findByMarcaContainingIgnoreCase(String marca, Pageable pageable);

    /**
     * Busca veículos por modelo (case-insensitive).
     *
     * @param modelo nome do modelo
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos do modelo especificado
     */
    Page<Veiculo> findByModeloContainingIgnoreCase(String modelo, Pageable pageable);

    /**
     * Busca veículos por marca e modelo (case-insensitive).
     *
     * @param marca nome da marca
     * @param modelo nome do modelo
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos encontrados
     */
    @Query("SELECT v FROM Veiculo v WHERE LOWER(v.marca) LIKE LOWER(CONCAT('%', :marca, '%')) AND LOWER(v.modelo) LIKE LOWER(CONCAT('%', :modelo, '%'))")
    Page<Veiculo> findByMarcaAndModelo(@Param("marca") String marca, @Param("modelo") String modelo, Pageable pageable);

    /**
     * Busca veículos por ano de fabricação.
     *
     * @param ano ano de fabricação
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos do ano especificado
     */
    Page<Veiculo> findByAno(Integer ano, Pageable pageable);

    /**
     * Busca veículos por chassi.
     *
     * @param chassi número do chassi
     * @return Optional contendo o veículo se encontrado
     */
    Optional<Veiculo> findByChassi(String chassi);

    /**
     * Busca avançada com múltiplos filtros opcionais.
     * Retorna veículos que atendem a TODOS os critérios fornecidos.
     *
     * <p>Nota: Usa Native Query com CAST para evitar erro de serialização BYTEA do PostgreSQL.
     * Hibernate estava serializando parâmetros string como binário em vez de TEXT.</p>
     *
     * @param clienteId ID do cliente (null para ignorar)
     * @param placa placa parcial (null para ignorar)
     * @param marca marca parcial (null para ignorar)
     * @param modelo modelo parcial (null para ignorar)
     * @param ano ano de fabricação (null para ignorar)
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos filtrados
     */
    @Query(value = """
        SELECT * FROM veiculos
        WHERE (:clienteId IS NULL OR cliente_id = CAST(:clienteId AS UUID))
        AND (:placa IS NULL OR LOWER(placa) LIKE LOWER(CONCAT('%', CAST(:placa AS TEXT), '%')))
        AND (:marca IS NULL OR LOWER(marca) LIKE LOWER(CONCAT('%', CAST(:marca AS TEXT), '%')))
        AND (:modelo IS NULL OR LOWER(modelo) LIKE LOWER(CONCAT('%', CAST(:modelo AS TEXT), '%')))
        AND (:ano IS NULL OR ano = CAST(:ano AS INTEGER))
        """,
        countQuery = """
        SELECT COUNT(*) FROM veiculos
        WHERE (:clienteId IS NULL OR cliente_id = CAST(:clienteId AS UUID))
        AND (:placa IS NULL OR LOWER(placa) LIKE LOWER(CONCAT('%', CAST(:placa AS TEXT), '%')))
        AND (:marca IS NULL OR LOWER(marca) LIKE LOWER(CONCAT('%', CAST(:marca AS TEXT), '%')))
        AND (:modelo IS NULL OR LOWER(modelo) LIKE LOWER(CONCAT('%', CAST(:modelo AS TEXT), '%')))
        AND (:ano IS NULL OR ano = CAST(:ano AS INTEGER))
        """,
        nativeQuery = true)
    Page<Veiculo> findByFiltros(
        @Param("clienteId") UUID clienteId,
        @Param("placa") String placa,
        @Param("marca") String marca,
        @Param("modelo") String modelo,
        @Param("ano") Integer ano,
        Pageable pageable
    );

    /**
     * Retorna lista de marcas únicas presentes nos veículos cadastrados.
     * Útil para filtros dinâmicos na UI.
     *
     * @return lista de nomes de marcas ordenadas alfabeticamente
     */
    @Query("SELECT DISTINCT v.marca FROM Veiculo v WHERE v.marca IS NOT NULL ORDER BY v.marca")
    List<String> findDistinctMarcas();

    /**
     * Retorna lista de modelos únicos presentes nos veículos cadastrados.
     * Útil para filtros dinâmicos na UI.
     *
     * @return lista de nomes de modelos ordenados alfabeticamente
     */
    @Query("SELECT DISTINCT v.modelo FROM Veiculo v WHERE v.modelo IS NOT NULL ORDER BY v.modelo")
    List<String> findDistinctModelos();

    /**
     * Retorna lista de anos únicos presentes nos veículos cadastrados.
     * Útil para filtros dinâmicos na UI.
     *
     * @return lista de anos ordenados decrescentemente
     */
    @Query("SELECT DISTINCT v.ano FROM Veiculo v WHERE v.ano IS NOT NULL ORDER BY v.ano DESC")
    List<Integer> findDistinctAnos();
}
