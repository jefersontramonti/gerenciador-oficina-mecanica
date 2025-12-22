package com.pitstop.usuario.repository;

import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Usuario entity with custom queries.
 *
 * <p><strong>Multi-tenancy:</strong> A maioria dos métodos exigem {@code oficinaId} como
 * primeiro parâmetro para garantir isolamento de dados entre oficinas.</p>
 *
 * <p><strong>EXCEÇÃO:</strong> {@code findByEmail} e {@code existsByEmail} NÃO filtram por
 * oficinaId porque o email é GLOBALMENTE único (um email = uma conta).</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-01-01
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    /**
     * Find usuario by email (case-insensitive).
     * GLOBALMENTE único - NÃO filtra por oficinaId.
     *
     * @param email Email to search
     * @return Optional containing usuario if found
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmail(@Param("email") String email);

    /**
     * Find usuario by email (case-insensitive) - alternative method name.
     * GLOBALMENTE único - NÃO filtra por oficinaId.
     *
     * @param email Email to search
     * @return Optional containing usuario if found
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Check if email already exists (case-insensitive).
     * GLOBALMENTE único - NÃO filtra por oficinaId.
     *
     * @param email Email to check
     * @return true if exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Check if email already exists (case-insensitive) - alternative method name.
     * GLOBALMENTE único - NÃO filtra por oficinaId.
     *
     * @param email Email to check
     * @return true if exists
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    /**
     * Find all active usuarios ordered by nome in a specific oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return List of active usuarios
     */
    @Query("SELECT u FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.ativo = true ORDER BY u.nome ASC")
    List<Usuario> findByOficinaIdAndAtivoTrueOrderByNomeAsc(@Param("oficinaId") UUID oficinaId);

    /**
     * Find all active usuarios in a specific oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return List of active usuarios
     */
    @Query("SELECT u FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.ativo = true")
    List<Usuario> findByOficinaIdAndAtivoTrue(@Param("oficinaId") UUID oficinaId);

    /**
     * Find usuarios by perfil in a specific oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param perfil Perfil to filter
     * @return List of usuarios
     */
    @Query("SELECT u FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.perfil = :perfil")
    List<Usuario> findByOficinaIdAndPerfil(@Param("oficinaId") UUID oficinaId, @Param("perfil") PerfilUsuario perfil);

    /**
     * Find usuarios by perfil and active status in a specific oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param perfil Perfil to filter
     * @return List of usuarios
     */
    @Query("SELECT u FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.perfil = :perfil AND u.ativo = true")
    List<Usuario> findByOficinaIdAndPerfilAndAtivoTrue(@Param("oficinaId") UUID oficinaId, @Param("perfil") PerfilUsuario perfil);

    /**
     * Count active usuarios with ADMIN perfil in a specific oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return Count of active admins
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.perfil = 'ADMIN' AND u.ativo = TRUE")
    long countActiveAdminsByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Count active admins (alternative method name) in a specific oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return Count of active admins
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.perfil = 'ADMIN' AND u.ativo = TRUE")
    long countAdminsAtivosByOficinaId(@Param("oficinaId") UUID oficinaId);

    /**
     * Busca usuário por ID em uma oficina.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param id ID do usuário
     * @return Optional contendo o usuário se encontrado
     */
    @Query("SELECT u FROM Usuario u WHERE u.oficina.id = :oficinaId AND u.id = :id")
    Optional<Usuario> findByOficinaIdAndId(@Param("oficinaId") UUID oficinaId, @Param("id") UUID id);

    /**
     * Alias para countAdminsAtivosByOficinaId (compatibilidade).
     *
     * @param oficinaId ID da oficina (tenant)
     * @return Count of active admins
     */
    default long countAdminsAtivos(UUID oficinaId) {
        return countAdminsAtivosByOficinaId(oficinaId);
    }

    /**
     * Busca todos os usuários de uma oficina com paginação.
     *
     * @param oficinaId ID da oficina (tenant)
     * @param pageable paginação
     * @return página de usuários
     */
    @Query("SELECT u FROM Usuario u WHERE u.oficina.id = :oficinaId")
    Page<Usuario> findByOficinaId(@Param("oficinaId") UUID oficinaId, Pageable pageable);
}
