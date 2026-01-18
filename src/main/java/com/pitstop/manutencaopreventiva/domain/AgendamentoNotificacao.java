package com.pitstop.manutencaopreventiva.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Representa um agendamento de notificação para manutenção preventiva.
 * Armazenado como JSON no banco de dados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoNotificacao implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Data para enviar a notificação */
    private LocalDate data;

    /** Hora para enviar a notificação */
    private LocalTime hora;

    /** Indica se a notificação já foi enviada */
    @Builder.Default
    private Boolean enviado = false;

    /** Data/hora em que a notificação foi enviada */
    private LocalDateTime enviadoEm;

    /** Mensagem de erro caso o envio tenha falhado */
    private String erroEnvio;

    /**
     * Retorna o LocalDateTime combinando data e hora.
     */
    @JsonIgnore
    public LocalDateTime getDataHora() {
        if (data == null || hora == null) {
            return null;
        }
        return LocalDateTime.of(data, hora);
    }

    /**
     * Verifica se está na hora de enviar a notificação.
     */
    @JsonIgnore
    public boolean isHoraDeEnviar() {
        if (data == null || hora == null || Boolean.TRUE.equals(enviado)) {
            return false;
        }
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime agendado = getDataHora();
        // Considera uma janela de 30 minutos para envio
        return !agora.isBefore(agendado) && agora.isBefore(agendado.plusMinutes(30));
    }

    /**
     * Marca a notificação como enviada.
     */
    public void marcarComoEnviado() {
        this.enviado = true;
        this.enviadoEm = LocalDateTime.now();
        this.erroEnvio = null;
    }

    /**
     * Marca a notificação como falha.
     */
    public void marcarComoFalha(String erro) {
        this.erroEnvio = erro;
    }
}
