package com.pitstop.financeiro.repository;

import com.pitstop.financeiro.domain.TabelaJurosParcelamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para tabelas de juros de parcelamento.
 */
@Repository
public interface TabelaJurosParcelamentoRepository extends JpaRepository<TabelaJurosParcelamento, UUID> {

    /**
     * Lista todas as faixas de juros de uma oficina.
     */
    List<TabelaJurosParcelamento> findByOficinaIdAndAtivoTrueOrderByParcelasMinimoAsc(UUID oficinaId);

    /**
     * Lista todas as faixas (ativas e inativas) de uma oficina.
     */
    List<TabelaJurosParcelamento> findByOficinaIdOrderByParcelasMinimoAsc(UUID oficinaId);

    /**
     * Busca uma faixa específica por ID e oficina.
     */
    Optional<TabelaJurosParcelamento> findByIdAndOficinaId(UUID id, UUID oficinaId);

    /**
     * Busca a faixa de juros aplicável para um número de parcelas.
     */
    @Query("""
        SELECT t FROM TabelaJurosParcelamento t
        WHERE t.oficina.id = :oficinaId
          AND t.ativo = true
          AND :parcelas >= t.parcelasMinimo
          AND :parcelas <= t.parcelasMaximo
        ORDER BY t.parcelasMinimo ASC
        """)
    Optional<TabelaJurosParcelamento> findFaixaParaParcelas(
        @Param("oficinaId") UUID oficinaId,
        @Param("parcelas") int parcelas
    );

    /**
     * Verifica se existe sobreposição de faixas.
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM TabelaJurosParcelamento t
        WHERE t.oficina.id = :oficinaId
          AND t.ativo = true
          AND t.id != :excludeId
          AND (
              (:minimo BETWEEN t.parcelasMinimo AND t.parcelasMaximo)
              OR (:maximo BETWEEN t.parcelasMinimo AND t.parcelasMaximo)
              OR (t.parcelasMinimo BETWEEN :minimo AND :maximo)
          )
        """)
    boolean existeSobreposicao(
        @Param("oficinaId") UUID oficinaId,
        @Param("minimo") int minimo,
        @Param("maximo") int maximo,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Conta faixas ativas de uma oficina.
     */
    long countByOficinaIdAndAtivoTrue(UUID oficinaId);
}
