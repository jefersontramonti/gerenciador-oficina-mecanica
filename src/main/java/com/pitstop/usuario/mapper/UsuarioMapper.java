package com.pitstop.usuario.mapper;

import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.dto.CreateUsuarioRequest;
import com.pitstop.usuario.dto.UpdateUsuarioRequest;
import com.pitstop.usuario.dto.UsuarioResponse;
import org.mapstruct.*;

/**
 * Mapper MapStruct para conversão entre entidade Usuario e DTOs.
 *
 * MapStruct gera a implementação em tempo de compilação,
 * garantindo alta performance e type-safety.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UsuarioMapper {

    /**
     * Converte CreateUsuarioRequest para entidade Usuario.
     *
     * IMPORTANTE: A senha será criptografada no Service antes de persistir.
     *
     * @param request DTO de criação
     * @return Entidade Usuario (sem ID, timestamps, etc.)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "ultimoAcesso", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Usuario toEntity(CreateUsuarioRequest request);

    /**
     * Converte entidade Usuario para UsuarioResponse.
     *
     * Nunca expõe a senha nos DTOs de resposta.
     *
     * @param usuario Entidade Usuario
     * @return DTO de resposta com dados seguros
     */
    @Mapping(source = "oficina.id", target = "oficinaId")
    UsuarioResponse toResponse(Usuario usuario);

    /**
     * Atualiza uma entidade Usuario existente com dados de UpdateUsuarioRequest.
     *
     * Apenas atualiza os campos não-nulos do request.
     * Campos ignorados: id, timestamps (controlados pelo JPA Auditing), senha (tratada manualmente).
     *
     * IMPORTANTE: A senha é criptografada manualmente no Service ANTES de chamar este método,
     * por isso é ignorada aqui para evitar sobrescrever o hash com texto plano.
     *
     * @param request DTO de atualização
     * @param usuario Entidade existente a ser atualizada
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "ultimoAcesso", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateUsuarioRequest request, @MappingTarget Usuario usuario);
}
