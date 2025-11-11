package com.pitstop.oficina.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

/**
 * Value Object representing contact information.
 *
 * <p>Contains phones, emails, and website for workshop communication.</p>
 *
 * @since 1.0.0
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Contato implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Landline phone number in format (XX) XXXX-XXXX.
     */
    @Column(name = "telefone_fixo", length = 15)
    @Pattern(regexp = "^\\(\\d{2}\\) \\d{4}-\\d{4}$", message = "Telefone fixo deve estar no formato (XX) XXXX-XXXX")
    private String telefoneFixo;

    /**
     * Mobile phone number (WhatsApp) in format (XX) XXXXX-XXXX.
     */
    @Column(name = "telefone_celular", nullable = false, length = 16)
    @Pattern(regexp = "^\\(\\d{2}\\) \\d{5}-\\d{4}$", message = "Telefone celular deve estar no formato (XX) XXXXX-XXXX")
    private String telefoneCelular;

    /**
     * Additional phone number.
     */
    @Column(name = "telefone_adicional", length = 16)
    @Pattern(regexp = "^\\(\\d{2}\\) \\d{4,5}-\\d{4}$", message = "Telefone adicional deve estar no formato válido")
    private String telefoneAdicional;

    /**
     * Primary email address.
     */
    @Column(name = "email", nullable = false, length = 200)
    @Email(message = "Email inválido")
    @Size(max = 200, message = "Email deve ter no máximo 200 caracteres")
    private String email;

    /**
     * Secondary email address.
     */
    @Column(name = "email_secundario", length = 200)
    @Email(message = "Email secundário inválido")
    @Size(max = 200, message = "Email secundário deve ter no máximo 200 caracteres")
    private String emailSecundario;

    /**
     * Website URL.
     */
    @Column(name = "website", length = 200)
    @Size(max = 200, message = "Website deve ter no máximo 200 caracteres")
    private String website;

    /**
     * Checks if basic contact info is complete (email and mobile phone).
     *
     * @return true if email and telefoneCelular are filled
     */
    public boolean isCompleto() {
        return email != null && !email.isBlank() &&
               telefoneCelular != null && !telefoneCelular.isBlank();
    }

    /**
     * Gets WhatsApp number (same as mobile phone).
     *
     * @return mobile phone number formatted for WhatsApp
     */
    public String getWhatsApp() {
        return telefoneCelular;
    }

    /**
     * Gets WhatsApp number without formatting (only digits).
     *
     * @return phone number with only digits
     */
    public String getWhatsAppSomenteNumeros() {
        if (telefoneCelular == null) {
            return null;
        }
        return telefoneCelular.replaceAll("\\D", "");
    }
}
