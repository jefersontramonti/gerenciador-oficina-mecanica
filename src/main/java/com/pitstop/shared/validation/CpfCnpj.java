package com.pitstop.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotacao de validacao para CPF e CNPJ brasileiros.
 *
 * <p>Valida tanto CPF (11 digitos) quanto CNPJ (14 digitos) incluindo
 * verificacao dos digitos verificadores usando o algoritmo oficial da Receita Federal.</p>
 *
 * <p>Aceita documentos com ou sem formatacao (pontos, tracos, barras).</p>
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * public class Cliente {
 *     &#64;CpfCnpj
 *     private String cpfCnpj;
 * }
 * </pre>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Documented
@Constraint(validatedBy = CpfCnpjValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CpfCnpj {

    String message() default "CPF ou CNPJ invalido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
