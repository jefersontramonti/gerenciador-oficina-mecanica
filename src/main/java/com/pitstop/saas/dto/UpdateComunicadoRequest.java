package com.pitstop.saas.dto;

import com.pitstop.saas.domain.PrioridadeComunicado;
import com.pitstop.saas.domain.TipoComunicado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateComunicadoRequest(
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 255, message = "Título deve ter no máximo 255 caracteres")
    String titulo,

    @Size(max = 500, message = "Resumo deve ter no máximo 500 caracteres")
    String resumo,

    @NotBlank(message = "Conteúdo é obrigatório")
    String conteudo,

    TipoComunicado tipo,
    PrioridadeComunicado prioridade,

    // Segmentação
    String[] planosAlvo,
    UUID[] oficinasAlvo,
    String[] statusOficinasAlvo,

    // Configurações
    Boolean requerConfirmacao,
    Boolean exibirNoLogin,

    // Reagendamento
    OffsetDateTime dataAgendamento
) {}
