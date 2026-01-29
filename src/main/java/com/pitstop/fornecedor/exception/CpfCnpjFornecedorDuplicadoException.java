package com.pitstop.fornecedor.exception;

/**
 * Exceção lançada quando um CPF/CNPJ de fornecedor já está cadastrado.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class CpfCnpjFornecedorDuplicadoException extends RuntimeException {

    public CpfCnpjFornecedorDuplicadoException(String cpfCnpj) {
        super("Já existe um fornecedor cadastrado com o CPF/CNPJ: " + cpfCnpj);
    }
}
