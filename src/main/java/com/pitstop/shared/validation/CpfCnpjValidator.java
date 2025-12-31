package com.pitstop.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementacao da validacao de CPF e CNPJ brasileiros.
 *
 * <p>Valida os digitos verificadores usando o algoritmo oficial da Receita Federal:</p>
 * <ul>
 *   <li>CPF: 11 digitos com 2 digitos verificadores (modulo 11)</li>
 *   <li>CNPJ: 14 digitos com 2 digitos verificadores (modulo 11)</li>
 * </ul>
 *
 * <p>Tambem rejeita documentos com todos os digitos iguais (ex: 111.111.111-11).</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class CpfCnpjValidator implements ConstraintValidator<CpfCnpj, String> {

    @Override
    public void initialize(CpfCnpj constraintAnnotation) {
        // Nenhuma inicializacao necessaria
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Aceita valores nulos (validacao de @NotBlank cuida disso)
        if (value == null || value.isBlank()) {
            return true;
        }

        // Remove formatacao (pontos, tracos, barras)
        String documento = value.replaceAll("[^0-9]", "");

        if (documento.length() == 11) {
            return isValidCpf(documento);
        } else if (documento.length() == 14) {
            return isValidCnpj(documento);
        }

        return false;
    }

    /**
     * Valida CPF usando algoritmo modulo 11 da Receita Federal.
     *
     * <p>Algoritmo:</p>
     * <ol>
     *   <li>Multiplica os 9 primeiros digitos por pesos de 10 a 2</li>
     *   <li>Calcula o resto da divisao por 11</li>
     *   <li>Se resto < 2, digito = 0; senao digito = 11 - resto</li>
     *   <li>Repete para o segundo digito com pesos de 11 a 2</li>
     * </ol>
     *
     * @param cpf CPF com 11 digitos (apenas numeros)
     * @return true se CPF valido
     */
    private boolean isValidCpf(String cpf) {
        // Rejeita CPFs com todos os digitos iguais
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // Calcula primeiro digito verificador
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int primeiroDigito = calcularDigito(soma);

            // Calcula segundo digito verificador
            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int segundoDigito = calcularDigito(soma);

            // Verifica se os digitos calculados conferem
            return cpf.charAt(9) == Character.forDigit(primeiroDigito, 10)
                && cpf.charAt(10) == Character.forDigit(segundoDigito, 10);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Valida CNPJ usando algoritmo modulo 11 da Receita Federal.
     *
     * <p>Algoritmo:</p>
     * <ol>
     *   <li>Multiplica os 12 primeiros digitos por pesos [5,4,3,2,9,8,7,6,5,4,3,2]</li>
     *   <li>Calcula o resto da divisao por 11</li>
     *   <li>Se resto < 2, digito = 0; senao digito = 11 - resto</li>
     *   <li>Repete para o segundo digito com pesos [6,5,4,3,2,9,8,7,6,5,4,3,2]</li>
     * </ol>
     *
     * @param cnpj CNPJ com 14 digitos (apenas numeros)
     * @return true se CNPJ valido
     */
    private boolean isValidCnpj(String cnpj) {
        // Rejeita CNPJs com todos os digitos iguais
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        try {
            // Pesos para calculo do primeiro digito
            int[] pesosPrimeiroDigito = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            // Pesos para calculo do segundo digito
            int[] pesosSegundoDigito = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

            // Calcula primeiro digito verificador
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * pesosPrimeiroDigito[i];
            }
            int primeiroDigito = calcularDigito(soma);

            // Calcula segundo digito verificador
            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * pesosSegundoDigito[i];
            }
            int segundoDigito = calcularDigito(soma);

            // Verifica se os digitos calculados conferem
            return cnpj.charAt(12) == Character.forDigit(primeiroDigito, 10)
                && cnpj.charAt(13) == Character.forDigit(segundoDigito, 10);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calcula digito verificador usando regra modulo 11.
     *
     * @param soma soma ponderada dos digitos
     * @return digito verificador (0-9)
     */
    private int calcularDigito(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
