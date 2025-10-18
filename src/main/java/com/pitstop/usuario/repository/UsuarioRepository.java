package com.pitstop.usuario.repository;

import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Usuario entity with custom queries.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Find usuario by email (case-insensitive).
     *
     * @param email Email to search
     * @return Optional containing usuario if found
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmail(@Param("email") String email);

    /**
     * Find usuario by email (case-insensitive) - alternative method name.
     *
     * @param email Email to search
     * @return Optional containing usuario if found
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Check if email already exists (case-insensitive).
     *
     * @param email Email to check
     * @return true if exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Check if email already exists (case-insensitive) - alternative method name.
     *
     * @param email Email to check
     * @return true if exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    /**
     * Find all active usuarios ordered by nome.
     *
     * @return List of active usuarios
     */
    List<Usuario> findByAtivoTrueOrderByNomeAsc();

    /**
     * Find all active usuarios.
     *
     * @return List of active usuarios
     */
    List<Usuario> findByAtivoTrue();

    /**
     * Find usuarios by perfil.
     *
     * @param perfil Perfil to filter
     * @return List of usuarios
     */
    List<Usuario> findByPerfil(PerfilUsuario perfil);

    /**
     * Find usuarios by perfil and active status.
     *
     * @param perfil Perfil to filter
     * @return List of usuarios
     */
    List<Usuario> findByPerfilAndAtivoTrue(PerfilUsuario perfil);

    /**
     * Count active usuarios with ADMIN perfil.
     *
     * @return Count of active admins
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.perfil = 'ADMIN' AND u.ativo = TRUE")
    long countActiveAdmins();

    /**
     * Count active admins (alternative method name).
     *
     * @return Count of active admins
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.perfil = 'ADMIN' AND u.ativo = TRUE")
    long countAdminsAtivos();
}
