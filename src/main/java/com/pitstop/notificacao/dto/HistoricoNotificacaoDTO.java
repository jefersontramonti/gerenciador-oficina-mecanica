package com.pitstop.notificacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO para visualizacao de historico de notificacao.
 *
 * @author PitStop Team
 */
@Builder
public record HistoricoNotificacaoDTO(
    UUID id,
    UUID oficinaId,

    // Identificacao
    EventoNotificacao evento,
    String eventoNome,
    TipoNotificacao tipoNotificacao,
    String canalNome,

    // Destinatario
    String destinatario,
    String nomeDestinatario,

    // Conteudo
    String assunto,
    String mensagem,
    Map<String, Object> variaveis,

    // Rastreamento
    UUID templateId,
    UUID ordemServicoId,
    UUID clienteId,
    UUID usuarioId,
    Boolean isManual,

    // Status
    StatusNotificacao status,
    String statusDescricao,
    Integer tentativas,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEnvio,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataEntrega,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataLeitura,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime dataAgendada,

    // API
    String idExterno,
    String erroMensagem,
    String erroCodigo,

    // Custos
    BigDecimal custo,
    String moedaCusto,

    // Auditoria
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
    /**
     * Converte entidade para DTO.
     */
    public static HistoricoNotificacaoDTO fromEntity(HistoricoNotificacao entity) {
        return HistoricoNotificacaoDTO.builder()
            .id(entity.getId())
            .oficinaId(entity.getOficinaId())
            // Identificacao
            .evento(entity.getEvento())
            .eventoNome(entity.getEvento() != null ? entity.getEvento().getNome() : null)
            .tipoNotificacao(entity.getTipoNotificacao())
            .canalNome(entity.getTipoNotificacao() != null ? entity.getTipoNotificacao().name() : null)
            // Destinatario
            .destinatario(mascarar(entity.getDestinatario(), entity.getTipoNotificacao()))
            .nomeDestinatario(entity.getNomeDestinatario())
            // Conteudo
            .assunto(entity.getAssunto())
            .mensagem(entity.getMensagem())
            .variaveis(entity.getVariaveis())
            // Rastreamento
            .templateId(entity.getTemplateId())
            .ordemServicoId(entity.getOrdemServicoId())
            .clienteId(entity.getClienteId())
            .usuarioId(entity.getUsuarioId())
            .isManual(entity.isManual())
            // Status
            .status(entity.getStatus())
            .statusDescricao(entity.getStatus() != null ? entity.getStatus().getDescricao() : null)
            .tentativas(entity.getTentativas())
            .dataEnvio(entity.getDataEnvio())
            .dataEntrega(entity.getDataEntrega())
            .dataLeitura(entity.getDataLeitura())
            .dataAgendada(entity.getDataAgendada())
            // API
            .idExterno(entity.getIdExterno())
            .erroMensagem(entity.getErroMensagem())
            .erroCodigo(entity.getErroCodigo())
            // Custos
            .custo(entity.getCusto())
            .moedaCusto(entity.getMoedaCusto())
            // Auditoria
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    /**
     * Mascara dados sensiveis do destinatario.
     * Email: j***@email.com
     * Telefone: 55119***99999
     */
    private static String mascarar(String valor, TipoNotificacao tipo) {
        if (valor == null || valor.isBlank()) {
            return valor;
        }

        if (tipo == TipoNotificacao.EMAIL) {
            // Mascara email
            int atIndex = valor.indexOf('@');
            if (atIndex > 1) {
                return valor.charAt(0) + "***" + valor.substring(atIndex);
            }
        } else if (tipo == TipoNotificacao.WHATSAPP || tipo == TipoNotificacao.SMS) {
            // Mascara telefone
            if (valor.length() > 6) {
                return valor.substring(0, 5) + "***" + valor.substring(valor.length() - 4);
            }
        }

        return valor;
    }

    /**
     * DTO resumido para listagem.
     */
    @Builder
    public record Resumido(
        UUID id,
        EventoNotificacao evento,
        String eventoNome,
        TipoNotificacao tipoNotificacao,
        String destinatario,
        StatusNotificacao status,
        String statusDescricao,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataEnvio,
        Boolean isManual
    ) {
        public static Resumido fromEntity(HistoricoNotificacao entity) {
            return Resumido.builder()
                .id(entity.getId())
                .evento(entity.getEvento())
                .eventoNome(entity.getEvento() != null ? entity.getEvento().getNome() : null)
                .tipoNotificacao(entity.getTipoNotificacao())
                .destinatario(mascarar(entity.getDestinatario(), entity.getTipoNotificacao()))
                .status(entity.getStatus())
                .statusDescricao(entity.getStatus() != null ? entity.getStatus().getDescricao() : null)
                .dataEnvio(entity.getDataEnvio())
                .isManual(entity.isManual())
                .build();
        }

        private static String mascarar(String valor, TipoNotificacao tipo) {
            return HistoricoNotificacaoDTO.mascarar(valor, tipo);
        }
    }
}
