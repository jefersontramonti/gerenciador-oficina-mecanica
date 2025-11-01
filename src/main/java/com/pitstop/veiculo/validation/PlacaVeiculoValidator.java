package com.pitstop.veiculo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Implementação da validação de placas de veículos brasileiros.
 *
 * <p>Valida placas nos formatos:</p>
 * <ul>
 *   <li>Padrão BR antigo: ABC1234 ou ABC-1234 (3 letras + 4 dígitos)</li>
 *   <li>Mercosul: ABC1D23 ou ABC-1D23 (3 letras + 1 dígito + 1 letra + 2 dígitos)</li>
 * </ul>
 *
 * <p>A validação é case-insensitive e aceita placas com ou sem hífen.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
public class PlacaVeiculoValidator implements ConstraintValidator<PlacaVeiculo, String> {

    /**
     * Regex para placa padrão BR (antiga): ABC1234 ou ABC-1234
     * 3 letras + hífen opcional + 4 dígitos
     */
    private static final Pattern PLACA_BR_ANTIGA = Pattern.compile(
        "^[A-Za-z]{3}-?\\d{4}$"
    );

    /**
     * Regex para placa Mercosul: ABC1D23 ou ABC-1D23
     * 3 letras + hífen opcional + 1 dígito + 1 letra + 2 dígitos
     */
    private static final Pattern PLACA_MERCOSUL = Pattern.compile(
        "^[A-Za-z]{3}-?\\d{1}[A-Za-z]{1}\\d{2}$"
    );

    @Override
    public void initialize(PlacaVeiculo constraintAnnotation) {
        // Nenhuma inicialização necessária
    }

    @Override
    public boolean isValid(String placa, ConstraintValidatorContext context) {
        // Aceita valores nulos (validação de @NotBlank cuida disso)
        if (placa == null || placa.isBlank()) {
            return true;
        }

        String placaLimpa = placa.trim();

        // Verifica se é placa BR antiga ou Mercosul
        return PLACA_BR_ANTIGA.matcher(placaLimpa).matches()
            || PLACA_MERCOSUL.matcher(placaLimpa).matches();
    }
}
