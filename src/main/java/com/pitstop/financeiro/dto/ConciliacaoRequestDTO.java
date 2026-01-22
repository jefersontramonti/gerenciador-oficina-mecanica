package com.pitstop.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para requisição de conciliação.
 */
@Data
@Builder
@NoArgsConstructor
public class ConciliacaoRequestDTO {

    /**
     * Conciliação de uma transação com um pagamento.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConciliarTransacaoDTO {
        @NotNull(message = "ID da transação é obrigatório")
        private UUID transacaoId;

        @NotNull(message = "ID do pagamento é obrigatório")
        private UUID pagamentoId;
    }

    /**
     * Ignorar uma transação.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IgnorarTransacaoDTO {
        @NotNull(message = "ID da transação é obrigatório")
        private UUID transacaoId;

        private String observacao;
    }

    /**
     * Conciliação em lote.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConciliacaoLoteDTO {
        private List<ConciliarTransacaoDTO> conciliacoes;
        private List<UUID> transacoesIgnorar;
    }
}
