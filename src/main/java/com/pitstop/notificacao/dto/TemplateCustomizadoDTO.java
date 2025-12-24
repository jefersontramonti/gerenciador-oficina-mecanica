package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.TemplateCustomizado;
import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para templates customizados.
 *
 * @author PitStop Team
 */
@Schema(description = "Template de notificacao customizado")
public record TemplateCustomizadoDTO(
    @Schema(description = "ID do template")
    UUID id,

    @Schema(description = "Tipo de template (evento)")
    TemplateNotificacao tipoTemplate,

    @Schema(description = "Canal de notificacao")
    TipoNotificacao tipoNotificacao,

    @Schema(description = "Assunto (para email)")
    String assunto,

    @Schema(description = "Corpo da mensagem")
    String corpo,

    @Schema(description = "Template ativo")
    Boolean ativo,

    @Schema(description = "Variaveis disponiveis (JSON)")
    String variaveisDisponiveis,

    @Schema(description = "Categoria do template")
    String categoria,

    @Schema(description = "Tags para organizacao")
    String tags,

    @Schema(description = "Observacoes")
    String observacoes,

    @Schema(description = "Se e template padrao do sistema")
    Boolean templatePadrao,

    @Schema(description = "Data de criacao")
    LocalDateTime createdAt,

    @Schema(description = "Data de atualizacao")
    LocalDateTime updatedAt
) {
    /**
     * Converte entidade para DTO.
     */
    public static TemplateCustomizadoDTO fromEntity(TemplateCustomizado entity) {
        return new TemplateCustomizadoDTO(
            entity.getId(),
            entity.getTipoTemplate(),
            entity.getTipoNotificacao(),
            entity.getAssunto(),
            entity.getCorpo(),
            entity.getAtivo(),
            entity.getVariaveisDisponiveis(),
            entity.getCategoria(),
            entity.getTags(),
            entity.getObservacoes(),
            entity.isTemplatePadrao(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
