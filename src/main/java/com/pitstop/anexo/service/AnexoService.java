package com.pitstop.anexo.service;

import com.pitstop.anexo.domain.Anexo;
import com.pitstop.anexo.domain.CategoriaAnexo;
import com.pitstop.anexo.domain.EntidadeTipo;
import com.pitstop.anexo.dto.AnexoPublicoResponse;
import com.pitstop.anexo.dto.AnexoResponse;
import com.pitstop.anexo.dto.AnexoUploadRequest;
import com.pitstop.anexo.dto.QuotaResponse;
import com.pitstop.anexo.exception.AnexoNotFoundException;
import com.pitstop.anexo.exception.InvalidFileTypeException;
import com.pitstop.anexo.exception.QuotaExceededException;
import com.pitstop.anexo.mapper.AnexoMapper;
import com.pitstop.anexo.repository.AnexoRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.shared.exception.ForbiddenException;
import com.pitstop.shared.security.CustomUserDetails;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Serviço de negócio para gerenciamento de anexos.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Upload de arquivos com validação de tipo e tamanho</li>
 *   <li>Controle de quota por oficina/plano</li>
 *   <li>Listagem de anexos por entidade</li>
 *   <li>Download de arquivos</li>
 *   <li>Soft delete de anexos</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnexoService {

    private final AnexoRepository anexoRepository;
    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final FileStorageService fileStorageService;
    private final AnexoMapper anexoMapper;

    /**
     * Tipos MIME permitidos para upload.
     */
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    /**
     * Tamanho máximo por arquivo (default 5MB).
     */
    @Value("${app.storage.max-file-size:5242880}")
    private long maxFileSize;

    /**
     * Quota por plano (em bytes).
     */
    @Value("${app.storage.quota.economico:524288000}") // 500MB
    private long quotaEconomico;

    @Value("${app.storage.quota.profissional:2147483648}") // 2GB
    private long quotaProfissional;

    @Value("${app.storage.quota.turbinado:10737418240}") // 10GB
    private long quotaTurbinado;

    /**
     * Faz upload de um arquivo.
     *
     * @param file Arquivo a ser enviado
     * @param request Metadados do anexo
     * @return Resposta com dados do anexo criado
     */
    @Transactional
    public AnexoResponse upload(MultipartFile file, AnexoUploadRequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        UUID usuarioId = getCurrentUsuarioId();

        log.info("Upload de anexo: oficina={}, entidade={}/{}, arquivo={}",
                oficinaId, request.entidadeTipo(), request.entidadeId(), file.getOriginalFilename());

        // 1. Carrega oficina e valida plano
        Oficina oficina = oficinaRepository.findById(oficinaId)
                .orElseThrow(() -> new ForbiddenException("Oficina não encontrada"));

        validarPlanoPermiteAnexos(oficina);

        // 2. Valida tipo de arquivo
        validarTipoArquivo(file);

        // 3. Valida tamanho do arquivo
        validarTamanhoArquivo(file);

        // 4. Valida quota
        validarQuota(oficina, file.getSize());

        // 5. Salva arquivo no storage
        String caminhoArquivo = fileStorageService.salvarArquivo(
                file, oficinaId, request.entidadeTipo()
        );

        // 6. Carrega usuário
        Usuario usuario = usuarioId != null ? usuarioRepository.findById(usuarioId).orElse(null) : null;

        // 7. Cria registro no banco
        Anexo anexo = Anexo.builder()
                .oficina(oficina)
                .entidadeTipo(request.entidadeTipo())
                .entidadeId(request.entidadeId())
                .categoria(request.categoria() != null ? request.categoria() : CategoriaAnexo.OUTROS)
                .nomeOriginal(sanitizarNomeArquivo(file.getOriginalFilename()))
                .nomeArquivo(caminhoArquivo.substring(caminhoArquivo.lastIndexOf('/') + 1))
                .tamanhoBytes(file.getSize())
                .mimeType(file.getContentType())
                .caminhoArquivo(caminhoArquivo)
                .descricao(request.descricao())
                .uploadedBy(usuario)
                .build();

        anexo = anexoRepository.save(anexo);

        log.info("Anexo criado: id={}, caminho={}", anexo.getId(), caminhoArquivo);

        return anexoMapper.toResponse(anexo);
    }

    /**
     * Lista anexos de uma entidade.
     */
    @Transactional(readOnly = true)
    public List<AnexoResponse> listarPorEntidade(EntidadeTipo entidadeTipo, UUID entidadeId) {
        UUID oficinaId = TenantContext.getTenantId();

        List<Anexo> anexos = anexoRepository.findByEntidade(oficinaId, entidadeTipo, entidadeId);

        return anexos.stream()
                .map(anexoMapper::toResponse)
                .toList();
    }

    /**
     * Busca um anexo por ID.
     */
    @Transactional(readOnly = true)
    public AnexoResponse buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Anexo anexo = anexoRepository.findByIdAndOficinaIdAndAtivoTrue(id, oficinaId)
                .orElseThrow(() -> new AnexoNotFoundException(id));

        return anexoMapper.toResponse(anexo);
    }

    /**
     * Faz download de um arquivo.
     */
    @Transactional(readOnly = true)
    public Resource download(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Anexo anexo = anexoRepository.findByIdAndOficinaIdAndAtivoTrue(id, oficinaId)
                .orElseThrow(() -> new AnexoNotFoundException(id));

        return fileStorageService.carregarArquivo(anexo.getCaminhoArquivo());
    }

    /**
     * Retorna o nome original do arquivo para download.
     */
    @Transactional(readOnly = true)
    public String getNomeOriginal(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Anexo anexo = anexoRepository.findByIdAndOficinaIdAndAtivoTrue(id, oficinaId)
                .orElseThrow(() -> new AnexoNotFoundException(id));

        return anexo.getNomeOriginal();
    }

    /**
     * Retorna o tipo MIME do arquivo.
     */
    @Transactional(readOnly = true)
    public String getMimeType(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Anexo anexo = anexoRepository.findByIdAndOficinaIdAndAtivoTrue(id, oficinaId)
                .orElseThrow(() -> new AnexoNotFoundException(id));

        return anexo.getMimeType();
    }

    /**
     * Deleta um anexo (soft delete).
     */
    @Transactional
    public void deletar(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Anexo anexo = anexoRepository.findByIdAndOficinaIdAndAtivoTrue(id, oficinaId)
                .orElseThrow(() -> new AnexoNotFoundException(id));

        // Soft delete no banco
        anexo.desativar();
        anexoRepository.save(anexo);

        // Deleta arquivo físico
        fileStorageService.deletarArquivo(anexo.getCaminhoArquivo());

        log.info("Anexo deletado: id={}", id);
    }

    /**
     * Retorna informações de quota da oficina atual.
     */
    @Transactional(readOnly = true)
    public QuotaResponse getQuota() {
        UUID oficinaId = TenantContext.getTenantId();

        Oficina oficina = oficinaRepository.findById(oficinaId)
                .orElseThrow(() -> new ForbiddenException("Oficina não encontrada"));

        long usado = anexoRepository.calcularUsoPorOficina(oficinaId);
        long limite = getQuotaPorPlano(oficina.getPlano());
        long totalAnexos = anexoRepository.countByOficinaId(oficinaId);

        return QuotaResponse.of(usado, limite, totalAnexos);
    }

    /**
     * Conta anexos de uma entidade.
     */
    @Transactional(readOnly = true)
    public long contarPorEntidade(EntidadeTipo entidadeTipo, UUID entidadeId) {
        UUID oficinaId = TenantContext.getTenantId();
        return anexoRepository.countByEntidade(oficinaId, entidadeTipo, entidadeId);
    }

    /**
     * Altera a visibilidade de um anexo para o cliente.
     *
     * @param id ID do anexo
     * @param visivelParaCliente Nova visibilidade
     * @return Anexo atualizado
     */
    @Transactional
    public AnexoResponse alterarVisibilidade(UUID id, Boolean visivelParaCliente) {
        UUID oficinaId = TenantContext.getTenantId();

        Anexo anexo = anexoRepository.findByIdAndOficinaIdAndAtivoTrue(id, oficinaId)
                .orElseThrow(() -> new AnexoNotFoundException(id));

        anexo.setVisivelParaCliente(visivelParaCliente);
        anexo = anexoRepository.save(anexo);

        log.info("Visibilidade do anexo {} alterada para: {}", id, visivelParaCliente);

        return anexoMapper.toResponse(anexo);
    }

    /**
     * Lista anexos visíveis para cliente de uma ordem de serviço.
     * Método público - não requer autenticação.
     *
     * @param oficinaId ID da oficina
     * @param entidadeId ID da ordem de serviço
     * @return Lista de anexos visíveis
     */
    @Transactional(readOnly = true)
    public List<AnexoPublicoResponse> listarVisiveisParaCliente(UUID oficinaId, UUID entidadeId) {
        List<Anexo> anexos = anexoRepository.findVisiveisParaCliente(
                oficinaId, EntidadeTipo.ORDEM_SERVICO, entidadeId
        );

        return anexos.stream()
                .map(anexoMapper::toPublicoResponse)
                .toList();
    }

    /**
     * Busca anexo para visualização pública.
     * Valida se o anexo pertence à OS e está marcado como visível.
     *
     * @param oficinaId ID da oficina
     * @param osId ID da ordem de serviço
     * @param anexoId ID do anexo
     * @return Resource do arquivo
     */
    @Transactional(readOnly = true)
    public Resource downloadPublico(UUID oficinaId, UUID osId, UUID anexoId) {
        Anexo anexo = anexoRepository.findVisivelParaCliente(
                anexoId, oficinaId, EntidadeTipo.ORDEM_SERVICO, osId
        ).orElseThrow(() -> new AnexoNotFoundException(anexoId));

        return fileStorageService.carregarArquivo(anexo.getCaminhoArquivo());
    }

    /**
     * Retorna o tipo MIME de um anexo público.
     */
    @Transactional(readOnly = true)
    public String getMimeTypePublico(UUID oficinaId, UUID osId, UUID anexoId) {
        Anexo anexo = anexoRepository.findVisivelParaCliente(
                anexoId, oficinaId, EntidadeTipo.ORDEM_SERVICO, osId
        ).orElseThrow(() -> new AnexoNotFoundException(anexoId));

        return anexo.getMimeType();
    }

    // ==================== Métodos privados ====================

    /**
     * Obtém o ID do usuário atual do SecurityContext.
     */
    private UUID getCurrentUsuarioId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getUserId();
            }
        } catch (Exception e) {
            log.warn("Não foi possível obter o ID do usuário atual", e);
        }
        return null;
    }

    /**
     * Valida se o plano da oficina permite anexos.
     */
    private void validarPlanoPermiteAnexos(Oficina oficina) {
        if (!oficina.getPlano().isAnexoImagensDocumentos()) {
            throw new ForbiddenException(
                    "Seu plano (" + oficina.getPlano().getNome() + ") não inclui anexo de imagens/documentos. " +
                    "Faça upgrade para o plano Turbinado para ter acesso a esta funcionalidade."
            );
        }
    }

    /**
     * Valida se o tipo de arquivo é permitido.
     */
    private void validarTipoArquivo(MultipartFile file) {
        String mimeType = file.getContentType();
        if (mimeType == null || !TIPOS_PERMITIDOS.contains(mimeType.toLowerCase())) {
            throw new InvalidFileTypeException(mimeType);
        }
    }

    /**
     * Valida se o tamanho do arquivo está dentro do limite.
     */
    private void validarTamanhoArquivo(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new QuotaExceededException(0, maxFileSize, file.getSize());
        }
    }

    /**
     * Valida se a oficina tem quota disponível.
     */
    private void validarQuota(Oficina oficina, long tamanhoArquivo) {
        long usado = anexoRepository.calcularUsoPorOficina(oficina.getId());
        long limite = getQuotaPorPlano(oficina.getPlano());

        if (usado + tamanhoArquivo > limite) {
            throw new QuotaExceededException(usado, limite, tamanhoArquivo);
        }
    }

    /**
     * Retorna a quota de storage por plano.
     */
    private long getQuotaPorPlano(PlanoAssinatura plano) {
        return switch (plano) {
            case ECONOMICO -> quotaEconomico;
            case PROFISSIONAL -> quotaProfissional;
            case TURBINADO -> quotaTurbinado;
        };
    }

    /**
     * Sanitiza o nome do arquivo removendo caracteres especiais.
     */
    private String sanitizarNomeArquivo(String nome) {
        if (nome == null) return "arquivo";
        // Remove caracteres perigosos, mantém letras, números, pontos, hífens e underscores
        return nome.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
