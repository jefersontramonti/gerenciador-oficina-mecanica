package com.pitstop.saas.controller;

import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saas/super-admins")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminController {

    private final UsuarioRepository usuarioRepository;

    /**
     * Lista todos os usuários com perfil SUPER_ADMIN
     */
    @GetMapping
    public ResponseEntity<List<SuperAdminDTO>> listarSuperAdmins() {
        log.info("Listando super admins");

        List<SuperAdminDTO> superAdmins = usuarioRepository.findByPerfil(PerfilUsuario.SUPER_ADMIN)
            .stream()
            .map(u -> new SuperAdminDTO(u.getId(), u.getNome(), u.getEmail()))
            .toList();

        return ResponseEntity.ok(superAdmins);
    }

    public record SuperAdminDTO(UUID id, String nome, String email) {}
}
