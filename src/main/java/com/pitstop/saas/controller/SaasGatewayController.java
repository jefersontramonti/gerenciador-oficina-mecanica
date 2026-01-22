package com.pitstop.saas.controller;

import com.pitstop.saas.domain.TipoGateway;
import com.pitstop.saas.dto.ConfiguracaoGatewayDTO;
import com.pitstop.saas.dto.ConfiguracaoGatewayRequestDTO;
import com.pitstop.saas.service.ConfiguracaoGatewayService;
import com.pitstop.shared.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for SUPER_ADMIN to manage SaaS payment gateway configuration.
 * This configuration is used to receive payments from workshops (oficinas).
 */
@RestController
@RequestMapping("/api/saas/configuracoes/gateway")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SaaS - Gateway de Pagamento", description = "Configuração do gateway para receber pagamentos das oficinas")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class SaasGatewayController {

    private final ConfiguracaoGatewayService configuracaoGatewayService;

    /**
     * List all gateway configurations.
     */
    @GetMapping
    @Operation(summary = "Listar configurações de gateway")
    public ResponseEntity<List<ConfiguracaoGatewayDTO>> listar() {
        log.debug("GET /api/saas/configuracoes/gateway");
        List<ConfiguracaoGatewayDTO> configs = configuracaoGatewayService.listarTodos();
        return ResponseEntity.ok(configs);
    }

    /**
     * Get Mercado Pago configuration.
     */
    @GetMapping("/mercadopago")
    @Operation(summary = "Obter configuração do Mercado Pago")
    public ResponseEntity<ConfiguracaoGatewayDTO> getMercadoPago() {
        log.debug("GET /api/saas/configuracoes/gateway/mercadopago");
        ConfiguracaoGatewayDTO config = configuracaoGatewayService.getMercadoPago();
        return ResponseEntity.ok(config);
    }

    /**
     * Save or update Mercado Pago configuration.
     */
    @PostMapping("/mercadopago")
    @Operation(summary = "Salvar configuração do Mercado Pago")
    public ResponseEntity<ConfiguracaoGatewayDTO> salvarMercadoPago(
            @Valid @RequestBody ConfiguracaoGatewayRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/saas/configuracoes/gateway/mercadopago - User: {}", userDetails.getUsername());

        // Force tipo to MERCADO_PAGO
        ConfiguracaoGatewayRequestDTO requestWithTipo = new ConfiguracaoGatewayRequestDTO(
            TipoGateway.MERCADO_PAGO,
            request.ativo(),
            request.sandbox(),
            request.accessToken(),
            request.publicKey(),
            request.webhookSecret()
        );

        ConfiguracaoGatewayDTO config = configuracaoGatewayService.salvar(
            requestWithTipo,
            userDetails.getUsuario().getId()
        );

        return ResponseEntity.ok(config);
    }

    /**
     * Validate Mercado Pago credentials.
     */
    @PostMapping("/mercadopago/validar")
    @Operation(summary = "Validar credenciais do Mercado Pago")
    public ResponseEntity<ConfiguracaoGatewayDTO> validarMercadoPago(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("POST /api/saas/configuracoes/gateway/mercadopago/validar - User: {}", userDetails.getUsername());

        ConfiguracaoGatewayDTO config = configuracaoGatewayService.validarCredenciais(
            TipoGateway.MERCADO_PAGO,
            userDetails.getUsuario().getId()
        );

        return ResponseEntity.ok(config);
    }

    /**
     * Check if Mercado Pago is configured and ready to receive payments.
     */
    @GetMapping("/mercadopago/status")
    @Operation(summary = "Verificar status do Mercado Pago")
    public ResponseEntity<Boolean> statusMercadoPago() {
        boolean configurado = configuracaoGatewayService.isMercadoPagoConfigurado();
        return ResponseEntity.ok(configurado);
    }
}
