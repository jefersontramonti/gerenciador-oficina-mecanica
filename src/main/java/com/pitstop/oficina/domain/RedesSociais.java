package com.pitstop.oficina.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

/**
 * Value Object representing social media profiles.
 *
 * <p>Contains Instagram and Facebook profiles for marketing and customer engagement.</p>
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
public class RedesSociais implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Instagram username (without @).
     * Example: "oficinapitstop"
     */
    @Column(name = "instagram", length = 100)
    @Size(max = 100, message = "Instagram deve ter no máximo 100 caracteres")
    private String instagram;

    /**
     * Facebook page URL or username.
     * Example: "facebook.com/oficinapitstop" or "oficinapitstop"
     */
    @Column(name = "facebook", length = 100)
    @Size(max = 100, message = "Facebook deve ter no máximo 100 caracteres")
    private String facebook;

    /**
     * Gets Instagram profile URL.
     *
     * @return full Instagram URL or empty string if not set
     */
    public String getInstagramUrl() {
        if (instagram == null || instagram.isBlank()) {
            return "";
        }
        // Remove @ if present
        String username = instagram.startsWith("@") ? instagram.substring(1) : instagram;
        return "https://instagram.com/" + username;
    }

    /**
     * Gets Facebook page URL.
     *
     * @return full Facebook URL or empty string if not set
     */
    public String getFacebookUrl() {
        if (facebook == null || facebook.isBlank()) {
            return "";
        }
        // If already a full URL, return as is
        if (facebook.startsWith("http://") || facebook.startsWith("https://")) {
            return facebook;
        }
        // If contains facebook.com, add https
        if (facebook.contains("facebook.com")) {
            return "https://" + facebook;
        }
        // Otherwise, assume it's just a username
        return "https://facebook.com/" + facebook;
    }

    /**
     * Checks if at least one social media profile is set.
     *
     * @return true if Instagram or Facebook is filled
     */
    public boolean temRedesSociais() {
        return (instagram != null && !instagram.isBlank()) ||
               (facebook != null && !facebook.isBlank());
    }
}
