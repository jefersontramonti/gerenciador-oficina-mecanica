package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * Request para envio de notificacao via orquestrador.
 *
 * @author PitStop Team
 */
@Builder
public record EnviarNotificacaoRequest(
    /**
     * Evento que disparou a notificacao.
     */
    @NotNull(message = "Evento e obrigatorio")
    EventoNotificacao evento,

    /**
     * Canal de envio (opcional).
     * Se nao informado, usa os canais configurados para o evento.
     */
    TipoNotificacao canal,

    /**
     * Destinatario (email ou telefone).
     */
    @NotBlank(message = "Destinatario e obrigatorio")
    String destinatario,

    /**
     * Nome do destinatario.
     */
    String nomeDestinatario,

    /**
     * Variaveis para substituicao no template.
     */
    @NotNull(message = "Variaveis sao obrigatorias")
    Map<String, Object> variaveis,

    /**
     * ID da ordem de servico relacionada (se aplicavel).
     */
    UUID ordemServicoId,

    /**
     * ID do cliente relacionado.
     */
    UUID clienteId,

    /**
     * ID do usuario que disparou (NULL = automatico).
     */
    UUID usuarioId,

    /**
     * Forcar envio mesmo fora do horario comercial.
     */
    Boolean forcarEnvio,

    /**
     * Ignorar modo simulacao e enviar realmente.
     */
    Boolean ignorarSimulacao
) {
    /**
     * Factory method para notificacao de OS.
     */
    public static EnviarNotificacaoRequest paraOS(
        EventoNotificacao evento,
        String destinatario,
        String nomeDestinatario,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId
    ) {
        return EnviarNotificacaoRequest.builder()
            .evento(evento)
            .destinatario(destinatario)
            .nomeDestinatario(nomeDestinatario)
            .variaveis(variaveis)
            .ordemServicoId(ordemServicoId)
            .clienteId(clienteId)
            .build();
    }

    /**
     * Factory method para notificacao manual.
     */
    public static EnviarNotificacaoRequest manual(
        EventoNotificacao evento,
        TipoNotificacao canal,
        String destinatario,
        String nomeDestinatario,
        Map<String, Object> variaveis,
        UUID usuarioId
    ) {
        return EnviarNotificacaoRequest.builder()
            .evento(evento)
            .canal(canal)
            .destinatario(destinatario)
            .nomeDestinatario(nomeDestinatario)
            .variaveis(variaveis)
            .usuarioId(usuarioId)
            .build();
    }
}
