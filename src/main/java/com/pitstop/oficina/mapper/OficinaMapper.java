package com.pitstop.oficina.mapper;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.dto.CreateOficinaRequest;
import com.pitstop.oficina.dto.OficinaResumoResponse;
import com.pitstop.oficina.dto.OficinaResponse;
import com.pitstop.oficina.dto.UpdateOficinaRequest;
import org.mapstruct.*;

/**
 * Mapper MapStruct para conversão entre Oficina entity e DTOs.
 *
 * @author PitStop Team
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OficinaMapper {

    /**
     * Converte CreateOficinaRequest para Oficina entity.
     * Campos de auditoria (createdAt, updatedAt) são ignorados (gerados automaticamente).
     * Status é definido no service.
     *
     * @param request DTO de criação
     * @return Entity Oficina
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dataAssinatura", ignore = true)
    @Mapping(target = "dataVencimentoPlano", ignore = true)
    @Mapping(target = "valorMensalidade", ignore = true)
    Oficina toEntity(CreateOficinaRequest request);

    /**
     * Converte Oficina entity para OficinaResponse (resposta completa).
     *
     * @param oficina Entity Oficina
     * @return DTO de resposta completa
     */
    OficinaResponse toResponse(Oficina oficina);

    /**
     * Converte Oficina entity para OficinaResumoResponse (resposta resumida para listagens).
     *
     * @param oficina Entity Oficina
     * @return DTO de resposta resumida
     */
    @Mapping(target = "cidade", source = "endereco.cidade")
    @Mapping(target = "estado", source = "endereco.estado")
    OficinaResumoResponse toResumoResponse(Oficina oficina);

    /**
     * Atualiza uma entity Oficina existente com dados de UpdateOficinaRequest.
     * Apenas campos não-nulos do request são copiados (partial update).
     *
     * @param request DTO de atualização
     * @param oficina Entity existente a ser atualizada
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cnpjCpf", ignore = true) // CNPJ não pode ser alterado
    @Mapping(target = "plano", ignore = true) // Plano só via upgradePlan()
    @Mapping(target = "status", ignore = true) // Status só via suspend/activate
    @Mapping(target = "dataAssinatura", ignore = true)
    @Mapping(target = "dataVencimentoPlano", ignore = true)
    @Mapping(target = "valorMensalidade", ignore = true)
    void updateEntityFromRequest(UpdateOficinaRequest request, @MappingTarget Oficina oficina);
}
