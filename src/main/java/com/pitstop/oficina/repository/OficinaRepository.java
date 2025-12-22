package com.pitstop.oficina.repository;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
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
 * Repository para operações de banco de dados da entidade Oficina.
 *
 * @author PitStop Team
 */
@Repository
public interface OficinaRepository extends JpaRepository<Oficina, UUID> {

    /**
     * Busca oficina por CNPJ (único no sistema).
     *
     * @param cnpj CNPJ da oficina
     * @return Optional contendo a oficina se encontrada
     */
    Optional<Oficina> findByCnpjCpf(String cnpjCpf);

    default Optional<Oficina> findByCnpj(String cnpj) {
        return findByCnpjCpf(cnpj);
    }

    /**
     * Verifica se já existe uma oficina com o CNPJ informado.
     *
     * @param cnpj CNPJ a verificar
     * @return true se existe, false caso contrário
     */
    boolean existsByCnpjCpf(String cnpjCpf);

    default boolean existsByCnpj(String cnpj) {
        return existsByCnpjCpf(cnpj);
    }

    /**
     * Busca oficinas por status com paginação.
     *
     * @param status Status da oficina (ATIVA, SUSPENSA, INATIVA, CANCELADA)
     * @param pageable Informações de paginação
     * @return Página de oficinas com o status informado
     */
    Page<Oficina> findByStatus(StatusOficina status, Pageable pageable);

    /**
     * Busca oficinas por plano de assinatura com paginação.
     *
     * @param plano Plano de assinatura (BASICO, PROFISSIONAL, TURBINADO)
     * @param pageable Informações de paginação
     * @return Página de oficinas com o plano informado
     */
    Page<Oficina> findByPlano(PlanoAssinatura plano, Pageable pageable);

    /**
     * Busca oficinas cujo vencimento do plano está dentro de um período.
     * Útil para enviar lembretes de renovação.
     *
     * @param inicio Data inicial do período
     * @param fim Data final do período
     * @return Lista de oficinas com vencimento no período
     */
    @Query("SELECT o FROM Oficina o WHERE o.dataVencimentoPlano BETWEEN :inicio AND :fim AND o.status = 'ATIVA'")
    List<Oficina> findByDataVencimentoBetween(
        @Param("inicio") LocalDate inicio,
        @Param("fim") LocalDate fim
    );

    /**
     * Busca oficinas com plano vencido e status ainda ativo.
     * Usado para job de suspensão automática.
     *
     * @param dataReferencia Data de referência (normalmente LocalDate.now())
     * @return Lista de oficinas com plano vencido
     */
    @Query("SELECT o FROM Oficina o WHERE o.dataVencimentoPlano < :dataReferencia AND o.status = 'ATIVA'")
    List<Oficina> findVencidas(@Param("dataReferencia") LocalDate dataReferencia);

    /**
     * Conta oficinas por status.
     *
     * @param status Status a contar
     * @return Número de oficinas com o status
     */
    long countByStatus(StatusOficina status);

    /**
     * Conta total de oficinas ativas (para métricas do super admin).
     *
     * @return Número de oficinas ativas
     */
    @Query("SELECT COUNT(o) FROM Oficina o WHERE o.status = 'ATIVA'")
    long countAtivas();

    /**
     * Busca oficinas com filtros avançados (para Super Admin).
     *
     * @param status Status da oficina (null = todos)
     * @param plano Plano de assinatura (null = todos)
     * @param nome Nome/nome fantasia da oficina (busca parcial)
     * @param cnpj CNPJ da oficina (busca parcial)
     * @param pageable Informações de paginação
     * @return Página de oficinas que atendem os filtros
     */
    @Query("""
        SELECT o FROM Oficina o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:plano IS NULL OR o.plano = :plano)
          AND (:nome IS NULL OR LOWER(o.razaoSocial) LIKE LOWER(CONCAT('%', :nome, '%'))
               OR LOWER(o.nomeFantasia) LIKE LOWER(CONCAT('%', :nome, '%')))
          AND (:cnpj IS NULL OR o.cnpjCpf LIKE CONCAT('%', :cnpj, '%'))
        ORDER BY o.id DESC
        """)
    Page<Oficina> findWithFilters(
        @Param("status") StatusOficina status,
        @Param("plano") PlanoAssinatura plano,
        @Param("nome") String nome,
        @Param("cnpj") String cnpj,
        Pageable pageable
    );

    /**
     * Calcula o MRR (Monthly Recurring Revenue) total do SaaS.
     *
     * @return Soma de todas as mensalidades de oficinas ativas
     */
    @Query("SELECT COALESCE(SUM(o.valorMensalidade), 0) FROM Oficina o WHERE o.status = 'ATIVA'")
    Double calculateMRR();

    /**
     * Busca oficinas por estado (UF).
     * Útil para relatórios geográficos.
     *
     * @param uf Estado (SP, RJ, MG, etc)
     * @param pageable Informações de paginação
     * @return Página de oficinas no estado
     */
    @Query("SELECT o FROM Oficina o WHERE o.endereco.estado = :uf")
    Page<Oficina> findByEstado(@Param("uf") String uf, Pageable pageable);

    /**
     * Busca oficinas que estão em período trial (7 dias gratuitos).
     *
     * @return Lista de oficinas em trial
     */
    @Query("""
        SELECT o FROM Oficina o
        WHERE o.status = 'ATIVA'
          AND o.dataAssinatura IS NOT NULL
          AND o.dataVencimentoPlano > CURRENT_DATE
          AND FUNCTION('DATE_DIFF', 'DAY', o.dataAssinatura, CURRENT_DATE) <= 7
        """)
    List<Oficina> findInTrial();

    /**
     * Busca oficinas por status e plano.
     * Usado para cálculo de MRR breakdown.
     *
     * @param status Status da oficina
     * @param plano Plano de assinatura
     * @return Lista de oficinas que atendem os critérios
     */
    List<Oficina> findByStatusAndPlano(StatusOficina status, PlanoAssinatura plano);

    /**
     * Busca oficinas com vencimento em um período (usado para trials expirando).
     * Usado para alertas de conversão.
     *
     * @param status Status da oficina (normalmente TRIAL)
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     * @param pageable Informações de paginação
     * @return Página de oficinas com vencimento no período
     */
    Page<Oficina> findByStatusAndDataVencimentoPlanoBetween(
        StatusOficina status,
        LocalDate dataInicio,
        LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Busca oficinas por vencimento em período.
     * Usado para listar pagamentos pendentes.
     *
     * @param dataInicio Data inicial
     * @param dataFim Data final
     * @param pageable Informações de paginação
     * @return Página de oficinas com vencimento no período
     */
    @Query("SELECT o FROM Oficina o WHERE o.dataVencimentoPlano BETWEEN :dataInicio AND :dataFim ORDER BY o.dataVencimentoPlano ASC")
    Page<Oficina> findByDataVencimentoPlanoBetween(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        Pageable pageable
    );

    /**
     * Busca oficinas ativas com vencimento antes de uma data.
     * Usado para listar inadimplentes.
     *
     * @param status Status da oficina
     * @param dataLimite Data limite
     * @param pageable Informações de paginação
     * @return Página de oficinas inadimplentes
     */
    @Query("SELECT o FROM Oficina o WHERE o.status = :status AND o.dataVencimentoPlano < :dataLimite ORDER BY o.dataVencimentoPlano ASC")
    Page<Oficina> findByStatusAndDataVencimentoPlanoBefore(
        @Param("status") StatusOficina status,
        @Param("dataLimite") LocalDate dataLimite,
        Pageable pageable
    );
}
