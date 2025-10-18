package com.pitstop.shared.security;

import com.pitstop.usuario.domain.Usuario;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security's UserDetails interface.
 *
 * <p>Wraps the {@link Usuario} entity to integrate with Spring Security authentication.
 *
 * <p><b>Authority mapping:</b>
 * The user's {@code perfil} (role) is mapped to a Spring Security {@code GrantedAuthority}.
 * For example, a user with {@code PerfilUsuario.ADMIN} will have authority "ADMIN".
 *
 * <p>This allows using {@code @PreAuthorize("hasAuthority('ADMIN')")} annotations in controllers.
 *
 * <p><b>Account status:</b>
 * <ul>
 *   <li><b>isEnabled()</b>: Returns {@code usuario.isAtivo()} (soft delete check)</li>
 *   <li><b>isAccountNonExpired()</b>: Always true (no expiration logic)</li>
 *   <li><b>isAccountNonLocked()</b>: Always true (no locking mechanism)</li>
 *   <li><b>isCredentialsNonExpired()</b>: Always true (no password expiration)</li>
 * </ul>
 *
 * <p><b>Single-tenant note:</b>
 * Currently, this class does NOT contain tenant information. When migrating to SaaS multi-tenant,
 * consider adding a {@code tenantId} field to enforce tenant isolation at the security level.
 */
@RequiredArgsConstructor
@Getter
public class CustomUserDetails implements UserDetails {

    private final Usuario usuario;

    /**
     * Returns the authorities granted to the user.
     *
     * <p>Maps the user's {@code perfil} (role) to a Spring Security authority.
     *
     * @return a collection containing a single GrantedAuthority (the user's role)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(usuario.getPerfil().name()));
    }

    /**
     * Returns the password used to authenticate the user.
     *
     * @return the BCrypt-hashed password
     */
    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    /**
     * Returns the username used to authenticate the user.
     *
     * <p>In PitStop, users authenticate with their email address, not a username.
     *
     * @return the user's email (used as username)
     */
    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    /**
     * Indicates whether the user's account has expired.
     *
     * <p>PitStop does not implement account expiration, so this always returns true.
     *
     * @return true (accounts never expire)
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     *
     * <p>PitStop does not implement account locking, so this always returns true.
     *
     * @return true (accounts are never locked)
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     *
     * <p>PitStop does not implement password expiration, so this always returns true.
     *
     * @return true (credentials never expire)
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     *
     * <p>Uses the {@code ativo} flag from the {@link Usuario} entity.
     * Disabled users cannot authenticate.
     *
     * @return true if the user is active (ativo = true), false otherwise
     */
    @Override
    public boolean isEnabled() {
        return usuario.isAtivo();
    }
}
