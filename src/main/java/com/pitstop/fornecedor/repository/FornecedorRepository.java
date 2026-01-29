package com.pitstop.fornecedor.repository;

import com.pitstop.fornecedor.domain.Fornecedor;
import com.pitstop.fornecedor.domain.TipoFornecedor;
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
 * Repositório JPA para a entidade {@link Fornecedor}.
 *
 * <p>Multi-tenancy: todos os métodos exigem {@code oficinaId}.</p>
 * <p>Soft delete: @Where(clause = "ativo = true") filtra automaticamente.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {

    @Query("SELECT f FROM Fornecedor f WHERE f.oficina.id = :oficinaId AND f.id = :id")
    Optional<Fornecedor> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    @Query(value = "SELECT * FROM fornecedores WHERE oficina_id = CAST(:oficinaId AS UUID) AND id = :id", nativeQuery = true)
    Optional<Fornecedor> findByOficinaIdAndIdIncludingInactive(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    @Query("SELECT f FROM Fornecedor f WHERE f.oficina.id = :oficinaId")
    Page<Fornecedor> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    @Query("SELECT f FROM Fornecedor f WHERE f.oficina.id = :oficinaId ORDER BY f.nomeFantasia ASC")
    List<Fornecedor> findAllByOficinaIdOrderByNomeFantasia(@Param("oficinaId") UUID oficinaId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Fornecedor f WHERE f.oficina.id = :oficinaId AND f.cpfCnpj = :cpfCnpj")
    boolean existsByOficinaIdAndCpfCnpj(@Param("oficinaId") UUID oficinaId, @Param("cpfCnpj") String cpfCnpj);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Fornecedor f WHERE f.oficina.id = :oficinaId AND f.cpfCnpj = :cpfCnpj AND f.id <> :id")
    boolean existsByOficinaIdAndCpfCnpjAndIdNot(@Param("oficinaId") UUID oficinaId, @Param("cpfCnpj") String cpfCnpj, @Param("id") UUID id);

    /**
     * Busca com múltiplos filtros usando Native Query para evitar BYTEA issues.
     */
    @Query(value = """
        SELECT * FROM fornecedores
        WHERE oficina_id = CAST(:oficinaId AS UUID)
        AND ativo = true
        AND (:nome IS NULL OR LOWER(nome_fantasia) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
        AND (:tipoStr IS NULL OR tipo = CAST(:tipoStr AS TEXT))
        AND (:cidade IS NULL OR LOWER(cidade) = LOWER(CAST(:cidade AS TEXT)))
        ORDER BY nome_fantasia ASC
        """,
        countQuery = """
        SELECT COUNT(*) FROM fornecedores
        WHERE oficina_id = CAST(:oficinaId AS UUID)
        AND ativo = true
        AND (:nome IS NULL OR LOWER(nome_fantasia) LIKE LOWER(CONCAT('%', CAST(:nome AS TEXT), '%')))
        AND (:tipoStr IS NULL OR tipo = CAST(:tipoStr AS TEXT))
        AND (:cidade IS NULL OR LOWER(cidade) = LOWER(CAST(:cidade AS TEXT)))
        """,
        nativeQuery = true)
    Page<Fornecedor> findByFiltros(
        @Param("oficinaId") UUID oficinaId,
        @Param("nome") String nome,
        @Param("tipoStr") String tipoStr,
        @Param("cidade") String cidade,
        Pageable pageable
    );

    @Query("SELECT COUNT(f) FROM Fornecedor f WHERE f.oficina.id = :oficinaId")
    long countByOficinaId(@Param("oficinaId") UUID oficinaId);
}
