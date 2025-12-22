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
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, UUID> {

    /**
     * Busca veículo por placa (normalizada, sem hífen) em uma oficina específica.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param placa placa do veículo (7 caracteres uppercase)
     * @return Optional contendo o veículo se encontrado
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.placa = :placa")
    Optional<Veiculo> findByOficinaIdAndPlaca(@Param("oficinaId") UUID oficinaId, @Param("placa") String placa);

    /**
     * Verifica se existe veículo com placa específica em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param placa placa do veículo
     * @return true se existe veículo com esta placa
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.placa = :placa")
    boolean existsByOficinaIdAndPlaca(@Param("oficinaId") UUID oficinaId, @Param("placa") String placa);

    /**
     * Verifica se existe outro veículo (diferente do ID fornecido) com mesma placa em uma oficina.
     * Útil para validação em operações de UPDATE.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param placa placa do veículo
     * @param id ID do veículo atual (para exclusão da busca)
     * @return true se existe outro veículo com esta placa
     */
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.placa = :placa AND v.id <> :id")
    boolean existsByOficinaIdAndPlacaAndIdNot(@Param("oficinaId") UUID oficinaId, @Param("placa") String placa, @Param("id") UUID id);

    /**
     * Busca todos os veículos de um cliente específico em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param clienteId ID do cliente
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos do cliente
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.clienteId = :clienteId")
    Page<Veiculo> findByOficinaIdAndClienteId(@Param("oficinaId") UUID oficinaId, @Param("clienteId") UUID clienteId, Pageable pageable);

    /**
     * Conta total de veículos em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return quantidade de veículos
     */
    @Query("SELECT COUNT(v) FROM Veiculo v WHERE v.oficina.id = :oficinaId")
    long countByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Conta total de veículos de um cliente em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param clienteId ID do cliente
     * @return quantidade de veículos
     */
    @Query("SELECT COUNT(v) FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.clienteId = :clienteId")
    long countByOficinaIdAndClienteId(@Param("oficinaId") UUID oficinaId, @Param("clienteId") UUID clienteId);

    /**
     * Busca veículos por marca (case-insensitive) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param marca nome da marca
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos da marca especificada
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND LOWER(v.marca) LIKE LOWER(CONCAT('%', :marca, '%'))")
    Page<Veiculo> findByOficinaIdAndMarcaContainingIgnoreCase(@Param("oficinaId") UUID oficinaId, @Param("marca") String marca, Pageable pageable);

    /**
     * Busca veículos por modelo (case-insensitive) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param modelo nome do modelo
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos do modelo especificado
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND LOWER(v.modelo) LIKE LOWER(CONCAT('%', :modelo, '%'))")
    Page<Veiculo> findByOficinaIdAndModeloContainingIgnoreCase(@Param("oficinaId") UUID oficinaId, @Param("modelo") String modelo, Pageable pageable);

    /**
     * Busca veículos por marca e modelo (case-insensitive) em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param marca nome da marca
     * @param modelo nome do modelo
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos encontrados
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND LOWER(v.marca) LIKE LOWER(CONCAT('%', :marca, '%')) AND LOWER(v.modelo) LIKE LOWER(CONCAT('%', :modelo, '%'))")
    Page<Veiculo> findByOficinaIdAndMarcaAndModelo(@Param("oficinaId") UUID oficinaId, @Param("marca") String marca, @Param("modelo") String modelo, Pageable pageable);

    /**
     * Busca veículos por ano de fabricação em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ano ano de fabricação
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos do ano especificado
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.ano = :ano")
    Page<Veiculo> findByOficinaIdAndAno(@Param("oficinaId") UUID oficinaId, @Param("ano") Integer ano, Pageable pageable);

    /**
     * Busca veículos por chassi em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param chassi número do chassi
     * @return Optional contendo o veículo se encontrado
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.chassi = :chassi")
    Optional<Veiculo> findByOficinaIdAndChassi(@Param("oficinaId") UUID oficinaId, @Param("chassi") String chassi);

    /**
     * Busca avançada com múltiplos filtros opcionais em uma oficina específica.
     * Retorna veículos que atendem a TODOS os critérios fornecidos.
     *
     * <p>Nota: Usa Native Query com CAST para evitar erro de serialização BYTEA do PostgreSQL.
     * Hibernate estava serializando parâmetros string como binário em vez de TEXT.</p>
     *
     * @param oficinaId ID da oficina (tenant)
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
        WHERE oficina_id = CAST(:oficinaId AS UUID)
        AND (:clienteId IS NULL OR cliente_id = CAST(:clienteId AS UUID))
        AND (:placa IS NULL OR LOWER(placa) LIKE LOWER(CONCAT('%', CAST(:placa AS TEXT), '%')))
        AND (:marca IS NULL OR LOWER(marca) LIKE LOWER(CONCAT('%', CAST(:marca AS TEXT), '%')))
        AND (:modelo IS NULL OR LOWER(modelo) LIKE LOWER(CONCAT('%', CAST(:modelo AS TEXT), '%')))
        AND (:ano IS NULL OR ano = CAST(:ano AS INTEGER))
        """,
        countQuery = """
        SELECT COUNT(*) FROM veiculos
        WHERE oficina_id = CAST(:oficinaId AS UUID)
        AND (:clienteId IS NULL OR cliente_id = CAST(:clienteId AS UUID))
        AND (:placa IS NULL OR LOWER(placa) LIKE LOWER(CONCAT('%', CAST(:placa AS TEXT), '%')))
        AND (:marca IS NULL OR LOWER(marca) LIKE LOWER(CONCAT('%', CAST(:marca AS TEXT), '%')))
        AND (:modelo IS NULL OR LOWER(modelo) LIKE LOWER(CONCAT('%', CAST(:modelo AS TEXT), '%')))
        AND (:ano IS NULL OR ano = CAST(:ano AS INTEGER))
        """,
        nativeQuery = true)
    Page<Veiculo> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("clienteId") UUID clienteId,
        @Param("placa") String placa,
        @Param("marca") String marca,
        @Param("modelo") String modelo,
        @Param("ano") Integer ano,
        Pageable pageable
    );

    /**
     * Retorna lista de marcas únicas presentes nos veículos cadastrados de uma oficina.
     * Útil para filtros dinâmicos na UI.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de nomes de marcas ordenadas alfabeticamente
     */
    @Query("SELECT DISTINCT v.marca FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.marca IS NOT NULL ORDER BY v.marca")
    List<String> findDistinctMarcasByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Retorna lista de modelos únicos presentes nos veículos cadastrados de uma oficina.
     * Útil para filtros dinâmicos na UI.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de nomes de modelos ordenados alfabeticamente
     */
    @Query("SELECT DISTINCT v.modelo FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.modelo IS NOT NULL ORDER BY v.modelo")
    List<String> findDistinctModelosByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Retorna lista de anos únicos presentes nos veículos cadastrados de uma oficina.
     * Útil para filtros dinâmicos na UI.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de anos ordenados decrescentemente
     */
    @Query("SELECT DISTINCT v.ano FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.ano IS NOT NULL ORDER BY v.ano DESC")
    List<Integer> findDistinctAnosByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca veículo por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID do veículo
     * @return Optional contendo o veículo se encontrado
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId AND v.id = :id")
    Optional<Veiculo> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Busca todos os veículos de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos
     */
    @Query("SELECT v FROM Veiculo v WHERE v.oficina.id = :oficinaId")
    Page<Veiculo> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

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
     * Alias para findDistinctModelosByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de modelos únicos
     */
    default List<String> findDistinctModelos(UUID oficinaId) {
        return findDistinctModelosByOficinaId(oficinaId);
    }

    /**
     * Alias para findDistinctAnosByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de anos únicos
     */
    default List<Integer> findDistinctAnos(UUID oficinaId) {
        return findDistinctAnosByOficinaId(oficinaId);
    }
}
