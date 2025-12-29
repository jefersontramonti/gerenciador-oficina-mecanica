package com.pitstop.saas.dto;

import com.pitstop.saas.domain.PrioridadeTicket;
import com.pitstop.saas.domain.TipoTicket;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateTicketRequest(
    // Oficina (opcional - pode ser ticket interno)
    UUID oficinaId,

    // Dados do solicitante
    UUID usuarioId,

    @NotBlank(message = "Nome do solicitante é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String usuarioNome,

    @NotBlank(message = "Email do solicitante é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    String usuarioEmail,

    // Classificação
    @NotNull(message = "Tipo do ticket é obrigatório")
    TipoTicket tipo,

    @NotNull(message = "Prioridade é obrigatória")
    PrioridadeTicket prioridade,

    // Conteúdo
    @NotBlank(message = "Assunto é obrigatório")
    @Size(max = 255, message = "Assunto deve ter no máximo 255 caracteres")
    String assunto,

    @NotBlank(message = "Descrição é obrigatória")
    String descricao,

    // Anexos (opcional)
    List<String> anexos,

    // Atribuição inicial (opcional)
    UUID atribuidoA
) {}
