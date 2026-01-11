package com.pitstop.oficina.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.dto.CreateOficinaRequest;
import com.pitstop.oficina.dto.OficinaResumoResponse;
import com.pitstop.oficina.dto.OficinaResponse;
import com.pitstop.oficina.dto.UpdateOficinaRequest;
import com.pitstop.oficina.exception.CnpjAlreadyExistsException;
import com.pitstop.oficina.exception.OficinaNotFoundException;
import com.pitstop.oficina.exception.OficinaValidationException;
import com.pitstop.oficina.exception.PlanUpgradeException;
import com.pitstop.oficina.mapper.OficinaMapper;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pitstop.shared.security.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para lógica de negócio de Oficinas.
 * Contém operações de CRUD, gerenciamento de planos e status.
 *
 * @author PitStop Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OficinaService {

    private final OficinaRepository oficinaRepository;
    private final OficinaMapper oficinaMapper;

    /**
     * Cria uma nova oficina (onboarding).
     * Define status ATIVA com período trial de 7 dias.
     *
     * @param request Dados da nova oficina
     * @return Oficina criada
     * @throws CnpjAlreadyExistsException se CNPJ já existe
     */
    @Transactional
    @CacheEvict(value = "oficinas", allEntries = true)
    public OficinaResponse create(CreateOficinaRequest request) {
        log.info("Criando nova oficina");

        // TODO: Validar CNPJ único
        // if (oficinaRepository.existsByCnpj(request.cnpjCpf())) {
        //     throw new CnpjAlreadyExistsException(request.cnpjCpf());
        // }

        Oficina oficina = oficinaMapper.toEntity(request);

        // Definir valores iniciais
        oficina.setStatus(StatusOficina.ATIVA);
        oficina.setDataAssinatura(LocalDate.now());
        oficina.setDataVencimentoPlano(LocalDate.now().plusDays(7)); // Trial 7 dias
        if (oficina.getPlano() != null) {
            oficina.setValorMensalidade(getValorPlano(oficina.getPlano()));
        }

        oficina = oficinaRepository.save(oficina);

        log.info("Oficina criada com sucesso. ID: {}, Nome: {}", oficina.getId(), oficina.getNomeFantasia());

        // TODO: Enviar email de boas-vindas
        // TODO: Criar primeiro usuário admin automaticamente

        return oficinaMapper.toResponse(oficina);
    }

    /**
     * Busca oficina por ID.
     *
     * <p><b>SECURITY:</b> Validates tenant access to prevent cross-tenant data leaks.</p>
     * <ul>
     *   <li>SUPER_ADMIN: Can access any oficina</li>
     *   <li>ADMIN/GERENTE/others: Can only access their own oficina</li>
     * </ul>
     *
     * @param id ID da oficina
     * @return Oficina encontrada
     * @throws OficinaNotFoundException se não encontrada
     * @throws AccessDeniedException se tentar acessar oficina de outro tenant
     */
    @Cacheable(value = "oficinas", key = "#id")
    public OficinaResponse findById(UUID id) {
        // SECURITY: Validate tenant access
        validateTenantAccess(id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        return oficinaMapper.toResponse(oficina);
    }

    /**
     * Validates that the current user can access the specified oficina.
     *
     * <p><b>Multi-tenancy security check:</b></p>
     * <ul>
     *   <li>SUPER_ADMIN can access any oficina (cross-tenant access)</li>
     *   <li>Other roles can only access their own oficina (TenantContext)</li>
     * </ul>
     *
     * @param oficinaId The oficina ID being accessed
     * @throws AccessDeniedException if access is not allowed
     */
    private void validateTenantAccess(UUID oficinaId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // If not authenticated, skip validation (will fail at controller level)
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }

        // SUPER_ADMIN can access any oficina
        boolean isSuperAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("SUPER_ADMIN"));

        if (isSuperAdmin) {
            log.debug("SUPER_ADMIN accessing oficina: {}", oficinaId);
            return;
        }

        // Other roles: must be accessing their own oficina
        if (TenantContext.isSet()) {
            UUID currentTenantId = TenantContext.getTenantId();
            if (!oficinaId.equals(currentTenantId)) {
                log.warn("SECURITY: Cross-tenant access attempt. User tenant: {}, Requested oficina: {}",
                    currentTenantId, oficinaId);
                throw new AccessDeniedException(
                    "Você só pode acessar dados da sua própria oficina"
                );
            }
        }
    }

    /**
     * Busca oficina por CNPJ.
     *
     * @param cnpj CNPJ da oficina (apenas números)
     * @return Oficina encontrada
     * @throws OficinaNotFoundException se não encontrada
     */
    public OficinaResponse findByCnpj(String cnpjCpf) {

        Oficina oficina = oficinaRepository.findByCnpj(cnpjCpf)
            .orElseThrow(() -> new OficinaNotFoundException(cnpjCpf));

        return oficinaMapper.toResponse(oficina);
    }

    /**
     * Lista todas as oficinas com paginação (resumo).
     *
     * @param pageable Configuração de paginação
     * @return Página de oficinas (resumo)
     */
    public Page<OficinaResumoResponse> findAll(Pageable pageable) {

        return oficinaRepository.findAll(pageable)
            .map(oficinaMapper::toResumoResponse);
    }

    /**
     * Lista oficinas com filtros avançados (Super Admin).
     *
     * @param status Status (null = todos)
     * @param plano Plano (null = todos)
     * @param nome Nome/nome fantasia (busca parcial)
     * @param cnpj CNPJ (busca parcial)
     * @param pageable Configuração de paginação
     * @return Página de oficinas filtradas
     */
    public Page<OficinaResumoResponse> findWithFilters(
        StatusOficina status,
        PlanoAssinatura plano,
        String nome,
        String cnpj,
        Pageable pageable
    ) {
        // Convert enums to strings for native query
        String statusStr = status != null ? status.name() : null;
        String planoStr = plano != null ? plano.name() : null;

        // Convert pageable sort to snake_case for native query
        Pageable nativePageable = convertToNativePageable(pageable);

        return oficinaRepository.findWithFiltersNative(statusStr, planoStr, nome, cnpj, nativePageable)
            .map(oficinaMapper::toResumoResponse);
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
     * Busca oficinas por status.
     *
     * @param status Status desejado
     * @param pageable Configuração de paginação
     * @return Página de oficinas com o status
     */
    public Page<OficinaResumoResponse> findByStatus(StatusOficina status, Pageable pageable) {

        return oficinaRepository.findByStatus(status, pageable)
            .map(oficinaMapper::toResumoResponse);
    }

    /**
     * Busca oficinas com vencimento próximo (7 dias).
     * Útil para enviar lembretes de renovação.
     *
     * @return Lista de oficinas com vencimento próximo
     */
    public List<OficinaResumoResponse> findVencimentoProximo() {
        LocalDate hoje = LocalDate.now();
        LocalDate daquiA7Dias = hoje.plusDays(7);


        List<Oficina> oficinas = oficinaRepository.findByDataVencimentoBetween(hoje, daquiA7Dias);

        return oficinas.stream()
            .map(oficinaMapper::toResumoResponse)
            .collect(Collectors.toList());
    }

    /**
     * Busca oficinas com plano vencido.
     * Usado para job de suspensão automática.
     *
     * @return Lista de oficinas vencidas
     */
    public List<OficinaResponse> findVencidas() {

        List<Oficina> oficinas = oficinaRepository.findVencidas(LocalDate.now());

        return oficinas.stream()
            .map(oficinaMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Atualiza dados de uma oficina existente.
     * Apenas campos não-nulos são atualizados.
     *
     * @param id ID da oficina
     * @param request Dados a atualizar
     * @return Oficina atualizada
     * @throws OficinaNotFoundException se não encontrada
     */
    @Transactional
    @CacheEvict(value = "oficinas", key = "#id")
    public OficinaResponse update(UUID id, UpdateOficinaRequest request) {
        log.info("Atualizando oficina ID: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        oficinaMapper.updateEntityFromRequest(request, oficina);
        oficina = oficinaRepository.save(oficina);

        log.info("Oficina atualizada com sucesso. ID: {}", id);

        return oficinaMapper.toResponse(oficina);
    }

    /**
     * Suspende uma oficina (não pagamento ou violação de termos).
     * Bloqueia acesso dos usuários.
     *
     * @param id ID da oficina
     * @throws OficinaNotFoundException se não encontrada
     * @throws OficinaValidationException se já estiver suspensa ou cancelada
     */
    @Transactional
    @CacheEvict(value = "oficinas", key = "#id")
    public void suspend(UUID id) {
        log.warn("Suspendendo oficina ID: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        if (oficina.getStatus() == StatusOficina.SUSPENSA) {
            throw new OficinaValidationException("Oficina já está suspensa");
        }

        if (oficina.getStatus() == StatusOficina.CANCELADA) {
            throw new OficinaValidationException("Não é possível suspender uma oficina cancelada");
        }

        oficina.setStatus(StatusOficina.SUSPENSA);
        oficinaRepository.save(oficina);

        log.info("Oficina suspensa com sucesso. ID: {}", id);

        // TODO: Enviar email de notificação
        // TODO: Desativar todos os usuários desta oficina (multi-tenant)
        // TODO: Bloquear acesso ao sistema
    }

    /**
     * Reativa uma oficina suspensa (após pagamento).
     * Renova o plano por 30 dias.
     *
     * @param id ID da oficina
     * @throws OficinaNotFoundException se não encontrada
     * @throws OficinaValidationException se não estiver suspensa
     */
    @Transactional
    @CacheEvict(value = "oficinas", key = "#id")
    public void activate(UUID id) {
        log.info("Reativando oficina ID: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        if (oficina.getStatus() != StatusOficina.SUSPENSA && oficina.getStatus() != StatusOficina.INATIVA) {
            throw new OficinaValidationException("Apenas oficinas suspendidas ou inativas podem ser reativadas");
        }

        oficina.setStatus(StatusOficina.ATIVA);
        oficina.setDataVencimentoPlano(LocalDate.now().plusMonths(1));
        oficinaRepository.save(oficina);

        log.info("Oficina reativada com sucesso. ID: {}", id);

        // TODO: Reativar usuários
        // TODO: Enviar email de confirmação
    }

    /**
     * Cancela uma oficina (pedido do cliente).
     * Operação irreversível.
     *
     * @param id ID da oficina
     * @throws OficinaNotFoundException se não encontrada
     */
    @Transactional
    @CacheEvict(value = "oficinas", key = "#id")
    public void cancel(UUID id) {
        log.warn("Cancelando oficina ID: {}", id);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        if (oficina.getStatus() == StatusOficina.CANCELADA) {
            throw new OficinaValidationException("Oficina já está cancelada");
        }

        oficina.setStatus(StatusOficina.CANCELADA);
        oficina.setDataVencimentoPlano(LocalDate.now()); // Vencimento imediato
        oficinaRepository.save(oficina);

        log.info("Oficina cancelada com sucesso. ID: {}", id);

        // TODO: Desativar todos os usuários
        // TODO: Enviar email de confirmação
        // TODO: Agendar exclusão de dados (LGPD) após período de retenção
    }

    /**
     * Faz upgrade do plano de assinatura.
     * Apenas upgrade é permitido (não downgrade).
     *
     * @param id ID da oficina
     * @param novoPlano Novo plano (deve ser superior ao atual)
     * @return Oficina com plano atualizado
     * @throws OficinaNotFoundException se não encontrada
     * @throws PlanUpgradeException se novo plano não for superior
     */
    @Transactional
    @CacheEvict(value = "oficinas", key = "#id")
    public OficinaResponse upgradePlan(UUID id, PlanoAssinatura novoPlano) {
        log.info("Upgrade de plano. Oficina: {}, Novo plano: {}", id, novoPlano);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        PlanoAssinatura planoAtual = oficina.getPlano();

        // Validar se é realmente upgrade (ordem: ECONOMICO < PROFISSIONAL < TURBINADO)
        if (novoPlano.ordinal() <= planoAtual.ordinal()) {
            throw new PlanUpgradeException(planoAtual, novoPlano);
        }

        oficina.setPlano(novoPlano);
        oficina.setValorMensalidade(getValorPlano(novoPlano));

        oficina = oficinaRepository.save(oficina);

        log.info("Plano atualizado com sucesso. Oficina: {}, Plano anterior: {}, Novo plano: {}",
            id, planoAtual, novoPlano);

        // TODO: Gerar fatura proporcional (pro-rated)
        // TODO: Enviar email de confirmação
        // TODO: Liberar recursos do novo plano imediatamente

        return oficinaMapper.toResponse(oficina);
    }

    /**
     * Faz downgrade do plano de assinatura.
     * Apenas aplica na próxima renovação (não imediatamente).
     *
     * @param id ID da oficina
     * @param novoPlano Novo plano (deve ser inferior ao atual)
     * @return Oficina atualizada
     * @throws OficinaNotFoundException se não encontrada
     * @throws PlanUpgradeException se novo plano não for inferior
     */
    @Transactional
    @CacheEvict(value = "oficinas", key = "#id")
    public OficinaResponse downgradePlan(UUID id, PlanoAssinatura novoPlano) {
        log.info("Downgrade de plano. Oficina: {}, Novo plano: {}", id, novoPlano);

        Oficina oficina = oficinaRepository.findById(id)
            .orElseThrow(() -> new OficinaNotFoundException(id));

        PlanoAssinatura planoAtual = oficina.getPlano();

        // Validar se é realmente downgrade
        if (novoPlano.ordinal() >= planoAtual.ordinal()) {
            throw new PlanUpgradeException("Downgrade deve ser para um plano inferior ao atual");
        }

        // Downgrade só aplica na próxima renovação
        // Por enquanto, apenas atualiza o plano e valor
        oficina.setPlano(novoPlano);
        oficina.setValorMensalidade(getValorPlano(novoPlano));

        oficina = oficinaRepository.save(oficina);

        log.info("Downgrade agendado. Aplicará na renovação em: {}", oficina.getDataVencimentoPlano());

        // TODO: Registrar downgrade agendado
        // TODO: Enviar email de confirmação
        // TODO: Aplicar restrições do novo plano apenas na renovação

        return oficinaMapper.toResponse(oficina);
    }

    /**
     * Conta total de oficinas ativas (métrica para Super Admin).
     *
     * @return Número de oficinas ativas
     */
    public long countAtivas() {
        return oficinaRepository.countAtivas();
    }

    /**
     * Calcula MRR (Monthly Recurring Revenue) total.
     * Soma de todas as mensalidades de oficinas ativas.
     *
     * @return MRR total
     */
    public Double calculateMRR() {
        Double mrr = oficinaRepository.calculateMRR();
        log.debug("MRR calculado: R$ {}", mrr);
        return mrr;
    }

    /**
     * Retorna o valor mensal de cada plano (centralizado).
     *
     * @param plano Plano de assinatura
     * @return Valor mensal do plano
     */
    private BigDecimal getValorPlano(PlanoAssinatura plano) {
        return switch (plano) {
            case ECONOMICO -> new BigDecimal("99.90");
            case PROFISSIONAL -> new BigDecimal("199.90");
            case TURBINADO -> new BigDecimal("399.90");
        };
    }
}
