package com.pitstop.ordemservico.repository;

import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.domain.TipoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência da entidade {@link ItemOS}.
 *
 * <p>A maioria das operações de ItemOS são gerenciadas via cascade do OrdemServico.
 * Este repositório é usado para queries específicas e estatísticas.</p>
 *
 * <p><strong>Multi-tenancy:</strong> Todos os métodos agora exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Repository
public interface ItemOSRepository extends JpaRepository<ItemOS, UUID> {

    /**
     * Busca todos os itens de uma OS específica em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da ordem de serviço
     * @return lista de itens da OS
     */
    @Query("SELECT i FROM ItemOS i WHERE i.ordemServico.oficina.id = :oficinaId AND i.ordemServico.id = :ordemServicoId ORDER BY i.createdAt ASC")
    List<ItemOS> findByOficinaIdAndOrdemServicoId(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId);

    /**
     * Busca itens por tipo (PECA ou SERVICO) de uma OS em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param ordemServicoId ID da ordem de serviço
     * @param tipo tipo do item
     * @return lista de itens do tipo especificado
     */
    @Query("SELECT i FROM ItemOS i WHERE i.ordemServico.oficina.id = :oficinaId AND i.ordemServico.id = :ordemServicoId AND i.tipo = :tipo")
    List<ItemOS> findByOficinaIdAndOrdemServicoIdAndTipo(@Param("oficinaId") UUID oficinaId, @Param("ordemServicoId") UUID ordemServicoId, @Param("tipo") TipoItem tipo);

    /**
     * Busca todos os itens de uma peça específica (histórico de uso) em uma oficina.
     * Útil para saber em quais OS uma peça foi utilizada.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pecaId ID da peça
     * @return lista de itens que usaram esta peça
     */
    @Query("SELECT i FROM ItemOS i WHERE i.ordemServico.oficina.id = :oficinaId AND i.pecaId = :pecaId")
    List<ItemOS> findByOficinaIdAndPecaId(@Param("oficinaId") UUID oficinaId, @Param("pecaId") UUID pecaId);

    /**
     * Conta quantidade de vezes que uma peça foi utilizada em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pecaId ID da peça
     * @return quantidade de OS que usaram esta peça
     */
    @Query("SELECT COUNT(i) FROM ItemOS i WHERE i.ordemServico.oficina.id = :oficinaId AND i.pecaId = :pecaId")
    long countByOficinaIdAndPecaId(@Param("oficinaId") UUID oficinaId, @Param("pecaId") UUID pecaId);

    /**
     * Ranking de peças mais utilizadas no período em uma oficina.
     * Retorna as peças mais vendidas/usadas.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return lista de [pecaId, quantidade, descricao]
     */
    @Query("SELECT i.pecaId, SUM(i.quantidade), i.descricao FROM ItemOS i WHERE i.ordemServico.oficina.id = :oficinaId AND i.tipo = 'PECA' GROUP BY i.pecaId, i.descricao ORDER BY SUM(i.quantidade) DESC")
    List<Object[]> rankingPecasMaisUtilizadasByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Estatísticas de consumo de uma peça específica em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pecaId ID da peça
     * @return [quantidadeTotal, valorTotal]
     */
    @Query("SELECT SUM(i.quantidade), SUM(i.valorTotal) FROM ItemOS i WHERE i.ordemServico.oficina.id = :oficinaId AND i.pecaId = :pecaId")
    Object[] estatisticasPecaByOficinaId(@Param("oficinaId") UUID oficinaId, @Param("pecaId") UUID pecaId);
}
