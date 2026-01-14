package com.pitstop.anexo.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para alterar visibilidade de anexo para o cliente.
 */
public record AlterarVisibilidadeRequest(
        @NotNull(message = "Campo visivelParaCliente é obrigatório")
        Boolean visivelParaCliente
) {}
