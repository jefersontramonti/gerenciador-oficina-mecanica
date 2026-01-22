package com.pitstop.financeiro.dto;

import com.pitstop.financeiro.domain.CategoriaDespesa;
import com.pitstop.financeiro.domain.StatusDespesa;
import com.pitstop.financeiro.domain.TipoPagamento;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTOs para operações com Despesas.
 */
public class DespesaDTO {

    /**
     * DTO para criação de despesa.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotNull(message = "Categoria é obrigatória")
        private CategoriaDespesa categoria;

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        private String descricao;

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        private BigDecimal valor;

        @NotNull(message = "Data de vencimento é obrigatória")
        private LocalDate dataVencimento;

        private String numeroDocumento;

        @Size(max = 200, message = "Fornecedor deve ter no máximo 200 caracteres")
        private String fornecedor;

        private String observacoes;

        @Builder.Default
        private Boolean recorrente = false;
    }

    /**
     * DTO para atualização de despesa.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        private CategoriaDespesa categoria;

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        private String descricao;

        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        private BigDecimal valor;

        private LocalDate dataVencimento;

        private String numeroDocumento;

        @Size(max = 200, message = "Fornecedor deve ter no máximo 200 caracteres")
        private String fornecedor;

        private String observacoes;

        private Boolean recorrente;
    }

    /**
     * DTO para registrar pagamento de despesa.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagamentoRequest {

        @NotNull(message = "Data de pagamento é obrigatória")
        private LocalDate dataPagamento;

        @NotNull(message = "Tipo de pagamento é obrigatório")
        private TipoPagamento tipoPagamento;
    }

    /**
     * DTO de resposta com dados completos da despesa.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {

        private UUID id;

        private CategoriaDespesa categoria;
        private String categoriaDescricao;
        private String categoriaGrupo;
        private String categoriaCor;

        private String descricao;
        private BigDecimal valor;

        private LocalDate dataVencimento;
        private LocalDate dataPagamento;

        private StatusDespesa status;
        private String statusDescricao;
        private String statusCor;

        private String numeroDocumento;
        private String fornecedor;
        private String observacoes;

        private Boolean recorrente;
        private TipoPagamento tipoPagamento;

        private UUID movimentacaoEstoqueId;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Campos calculados
        private Boolean vencida;
        private Long diasAtraso;
    }

    /**
     * DTO resumido para listagens.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {

        private UUID id;
        private CategoriaDespesa categoria;
        private String categoriaDescricao;
        private String categoriaCor;
        private String descricao;
        private BigDecimal valor;
        private LocalDate dataVencimento;
        private LocalDate dataPagamento;
        private StatusDespesa status;
        private String statusDescricao;
        private String statusCor;
        private String fornecedor;
        private Boolean recorrente;
        private Boolean vencida;
    }

    /**
     * DTO para resumo de despesas (dashboard).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resumo {

        private BigDecimal totalPendente;
        private BigDecimal totalVencido;
        private BigDecimal totalPagoMes;
        private Long quantidadePendente;
        private Long quantidadeVencida;
        private Long quantidadeAVencer7Dias;
    }

    /**
     * DTO para listagem de categorias disponíveis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaInfo {

        private CategoriaDespesa codigo;
        private String descricao;
        private String grupo;
        private String cor;
    }
}
