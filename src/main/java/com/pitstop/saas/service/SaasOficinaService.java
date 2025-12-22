package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.SaasPagamentoRepository;
import com.pitstop.shared.audit.service.AuditService;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for managing workshops in the SaaS platform.
 *
 * Handles workshop lifecycle including creation with trial period,
 * status transitions, and administrative operations.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaasOficinaService {

    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SaasPagamentoRepository pagamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;

    private static final int TRIAL_DAYS = 30;

    /**
     * Creates a new workshop with automatic trial period.
     *
     * Sets up the workshop with TRIAL status, creates initial admin user,
     * and configures 30-day trial period.
     *
     * @param request creation request with workshop and admin details
     * @return created workshop details
     */
    @Transactional
    public OficinaDetailResponse createOficina(CreateOficinaRequest request) {
        log.info("Creating new workshop: {}", request.nomeFantasia());

        // Validate CNPJ uniqueness
        if (oficinaRepository.existsByCnpj(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado no sistema");
        }

        // Validate admin email uniqueness
        if (usuarioRepository.existsByEmail(request.emailAdmin())) {
            throw new IllegalArgumentException("Email do administrador já está em uso");
        }

        // Create workshop with trial period
        // TODO: Add trial fields to Oficina entity (dataInicioTrial, dataFimTrial)
        LocalDate hoje = LocalDate.now();

        // Format phone number: 11987654321 -> (11) 98765-4321
        String telefoneFormatado = formatarTelefone(request.telefone());

        // Format CEP: 01234567 -> 01234-567
        String cepFormatado = formatarCep(request.cep());

        // Create Contato object
        com.pitstop.oficina.domain.Contato contato = com.pitstop.oficina.domain.Contato.builder()
            .email(request.email())
            .telefoneCelular(telefoneFormatado)
            .build();

        // Create Endereco object
        com.pitstop.cliente.domain.Endereco endereco = com.pitstop.cliente.domain.Endereco.builder()
            .cep(cepFormatado)
            .logradouro(request.logradouro())
            .numero(request.numero())
            .complemento(request.complemento())
            .bairro(request.bairro())
            .cidade(request.cidade())
            .estado(request.estado())
            .build();

        Oficina oficina = Oficina.builder()
            .razaoSocial(request.razaoSocial())
            .nomeFantasia(request.nomeFantasia())
            .cnpjCpf(request.cnpj())
            .tipoPessoa(com.pitstop.oficina.domain.TipoPessoa.PESSOA_JURIDICA)
            .nomeResponsavel(request.nomeAdmin())
            .contato(contato)
            .endereco(endereco)
            .status(StatusOficina.TRIAL)
            .plano(request.plano())
            .dataAssinatura(hoje)
            .dataVencimentoPlano(hoje.plusDays(TRIAL_DAYS).withDayOfMonth(10))
            .valorMensalidade(request.plano().getValorMensal())
            .ativo(true)
            .build();

        oficina = oficinaRepository.save(oficina);
        log.info("Workshop created with ID: {} (TRIAL until {})", oficina.getId(), oficina.getDataVencimentoPlano());

        // Create admin user
        Usuario admin = Usuario.builder()
            .oficina(oficina)
            .nome(request.nomeAdmin())
            .email(request.emailAdmin())
            .senha(passwordEncoder.encode(request.senhaAdmin()))
            .perfil(PerfilUsuario.ADMIN)
            .ativo(true)
            .build();

        usuarioRepository.save(admin);
        log.info("Admin user created for workshop: {}", admin.getEmail());

        // Audit log
        auditService.log(
            "CREATE_OFICINA",
            "Oficina",
            oficina.getId(),
            String.format("Created workshop: %s (CNPJ: %s, Plan: %s)",
                oficina.getNomeFantasia(), oficina.getCnpjCpf(), oficina.getPlano())
        );

        return buildDetailResponse(oficina);
    }

    /**
     * Updates workshop information.
     *
     * CNPJ and status cannot be changed through this method.
     *
     * @param id workshop identifier
     * @param request update request
     * @return updated workshop details
     */
    @Transactional
    public OficinaDetailResponse updateOficina(UUID id, UpdateOficinaRequest request) {
        log.info("Updating workshop: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        // TODO: Implement proper field updates when Oficina entity is adjusted for SaaS
        // Update allowed fields
        oficina.setRazaoSocial(request.razaoSocial());
        oficina.setNomeFantasia(request.nomeFantasia());
        oficina.setPlano(request.plano());
        oficina.setValorMensalidade(request.plano().getValorMensal());

        oficina = oficinaRepository.save(oficina);

        // Audit log
        auditService.log(
            "UPDATE_OFICINA",
            "Oficina",
            oficina.getId(),
            String.format("Updated workshop: %s (Plan changed to: %s)",
                oficina.getNomeFantasia(), oficina.getPlano())
        );

        return buildDetailResponse(oficina);
    }

    /**
     * Gets detailed information about a workshop.
     *
     * @param id workshop identifier
     * @return complete workshop details
     */
    @Transactional(readOnly = true)
    public OficinaDetailResponse getOficinaDetail(UUID id) {
        log.debug("Fetching workshop details: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        return buildDetailResponse(oficina);
    }

    /**
     * Activates a workshop (changes status to ATIVA).
     *
     * Usually triggered after first payment confirmation.
     *
     * @param id workshop identifier
     * @return updated workshop details
     */
    @Transactional
    public OficinaDetailResponse activateOficina(UUID id) {
        log.info("Activating workshop: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        if (oficina.getStatus() == StatusOficina.ATIVA) {
            throw new IllegalStateException("Oficina já está ativa");
        }

        oficina.setStatus(StatusOficina.ATIVA);
        oficina.setAtivo(true);
        oficina = oficinaRepository.save(oficina);

        auditService.log(
            "ACTIVATE_OFICINA",
            "Oficina",
            oficina.getId(),
            String.format("Activated workshop: %s", oficina.getNomeFantasia())
        );

        return buildDetailResponse(oficina);
    }

    /**
     * Suspends a workshop (changes status to SUSPENSA).
     *
     * Usually triggered by payment overdue or manual intervention.
     * Users cannot access the system when suspended.
     *
     * @param id workshop identifier
     * @return updated workshop details
     */
    @Transactional
    public OficinaDetailResponse suspendOficina(UUID id) {
        log.warn("Suspending workshop: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        if (oficina.getStatus() == StatusOficina.CANCELADA) {
            throw new IllegalStateException("Não é possível suspender oficina cancelada");
        }

        oficina.setStatus(StatusOficina.SUSPENSA);
        oficina.setAtivo(false);
        oficina = oficinaRepository.save(oficina);

        auditService.log(
            "SUSPEND_OFICINA",
            "Oficina",
            oficina.getId(),
            String.format("Suspended workshop: %s", oficina.getNomeFantasia())
        );

        return buildDetailResponse(oficina);
    }

    /**
     * Cancels a workshop subscription (changes status to CANCELADA).
     *
     * Data is preserved (soft delete) but access is permanently revoked.
     * Workshop cannot be reactivated after cancellation.
     *
     * @param id workshop identifier
     * @return updated workshop details
     */
    @Transactional
    public OficinaDetailResponse cancelOficina(UUID id) {
        log.warn("Cancelling workshop: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        if (oficina.getStatus() == StatusOficina.CANCELADA) {
            throw new IllegalStateException("Oficina já está cancelada");
        }

        oficina.setStatus(StatusOficina.CANCELADA);
        oficina.setAtivo(false);
        oficina = oficinaRepository.save(oficina);

        auditService.log(
            "CANCEL_OFICINA",
            "Oficina",
            oficina.getId(),
            String.format("Cancelled workshop: %s", oficina.getNomeFantasia())
        );

        return buildDetailResponse(oficina);
    }

    /**
     * Builds a complete detail response for a workshop.
     *
     * Aggregates data from multiple tables for comprehensive view.
     * TODO: Add trial fields and address embeddable support
     */
    private OficinaDetailResponse buildDetailResponse(Oficina oficina) {
        UUID id = oficina.getId();

        // Calculate trial days remaining
        // TODO: Use dataFimTrial when field is added to Oficina
        Integer diasRestantes = null;
        if (oficina.getStatus() == StatusOficina.TRIAL && oficina.getDataVencimentoPlano() != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), oficina.getDataVencimentoPlano());
            diasRestantes = (int) Math.max(0, dias);
        }

        // Get usage statistics
        Long totalUsuarios = countByOficina("usuarios", id);
        Long totalClientes = countByOficina("clientes", id);
        Long totalVeiculos = countByOficina("veiculos", id);
        Long totalOrdensServico = countByOficina("ordem_servico", id);
        Long totalPecas = countByOficina("pecas", id);

        // Get financial statistics
        BigDecimal totalFaturamento = getTotalFaturamento(id);
        long pagamentosRealizados = pagamentoRepository.countByOficinaId(id);

        LocalDate ultimoPagamento = pagamentoRepository.findMostRecentByOficinaId(id)
            .map(p -> p.getDataPagamento())
            .orElse(null);

        // Count pending payments (TODO: implement proper logic when payment tracking is complete)
        int pagamentosPendentes = 0;

        return new OficinaDetailResponse(
            oficina.getId(),
            oficina.getRazaoSocial(),
            oficina.getNomeFantasia(),
            oficina.getCnpjCpf(),
            oficina.getContato() != null ? oficina.getContato().getEmail() : null,
            oficina.getContato() != null ? oficina.getContato().getTelefoneCelular() : null,
            oficina.getStatus(),
            oficina.getPlano(),
            oficina.getValorMensalidade(),
            oficina.getDataAssinatura(), // dataInicioTrial
            oficina.getDataVencimentoPlano(), // dataFimTrial
            diasRestantes,
            oficina.getDataVencimentoPlano(),
            oficina.getAtivo(),
            oficina.getEndereco() != null ? oficina.getEndereco().getCep() : null,
            oficina.getEndereco() != null ? oficina.getEndereco().getLogradouro() : null,
            oficina.getEndereco() != null ? oficina.getEndereco().getNumero() : null,
            oficina.getEndereco() != null ? oficina.getEndereco().getComplemento() : null,
            oficina.getEndereco() != null ? oficina.getEndereco().getBairro() : null,
            oficina.getEndereco() != null ? oficina.getEndereco().getCidade() : null,
            oficina.getEndereco() != null ? oficina.getEndereco().getEstado() : null,
            totalUsuarios,
            totalClientes,
            totalVeiculos,
            totalOrdensServico,
            totalPecas,
            totalFaturamento,
            (int) pagamentosRealizados,
            pagamentosPendentes,
            ultimoPagamento,
            oficina.getCreatedAt(),
            oficina.getUpdatedAt()
        );
    }

    private Long countByOficina(String tableName, UUID oficinaId) {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE oficina_id = ?", tableName);
        return jdbcTemplate.queryForObject(sql, Long.class, oficinaId);
    }

    private BigDecimal getTotalFaturamento(UUID oficinaId) {
        String sql = """
            SELECT COALESCE(SUM(valor_final), 0)
            FROM ordem_servico
            WHERE oficina_id = ?
            AND status IN ('FINALIZADO', 'ENTREGUE')
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, oficinaId);
    }

    /**
     * Formats phone number from 11987654321 to (11) 98765-4321.
     */
    private String formatarTelefone(String telefone) {
        if (telefone == null || telefone.length() != 11) {
            throw new IllegalArgumentException("Telefone deve ter 11 dígitos");
        }
        // 11987654321 -> (11) 98765-4321
        return String.format("(%s) %s-%s",
            telefone.substring(0, 2),
            telefone.substring(2, 7),
            telefone.substring(7)
        );
    }

    /**
     * Formats CEP from 01234567 to 01234-567.
     */
    private String formatarCep(String cep) {
        if (cep == null || cep.length() != 8) {
            throw new IllegalArgumentException("CEP deve ter 8 dígitos");
        }
        // 01234567 -> 01234-567
        return String.format("%s-%s",
            cep.substring(0, 5),
            cep.substring(5)
        );
    }
}
