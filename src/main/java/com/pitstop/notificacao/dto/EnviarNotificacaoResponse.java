package com.pitstop.notificacao.dto;

import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response do envio de notificacao.
 *
 * @author PitStop Team
 */
@Builder
public record EnviarNotificacaoResponse(
    /**
     * Se o envio foi bem sucedido.
     */
    boolean sucesso,

    /**
     * Mensagem descritiva.
     */
    String mensagem,

    /**
     * Resultados por canal enviado.
     */
    List<ResultadoCanal> resultados,

    /**
     * Timestamp do processamento.
     */
    LocalDateTime timestamp
) {
    /**
     * Resultado do envio por canal.
     */
    @Builder
    public record ResultadoCanal(
        /**
         * Canal usado.
         */
        TipoNotificacao canal,

        /**
         * ID do historico gerado.
         */
        UUID historicoId,

        /**
         * Status do envio.
         */
        StatusNotificacao status,

        /**
         * ID externo retornado pela API.
         */
        String idExterno,

        /**
         * Mensagem de erro (se falhou).
         */
        String erro,

        /**
         * Se esta agendado para envio posterior.
         */
        LocalDateTime agendadoPara
    ) {
        public static ResultadoCanal sucesso(TipoNotificacao canal, UUID historicoId, String idExterno) {
            return ResultadoCanal.builder()
                .canal(canal)
                .historicoId(historicoId)
                .status(StatusNotificacao.ENVIADO)
                .idExterno(idExterno)
                .build();
        }

        public static ResultadoCanal falha(TipoNotificacao canal, UUID historicoId, String erro) {
            return ResultadoCanal.builder()
                .canal(canal)
                .historicoId(historicoId)
                .status(StatusNotificacao.FALHA)
                .erro(erro)
                .build();
        }

        public static ResultadoCanal agendado(TipoNotificacao canal, UUID historicoId, LocalDateTime agendadoPara) {
            return ResultadoCanal.builder()
                .canal(canal)
                .historicoId(historicoId)
                .status(StatusNotificacao.AGENDADO)
                .agendadoPara(agendadoPara)
                .build();
        }

        public static ResultadoCanal simulado(TipoNotificacao canal, UUID historicoId) {
            return ResultadoCanal.builder()
                .canal(canal)
                .historicoId(historicoId)
                .status(StatusNotificacao.ENVIADO)
                .idExterno("SIMULADO")
                .build();
        }
    }

    /**
     * Factory method para sucesso total.
     */
    public static EnviarNotificacaoResponse sucesso(List<ResultadoCanal> resultados) {
        return EnviarNotificacaoResponse.builder()
            .sucesso(true)
            .mensagem("Notificacao enviada com sucesso")
            .resultados(resultados)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Factory method para sucesso parcial.
     */
    public static EnviarNotificacaoResponse parcial(List<ResultadoCanal> resultados) {
        long falhas = resultados.stream()
            .filter(r -> r.status() == StatusNotificacao.FALHA)
            .count();
        return EnviarNotificacaoResponse.builder()
            .sucesso(false)
            .mensagem("Notificacao enviada parcialmente. " + falhas + " canal(is) falharam.")
            .resultados(resultados)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Factory method para falha total.
     */
    public static EnviarNotificacaoResponse falha(String erro) {
        return EnviarNotificacaoResponse.builder()
            .sucesso(false)
            .mensagem(erro)
            .resultados(List.of())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Factory method para agendamento.
     */
    public static EnviarNotificacaoResponse agendado(List<ResultadoCanal> resultados) {
        return EnviarNotificacaoResponse.builder()
            .sucesso(true)
            .mensagem("Notificacao agendada para envio posterior")
            .resultados(resultados)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
