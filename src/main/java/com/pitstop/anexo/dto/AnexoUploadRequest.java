package com.pitstop.anexo.dto;

import com.pitstop.anexo.domain.CategoriaAnexo;
import com.pitstop.anexo.domain.EntidadeTipo;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO de request para upload de anexo.
 *
 * <p>O arquivo em si é enviado como MultipartFile separado.</p>
 */
public record AnexoUploadRequest(
        @NotNull(message = "Tipo de entidade é obrigatório")
        EntidadeTipo entidadeTipo,

        @NotNull(message = "ID da entidade é obrigatório")
        UUID entidadeId,

        CategoriaAnexo categoria,

        String descricao
) {}
