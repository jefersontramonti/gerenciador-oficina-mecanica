package com.pitstop.anexo.dto;

import com.pitstop.anexo.domain.CategoriaAnexo;
import com.pitstop.anexo.domain.EntidadeTipo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta para Anexo.
 */
public record AnexoResponse(
        UUID id,
        EntidadeTipo entidadeTipo,
        UUID entidadeId,
        CategoriaAnexo categoria,
        String nomeOriginal,
        Long tamanhoBytes,
        String tamanhoFormatado,
        String mimeType,
        boolean isImagem,
        boolean isPdf,
        String descricao,
        String uploadedByNome,
        LocalDateTime uploadedAt,
        String urlDownload,
        String urlThumbnail,
        boolean visivelParaCliente
) {}
