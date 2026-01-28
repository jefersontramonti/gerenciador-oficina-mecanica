package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.CategoriaPeca;
import com.pitstop.estoque.domain.UnidadeMedida;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para criação de nova peça no estoque.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public record CreatePecaDTO(
        @NotBlank(message = "Código é obrigatório")
        @Size(min = 3, max = 50, message = "Código deve ter entre 3 e 50 caracteres")
        String codigo,

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 150, message = "Nome deve ter entre 2 e 150 caracteres")
        String nome,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 3, max = 500, message = "Descrição deve ter entre 3 e 500 caracteres")
        String descricao,

        @Size(max = 100, message = "Marca deve ter no máximo 100 caracteres")
        String marca,

        @Size(max = 500, message = "Aplicação deve ter no máximo 500 caracteres")
        String aplicacao,

        @Size(max = 100, message = "Localização deve ter no máximo 100 caracteres")
        String localizacao,

        UUID localArmazenamentoId,

        @NotNull(message = "Unidade de medida é obrigatória")
        UnidadeMedida unidadeMedida,

        @Size(max = 100, message = "Código original deve ter no máximo 100 caracteres")
        String codigoOriginal,

        @Size(max = 100, message = "Código do fabricante deve ter no máximo 100 caracteres")
        String codigoFabricante,

        @Size(max = 50, message = "Código de barras deve ter no máximo 50 caracteres")
        String codigoBarras,

        @Size(max = 20, message = "NCM deve ter no máximo 20 caracteres")
        String ncm,

        CategoriaPeca categoria,

        @NotNull(message = "Quantidade mínima é obrigatória")
        @Min(value = 0, message = "Quantidade mínima não pode ser negativa")
        Integer quantidadeMinima,

        @Min(value = 0, message = "Quantidade máxima não pode ser negativa")
        Integer quantidadeMaxima,

        @Min(value = 0, message = "Ponto de pedido não pode ser negativo")
        Integer pontoPedido,

        @NotNull(message = "Valor de custo é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor de custo não pode ser negativo")
        BigDecimal valorCusto,

        @NotNull(message = "Valor de venda é obrigatório")
        @DecimalMin(value = "0.00", message = "Valor de venda não pode ser negativo")
        BigDecimal valorVenda,

        @Size(max = 200, message = "Fornecedor principal deve ter no máximo 200 caracteres")
        String fornecedorPrincipal,

        String observacoes,

        @Min(value = 0, message = "Quantidade inicial não pode ser negativa")
        Integer quantidadeInicial
) {
}
