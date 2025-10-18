package com.pitstop.shared.security;

import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * <p>Loads user-specific data from the database during authentication.
 *
 * <p><b>Usage in Spring Security:</b>
 * <ul>
 *   <li>Called by {@link JwtAuthenticationFilter} to load user by email extracted from JWT</li>
 *   <li>Called by Spring Security's AuthenticationManager during login (if used)</li>
 * </ul>
 *
 * <p><b>Authentication flow:</b>
 * <ol>
 *   <li>User sends POST /api/auth/login with email + password</li>
 *   <li>AuthenticationService validates credentials manually</li>
 *   <li>JWT token is generated and returned</li>
 *   <li>On subsequent requests, {@link JwtAuthenticationFilter} extracts email from token</li>
 *   <li>This service loads the full user from database</li>
 *   <li>Spring Security populates SecurityContext with UserDetails</li>
 * </ol>
 *
 * <p><b>Single-tenant note:</b>
 * Currently queries users without tenant filtering. When migrating to SaaS multi-tenant,
 * modify the repository query to include {@code AND tenant_id = ?} based on tenant context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Loads a user by their email address (username).
     *
     * <p>This method is called by Spring Security during authentication.
     *
     * @param email the email address (used as username in PitStop)
     * @return UserDetails implementation containing the user data
     * @throws UsernameNotFoundException if user not found by email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("Usuário não encontrado: " + email);
                });

        log.debug("User loaded successfully: {} (Perfil: {})", usuario.getEmail(), usuario.getPerfil());

        return new CustomUserDetails(usuario);
    }
}
