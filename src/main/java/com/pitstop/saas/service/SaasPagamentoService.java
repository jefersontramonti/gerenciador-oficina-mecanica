package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.SaasPagamento;
import com.pitstop.saas.dto.PagamentoHistoricoResponse;
import com.pitstop.saas.dto.RegistrarPagamentoRequest;
import com.pitstop.saas.repository.SaasPagamentoRepository;
import com.pitstop.shared.audit.service.AuditService;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing SaaS subscription payments.
 *
 * Handles payment registration, tracking, overdue detection,
 * and automatic status updates based on payment confirmations.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaasPagamentoService {

    private final SaasPagamentoRepository pagamentoRepository;
    private final OficinaRepository oficinaRepository;
    private final AuditService auditService;

    private static final int OVERDUE_GRACE_PERIOD_DAYS = 5;

    /**
     * Registers a new subscription payment.
     *
     * Automatically updates workshop status if payment confirms active subscription
     * (e.g., TRIAL → ATIVA on first payment).
     *
     * @param request payment registration details
     * @return registered payment details
     */
    @Transactional
    public PagamentoHistoricoResponse registrarPagamento(RegistrarPagamentoRequest request) {
        log.info("Registering payment for workshop {} - month {}",
            request.oficinaId(), request.mesReferencia());

        // Validate workshop exists
        Oficina oficina = oficinaRepository.findById(request.oficinaId())
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada"));

        // Check for duplicate payment
        LocalDate mesRef = request.mesReferencia().atDay(1);
        if (pagamentoRepository.findByOficinaIdAndReferenciaMes(request.oficinaId(), mesRef).isPresent()) {
            throw new IllegalArgumentException(
                "Já existe um pagamento registrado para este mês de referência"
            );
        }

        // Calculate due date (10th of next month)
        LocalDate dataVencimento = request.mesReferencia()
            .atDay(1)
            .plusMonths(1)
            .withDayOfMonth(10);

        // Create payment record
        SaasPagamento pagamento = SaasPagamento.builder()
            .oficina(oficina)
            .referenciaMes(mesRef)
            .valorPago(request.valor())
            .dataPagamento(request.dataPagamento())
            .dataVencimento(dataVencimento)
            .formaPagamento(request.formaPagamento())
            .observacao(request.observacao())
            .build();

        pagamento = pagamentoRepository.save(pagamento);
        log.info("Payment registered with ID: {}", pagamento.getId());

        // Update workshop status if needed
        updateOficinaStatusAfterPayment(oficina);

        // Audit log
        auditService.log(
            "REGISTRAR_PAGAMENTO",
            "SaasPagamento",
            pagamento.getId(),
            String.format("Pagamento registrado para %s - %s: R$ %.2f",
                oficina.getNomeFantasia(), request.mesReferencia(), request.valor())
        );

        return buildPagamentoResponse(pagamento);
    }

    /**
     * Gets payment history for a specific workshop.
     *
     * @param oficinaId workshop identifier
     * @param pageable pagination parameters
     * @return paginated payment history
     */
    @Transactional(readOnly = true)
    public Page<PagamentoHistoricoResponse> getHistoricoPagamentos(UUID oficinaId, Pageable pageable) {
        log.debug("Fetching payment history for workshop: {}", oficinaId);

        return pagamentoRepository.findByOficinaIdOrderByReferenciaMesDesc(oficinaId, pageable)
            .map(this::buildPagamentoResponse);
    }

    /**
     * Gets all payments within a date range.
     *
     * @param dataInicio start date (inclusive)
     * @param dataFim end date (inclusive)
     * @param pageable pagination parameters
     * @return paginated payments
     */
    @Transactional(readOnly = true)
    public Page<PagamentoHistoricoResponse> getPagamentosPorPeriodo(
        LocalDate dataInicio,
        LocalDate dataFim,
        Pageable pageable
    ) {
        log.debug("Fetching payments between {} and {}", dataInicio, dataFim);

        return pagamentoRepository.findByDataPagamentoBetween(dataInicio, dataFim, pageable)
            .map(this::buildPagamentoResponse);
    }

    /**
     * Gets all workshops with pending payments.
     *
     * Returns workshops where payment is due but not yet received
     * (within grace period, not yet considered overdue).
     *
     * @param pageable pagination parameters
     * @return paginated list of workshops with pending payments
     */
    @Transactional(readOnly = true)
    public Page<Oficina> getOficinasPagamentosPendentes(Pageable pageable) {
        log.debug("Fetching workshops with pending payments");

        LocalDate hoje = LocalDate.now();
        LocalDate limiteGracePeriod = hoje.minusDays(OVERDUE_GRACE_PERIOD_DAYS);

        // Find workshops with due date between grace period limit and today
        return oficinaRepository.findByDataVencimentoPlanoBetween(
            limiteGracePeriod,
            hoje,
            pageable
        );
    }

    /**
     * Gets all workshops with overdue payments.
     *
     * Returns workshops where payment is past grace period and should
     * be suspended if not paid immediately.
     *
     * @param pageable pagination parameters
     * @return paginated list of workshops with overdue payments
     */
    @Transactional(readOnly = true)
    public Page<Oficina> getOficinasInadimplentes(Pageable pageable) {
        log.debug("Fetching workshops with overdue payments");

        LocalDate hoje = LocalDate.now();
        LocalDate limiteOverdue = hoje.minusDays(OVERDUE_GRACE_PERIOD_DAYS);

        // Find active workshops with due date before grace period
        return oficinaRepository.findByStatusAndDataVencimentoPlanoBefore(
            StatusOficina.ATIVA,
            limiteOverdue,
            pageable
        );
    }

    /**
     * Gets all overdue payments (paid late).
     *
     * @param pageable pagination parameters
     * @return paginated list of late payments
     */
    @Transactional(readOnly = true)
    public Page<PagamentoHistoricoResponse> getPagamentosAtrasados(Pageable pageable) {
        log.debug("Fetching overdue payments");

        return pagamentoRepository.findPagamentosAtrasados(pageable)
            .map(this::buildPagamentoResponse);
    }

    /**
     * Updates workshop status after payment confirmation.
     *
     * Transitions:
     * - TRIAL → ATIVA (on first payment)
     * - SUSPENSA → ATIVA (on payment after suspension)
     */
    private void updateOficinaStatusAfterPayment(Oficina oficina) {
        StatusOficina oldStatus = oficina.getStatus();

        if (oldStatus == StatusOficina.TRIAL || oldStatus == StatusOficina.SUSPENSA) {
            oficina.setStatus(StatusOficina.ATIVA);
            oficina.setAtivo(true);

            // Update next due date to next month's 10th
            LocalDate proximoVencimento = LocalDate.now()
                .plusMonths(1)
                .withDayOfMonth(10);
            oficina.setDataVencimentoPlano(proximoVencimento);

            oficinaRepository.save(oficina);

            log.info("Workshop {} status changed: {} → ATIVA",
                oficina.getNomeFantasia(), oldStatus);

            auditService.log(
                "ATIVAR_OFICINA_AUTOMATICO",
                "Oficina",
                oficina.getId(),
                String.format("Status alterado automaticamente de %s para ATIVA após pagamento",
                    oldStatus)
            );
        }
    }

    /**
     * Builds a payment response DTO from entity.
     */
    private PagamentoHistoricoResponse buildPagamentoResponse(SaasPagamento pagamento) {
        boolean atrasado = pagamento.isAtrasado();
        Integer diasAtraso = atrasado ? pagamento.getDiasAtraso() : null;

        return new PagamentoHistoricoResponse(
            pagamento.getId(),
            pagamento.getOficina().getId(),
            pagamento.getOficina().getNomeFantasia(),
            pagamento.getReferenciaMesAsYearMonth(),
            pagamento.getValorPago(),
            pagamento.getDataPagamento(),
            pagamento.getDataVencimento(),
            pagamento.getFormaPagamento(),
            pagamento.getObservacao(),
            atrasado,
            diasAtraso,
            pagamento.getCreatedAt()
        );
    }
}
