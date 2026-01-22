package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.StatusFatura;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.FaturaRepository;
import com.pitstop.shared.exception.BusinessException;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for workshop's "Minha Conta" features.
 * Handles invoice viewing and payment initiation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinhaContaService {

    private final FaturaRepository faturaRepository;
    private final OficinaRepository oficinaRepository;
    private final FaturaMercadoPagoService faturaMercadoPagoService;
    private final FaturaPDFService faturaPDFService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Get financial summary for a workshop.
     */
    @Transactional(readOnly = true)
    public MinhaContaResumoDTO getResumo(UUID oficinaId) {
        Oficina oficina = oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada: " + oficinaId));

        PlanoAssinatura plano = oficina.getPlano();

        // Count invoices by status
        int faturasPendentes = faturaRepository.countByOficinaIdAndStatus(oficinaId, StatusFatura.PENDENTE);
        int faturasVencidas = faturaRepository.countByOficinaIdAndStatus(oficinaId, StatusFatura.VENCIDO);
        int faturasPagas = faturaRepository.countByOficinaIdAndStatus(oficinaId, StatusFatura.PAGO);
        int totalFaturas = faturasPendentes + faturasVencidas + faturasPagas;

        // Sum values
        BigDecimal valorPendente = faturaRepository.sumValorTotalByOficinaIdAndStatus(oficinaId, StatusFatura.PENDENTE);
        BigDecimal valorVencido = faturaRepository.sumValorTotalByOficinaIdAndStatus(oficinaId, StatusFatura.VENCIDO);

        // Sum paid in last 12 months
        LocalDate dataInicio = LocalDate.now().minusMonths(12);
        BigDecimal valorPagoUltimos12Meses = faturaRepository.sumValorPagoUltimos12Meses(oficinaId, dataInicio.atStartOfDay());

        // Get next pending invoice (closest to due date)
        FaturaResumoDTO proximaFatura = faturaRepository
            .findFirstByOficinaIdAndStatusInOrderByDataVencimentoAsc(
                oficinaId,
                List.of(StatusFatura.PENDENTE, StatusFatura.VENCIDO)
            )
            .map(FaturaResumoDTO::fromEntity)
            .orElse(null);

        return MinhaContaResumoDTO.criar(
            plano != null ? plano.name() : null,
            plano != null ? plano.getNome() : null,
            oficina.getValorMensalidade() != null ? oficina.getValorMensalidade() : (plano != null ? plano.getValorMensal() : BigDecimal.ZERO),
            oficina.getDataVencimentoPlano(),
            totalFaturas,
            faturasPendentes,
            faturasVencidas,
            faturasPagas,
            valorPendente != null ? valorPendente : BigDecimal.ZERO,
            valorVencido != null ? valorVencido : BigDecimal.ZERO,
            valorPagoUltimos12Meses != null ? valorPagoUltimos12Meses : BigDecimal.ZERO,
            proximaFatura
        );
    }

    /**
     * List invoices for a workshop with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<FaturaResumoDTO> listarFaturas(UUID oficinaId, String status, Pageable pageable) {
        StatusFatura statusFatura = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFatura = StatusFatura.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Status inválido: {}", status);
            }
        }

        Page<Fatura> faturas;
        if (statusFatura != null) {
            faturas = faturaRepository.findByOficinaIdAndStatus(oficinaId, statusFatura, pageable);
        } else {
            faturas = faturaRepository.findByOficinaId(oficinaId, pageable);
        }

        return faturas.map(FaturaResumoDTO::fromEntity);
    }

    /**
     * Get invoice detail, ensuring it belongs to the workshop.
     */
    @Transactional(readOnly = true)
    public FaturaDTO getFatura(UUID oficinaId, UUID faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + faturaId));

        // Security check: ensure invoice belongs to this workshop
        if (!fatura.getOficina().getId().equals(oficinaId)) {
            throw new BusinessException("Fatura não pertence a esta oficina");
        }

        return FaturaDTO.fromEntity(fatura);
    }

    /**
     * Generate PDF for an invoice.
     */
    @Transactional(readOnly = true)
    public byte[] gerarPdfFatura(UUID oficinaId, UUID faturaId) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + faturaId));

        // Security check
        if (!fatura.getOficina().getId().equals(oficinaId)) {
            throw new BusinessException("Fatura não pertence a esta oficina");
        }

        return faturaPDFService.gerarPDF(fatura);
    }

    /**
     * Initiate payment for an invoice.
     * Creates Mercado Pago preference and returns checkout URL.
     */
    @Transactional
    public IniciarPagamentoFaturaDTO iniciarPagamento(
        UUID oficinaId,
        UUID faturaId,
        String metodoPagamento,
        String emailPagador
    ) {
        Fatura fatura = faturaRepository.findById(faturaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + faturaId));

        // Security check
        if (!fatura.getOficina().getId().equals(oficinaId)) {
            throw new BusinessException("Fatura não pertence a esta oficina");
        }

        // Check if invoice can be paid
        if (!fatura.isPagavel()) {
            return IniciarPagamentoFaturaDTO.erro(
                faturaId.toString(),
                "Esta fatura não pode ser paga. Status atual: " + fatura.getStatus()
            );
        }

        try {
            // Create Mercado Pago preference based on payment method
            IniciarPagamentoFaturaDTO resultado = faturaMercadoPagoService.criarPreferencia(
                fatura,
                emailPagador,
                metodoPagamento,
                frontendUrl + "/minha-conta/faturas?status=success",
                frontendUrl + "/minha-conta/faturas?status=failure",
                frontendUrl + "/minha-conta/faturas?status=pending"
            );

            // Update invoice with payment link
            if (resultado.initPoint() != null) {
                fatura.setLinkPagamento(resultado.initPoint());
            }
            if (resultado.pixQrCode() != null) {
                fatura.setQrCodePix(resultado.pixQrCode());
            }
            faturaRepository.save(fatura);

            log.info("Payment initiated for invoice {} - Preference: {}",
                fatura.getNumero(), resultado.preferenceId());

            return resultado;

        } catch (Exception e) {
            log.error("Error initiating payment for invoice {}: {}", faturaId, e.getMessage(), e);
            return IniciarPagamentoFaturaDTO.erro(
                faturaId.toString(),
                "Erro ao iniciar pagamento: " + e.getMessage()
            );
        }
    }

    /**
     * List paid invoices (payment history).
     */
    @Transactional(readOnly = true)
    public Page<FaturaResumoDTO> listarPagamentos(UUID oficinaId, Pageable pageable) {
        return faturaRepository.findByOficinaIdAndStatus(oficinaId, StatusFatura.PAGO, pageable)
            .map(FaturaResumoDTO::fromEntity);
    }
}
