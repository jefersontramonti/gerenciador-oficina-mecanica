package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.TipoLocal;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para atualização de locais de armazenamento existentes.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Schema(description = "Dados para atualização de um local de armazenamento")
public record UpdateLocalArmazenamentoDTO(

        @NotBlank(message = "Código é obrigatório")
        @Size(min = 2, max = 50, message = "Código deve ter entre 2 e 50 caracteres")
        @Schema(description = "Código único do local (será normalizado para UPPERCASE)", example = "DEP-A")
        String codigo,

        @NotNull(message = "Tipo é obrigatório")
        @Schema(description = "Tipo do local de armazenamento", example = "PRATELEIRA")
        TipoLocal tipo,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 3, max = 200, message = "Descrição deve ter entre 3 e 200 caracteres")
        @Schema(description = "Descrição do local", example = "Prateleira 3 - Setor de Filtros")
        String descricao,

        @Schema(description = "ID do local pai na hierarquia (null para locais raiz)", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID localizacaoPaiId,

        @Positive(message = "Capacidade máxima deve ser positiva")
        @Schema(description = "Capacidade máxima de itens (opcional)", example = "50")
        Integer capacidadeMaxima,

        @Size(max = 1000, message = "Observações não podem exceder 1000 caracteres")
        @Schema(description = "Observações adicionais", example = "Local para armazenar filtros de óleo")
        String observacoes
) {
}
