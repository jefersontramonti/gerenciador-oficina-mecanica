package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.ExtratoBancario;
import com.pitstop.financeiro.domain.StatusExtrato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para extratos bancários.
 */
@Repository
public interface ExtratoBancarioRepository extends JpaRepository<ExtratoBancario, UUID> {

    /**
     * Busca extratos por oficina.
     */
    @Query("SELECT e FROM ExtratoBancario e WHERE e.oficina.id = :oficinaId ORDER BY e.dataImportacao DESC")
    Page<ExtratoBancario> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);

    /**
     * Busca extrato por ID e oficina.
     */
    @Query("SELECT e FROM ExtratoBancario e LEFT JOIN FETCH e.transacoes WHERE e.id = :id AND e.oficina.id = :oficinaId")
    Optional<ExtratoBancario> findByIdAndOficinaId(@Param("id") UUID id, @Param("oficinaId") UUID oficinaId);

    /**
     * Verifica se já existe extrato com mesmo hash (duplicata).
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM ExtratoBancario e WHERE e.oficina.id = :oficinaId AND e.arquivoHash = :hash")
    boolean existsByOficinaIdAndArquivoHash(@Param("oficinaId") UUID oficinaId, @Param("hash") String hash);

    /**
     * Busca extratos por período.
     */
    @Query("SELECT e FROM ExtratoBancario e WHERE e.oficina.id = :oficinaId AND e.dataInicio >= :inicio AND e.dataFim <= :fim ORDER BY e.dataImportacao DESC")
    List<ExtratoBancario> findByOficinaIdAndPeriodo(
        @Param("oficinaId") UUID oficinaId,
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    /**
     * Busca extratos por status.
     */
    @Query("SELECT e FROM ExtratoBancario e WHERE e.oficina.id = :oficinaId AND e.status = :status ORDER BY e.dataImportacao DESC")
    List<ExtratoBancario> findByOficinaIdAndStatus(
        @Param("oficinaId") UUID oficinaId,
        @Param("status") StatusExtrato status
    );

    /**
     * Conta extratos pendentes.
     */
    @Query("SELECT COUNT(e) FROM ExtratoBancario e WHERE e.oficina.id = :oficinaId AND e.status = 'PENDENTE'")
    long countPendentesByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca extratos por conta bancária.
     */
    @Query("SELECT e FROM ExtratoBancario e WHERE e.contaBancaria.id = :contaId ORDER BY e.dataImportacao DESC")
    List<ExtratoBancario> findByContaBancariaId(@Param("contaId") UUID contaId);
}
