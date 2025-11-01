package com.pitstop.veiculo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation customizada para validação de placas de veículos brasileiros.
 *
 * <p>Formatos aceitos:</p>
 * <ul>
 *   <li>Placa antiga (Padrão BR): ABC1234 ou ABC-1234</li>
 *   <li>Placa Mercosul: ABC1D23 ou ABC-1D23</li>
 * </ul>
 *
 * <p>A validação aceita placas com ou sem hífen, pois a normalização
 * é feita posteriormente no @PrePersist da entidade.</p>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlacaVeiculoValidator.class)
@Documented
public @interface PlacaVeiculo {

    String message() default "Placa inválida. Formatos aceitos: ABC1234 (padrão BR) ou ABC1D23 (Mercosul)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
