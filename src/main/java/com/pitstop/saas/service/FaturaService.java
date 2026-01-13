package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.ItemFatura;
import com.pitstop.saas.domain.StatusFatura;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.FaturaRepository;
import com.pitstop.shared.audit.service.AuditService;
import com.pitstop.shared.exception.BusinessException;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing invoices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FaturaService {

    private final FaturaRepository faturaRepository;
    private final OficinaRepository oficinaRepository;
    private final AuditService auditService;

    // =====================================
    // QUERY METHODS
    // =====================================

    /**
     * Find invoice by ID.
     */
    @Transactional(readOnly = true)
    public FaturaDTO findById(UUID id) {
        Fatura fatura = faturaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + id));
        return FaturaDTO.fromEntity(fatura);
    }

    /**
     * Find invoice by number.
     */
    @Transactional(readOnly = true)
    public FaturaDTO findByNumero(String numero) {
        Fatura fatura = faturaRepository.findByNumero(numero)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + numero));
        return FaturaDTO.fromEntity(fatura);
    }

    /**
     * List invoices with filters.
     */
    @Transactional(readOnly = true)
    public Page<FaturaResumoDTO> findWithFilters(
        UUID oficinaId,
        StatusFatura status,
        LocalDate dataInicio,
        LocalDate dataFim,
        Pageable pageable
    ) {
        return faturaRepository.findWithFilters(oficinaId, status, dataInicio, dataFim, pageable)
            .map(FaturaResumoDTO::fromEntity);
    }

    /**
     * Find invoices for a specific workshop.
     */
    @Transactional(readOnly = true)
    public Page<FaturaResumoDTO> findByOficina(UUID oficinaId, Pageable pageable) {
        return faturaRepository.findByOficinaId(oficinaId, pageable)
            .map(FaturaResumoDTO::fromEntity);
    }

    /**
     * Get pending or overdue invoices for a workshop.
     */
    @Transactional(readOnly = true)
    public List<FaturaResumoDTO> findPendentesOuVencidas(UUID oficinaId) {
        return faturaRepository.findPendentesOuVencidasByOficina(oficinaId)
            .stream()
            .map(FaturaResumoDTO::fromEntity)
            .toList();
    }

    // =====================================
    // GENERATION METHODS
    // =====================================

    /**
     * Generate monthly invoices for all active workshops.
     * Called by scheduled job on the 1st of each month.
     */
    @Transactional
    public int gerarFaturasMensais() {
        LocalDate mesReferencia = LocalDate.now().withDayOfMonth(1);
        log.info("Generating monthly invoices for {}", mesReferencia);

        // Get all active and trial workshops
        List<Oficina> oficinas = oficinaRepository.findByStatusIn(
            List.of(StatusOficina.ATIVA, StatusOficina.TRIAL)
        );

        int count = 0;
        for (Oficina oficina : oficinas) {
            try {
                if (!faturaRepository.existsFaturaParaMes(oficina.getId(), mesReferencia)) {
                    gerarFatura(oficina.getId(), mesReferencia);
                    count++;
                }
            } catch (Exception e) {
                log.error("Error generating invoice for workshop {}: {}", oficina.getId(), e.getMessage());
            }
        }

        log.info("Generated {} invoices for {}", count, mesReferencia);
        return count;
    }

    /**
     * Generate invoice for a specific workshop and month.
     */
    @Transactional
    public FaturaDTO gerarFatura(UUID oficinaId, LocalDate mesReferencia) {
        Oficina oficina = oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada: " + oficinaId));

        // Check if invoice already exists
        LocalDate mesRef = mesReferencia.withDayOfMonth(1);
        if (faturaRepository.existsFaturaParaMes(oficinaId, mesRef)) {
            throw new BusinessException("Já existe fatura para o mês " + mesRef + " nesta oficina");
        }

        // Get next invoice number
        Long seq = faturaRepository.getNextNumeroSequence();
        String numero = Fatura.gerarNumero(seq.intValue());

        // Calculate due date: 10th of the next month
        LocalDate dataVencimento = mesRef.plusMonths(1).withDayOfMonth(10);

        // Create invoice
        Fatura fatura = Fatura.builder()
            .numero(numero)
            .oficina(oficina)
            .planoCodigo(oficina.getPlano().name())
            .status(StatusFatura.PENDENTE)
            .mesReferencia(mesRef)
            .dataEmissao(LocalDate.now())
            .dataVencimento(dataVencimento)
            .build();

        // Add subscription item
        BigDecimal valorMensalidade = oficina.getValorMensalidade() != null
            ? oficina.getValorMensalidade()
            : oficina.getPlano().getValorMensal();

        ItemFatura item = ItemFatura.criarItemMensalidade(
            oficina.getPlano().getNome(),
            valorMensalidade
        );
        fatura.addItem(item);

        fatura = faturaRepository.save(fatura);

        log.info("Invoice {} generated for workshop {} - {} - R$ {}",
            numero, oficina.getNomeFantasia(), mesRef, fatura.getValorTotal());

        auditService.log(
            "GENERATE_INVOICE",
            "Fatura",
            fatura.getId(),
            String.format("Invoice %s generated for %s (R$ %s)",
                numero, oficina.getNomeFantasia(), fatura.getValorTotal())
        );

        return FaturaDTO.fromEntity(fatura);
    }

    /**
     * Create invoice manually with custom items.
     */
    @Transactional
    public FaturaDTO criarFaturaManual(CreateFaturaRequest request) {
        Oficina oficina = oficinaRepository.findById(request.oficinaId())
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        // Check if invoice already exists for this month
        LocalDate mesRef = request.mesReferencia().withDayOfMonth(1);
        if (faturaRepository.existsFaturaParaMes(request.oficinaId(), mesRef)) {
            throw new BusinessException("Já existe fatura para o mês " + mesRef + " nesta oficina");
        }

        // Get next invoice number
        Long seq = faturaRepository.getNextNumeroSequence();
        String numero = Fatura.gerarNumero(seq.intValue());

        // Create invoice
        Fatura fatura = Fatura.builder()
            .numero(numero)
            .oficina(oficina)
            .planoCodigo(oficina.getPlano().name())
            .status(StatusFatura.PENDENTE)
            .mesReferencia(mesRef)
            .dataEmissao(LocalDate.now())
            .dataVencimento(request.dataVencimento())
            .observacao(request.observacao())
            .build();

        // Add items
        for (CreateFaturaRequest.ItemRequest itemReq : request.itens()) {
            ItemFatura item = ItemFatura.criarItemExtra(
                itemReq.descricao(),
                itemReq.quantidade(),
                itemReq.valorUnitario()
            );
            fatura.addItem(item);
        }

        // Apply discount if provided
        if (request.desconto() != null && request.desconto().compareTo(BigDecimal.ZERO) > 0) {
            fatura.aplicarDesconto(request.desconto());
        }

        fatura = faturaRepository.save(fatura);

        log.info("Manual invoice {} created for workshop {}", numero, oficina.getNomeFantasia());

        auditService.log(
            "CREATE_INVOICE_MANUAL",
            "Fatura",
            fatura.getId(),
            String.format("Manual invoice %s created for %s (R$ %s)",
                numero, oficina.getNomeFantasia(), fatura.getValorTotal())
        );

        return FaturaDTO.fromEntity(fatura);
    }

    // =====================================
    // STATUS CHANGE METHODS
    // =====================================

    /**
     * Register payment for an invoice.
     */
    @Transactional
    public FaturaDTO registrarPagamento(UUID faturaId, RegistrarPagamentoFaturaRequest request) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada"));

        if (!fatura.isPagavel()) {
            throw new BusinessException("Esta fatura não pode ser paga. Status: " + fatura.getStatus());
        }

        fatura.marcarComoPago(request.metodoPagamento(), request.transacaoId());

        if (request.observacao() != null && !request.observacao().isBlank()) {
            String obs = fatura.getObservacao() != null ? fatura.getObservacao() + "\n" : "";
            fatura.setObservacao(obs + "Pagamento: " + request.observacao());
        }

        fatura = faturaRepository.save(fatura);

        // Reactivate workshop if it was suspended
        Oficina oficina = fatura.getOficina();
        if (oficina.getStatus() == StatusOficina.SUSPENSA) {
            oficina.setStatus(StatusOficina.ATIVA);
            oficina.setDataVencimentoPlano(LocalDate.now().plusMonths(1));
            oficinaRepository.save(oficina);
            log.info("Workshop {} reactivated after payment", oficina.getNomeFantasia());
        }

        log.info("Payment registered for invoice {} - {} via {}",
            fatura.getNumero(), fatura.getValorTotal(), request.metodoPagamento());

        auditService.log(
            "REGISTER_PAYMENT",
            "Fatura",
            fatura.getId(),
            String.format("Payment R$ %s registered via %s",
                fatura.getValorTotal(), request.metodoPagamento())
        );

        return FaturaDTO.fromEntity(fatura);
    }

    /**
     * Cancel an invoice.
     */
    @Transactional
    public FaturaDTO cancelar(UUID faturaId, String motivo) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada"));

        if (!fatura.isCancelavel()) {
            throw new BusinessException("Esta fatura não pode ser cancelada. Status: " + fatura.getStatus());
        }

        fatura.cancelar(motivo);
        fatura = faturaRepository.save(fatura);

        log.info("Invoice {} cancelled: {}", fatura.getNumero(), motivo);

        auditService.log(
            "CANCEL_INVOICE",
            "Fatura",
            fatura.getId(),
            String.format("Invoice %s cancelled: %s", fatura.getNumero(), motivo)
        );

        return FaturaDTO.fromEntity(fatura);
    }

    /**
     * Mark overdue invoices as VENCIDO.
     * Called by scheduled job daily.
     */
    @Transactional
    public int processarFaturasVencidas() {
        List<Fatura> vencidas = faturaRepository.findPendentesVencidas(LocalDate.now());

        int count = 0;
        for (Fatura fatura : vencidas) {
            fatura.marcarComoVencido();
            faturaRepository.save(fatura);
            count++;
            log.warn("Invoice {} marked as overdue", fatura.getNumero());
        }

        log.info("Processed {} overdue invoices", count);
        return count;
    }

    // =====================================
    // STATISTICS METHODS
    // =====================================

    /**
     * Get invoice summary statistics.
     */
    @Transactional(readOnly = true)
    public FaturasResumoSummaryDTO getSummary() {
        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1);

        return new FaturasResumoSummaryDTO(
            faturaRepository.countByStatus(StatusFatura.PENDENTE),
            faturaRepository.countByStatus(StatusFatura.VENCIDO),
            faturaRepository.countByStatus(StatusFatura.PAGO),
            faturaRepository.countByStatus(StatusFatura.CANCELADO),
            faturaRepository.sumValorPendente(),
            calculateValorVencido(),
            faturaRepository.sumValorRecebido(inicioMes, fimMes),
            faturaRepository.countInadimplentes(LocalDate.now())
        );
    }

    private BigDecimal calculateValorVencido() {
        // Sum of overdue invoices
        return faturaRepository.findPendentesVencidas(LocalDate.now())
            .stream()
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
