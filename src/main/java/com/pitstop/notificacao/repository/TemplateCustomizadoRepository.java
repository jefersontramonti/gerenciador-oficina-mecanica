package com.pitstop.notificacao.repository;

import com.pitstop.notificacao.domain.TemplateCustomizado;
import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository para templates customizados.
 *
 * @author PitStop Team
 */
@Repository
public interface TemplateCustomizadoRepository extends JpaRepository<TemplateCustomizado, UUID> {

    /**
     * Busca template específico da oficina.
     *
     * @param oficinaId ID da oficina
     * @param tipoTemplate Tipo de template
     * @param tipoNotificacao Canal de notificação
     * @return Template se encontrado
     */
    Optional<TemplateCustomizado> findByOficinaIdAndTipoTemplateAndTipoNotificacaoAndAtivoTrue(
        UUID oficinaId,
        TemplateNotificacao tipoTemplate,
        TipoNotificacao tipoNotificacao
    );

    /**
     * Busca template padrão do sistema (oficina_id = NULL).
     *
     * @param tipoTemplate Tipo de template
     * @param tipoNotificacao Canal de notificação
     * @return Template padrão se encontrado
     */
    @Query("""
        SELECT t FROM TemplateCustomizado t
        WHERE t.oficinaId IS NULL
          AND t.tipoTemplate = :tipoTemplate
          AND t.tipoNotificacao = :tipoNotificacao
          AND t.ativo = true
        """)
    Optional<TemplateCustomizado> findTemplatePadrao(
        @Param("tipoTemplate") TemplateNotificacao tipoTemplate,
        @Param("tipoNotificacao") TipoNotificacao tipoNotificacao
    );

    /**
     * Lista todos os templates de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return Lista de templates
     */
    List<TemplateCustomizado> findByOficinaIdAndAtivoTrueOrderByTipoTemplate(UUID oficinaId);

    /**
     * Lista todos os templates padrão do sistema.
     *
     * @return Lista de templates padrão
     */
    @Query("""
        SELECT t FROM TemplateCustomizado t
        WHERE t.oficinaId IS NULL
          AND t.ativo = true
        ORDER BY t.tipoTemplate, t.tipoNotificacao
        """)
    List<TemplateCustomizado> findAllTemplatesPadrao();

    /**
     * Conta quantos templates customizados uma oficina possui.
     *
     * @param oficinaId ID da oficina
     * @return Quantidade de templates
     */
    long countByOficinaIdAndAtivoTrue(UUID oficinaId);

    /**
     * Verifica se oficina tem template customizado para um tipo específico.
     *
     * @param oficinaId ID da oficina
     * @param tipoTemplate Tipo de template
     * @param tipoNotificacao Canal
     * @return true se existe
     */
    boolean existsByOficinaIdAndTipoTemplateAndTipoNotificacaoAndAtivoTrue(
        UUID oficinaId,
        TemplateNotificacao tipoTemplate,
        TipoNotificacao tipoNotificacao
    );
}
