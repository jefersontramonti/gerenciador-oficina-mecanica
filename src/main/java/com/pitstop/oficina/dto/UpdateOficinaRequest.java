package com.pitstop.oficina.dto;

import com.pitstop.cliente.domain.Endereco;
import com.pitstop.oficina.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * DTO para atualização de dados de uma oficina existente.
 * Todos os campos são opcionais (apenas os informados serão atualizados).
 *
 * @param nome Razão social (opcional)
 * @param nomeFantasia Nome fantasia (opcional)
 * @param inscricaoEstadual Inscrição estadual (opcional)
 * @param inscricaoMunicipal Inscrição municipal (opcional)
 * @param tipoPessoa Tipo de pessoa (opcional)
 * @param regimeTributario Regime tributário (opcional)
 * @param contato Dados de contato (opcional)
 * @param endereco Endereço (opcional)
 * @param informacoesOperacionais Informações operacionais (opcional)
 * @param redesSociais Redes sociais (opcional)
 * @param dadosBancarios Dados bancários (opcional)
 * @param logoUrl URL da logo da oficina (opcional)
 * @param valorHora Valor/hora de mão de obra (opcional)
 *
 * @author PitStop Team
 */
public record UpdateOficinaRequest(
    String nome,

    String nomeFantasia,

    String inscricaoEstadual,

    String inscricaoMunicipal,

    TipoPessoa tipoPessoa,

    RegimeTributario regimeTributario,

    @Valid
    Contato contato,

    @Valid
    Endereco endereco,

    @Valid
    InformacoesOperacionais informacoesOperacionais,

    @Valid
    RedesSociais redesSociais,

    @Valid
    DadosBancarios dadosBancarios,

    String logoUrl,

    @DecimalMin(value = "0.00", message = "Valor/hora deve ser maior ou igual a zero")
    @DecimalMax(value = "10000.00", message = "Valor/hora deve ser no máximo R$ 10.000")
    BigDecimal valorHora
) {
}
