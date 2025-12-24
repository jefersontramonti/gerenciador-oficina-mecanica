package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request para criar/atualizar template customizado.
 *
 * @author PitStop Team
 */
@Schema(description = "Dados para criar/atualizar template")
public record TemplateCustomizadoRequest(
    @Schema(description = "Tipo de template (evento)", required = true)
    @NotNull(message = "Tipo de template e obrigatorio")
    TemplateNotificacao tipoTemplate,

    @Schema(description = "Canal de notificacao", required = true)
    @NotNull(message = "Tipo de notificacao e obrigatorio")
    TipoNotificacao tipoNotificacao,

    @Schema(description = "Assunto (para email)")
    String assunto,

    @Schema(description = "Corpo da mensagem", required = true)
    @NotBlank(message = "Corpo do template e obrigatorio")
    String corpo,

    @Schema(description = "Template ativo")
    Boolean ativo,

    @Schema(description = "Categoria do template")
    String categoria,

    @Schema(description = "Tags para organizacao")
    String tags,

    @Schema(description = "Observacoes")
    String observacoes
) {}
