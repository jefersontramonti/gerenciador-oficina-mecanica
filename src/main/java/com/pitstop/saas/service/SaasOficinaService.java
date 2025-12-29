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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
    private final EntityManager entityManager;

    private static final int TRIAL_DAYS = 30;

    /**
     * Lists all workshops with pagination and optional filters.
     *
     * @param status filter by status (optional)
     * @param plano filter by plan (optional)
     * @param nome filter by name (partial match, optional)
     * @param pageable pagination configuration
     * @return page of workshop summaries
     */
    @Transactional(readOnly = true)
    public Page<OficinaResumoDTO> findAll(
        StatusOficina status,
        com.pitstop.oficina.domain.PlanoAssinatura plano,
        String nome,
        Pageable pageable
    ) {
        log.debug("Fetching workshops with filters - status: {}, plano: {}, nome: {}", status, plano, nome);

        Page<Oficina> oficinas;

        // Use repository method with filters if available, otherwise filter in memory
        if (status != null || plano != null || (nome != null && !nome.isEmpty())) {
            // Convert enums to strings for native query
            String statusStr = status != null ? status.name() : null;
            String planoStr = plano != null ? plano.name() : null;
            // Convert pageable sort to snake_case for native query
            Pageable nativePageable = convertToNativePageable(pageable);
            oficinas = oficinaRepository.findWithFiltersNative(statusStr, planoStr, nome, null, nativePageable);
        } else {
            oficinas = oficinaRepository.findAll(pageable);
        }

        return oficinas.map(this::toResumoDTO);
    }

    /**
     * Converts a Pageable with camelCase sort properties to snake_case for native queries.
     */
    private Pageable convertToNativePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "created_at"));
        }

        Sort.Order[] orders = pageable.getSort().stream()
            .map(order -> new Sort.Order(order.getDirection(), toSnakeCase(order.getProperty())))
            .toArray(Sort.Order[]::new);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    /**
     * Converts camelCase to snake_case.
     */
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Converts Oficina entity to OficinaResumoDTO.
     */
    private OficinaResumoDTO toResumoDTO(Oficina oficina) {
        UUID id = oficina.getId();

        // Calculate trial days remaining
        Integer diasRestantes = null;
        if (oficina.getStatus() == StatusOficina.TRIAL && oficina.getDataVencimentoPlano() != null) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), oficina.getDataVencimentoPlano());
            diasRestantes = (int) Math.max(0, dias);
        }

        // Get counts (simplified for list view)
        Long totalUsuarios = countByOficina("usuarios", id);
        Long totalOrdensServico = countByOficina("ordem_servico", id);
        Long totalClientes = countByOficina("clientes", id);

        return new OficinaResumoDTO(
            oficina.getId(),
            oficina.getNomeFantasia(),
            oficina.getCnpjCpf(),
            oficina.getStatus(),
            oficina.getPlano(),
            oficina.getValorMensalidade(),
            oficina.getDataVencimentoPlano(),
            diasRestantes,
            totalUsuarios,
            totalOrdensServico,
            totalClientes,
            oficina.getCreatedAt()
        );
    }

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
     * Uses native SQL to update specific fields to avoid triggering entity validation
     * on legacy data that may have invalid format (phone, CEP, etc).
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

        // Use native SQL to update all fields without triggering full entity validation
        String sql = """
            UPDATE oficinas
            SET razao_social = ?,
                nome_fantasia = ?,
                email = ?,
                telefone_celular = ?,
                plano = ?,
                valor_mensalidade = ?,
                endereco_cep = ?,
                endereco_logradouro = ?,
                endereco_numero = ?,
                endereco_complemento = ?,
                endereco_bairro = ?,
                endereco_cidade = ?,
                endereco_estado = ?,
                updated_at = ?
            WHERE id = ?
            """;
        jdbcTemplate.update(sql,
            request.razaoSocial(),
            request.nomeFantasia(),
            request.email(),
            request.telefone(),
            request.plano(),
            request.valorMensalidade(),
            request.cep(),
            request.logradouro(),
            request.numero(),
            request.complemento(),
            request.bairro(),
            request.cidade(),
            request.estado(),
            LocalDateTime.now(),
            id
        );

        // Audit log
        auditService.log(
            "UPDATE_OFICINA",
            "Oficina",
            id,
            String.format("Updated workshop: %s (Plan: %s, Value: R$ %.2f)",
                request.nomeFantasia(), request.plano(), request.valorMensalidade())
        );

        // Reload entity to get updated state (clear cache first to get fresh data)
        return buildDetailResponse(reloadOficina(id));
    }

    /**
     * Clears the persistence context and reloads an Oficina entity.
     * This ensures we get fresh data after native SQL updates.
     */
    private Oficina reloadOficina(UUID id) {
        entityManager.clear();
        return oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));
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
     * Uses native SQL to update status to avoid triggering entity validation
     * on legacy data that may have invalid format (phone, CEP, etc).
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

        // Use native SQL to avoid triggering entity validation on legacy data
        String sql = "UPDATE oficinas SET status = ?, ativo = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, StatusOficina.ATIVA.name(), true, LocalDateTime.now(), id);

        auditService.log(
            "ACTIVATE_OFICINA",
            "Oficina",
            id,
            String.format("Activated workshop: %s", oficina.getNomeFantasia())
        );

        // Reload entity to get updated state (clear cache first to get fresh data)
        return buildDetailResponse(reloadOficina(id));
    }

    /**
     * Suspends a workshop (changes status to SUSPENSA).
     *
     * Usually triggered by payment overdue or manual intervention.
     * Users cannot access the system when suspended.
     *
     * Uses native SQL to update status to avoid triggering entity validation
     * on legacy data that may have invalid format (phone, CEP, etc).
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

        // Use native SQL to avoid triggering entity validation on legacy data
        String sql = "UPDATE oficinas SET status = ?, ativo = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, StatusOficina.SUSPENSA.name(), false, LocalDateTime.now(), id);

        auditService.log(
            "SUSPEND_OFICINA",
            "Oficina",
            id,
            String.format("Suspended workshop: %s", oficina.getNomeFantasia())
        );

        // Reload entity to get updated state (clear cache first to get fresh data)
        return buildDetailResponse(reloadOficina(id));
    }

    /**
     * Cancels a workshop subscription (changes status to CANCELADA).
     *
     * Data is preserved (soft delete) but access is permanently revoked.
     * Workshop cannot be reactivated after cancellation.
     *
     * Uses native SQL to update status to avoid triggering entity validation
     * on legacy data that may have invalid format (phone, CEP, etc).
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

        // Use native SQL to avoid triggering entity validation on legacy data
        String sql = "UPDATE oficinas SET status = ?, ativo = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, StatusOficina.CANCELADA.name(), false, LocalDateTime.now(), id);

        auditService.log(
            "CANCEL_OFICINA",
            "Oficina",
            id,
            String.format("Cancelled workshop: %s", oficina.getNomeFantasia())
        );

        // Reload entity to get updated state (clear cache first to get fresh data)
        return buildDetailResponse(reloadOficina(id));
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

    /**
     * Gets detailed metrics for a specific workshop.
     *
     * Includes usage statistics, resource consumption, and activity data.
     *
     * @param id workshop identifier
     * @return detailed metrics DTO
     */
    @Transactional(readOnly = true)
    public OficinaMetricasDTO getOficinaMetricas(UUID id) {
        log.debug("Fetching detailed metrics for workshop: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        // Usage statistics
        Long totalUsuarios = countByOficina("usuarios", id);
        Long totalClientes = countByOficina("clientes", id);
        Long totalVeiculos = countByOficina("veiculos", id);
        Long totalPecas = countByOficina("pecas", id);

        // OS statistics for the current month
        LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
        Integer osNoMes = countOSByPeriodo(id, inicioMes, LocalDate.now());
        Integer osFinalizadasMes = countOSByStatusAndPeriodo(id, "FINALIZADO", inicioMes, LocalDate.now())
                                 + countOSByStatusAndPeriodo(id, "ENTREGUE", inicioMes, LocalDate.now());
        Integer osCanceladasMes = countOSByStatusAndPeriodo(id, "CANCELADO", inicioMes, LocalDate.now());

        // Financial metrics
        BigDecimal faturamentoMes = getFaturamentoByPeriodo(id, inicioMes, LocalDate.now());
        BigDecimal faturamentoTotal = getTotalFaturamento(id);
        BigDecimal ticketMedio = osFinalizadasMes > 0
            ? faturamentoMes.divide(BigDecimal.valueOf(osFinalizadasMes), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Conversion rate (approved quotes / total quotes)
        Integer orcamentosTotais = countOSByStatusAndPeriodo(id, "ORCAMENTO", inicioMes, LocalDate.now());
        Integer orcamentosAprovados = countOSByStatusAndPeriodo(id, "APROVADO", inicioMes, LocalDate.now());
        Double taxaConversao = orcamentosTotais > 0
            ? (double) orcamentosAprovados / orcamentosTotais * 100
            : 0.0;

        // Stock alerts
        Integer pecasEstoqueBaixo = countPecasEstoqueBaixo(id);
        BigDecimal valorEstoqueTotal = getValorEstoqueTotal(id);

        // Get last access from oficina
        LocalDateTime ultimoAcesso = oficina.getUltimoAcesso();

        // Login stats (simplified - TODO: implement proper login tracking)
        Integer loginsUltimos30Dias = countLogins30Dias(id);

        // Plan limits (defaults based on plan)
        Integer limiteUsuarios = getLimiteUsuarios(oficina);
        Long limiteEspaco = getLimiteEspaco(oficina);
        Integer limiteOSMes = getLimiteOSMes(oficina);

        // Space used (simplified - TODO: implement proper storage tracking)
        Long espacoUsado = 0L; // Would calculate from uploads/attachments

        return new OficinaMetricasDTO(
            totalUsuarios.intValue(),
            limiteUsuarios,
            espacoUsado,
            limiteEspaco,
            osNoMes,
            limiteOSMes,
            totalClientes.intValue(),
            totalVeiculos.intValue(),
            totalPecas.intValue(),
            faturamentoMes,
            faturamentoTotal,
            ultimoAcesso,
            loginsUltimos30Dias,
            new ArrayList<>(), // TODO: Implement login history
            osFinalizadasMes,
            osCanceladasMes,
            ticketMedio,
            taxaConversao,
            pecasEstoqueBaixo,
            valorEstoqueTotal
        );
    }

    /**
     * Updates resource limits for a workshop.
     *
     * @param id workshop identifier
     * @param request limits update request
     * @return updated workshop details
     */
    @Transactional
    public OficinaDetailResponse updateLimites(UUID id, UpdateLimitesRequest request) {
        log.info("Updating limits for workshop: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        // TODO: When limit fields are added to Oficina entity, update them here
        // For now, log the action and return current state

        String changes = String.format(
            "Limites atualizados - Usuários: %s, Espaço: %s, OS/mês: %s",
            request.limiteUsuarios(),
            request.limiteEspaco(),
            request.limiteOSMes()
        );

        auditService.log(
            "UPDATE_LIMITES",
            "Oficina",
            oficina.getId(),
            changes + (request.motivo() != null ? " | Motivo: " + request.motivo() : "")
        );

        return buildDetailResponse(oficina);
    }

    // Helper methods for metrics

    private Integer countOSByPeriodo(UUID oficinaId, LocalDate inicio, LocalDate fim) {
        String sql = """
            SELECT COUNT(*) FROM ordem_servico
            WHERE oficina_id = ? AND DATE(data_abertura) BETWEEN ? AND ?
            """;
        return jdbcTemplate.queryForObject(sql, Integer.class, oficinaId, inicio, fim);
    }

    private Integer countOSByStatusAndPeriodo(UUID oficinaId, String status, LocalDate inicio, LocalDate fim) {
        String sql = """
            SELECT COUNT(*) FROM ordem_servico
            WHERE oficina_id = ? AND status = ? AND DATE(data_abertura) BETWEEN ? AND ?
            """;
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, oficinaId, status, inicio, fim);
        return result != null ? result : 0;
    }

    private BigDecimal getFaturamentoByPeriodo(UUID oficinaId, LocalDate inicio, LocalDate fim) {
        String sql = """
            SELECT COALESCE(SUM(valor_final), 0) FROM ordem_servico
            WHERE oficina_id = ? AND status IN ('FINALIZADO', 'ENTREGUE')
            AND DATE(data_finalizacao) BETWEEN ? AND ?
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, oficinaId, inicio, fim);
    }

    private Integer countPecasEstoqueBaixo(UUID oficinaId) {
        String sql = """
            SELECT COUNT(*) FROM pecas
            WHERE oficina_id = ? AND quantidade_atual <= quantidade_minima AND ativo = true
            """;
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, oficinaId);
        return result != null ? result : 0;
    }

    private BigDecimal getValorEstoqueTotal(UUID oficinaId) {
        String sql = """
            SELECT COALESCE(SUM(quantidade_atual * valor_custo), 0) FROM pecas
            WHERE oficina_id = ? AND ativo = true
            """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, oficinaId);
    }

    private Integer countLogins30Dias(UUID oficinaId) {
        // TODO: Implement proper login tracking
        // For now return count of active users as approximation
        String sql = "SELECT COUNT(*) FROM usuarios WHERE oficina_id = ? AND ativo = true";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, oficinaId);
        return result != null ? result * 10 : 0; // Approximate logins
    }

    private Integer getLimiteUsuarios(Oficina oficina) {
        return switch (oficina.getPlano()) {
            case ECONOMICO -> 3;
            case PROFISSIONAL -> 10;
            case TURBINADO -> 50;
        };
    }

    private Long getLimiteEspaco(Oficina oficina) {
        return switch (oficina.getPlano()) {
            case ECONOMICO -> 1024L * 1024 * 1024; // 1 GB
            case PROFISSIONAL -> 10L * 1024 * 1024 * 1024; // 10 GB
            case TURBINADO -> 100L * 1024 * 1024 * 1024; // 100 GB
        };
    }

    private Integer getLimiteOSMes(Oficina oficina) {
        return switch (oficina.getPlano()) {
            case ECONOMICO -> 100;
            case PROFISSIONAL -> 500;
            case TURBINADO -> null; // Unlimited
        };
    }
}
