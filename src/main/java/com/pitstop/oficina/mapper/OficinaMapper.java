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
    @Mapping(target = "email", source = "contato.email")
    @Mapping(target = "telefone", source = "contato.telefoneFixo")
    @Mapping(target = "celular", source = "contato.telefoneCelular")
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
    @Mapping(target = "razaoSocial", source = "nome") // Map nome to razaoSocial
    @Mapping(target = "contato.telefoneFixo", source = "contato.telefoneFixo")
    @Mapping(target = "contato.telefoneCelular", source = "contato.telefoneCelular")
    @Mapping(target = "contato.email", source = "contato.email")
    @Mapping(target = "contato.telefoneAdicional", source = "contato.telefoneAdicional")
    @Mapping(target = "contato.emailSecundario", source = "contato.emailSecundario")
    @Mapping(target = "contato.website", source = "contato.website")
    void updateEntityFromRequest(UpdateOficinaRequest request, @MappingTarget Oficina oficina);

    /**
     * Método auxiliar para criar instâncias de objetos embutidos.
     * Chamado após o mapeamento para garantir que objetos embutidos existam.
     */
    @AfterMapping
    default void ensureEmbeddedObjects(@MappingTarget Oficina oficina, UpdateOficinaRequest request) {
        // Garantir que o objeto contato existe
        if (oficina.getContato() == null && request.contato() != null) {
            oficina.setContato(new com.pitstop.oficina.domain.Contato());
        }

        // Garantir que informacoesOperacionais existe e mesclar campos
        if (request.informacoesOperacionais() != null) {
            if (oficina.getInformacoesOperacionais() == null) {
                oficina.setInformacoesOperacionais(new com.pitstop.oficina.domain.InformacoesOperacionais());
            }
            mergeInformacoesOperacionais(oficina.getInformacoesOperacionais(), request.informacoesOperacionais());
        }

        // Garantir que redesSociais existe e mesclar campos
        if (request.redesSociais() != null) {
            if (oficina.getRedesSociais() == null) {
                oficina.setRedesSociais(new com.pitstop.oficina.domain.RedesSociais());
            }
            mergeRedesSociais(oficina.getRedesSociais(), request.redesSociais());
        }

        // Garantir que dadosBancarios existe e mesclar campos
        if (request.dadosBancarios() != null) {
            if (oficina.getDadosBancarios() == null) {
                oficina.setDadosBancarios(new com.pitstop.oficina.domain.DadosBancarios());
            }
            mergeDadosBancarios(oficina.getDadosBancarios(), request.dadosBancarios());
        }

        // Atualizar valorHora se fornecido
        if (request.valorHora() != null) {
            oficina.setValorHora(request.valorHora());
        }
    }

    /**
     * Mescla campos de InformacoesOperacionais (apenas campos não-nulos são atualizados)
     */
    private static void mergeInformacoesOperacionais(
            com.pitstop.oficina.domain.InformacoesOperacionais target,
            com.pitstop.oficina.domain.InformacoesOperacionais source) {
        if (source.getHorarioFuncionamento() != null) target.setHorarioFuncionamento(source.getHorarioFuncionamento());
        if (source.getCapacidadeSimultanea() != null) target.setCapacidadeSimultanea(source.getCapacidadeSimultanea());
        if (source.getNumeroFuncionarios() != null) target.setNumeroFuncionarios(source.getNumeroFuncionarios());
        if (source.getNumeroMecanicos() != null) target.setNumeroMecanicos(source.getNumeroMecanicos());
    }

    /**
     * Mescla campos de RedesSociais (apenas campos não-nulos são atualizados)
     */
    private static void mergeRedesSociais(
            com.pitstop.oficina.domain.RedesSociais target,
            com.pitstop.oficina.domain.RedesSociais source) {
        if (source.getInstagram() != null) target.setInstagram(source.getInstagram());
        if (source.getFacebook() != null) target.setFacebook(source.getFacebook());
    }

    /**
     * Mescla campos de DadosBancarios (apenas campos não-nulos são atualizados)
     */
    private static void mergeDadosBancarios(
            com.pitstop.oficina.domain.DadosBancarios target,
            com.pitstop.oficina.domain.DadosBancarios source) {
        if (source.getBanco() != null) target.setBanco(source.getBanco());
        if (source.getAgencia() != null) target.setAgencia(source.getAgencia());
        if (source.getConta() != null) target.setConta(source.getConta());
        if (source.getDigitoConta() != null) target.setDigitoConta(source.getDigitoConta());
        if (source.getTipoConta() != null) target.setTipoConta(source.getTipoConta());
        if (source.getChavePix() != null) target.setChavePix(source.getChavePix());
    }
}
