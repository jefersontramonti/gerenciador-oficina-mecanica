package com.pitstop.anexo.mapper;

import com.pitstop.anexo.domain.Anexo;
import com.pitstop.anexo.dto.AnexoPublicoResponse;
import com.pitstop.anexo.dto.AnexoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper para conversão entre Anexo e DTOs.
 */
@Mapper(componentModel = "spring")
public interface AnexoMapper {

    @Mapping(target = "isImagem", expression = "java(anexo.isImagem())")
    @Mapping(target = "isPdf", expression = "java(anexo.isPdf())")
    @Mapping(target = "tamanhoFormatado", expression = "java(anexo.getTamanhoFormatado())")
    @Mapping(target = "uploadedByNome", source = "uploadedBy.nome")
    @Mapping(target = "urlDownload", expression = "java(\"/api/anexos/\" + anexo.getId() + \"/download\")")
    @Mapping(target = "urlThumbnail", expression = "java(anexo.isImagem() ? \"/api/anexos/\" + anexo.getId() + \"/thumbnail\" : null)")
    AnexoResponse toResponse(Anexo anexo);

    /**
     * Converte Anexo para DTO público (usado na página de aprovação).
     */
    @Mapping(target = "isImagem", expression = "java(anexo.isImagem())")
    AnexoPublicoResponse toPublicoResponse(Anexo anexo);
}
