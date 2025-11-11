package com.pitstop.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

/**
 * Utilitário para acessar informações do usuário autenticado via Spring Security.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Retorna a autenticação atual do Spring Security.
     *
     * @return Authentication ou null se não autenticado
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Retorna o UserDetails do usuário autenticado.
     *
     * @return UserDetails ou null se não autenticado
     */
    public static UserDetails getCurrentUser() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Retorna o username (email) do usuário autenticado.
     *
     * @return username ou null se não autenticado
     */
    public static String getCurrentUsername() {
        UserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getUsername() : null;
    }

    /**
     * Retorna o ID (UUID) do usuário autenticado.
     * Assume que o username é o email do usuário.
     *
     * @return UUID do usuário autenticado
     * @throws IllegalStateException se usuário não está autenticado
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Usuário não está autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUserId();
        }

        throw new IllegalStateException("Principal não é do tipo CustomUserDetails");
    }

    /**
     * Verifica se há um usuário autenticado.
     *
     * @return true se autenticado
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
