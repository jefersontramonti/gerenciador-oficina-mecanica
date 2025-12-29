package com.pitstop.saas.repository;

import com.pitstop.saas.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    Optional<FeatureFlag> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<FeatureFlag> findByCategoria(String categoria);

    List<FeatureFlag> findByHabilitadoGlobalTrue();

    @Query("SELECT DISTINCT f.categoria FROM FeatureFlag f ORDER BY f.categoria")
    List<String> findAllCategorias();

    @Query(value = "SELECT * FROM feature_flags f WHERE f.habilitado_global = true OR :oficinaId = ANY(f.habilitado_por_oficina)", nativeQuery = true)
    List<FeatureFlag> findEnabledForOficina(@Param("oficinaId") UUID oficinaId);

    @Query("SELECT f FROM FeatureFlag f ORDER BY f.categoria, f.nome")
    List<FeatureFlag> findAllOrderByCategoriaAndNome();
}
