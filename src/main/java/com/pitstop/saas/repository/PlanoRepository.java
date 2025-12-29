package com.pitstop.saas.repository;

import com.pitstop.saas.domain.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Plano entity operations.
 */
@Repository
public interface PlanoRepository extends JpaRepository<Plano, UUID> {

    /**
     * Find a plan by its unique code.
     *
     * @param codigo the plan code (e.g., "ECONOMICO", "PROFISSIONAL")
     * @return the plan if found
     */
    Optional<Plano> findByCodigo(String codigo);

    /**
     * Check if a plan with the given code exists.
     *
     * @param codigo the plan code
     * @return true if exists
     */
    boolean existsByCodigo(String codigo);

    /**
     * Check if a plan with the given code exists, excluding a specific ID.
     *
     * @param codigo the plan code
     * @param id the ID to exclude
     * @return true if exists
     */
    @Query("SELECT COUNT(p) > 0 FROM Plano p WHERE p.codigo = :codigo AND p.id != :id")
    boolean existsByCodigoAndIdNot(@Param("codigo") String codigo, @Param("id") UUID id);

    /**
     * Find all active plans.
     *
     * @return list of active plans
     */
    List<Plano> findByAtivoTrueOrderByOrdemExibicaoAsc();

    /**
     * Find all visible and active plans (for pricing page).
     *
     * @return list of visible plans ordered by display order
     */
    @Query("SELECT p FROM Plano p WHERE p.ativo = true AND p.visivel = true ORDER BY p.ordemExibicao ASC")
    List<Plano> findVisiblePlans();

    /**
     * Find all plans ordered by display order.
     *
     * @return list of all plans
     */
    List<Plano> findAllByOrderByOrdemExibicaoAsc();

    /**
     * Find the recommended plan.
     *
     * @return the recommended plan if any
     */
    Optional<Plano> findByRecomendadoTrue();

    /**
     * Count active plans.
     *
     * @return count of active plans
     */
    long countByAtivoTrue();

    /**
     * Find plans by feature enabled.
     *
     * @param featureKey the feature key to check (use native query for JSONB)
     * @return list of plans with the feature enabled
     */
    @Query(value = "SELECT * FROM planos WHERE ativo = true AND (features->:featureKey)::boolean = true ORDER BY ordem_exibicao", nativeQuery = true)
    List<Plano> findByFeatureEnabled(@Param("featureKey") String featureKey);
}
