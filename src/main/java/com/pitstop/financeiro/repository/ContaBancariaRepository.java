package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.ContaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, UUID> {

    /**
     * Busca todas as contas bancárias de uma oficina.
     */
    List<ContaBancaria> findByOficinaIdOrderByPadraoDescNomeAsc(UUID oficinaId);

    /**
     * Busca apenas as contas ativas de uma oficina.
     */
    List<ContaBancaria> findByOficinaIdAndAtivoTrueOrderByPadraoDescNomeAsc(UUID oficinaId);

    /**
     * Busca a conta padrão da oficina.
     */
    Optional<ContaBancaria> findByOficinaIdAndPadraoTrue(UUID oficinaId);

    /**
     * Busca contas com PIX configurado.
     */
    List<ContaBancaria> findByOficinaIdAndChavePixIsNotNullAndAtivoTrue(UUID oficinaId);

    /**
     * Conta quantas contas ativas a oficina possui.
     */
    long countByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Busca conta bancária por ID com validação de tenant.
     * IMPORTANTE: Sempre usar este método ao invés de findById() para garantir isolamento multi-tenant.
     */
    @Query("SELECT c FROM ContaBancaria c WHERE c.oficina.id = :oficinaId AND c.id = :id")
    Optional<ContaBancaria> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);
}
