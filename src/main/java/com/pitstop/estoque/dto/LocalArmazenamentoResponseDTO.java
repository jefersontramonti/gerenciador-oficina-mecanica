package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.TipoLocal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO completo para resposta de local de armazenamento.
 * Inclui informações do pai (simplificado) para evitar recursão.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Schema(description = "Resposta completa de um local de armazenamento")
public record LocalArmazenamentoResponseDTO(

        @Schema(description = "ID do local", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Código único do local", example = "DEP-A")
        String codigo,

        @Schema(description = "Tipo do local", example = "PRATELEIRA")
        TipoLocal tipo,

        @Schema(description = "Descrição do local", example = "Prateleira 3 - Setor de Filtros")
        String descricao,

        @Schema(description = "Local pai (versão simplificada, null se for raiz)")
        LocalArmazenamentoSimplificadoDTO localizacaoPai,

        @Schema(description = "Capacidade máxima de itens", example = "50")
        Integer capacidadeMaxima,

        @Schema(description = "Observações adicionais", example = "Local para armazenar filtros de óleo")
        String observacoes,

        @Schema(description = "Caminho completo hierárquico", example = "Depósito Principal > Setor A > Prateleira 3")
        String caminhoCompleto,

        @Schema(description = "Nível na hierarquia (0 = raiz)", example = "2")
        Integer nivel,

        @Schema(description = "Indica se é um local raiz (sem pai)", example = "false")
        Boolean isRaiz,

        @Schema(description = "Indica se possui locais filhos", example = "true")
        Boolean temFilhos,

        @Schema(description = "Status ativo/inativo", example = "true")
        Boolean ativo,

        @Schema(description = "Data de criação")
        LocalDateTime createdAt,

        @Schema(description = "Data de última atualização")
        LocalDateTime updatedAt
) {
}
