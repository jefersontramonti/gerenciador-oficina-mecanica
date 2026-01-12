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
     * Landline phone number.
     * Accepts formats: (XX) XXXX-XXXX or just digits.
     */
    @Column(name = "telefone_fixo", length = 20)
    @Size(max = 20, message = "Telefone fixo deve ter no máximo 20 caracteres")
    private String telefoneFixo;

    /**
     * Mobile phone number (WhatsApp).
     * Accepts formats: (XX) XXXXX-XXXX or just digits.
     */
    @Column(name = "telefone_celular", length = 20)
    @Size(max = 20, message = "Telefone celular deve ter no máximo 20 caracteres")
    private String telefoneCelular;

    /**
     * Additional phone number.
     */
    @Column(name = "telefone_adicional", length = 20)
    @Size(max = 20, message = "Telefone adicional deve ter no máximo 20 caracteres")
    private String telefoneAdicional;

    /**
     * Primary email address.
     */
    @Column(name = "email", length = 200)
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
     * Checks if basic contact info is complete (at least one contact method).
     *
     * @return true if email or any phone is filled
     */
    public boolean isCompleto() {
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = (telefoneCelular != null && !telefoneCelular.isBlank()) ||
                          (telefoneFixo != null && !telefoneFixo.isBlank());
        return hasEmail || hasPhone;
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
