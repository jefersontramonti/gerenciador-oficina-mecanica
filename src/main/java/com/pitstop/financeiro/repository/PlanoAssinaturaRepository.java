package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.PlanoAssinatura;
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
 * Repositório para planos de assinatura.
 */
@Repository
public interface PlanoAssinaturaRepository extends JpaRepository<PlanoAssinatura, UUID> {

    /**
     * Busca planos por oficina.
     */
    @Query("SELECT p FROM PlanoAssinatura p WHERE p.oficina.id = :oficinaId ORDER BY p.nome")
    List<PlanoAssinatura> findByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca planos ativos por oficina.
     */
    @Query("SELECT p FROM PlanoAssinatura p WHERE p.oficina.id = :oficinaId AND p.ativo = true ORDER BY p.valor")
    List<PlanoAssinatura> findAtivosByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca plano por ID e oficina.
     */
    @Query("SELECT p FROM PlanoAssinatura p WHERE p.id = :id AND p.oficina.id = :oficinaId")
    Optional<PlanoAssinatura> findByIdAndOficinaId(@Param("id") UUID id, @Param("oficinaId") UUID oficinaId);

    /**
     * Busca planos com paginação.
     */
    @Query("SELECT p FROM PlanoAssinatura p WHERE p.oficina.id = :oficinaId ORDER BY p.ativo DESC, p.nome")
    Page<PlanoAssinatura> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Conta assinaturas ativas por plano.
     */
    @Query("SELECT COUNT(a) FROM Assinatura a WHERE a.plano.id = :planoId AND a.status = 'ATIVA'")
    long countAssinaturasAtivasByPlanoId(@Param("planoId") UUID planoId);

    /**
     * Verifica se existe plano com mesmo nome na oficina.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PlanoAssinatura p WHERE p.oficina.id = :oficinaId AND LOWER(p.nome) = LOWER(:nome) AND (:excludeId IS NULL OR p.id <> :excludeId)")
    boolean existsByOficinaIdAndNome(@Param("oficinaId") UUID oficinaId, @Param("nome") String nome, @Param("excludeId") UUID excludeId);
}
