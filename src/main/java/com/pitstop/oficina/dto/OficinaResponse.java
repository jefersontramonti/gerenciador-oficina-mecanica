package com.pitstop.oficina.dto;

import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de resposta com dados completos da oficina.
 *
 * @param id ID único da oficina
 * @param nome Razão social
 * @param nomeFantasia Nome fantasia
 * @param cnpj CNPJ
 * @param inscricaoEstadual Inscrição estadual
 * @param inscricaoMunicipal Inscrição municipal
 * @param tipoPessoa Tipo de pessoa
 * @param regimeTributario Regime tributário
 * @param planoAssinatura Plano de assinatura atual
 * @param status Status da oficina (ATIVA, SUSPENSA, INATIVA, CANCELADA)
 * @param dataAssinatura Data de início da assinatura
 * @param dataVencimentoPlano Data de vencimento do plano
 * @param valorMensalidade Valor da mensalidade
 * @param contato Dados de contato
 * @param endereco Endereço completo
 * @param informacoesOperacionais Informações operacionais
 * @param redesSociais Redes sociais
 * @param dadosBancarios Dados bancários
 * @param logoUrl URL da logo
 * @param createdAt Data de criação do registro
 * @param updatedAt Data da última atualização
 *
 * @author PitStop Team
 */
public record OficinaResponse(
    UUID id,
    String nomeFantasia,
    String razaoSocial,
    String cnpjCpf,
    String inscricaoEstadual,
    String inscricaoMunicipal,
    TipoPessoa tipoPessoa,
    String nomeResponsavel,
    String email,
    String telefone,
    String celular,
    RegimeTributario regimeTributario,
    PlanoAssinatura plano,
    StatusOficina status,
    LocalDate dataAssinatura,
    LocalDate dataVencimentoPlano,
    BigDecimal valorMensalidade,
    Endereco endereco,
    InformacoesOperacionais informacoesOperacionais,
    RedesSociais redesSociais,
    DadosBancarios dadosBancarios,
    String logoUrl
) {
}
