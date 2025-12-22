package com.pitstop.oficina.dto;

import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para criação de uma nova oficina (onboarding).
 *
 * @param nome Razão social da oficina
 * @param nomeFantasia Nome fantasia da oficina
 * @param cnpj CNPJ da oficina (apenas números)
 * @param inscricaoEstadual Inscrição estadual (opcional)
 * @param inscricaoMunicipal Inscrição municipal (opcional)
 * @param tipoPessoa Tipo de pessoa (JURIDICA ou FISICA para MEI)
 * @param regimeTributario Regime tributário (SIMPLES_NACIONAL, LUCRO_PRESUMIDO, LUCRO_REAL, MEI)
 * @param planoAssinatura Plano escolhido (ECONOMICO, PROFISSIONAL, TURBINADO)
 * @param contato Dados de contato (telefone, celular, email)
 * @param endereco Endereço completo da oficina
 * @param informacoesOperacionais Informações operacionais (horários, especialidades)
 * @param redesSociais Redes sociais (opcional)
 * @param dadosBancarios Dados bancários para recebimento (opcional)
 *
 * @author PitStop Team
 */
public record CreateOficinaRequest(
    @NotBlank(message = "Nome (razão social) é obrigatório")
    String nome,

    String nomeFantasia,

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos")
    String cnpj,

    String inscricaoEstadual,

    String inscricaoMunicipal,

    @NotNull(message = "Tipo de pessoa é obrigatório")
    TipoPessoa tipoPessoa,

    @NotNull(message = "Regime tributário é obrigatório")
    RegimeTributario regimeTributario,

    @NotNull(message = "Plano de assinatura é obrigatório")
    PlanoAssinatura planoAssinatura,

    @Valid
    @NotNull(message = "Dados de contato são obrigatórios")
    Contato contato,

    @Valid
    @NotNull(message = "Endereço é obrigatório")
    Endereco endereco,

    @Valid
    InformacoesOperacionais informacoesOperacionais,

    @Valid
    RedesSociais redesSociais,

    @Valid
    DadosBancarios dadosBancarios
) {
}
