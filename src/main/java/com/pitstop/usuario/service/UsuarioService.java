package com.pitstop.usuario.service;

import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.dto.CreateUsuarioRequest;
import com.pitstop.usuario.dto.UpdateUsuarioRequest;
import com.pitstop.usuario.dto.UsuarioResponse;
import com.pitstop.usuario.exception.CannotDeleteLastAdminException;
import com.pitstop.usuario.exception.EmailAlreadyExistsException;
import com.pitstop.usuario.exception.UsuarioNotFoundException;
import com.pitstop.usuario.mapper.UsuarioMapper;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Service responsável pela lógica de negócio do módulo de Usuário.
 *
 * Implementa todas as regras de validação, segurança e consistência:
 * - Email único (case-insensitive)
 * - Criptografia de senhas com BCrypt
 * - Proteção do último admin ativo
 * - Cache de consultas frequentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria um novo usuário no sistema.
     *
     * Validações:
     * - Email único (case-insensitive)
     * - Senha criptografada com BCrypt
     * - Email normalizado para lowercase
     *
     * @param request Dados do novo usuário
     * @return DTO com dados do usuário criado
     * @throws EmailAlreadyExistsException se o email já estiver em uso
     */
    @Transactional
    @CacheEvict(value = "usuarios", allEntries = true)
    public UsuarioResponse create(CreateUsuarioRequest request) {
        log.debug("Criando novo usuário com email: {}", request.email());

        UUID oficinaId = TenantContext.getTenantId();

        // Validar email único (case-insensitive)
        // Email é globalmente único (não filtra por oficinaId)
        String emailNormalizado = StringUtils.hasText(request.email())
                ? request.email().trim().toLowerCase()
                : request.email();

        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            log.warn("Tentativa de criar usuário com email duplicado: {}", emailNormalizado);
            throw new EmailAlreadyExistsException(emailNormalizado);
        }

        // Converter DTO para entidade
        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setEmail(emailNormalizado);

        // Criptografar senha
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        // Persistir
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário criado com sucesso. ID: {}, Email: {}, Perfil: {}",
                usuarioSalvo.getId(), usuarioSalvo.getEmail(), usuarioSalvo.getPerfil());

        return usuarioMapper.toResponse(usuarioSalvo);
    }

    /**
     * Busca um usuário por ID.
     *
     * @param id ID do usuário
     * @return DTO com dados do usuário
     * @throws UsuarioNotFoundException se o usuário não existir
     */
    @Cacheable(value = "usuarios", key = "#id")
    public UsuarioResponse findById(UUID id) {
        log.debug("Buscando usuário por ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Usuario usuario = usuarioRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado com ID: {}", id);
                    return new UsuarioNotFoundException(id);
                });

        return usuarioMapper.toResponse(usuario);
    }

    /**
     * Busca um usuário por email (case-insensitive).
     *
     * @param email Email do usuário
     * @return DTO com dados do usuário
     * @throws UsuarioNotFoundException se o usuário não existir
     */
    @Cacheable(value = "usuarios", key = "#email.toLowerCase()")
    public UsuarioResponse findByEmail(String email) {
        log.debug("Buscando usuário por email: {}", email);

        String emailNormalizado = StringUtils.hasText(email)
                ? email.trim().toLowerCase()
                : email;

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado com email: {}", emailNormalizado);
                    return new UsuarioNotFoundException(emailNormalizado);
                });

        return usuarioMapper.toResponse(usuario);
    }

    /**
     * Lista todos os usuários com paginação.
     *
     * @param pageable Configuração de paginação e ordenação
     * @return Página de usuários
     */
    public Page<UsuarioResponse> findAll(Pageable pageable) {
        log.debug("Listando usuários com paginação: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        UUID oficinaId = TenantContext.getTenantId();
        return usuarioRepository.findByOficinaId(oficinaId, pageable)
                .map(usuarioMapper::toResponse);
    }

    /**
     * Lista usuários por perfil.
     *
     * @param perfil Perfil de acesso
     * @return Lista de usuários com o perfil especificado
     */
    // @Cacheable(value = "usuarios", key = "'perfil:' + #perfil.name()") // Temporariamente desabilitado
    public List<UsuarioResponse> findByPerfil(PerfilUsuario perfil) {
        log.debug("Buscando usuários com perfil: {}", perfil);

        UUID oficinaId = TenantContext.getTenantId();
        return usuarioRepository.findByOficinaIdAndPerfil(oficinaId, perfil).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    /**
     * Lista apenas usuários ativos.
     *
     * @return Lista de usuários ativos
     */
    @Cacheable(value = "usuarios", key = "'ativos'")
    public List<UsuarioResponse> findAllAtivos() {
        log.debug("Buscando todos os usuários ativos");

        UUID oficinaId = TenantContext.getTenantId();
        return usuarioRepository.findByOficinaIdAndAtivoTrue(oficinaId).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    /**
     * Atualiza os dados de um usuário existente.
     *
     * Apenas atualiza os campos não-nulos do request.
     *
     * Validações:
     * - Email único se estiver sendo alterado
     * - Senha criptografada se estiver sendo alterada
     * - Não permite desativar o último admin
     *
     * @param id ID do usuário a ser atualizado
     * @param request Dados para atualização
     * @return DTO com dados atualizados
     * @throws UsuarioNotFoundException se o usuário não existir
     * @throws EmailAlreadyExistsException se tentar usar email já existente
     * @throws CannotDeleteLastAdminException se tentar desativar o último admin
     */
    @Transactional
    @CacheEvict(value = "usuarios", allEntries = true)
    public UsuarioResponse update(UUID id, UpdateUsuarioRequest request) {
        log.debug("Atualizando usuário ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Usuario usuario = usuarioRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para atualização. ID: {}", id);
                    return new UsuarioNotFoundException(id);
                });

        // Validar email único se estiver sendo alterado
        if (StringUtils.hasText(request.email())) {
            String emailNormalizado = request.email().trim().toLowerCase();

            // Verificar se o email mudou e se já está em uso (email é globalmente único)
            if (!usuario.getEmail().equalsIgnoreCase(emailNormalizado)) {
                if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
                    log.warn("Tentativa de atualizar para email duplicado: {}", emailNormalizado);
                    throw new EmailAlreadyExistsException(emailNormalizado);
                }
                usuario.setEmail(emailNormalizado);
            }
        }

        // Criptografar nova senha se fornecida
        if (StringUtils.hasText(request.senha())) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        // Validar desativação do último admin
        if (request.ativo() != null && !request.ativo()) {
            if (usuario.getPerfil() == PerfilUsuario.ADMIN) {
                long adminsAtivos = usuarioRepository.countAdminsAtivos(oficinaId);
                if (adminsAtivos <= 1) {
                    log.warn("Tentativa de desativar o último administrador ativo. ID: {}", id);
                    throw new CannotDeleteLastAdminException();
                }
            }
        }

        // Aplicar outras atualizações (nome, perfil, ativo)
        usuarioMapper.updateEntityFromDto(request, usuario);

        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        log.info("Usuário atualizado com sucesso. ID: {}, Email: {}",
                usuarioAtualizado.getId(), usuarioAtualizado.getEmail());

        return usuarioMapper.toResponse(usuarioAtualizado);
    }

    /**
     * Desativa um usuário (soft delete).
     *
     * Não exclui fisicamente do banco, apenas marca como inativo.
     *
     * Validações:
     * - Não permite desativar o último admin ativo
     *
     * @param id ID do usuário a ser desativado
     * @throws UsuarioNotFoundException se o usuário não existir
     * @throws CannotDeleteLastAdminException se for o último admin ativo
     */
    @Transactional
    @CacheEvict(value = "usuarios", allEntries = true)
    public void delete(UUID id) {
        log.debug("Desativando usuário ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Usuario usuario = usuarioRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para desativação. ID: {}", id);
                    return new UsuarioNotFoundException(id);
                });

        // Validar se não é o último admin ativo
        if (usuario.getPerfil() == PerfilUsuario.ADMIN && usuario.getAtivo()) {
            long adminsAtivos = usuarioRepository.countAdminsAtivos(oficinaId);
            if (adminsAtivos <= 1) {
                log.warn("Tentativa de desativar o último administrador ativo. ID: {}", id);
                throw new CannotDeleteLastAdminException();
            }
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        log.info("Usuário desativado com sucesso. ID: {}, Email: {}",
                usuario.getId(), usuario.getEmail());
    }

    /**
     * Reativa um usuário previamente desativado.
     *
     * @param id ID do usuário a ser reativado
     * @throws UsuarioNotFoundException se o usuário não existir
     */
    @Transactional
    @CacheEvict(value = "usuarios", allEntries = true)
    public UsuarioResponse reactivate(UUID id) {
        log.debug("Reativando usuário ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Usuario usuario = usuarioRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para reativação. ID: {}", id);
                    return new UsuarioNotFoundException(id);
                });

        usuario.setAtivo(true);
        Usuario usuarioReativado = usuarioRepository.save(usuario);

        log.info("Usuário reativado com sucesso. ID: {}, Email: {}",
                usuarioReativado.getId(), usuarioReativado.getEmail());

        return usuarioMapper.toResponse(usuarioReativado);
    }
}
