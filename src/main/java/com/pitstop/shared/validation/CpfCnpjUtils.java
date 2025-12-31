package com.pitstop.shared.validation;

/**
 * Utilitario para validacao e formatacao de CPF e CNPJ.
 *
 * <p>Fornece metodos estaticos para:</p>
 * <ul>
 *   <li>Validar CPF/CNPJ com digitos verificadores</li>
 *   <li>Formatar CPF/CNPJ para exibicao</li>
 *   <li>Remover formatacao de CPF/CNPJ</li>
 *   <li>Identificar tipo de documento (CPF ou CNPJ)</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public final class CpfCnpjUtils {

    private CpfCnpjUtils() {
        // Utilitario - nao deve ser instanciado
    }

    /**
     * Valida CPF ou CNPJ incluindo digitos verificadores.
     *
     * @param documento CPF ou CNPJ (com ou sem formatacao)
     * @return true se documento valido
     */
    public static boolean isValid(String documento) {
        if (documento == null || documento.isBlank()) {
            return false;
        }

        String limpo = removeFormatacao(documento);

        if (limpo.length() == 11) {
            return isValidCpf(limpo);
        } else if (limpo.length() == 14) {
            return isValidCnpj(limpo);
        }

        return false;
    }

    /**
     * Valida CPF com digitos verificadores.
     *
     * @param cpf CPF (com ou sem formatacao)
     * @return true se CPF valido
     */
    public static boolean isValidCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return false;
        }

        String limpo = removeFormatacao(cpf);

        if (limpo.length() != 11) {
            return false;
        }

        // Rejeita CPFs com todos os digitos iguais
        if (limpo.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // Calcula primeiro digito verificador
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += Character.getNumericValue(limpo.charAt(i)) * (10 - i);
            }
            int primeiroDigito = calcularDigito(soma);

            // Calcula segundo digito verificador
            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += Character.getNumericValue(limpo.charAt(i)) * (11 - i);
            }
            int segundoDigito = calcularDigito(soma);

            // Verifica se os digitos calculados conferem
            return limpo.charAt(9) == Character.forDigit(primeiroDigito, 10)
                && limpo.charAt(10) == Character.forDigit(segundoDigito, 10);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valida CNPJ com digitos verificadores.
     *
     * @param cnpj CNPJ (com ou sem formatacao)
     * @return true se CNPJ valido
     */
    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return false;
        }

        String limpo = removeFormatacao(cnpj);

        if (limpo.length() != 14) {
            return false;
        }

        // Rejeita CNPJs com todos os digitos iguais
        if (limpo.matches("(\\d)\\1{13}")) {
            return false;
        }

        try {
            int[] pesosPrimeiroDigito = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int[] pesosSegundoDigito = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

            // Calcula primeiro digito verificador
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += Character.getNumericValue(limpo.charAt(i)) * pesosPrimeiroDigito[i];
            }
            int primeiroDigito = calcularDigito(soma);

            // Calcula segundo digito verificador
            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += Character.getNumericValue(limpo.charAt(i)) * pesosSegundoDigito[i];
            }
            int segundoDigito = calcularDigito(soma);

            // Verifica se os digitos calculados conferem
            return limpo.charAt(12) == Character.forDigit(primeiroDigito, 10)
                && limpo.charAt(13) == Character.forDigit(segundoDigito, 10);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formata CPF para exibicao (XXX.XXX.XXX-XX).
     *
     * @param cpf CPF com 11 digitos
     * @return CPF formatado ou original se invalido
     */
    public static String formatarCpf(String cpf) {
        String limpo = removeFormatacao(cpf);
        if (limpo.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            limpo.substring(0, 3),
            limpo.substring(3, 6),
            limpo.substring(6, 9),
            limpo.substring(9, 11));
    }

    /**
     * Formata CNPJ para exibicao (XX.XXX.XXX/XXXX-XX).
     *
     * @param cnpj CNPJ com 14 digitos
     * @return CNPJ formatado ou original se invalido
     */
    public static String formatarCnpj(String cnpj) {
        String limpo = removeFormatacao(cnpj);
        if (limpo.length() != 14) {
            return cnpj;
        }
        return String.format("%s.%s.%s/%s-%s",
            limpo.substring(0, 2),
            limpo.substring(2, 5),
            limpo.substring(5, 8),
            limpo.substring(8, 12),
            limpo.substring(12, 14));
    }

    /**
     * Formata CPF ou CNPJ automaticamente baseado no tamanho.
     *
     * @param documento CPF ou CNPJ
     * @return documento formatado
     */
    public static String formatar(String documento) {
        String limpo = removeFormatacao(documento);
        if (limpo.length() == 11) {
            return formatarCpf(limpo);
        } else if (limpo.length() == 14) {
            return formatarCnpj(limpo);
        }
        return documento;
    }

    /**
     * Remove formatacao de CPF/CNPJ (pontos, tracos, barras).
     *
     * @param documento documento formatado
     * @return apenas digitos
     */
    public static String removeFormatacao(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("[^0-9]", "");
    }

    /**
     * Verifica se documento e CPF (11 digitos).
     *
     * @param documento CPF ou CNPJ
     * @return true se for CPF
     */
    public static boolean isCpf(String documento) {
        return removeFormatacao(documento).length() == 11;
    }

    /**
     * Verifica se documento e CNPJ (14 digitos).
     *
     * @param documento CPF ou CNPJ
     * @return true se for CNPJ
     */
    public static boolean isCnpj(String documento) {
        return removeFormatacao(documento).length() == 14;
    }

    private static int calcularDigito(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
