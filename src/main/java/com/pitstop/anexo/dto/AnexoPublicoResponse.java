package com.pitstop.anexo.dto;

import com.pitstop.anexo.domain.CategoriaAnexo;

import java.util.UUID;

/**
 * DTO para anexos visíveis na página pública de aprovação.
 * Contém apenas campos necessários para exibição ao cliente.
 */
public record AnexoPublicoResponse(
        UUID id,
        CategoriaAnexo categoria,
        String nomeOriginal,
        String mimeType,
        boolean isImagem,
        String descricao
) {}
