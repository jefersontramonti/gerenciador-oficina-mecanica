package com.pitstop.cliente.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de cliente no sistema PitStop.
 * Define a categoria tributária do cliente para fins de validação de CPF/CNPJ.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum TipoCliente {

    /**
     * Pessoa Física (Individual) - requer CPF válido (11 dígitos).
     */
    PESSOA_FISICA("Pessoa Física", 11),

    /**
     * Pessoa Jurídica (Business) - requer CNPJ válido (14 dígitos).
     */
    PESSOA_JURIDICA("Pessoa Jurídica", 14);

    /**
     * Descrição legível do tipo de cliente.
     */
    private final String descricao;

    /**
     * Número de dígitos esperado para o documento (CPF: 11, CNPJ: 14).
     */
    private final int digitosDocumento;

    /**
     * Valida se um documento possui o número correto de dígitos para este tipo.
     *
     * @param documentoSemFormatacao documento sem formatação (apenas dígitos)
     * @return true se o documento tem o número correto de dígitos
     */
    public boolean validarTamanhoDocumento(String documentoSemFormatacao) {
        if (documentoSemFormatacao == null) {
            return false;
        }
        return documentoSemFormatacao.replaceAll("\\D", "").length() == this.digitosDocumento;
    }
}
