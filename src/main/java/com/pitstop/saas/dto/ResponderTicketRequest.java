package com.pitstop.saas.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record ResponderTicketRequest(
    @NotBlank(message = "Conteúdo da resposta é obrigatório")
    String conteudo,

    // Se true, é uma nota interna (não visível para o cliente)
    boolean isInterno,

    // Anexos (opcional)
    List<String> anexos,

    // Autor da mensagem (SUPER_ADMIN que está respondendo)
    UUID autorId,
    String autorNome
) {}
