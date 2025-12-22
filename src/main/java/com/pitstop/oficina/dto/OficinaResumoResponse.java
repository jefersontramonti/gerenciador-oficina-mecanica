package com.pitstop.oficina.dto;

import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de resposta resumida da oficina (para listagens).
 * Contém apenas os dados essenciais para economizar banda.
 *
 * @param id ID da oficina
 * @param nome Razão social
 * @param nomeFantasia Nome fantasia
 * @param cnpj CNPJ (mascarado)
 * @param planoAssinatura Plano atual
 * @param status Status (ATIVA, SUSPENSA, etc)
 * @param dataVencimentoPlano Data de vencimento
 * @param valorMensalidade Valor da mensalidade
 * @param cidade Cidade onde está localizada
 * @param estado UF do estado
 *
 * @author PitStop Team
 */
public record OficinaResumoResponse(
    UUID id,
    String nomeFantasia,
    String razaoSocial,
    String cnpjCpf,
    PlanoAssinatura plano,
    StatusOficina status,
    LocalDate dataVencimentoPlano,
    BigDecimal valorMensalidade,
    String cidade,
    String estado
) {
    /**
     * Máscara o CNPJ para exibição (XX.XXX.XXX/0001-XX).
     */
    public String getCnpjMascarado() {
        if (cnpjCpf == null || cnpjCpf.length() != 14) {
            return cnpjCpf;
        }
        return String.format("%s.%s.%s/%s-%s",
            cnpjCpf.substring(0, 2),
            cnpjCpf.substring(2, 5),
            cnpjCpf.substring(5, 8),
            cnpjCpf.substring(8, 12),
            cnpjCpf.substring(12, 14)
        );
    }

    /**
     * Verifica se o plano está próximo do vencimento (7 dias ou menos).
     */
    public boolean isVencimentoProximo() {
        if (dataVencimentoPlano == null) {
            return false;
        }
        LocalDate hoje = LocalDate.now();
        LocalDate daquiA7Dias = hoje.plusDays(7);
        return dataVencimentoPlano.isBefore(daquiA7Dias) || dataVencimentoPlano.isEqual(daquiA7Dias);
    }

    /**
     * Verifica se o plano está vencido.
     */
    public boolean isVencido() {
        if (dataVencimentoPlano == null) {
            return false;
        }
        return dataVencimentoPlano.isBefore(LocalDate.now());
    }
}
