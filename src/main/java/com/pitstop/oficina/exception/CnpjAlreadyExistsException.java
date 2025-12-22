package com.pitstop.oficina.exception;

/**
 * Exceção lançada quando se tenta cadastrar uma oficina com CNPJ já existente.
 *
 * @author PitStop Team
 */
public class CnpjAlreadyExistsException extends RuntimeException {

    public CnpjAlreadyExistsException(String cnpj) {
        super("Já existe uma oficina cadastrada com o CNPJ: " + formatCnpj(cnpj));
    }

    private static String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return String.format("%s.%s.%s/%s-%s",
            cnpj.substring(0, 2),
            cnpj.substring(2, 5),
            cnpj.substring(5, 8),
            cnpj.substring(8, 12),
            cnpj.substring(12, 14)
        );
    }
}
