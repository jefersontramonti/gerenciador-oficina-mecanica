package com.pitstop.manutencaopreventiva.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para feedback detalhado sobre o status das notificações após criar um agendamento.
 * Informa ao usuário o que aconteceu com cada canal de notificação.
 */
public record NotificacaoFeedbackDTO(
    /**
     * Indica se pelo menos uma notificação foi criada.
     */
    boolean notificacoesCriadas,

    /**
     * Indica se as notificações serão enviadas imediatamente.
     * Se false, significa que estão agendadas para o próximo horário comercial.
     */
    boolean envioImediato,

    /**
     * Motivo pelo qual o envio não é imediato (ex: "Fora do horário comercial").
     */
    String motivoAtraso,

    /**
     * Horário em que as notificações agendadas serão enviadas.
     */
    @JsonFormat(pattern = "HH:mm")
    LocalTime horarioPrevistaEnvio,

    /**
     * Quantidade total de notificações criadas.
     */
    int totalNotificacoes,

    /**
     * Detalhes de cada canal de notificação.
     */
    List<CanalFeedbackDTO> canais,

    /**
     * Mensagem resumida para exibir ao usuário.
     */
    String mensagemUsuario
) {
    /**
     * Feedback detalhado por canal.
     */
    public record CanalFeedbackDTO(
        String canal,
        boolean criado,
        String destinatario,
        String status,
        String motivo
    ) {}

    /**
     * Builder para facilitar a construção do feedback.
     */
    public static class Builder {
        private boolean notificacoesCriadas = false;
        private boolean envioImediato = true;
        private String motivoAtraso = null;
        private LocalTime horarioPrevistaEnvio = null;
        private int totalNotificacoes = 0;
        private List<CanalFeedbackDTO> canais = new ArrayList<>();
        private String mensagemUsuario = null;

        public Builder notificacoesCriadas(boolean criadas) {
            this.notificacoesCriadas = criadas;
            return this;
        }

        public Builder envioImediato(boolean imediato) {
            this.envioImediato = imediato;
            return this;
        }

        public Builder motivoAtraso(String motivo) {
            this.motivoAtraso = motivo;
            return this;
        }

        public Builder horarioPrevistaEnvio(LocalTime horario) {
            this.horarioPrevistaEnvio = horario;
            return this;
        }

        public Builder totalNotificacoes(int total) {
            this.totalNotificacoes = total;
            return this;
        }

        public Builder addCanal(CanalFeedbackDTO canal) {
            this.canais.add(canal);
            return this;
        }

        public Builder mensagemUsuario(String mensagem) {
            this.mensagemUsuario = mensagem;
            return this;
        }

        public NotificacaoFeedbackDTO build() {
            // Gera mensagem automática se não foi definida
            if (mensagemUsuario == null) {
                mensagemUsuario = gerarMensagemAutomatica();
            }

            return new NotificacaoFeedbackDTO(
                notificacoesCriadas,
                envioImediato,
                motivoAtraso,
                horarioPrevistaEnvio,
                totalNotificacoes,
                canais,
                mensagemUsuario
            );
        }

        private String gerarMensagemAutomatica() {
            if (!notificacoesCriadas || totalNotificacoes == 0) {
                return "Nenhuma notificação foi enviada. Verifique se os canais estão configurados e se o cliente possui dados de contato.";
            }

            String canaisStr = canais.stream()
                .filter(c -> c.criado())
                .map(c -> c.canal())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

            if (envioImediato) {
                return String.format(
                    "%d notificação(ões) enviada(s) via %s.",
                    totalNotificacoes, canaisStr
                );
            } else {
                if (horarioPrevistaEnvio != null) {
                    return String.format(
                        "%d notificação(ões) agendada(s) via %s. Serão enviadas a partir das %s (%s).",
                        totalNotificacoes, canaisStr,
                        horarioPrevistaEnvio.toString(),
                        motivoAtraso != null ? motivoAtraso : "fora do horário comercial"
                    );
                } else {
                    return String.format(
                        "%d notificação(ões) agendada(s) via %s. Serão enviadas no próximo horário comercial (%s).",
                        totalNotificacoes, canaisStr,
                        motivoAtraso != null ? motivoAtraso : "fora do horário comercial"
                    );
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
