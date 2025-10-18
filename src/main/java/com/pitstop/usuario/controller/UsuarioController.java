package com.pitstop.usuario.controller;

import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.dto.CreateUsuarioRequest;
import com.pitstop.usuario.dto.UpdateUsuarioRequest;
import com.pitstop.usuario.dto.UsuarioResponse;
import com.pitstop.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para gerenciamento de usuários do sistema.
 *
 * Endpoints com controle de acesso baseado em perfis (RBAC):
 * - ADMIN: Acesso completo (criar, editar, visualizar, desativar usuários)
 * - GERENTE: Pode visualizar usuários
 * - ATENDENTE/MECANICO: Sem acesso
 *
 * Base path: /api/usuarios
 */
@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gerenciamento de usuários do sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Cria um novo usuário no sistema.
     *
     * Apenas ADMIN pode criar usuários.
     *
     * @param request Dados do novo usuário
     * @return Usuário criado com status 201
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Criar novo usuário", description = "Cria um novo usuário no sistema. Apenas administradores podem criar usuários.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já existente"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para criar usuários")
    })
    public ResponseEntity<UsuarioResponse> create(
            @Valid @RequestBody CreateUsuarioRequest request
    ) {
        log.info("Requisição para criar usuário: email={}, perfil={}", request.email(), request.perfil());
        UsuarioResponse response = usuarioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca um usuário por ID.
     *
     * ADMIN e GERENTE podem visualizar qualquer usuário.
     *
     * @param id ID do usuário
     * @return Dados do usuário
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados de um usuário específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para visualizar usuários")
    })
    public ResponseEntity<UsuarioResponse> findById(
            @Parameter(description = "ID do usuário") @PathVariable UUID id
    ) {
        log.debug("Requisição para buscar usuário por ID: {}", id);
        UsuarioResponse response = usuarioService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca um usuário por email.
     *
     * ADMIN e GERENTE podem buscar por email.
     *
     * @param email Email do usuário
     * @return Dados do usuário
     */
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar usuário por email", description = "Retorna os dados de um usuário pelo email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<UsuarioResponse> findByEmail(
            @Parameter(description = "Email do usuário") @PathVariable String email
    ) {
        log.debug("Requisição para buscar usuário por email: {}", email);
        UsuarioResponse response = usuarioService.findByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos os usuários com paginação.
     *
     * ADMIN e GERENTE podem listar usuários.
     *
     * @param pageable Configuração de paginação (page, size, sort)
     * @return Página de usuários
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar todos os usuários", description = "Retorna lista paginada de usuários")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<Page<UsuarioResponse>> findAll(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable
    ) {
        log.debug("Requisição para listar usuários: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        Page<UsuarioResponse> response = usuarioService.findAll(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista usuários por perfil.
     *
     * ADMIN e GERENTE podem listar por perfil.
     *
     * @param perfil Perfil de acesso (ADMIN, GERENTE, ATENDENTE, MECANICO)
     * @return Lista de usuários com o perfil especificado
     */
    @GetMapping("/perfil/{perfil}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar usuários por perfil", description = "Retorna lista de usuários com perfil específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Perfil inválido"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<List<UsuarioResponse>> findByPerfil(
            @Parameter(description = "Perfil de acesso") @PathVariable PerfilUsuario perfil
    ) {
        log.debug("Requisição para listar usuários por perfil: {}", perfil);
        List<UsuarioResponse> response = usuarioService.findByPerfil(perfil);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista apenas usuários ativos.
     *
     * ADMIN e GERENTE podem listar usuários ativos.
     *
     * @return Lista de usuários ativos
     */
    @GetMapping("/ativos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar usuários ativos", description = "Retorna lista de usuários ativos no sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<List<UsuarioResponse>> findAllAtivos() {
        log.debug("Requisição para listar usuários ativos");
        List<UsuarioResponse> response = usuarioService.findAllAtivos();
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza os dados de um usuário existente.
     *
     * Apenas ADMIN pode atualizar usuários.
     * Apenas campos não-nulos do request serão atualizados.
     *
     * @param id ID do usuário a ser atualizado
     * @param request Dados para atualização
     * @return Usuário atualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente. Apenas campos fornecidos serão atualizados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já existente"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<UsuarioResponse> update(
            @Parameter(description = "ID do usuário") @PathVariable UUID id,
            @Valid @RequestBody UpdateUsuarioRequest request
    ) {
        log.info("Requisição para atualizar usuário ID: {}", id);
        UsuarioResponse response = usuarioService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Desativa um usuário (soft delete).
     *
     * Apenas ADMIN pode desativar usuários.
     * Não permite desativar o último admin ativo.
     *
     * @param id ID do usuário a ser desativado
     * @return Status 204 (sem conteúdo)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Desativar usuário", description = "Desativa um usuário do sistema (soft delete). Não permite desativar o último admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível desativar o último admin"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do usuário") @PathVariable UUID id
    ) {
        log.info("Requisição para desativar usuário ID: {}", id);
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um usuário previamente desativado.
     *
     * Apenas ADMIN pode reativar usuários.
     *
     * @param id ID do usuário a ser reativado
     * @return Usuário reativado
     */
    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Reativar usuário", description = "Reativa um usuário previamente desativado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário reativado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    public ResponseEntity<UsuarioResponse> reactivate(
            @Parameter(description = "ID do usuário") @PathVariable UUID id
    ) {
        log.info("Requisição para reativar usuário ID: {}", id);
        UsuarioResponse response = usuarioService.reactivate(id);
        return ResponseEntity.ok(response);
    }
}
