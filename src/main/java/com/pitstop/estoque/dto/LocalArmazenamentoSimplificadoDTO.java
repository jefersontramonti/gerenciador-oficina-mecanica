package com.pitstop.estoque.dto;

import com.pitstop.estoque.domain.TipoLocal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO simplificado para local de armazenamento.
 * Usado em respostas onde não é necessário incluir toda a hierarquia,
 * evitando recursão infinita na serialização JSON.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Schema(description = "Versão simplificada de um local de armazenamento (sem hierarquia completa)")
public record LocalArmazenamentoSimplificadoDTO(

        @Schema(description = "ID do local", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Código único do local", example = "DEP-A")
        String codigo,

        @Schema(description = "Tipo do local", example = "PRATELEIRA")
        TipoLocal tipo,

        @Schema(description = "Descrição do local", example = "Prateleira 3 - Setor de Filtros")
        String descricao,

        @Schema(description = "Caminho completo hierárquico", example = "Depósito Principal > Setor A > Prateleira 3")
        String caminhoCompleto
) {
}
