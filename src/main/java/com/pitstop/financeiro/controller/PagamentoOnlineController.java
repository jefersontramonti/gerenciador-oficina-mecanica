package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.domain.PagamentoOnline;
import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.repository.PagamentoOnlineRepository;
import com.pitstop.financeiro.service.MercadoPagoService;
import com.pitstop.shared.security.feature.RequiresFeature;
import com.pitstop.shared.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller para gerenciar pagamentos online.
 */
@Slf4j
@RestController
@RequestMapping("/api/pagamentos-online")
@RequiredArgsConstructor
@Tag(name = "Pagamentos Online", description = "Gerenciamento de pagamentos via gateway")
@RequiresFeature("INTEGRACAO_MERCADO_PAGO")
public class PagamentoOnlineController {

    private final PagamentoOnlineRepository repository;
    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Criar checkout de pagamento")
    public ResponseEntity<CheckoutResponseDTO> criarCheckout(
            @Valid @RequestBody CriarCheckoutRequestDTO request) {

        log.info("Criando checkout para OS {}", request.getOrdemServicoId());

        CheckoutResponseDTO response = mercadoPagoService.criarCheckout(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Buscar pagamento online por ID")
    public ResponseEntity<PagamentoOnlineDTO> buscarPorId(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        // Validar que o pagamento pertence à oficina do usuário
        PagamentoOnline po = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Pagamento online não encontrado"));
        return ResponseEntity.ok(toDTO(po));
    }

    @GetMapping("/ordem-servico/{osId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE', 'MECANICO')")
    @Operation(summary = "Listar pagamentos online de uma OS")
    public ResponseEntity<List<PagamentoOnlineDTO>> listarPorOS(@PathVariable UUID osId) {
        UUID oficinaId = TenantContext.getTenantId();
        List<PagamentoOnline> pagamentos = repository.findByOficinaIdAndOrdemServicoId(oficinaId, osId);

        List<PagamentoOnlineDTO> dtos = pagamentos.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar todos os pagamentos online da oficina")
    public ResponseEntity<Page<PagamentoOnlineDTO>> listar(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();

        Page<PagamentoOnline> page = repository.findByOficinaIdOrderByCreatedAtDesc(oficinaId, pageable);

        Page<PagamentoOnlineDTO> dtos = page.map(this::toDTO);

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/atualizar-status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Forçar atualização de status do pagamento")
    public ResponseEntity<PagamentoOnlineDTO> atualizarStatus(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        PagamentoOnline po = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Pagamento online não encontrado"));

        if (po.getIdExterno() != null) {
            mercadoPagoService.processarPagamento(po.getIdExterno());
            // Recarregar após processar
            po = repository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento online não encontrado"));
        }

        return ResponseEntity.ok(toDTO(po));
    }

    @PostMapping("/processar-brick")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE', 'ATENDENTE')")
    @Operation(summary = "Processar pagamento do Checkout Brick (inline)")
    public ResponseEntity<java.util.Map<String, Object>> processarBrick(
            @RequestBody java.util.Map<String, Object> request) {

        log.info("Processando pagamento Brick: {}", request);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> formData = (java.util.Map<String, Object>) request.get("formData");
        String ordemServicoId = (String) request.get("ordemServicoId");
        String preferenceId = (String) request.get("preferenceId");

        if (formData == null) {
            throw new IllegalArgumentException("Dados do pagamento são obrigatórios");
        }

        // Processar pagamento via Mercado Pago
        java.util.Map<String, Object> result = mercadoPagoService.processarPagamentoBrick(
            formData, UUID.fromString(ordemServicoId), preferenceId);

        return ResponseEntity.ok(result);
    }

    // ========== Métodos auxiliares ==========

    private PagamentoOnlineDTO toDTO(PagamentoOnline po) {
        return PagamentoOnlineDTO.builder()
            .id(po.getId())
            .ordemServicoId(po.getOrdemServicoId())
            .pagamentoId(po.getPagamentoId())
            .gateway(po.getGateway())
            .gatewayDescricao(po.getGateway().getDescricao())
            .preferenceId(po.getPreferenceId())
            .idExterno(po.getIdExterno())
            .idCobranca(po.getIdCobranca())
            .status(po.getStatus())
            .statusDescricao(po.getStatus().getDescricao())
            .statusDetalhe(po.getStatusDetalhe())
            .valor(po.getValor())
            .valorLiquido(po.getValorLiquido())
            .valorTaxa(po.getValorTaxa())
            .metodoPagamento(po.getMetodoPagamento())
            .bandeiraCartao(po.getBandeiraCartao())
            .ultimosDigitos(po.getUltimosDigitos())
            .parcelas(po.getParcelas())
            .urlCheckout(po.getUrlCheckout())
            .urlQrCode(po.getUrlQrCode())
            .codigoPix(po.getCodigoPix())
            .dataExpiracao(po.getDataExpiracao())
            .dataAprovacao(po.getDataAprovacao())
            .erroMensagem(po.getErroMensagem())
            .erroCodigo(po.getErroCodigo())
            .tentativas(po.getTentativas())
            .emailPagador(po.getEmailPagador())
            .nomePagador(po.getNomePagador())
            .documentoPagador(po.getDocumentoPagador())
            .expirado(po.isExpirado())
            .aprovado(po.isAprovado())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
