package com.pitstop.financeiro.controller;

import com.pitstop.financeiro.domain.ConfiguracaoGateway;
import com.pitstop.financeiro.domain.TipoGateway;
import com.pitstop.financeiro.dto.ConfiguracaoGatewayDTO;
import com.pitstop.financeiro.dto.ConfiguracaoGatewayRequestDTO;
import com.pitstop.financeiro.repository.ConfiguracaoGatewayRepository;
import com.pitstop.financeiro.service.MercadoPagoService;
import com.pitstop.shared.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller para gerenciar configurações de gateways de pagamento.
 */
@Slf4j
@RestController
@RequestMapping("/api/configuracoes/gateways")
@RequiredArgsConstructor
@Tag(name = "Configuração de Gateways", description = "Gerenciamento de gateways de pagamento")
public class ConfiguracaoGatewayController {

    private final ConfiguracaoGatewayRepository repository;
    private final MercadoPagoService mercadoPagoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar configurações de gateways da oficina")
    public ResponseEntity<List<ConfiguracaoGatewayDTO>> listar() {
        UUID oficinaId = TenantContext.getTenantId();
        List<ConfiguracaoGateway> configs = repository.findByOficinaId(oficinaId);

        List<ConfiguracaoGatewayDTO> dtos = configs.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar configuração por ID")
    public ResponseEntity<ConfiguracaoGatewayDTO> buscarPorId(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        return ResponseEntity.ok(toDTO(config));
    }

    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar configuração por tipo de gateway")
    public ResponseEntity<ConfiguracaoGatewayDTO> buscarPorTipo(@PathVariable TipoGateway tipo) {
        UUID oficinaId = TenantContext.getTenantId();

        return repository.findByOficinaIdAndTipoGateway(oficinaId, tipo)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public-key")
    @Operation(summary = "Obter Public Key do gateway ativo para uso no frontend (Checkout Bricks)")
    public ResponseEntity<java.util.Map<String, String>> getPublicKey() {
        UUID oficinaId = TenantContext.getTenantId();

        return repository.findGatewayAtivo(oficinaId, TipoGateway.MERCADO_PAGO)
            .filter(config -> config.getPublicKey() != null && !config.getPublicKey().isBlank())
            .map(config -> {
                java.util.Map<String, String> response = new java.util.HashMap<>();
                response.put("publicKey", config.getPublicKey());
                response.put("ambiente", config.getAmbiente().name());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Criar nova configuração de gateway")
    public ResponseEntity<ConfiguracaoGatewayDTO> criar(
            @Valid @RequestBody ConfiguracaoGatewayRequestDTO request) {

        UUID oficinaId = TenantContext.getTenantId();

        // Verificar se já existe configuração para este gateway
        if (repository.existsByOficinaIdAndTipoGateway(oficinaId, request.getTipoGateway())) {
            throw new IllegalArgumentException("Já existe configuração para este gateway. Use PUT para atualizar.");
        }

        ConfiguracaoGateway config = ConfiguracaoGateway.builder()
            .tipoGateway(request.getTipoGateway())
            .ambiente(request.getAmbiente())
            .accessToken(request.getAccessToken())
            .publicKey(request.getPublicKey())
            .clientId(request.getClientId())
            .clientSecret(request.getClientSecret())
            .ativo(request.getAtivo() != null ? request.getAtivo() : false)
            .padrao(request.getPadrao() != null ? request.getPadrao() : false)
            .taxaPercentual(request.getTaxaPercentual())
            .taxaFixa(request.getTaxaFixa())
            .observacoes(request.getObservacoes())
            .build();

        // Se for o primeiro ou definido como padrão, desmarcar outros
        if (Boolean.TRUE.equals(config.getPadrao())) {
            desmarcarOutrosPadrao(oficinaId);
        }

        config = repository.save(config);

        log.info("Configuração de gateway {} criada para oficina {}", request.getTipoGateway(), oficinaId);

        return ResponseEntity.ok(toDTO(config));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar configuração de gateway")
    public ResponseEntity<ConfiguracaoGatewayDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ConfiguracaoGatewayRequestDTO request) {

        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        config.setAmbiente(request.getAmbiente());
        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            config.setAccessToken(request.getAccessToken());
        }
        if (request.getPublicKey() != null) {
            config.setPublicKey(request.getPublicKey());
        }
        if (request.getClientId() != null) {
            config.setClientId(request.getClientId());
        }
        if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
            config.setClientSecret(request.getClientSecret());
        }
        if (request.getAtivo() != null) {
            config.setAtivo(request.getAtivo());
        }
        if (request.getPadrao() != null) {
            if (Boolean.TRUE.equals(request.getPadrao())) {
                desmarcarOutrosPadrao(config.getOficina().getId());
            }
            config.setPadrao(request.getPadrao());
        }
        if (request.getTaxaPercentual() != null) {
            config.setTaxaPercentual(request.getTaxaPercentual());
        }
        if (request.getTaxaFixa() != null) {
            config.setTaxaFixa(request.getTaxaFixa());
        }
        if (request.getObservacoes() != null) {
            config.setObservacoes(request.getObservacoes());
        }

        config = repository.save(config);

        log.info("Configuração de gateway {} atualizada", id);

        return ResponseEntity.ok(toDTO(config));
    }

    @PostMapping("/{id}/validar")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Validar credenciais do gateway")
    public ResponseEntity<ConfiguracaoGatewayDTO> validar(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        boolean valido = false;

        if (config.getTipoGateway() == TipoGateway.MERCADO_PAGO) {
            valido = mercadoPagoService.validarCredenciais(
                config.getOficina().getId(),
                config.getAccessToken()
            );
        }

        config.setStatusValidacao(valido ? "VALIDO" : "INVALIDO");
        config.setDataUltimaValidacao(LocalDateTime.now());
        config = repository.save(config);

        log.info("Validação de gateway {} - Resultado: {}", id, valido ? "válido" : "inválido");

        return ResponseEntity.ok(toDTO(config));
    }

    @PostMapping("/{id}/ativar")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Ativar gateway")
    public ResponseEntity<ConfiguracaoGatewayDTO> ativar(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        if (!config.isConfiguracaoCompleta()) {
            throw new IllegalArgumentException("Configure as credenciais antes de ativar o gateway");
        }

        config.setAtivo(true);
        config = repository.save(config);

        return ResponseEntity.ok(toDTO(config));
    }

    @PostMapping("/{id}/desativar")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Desativar gateway")
    public ResponseEntity<ConfiguracaoGatewayDTO> desativar(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        config.setAtivo(false);
        config = repository.save(config);

        return ResponseEntity.ok(toDTO(config));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Remover configuração de gateway")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoGateway config = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new IllegalArgumentException("Configuração não encontrada"));

        repository.delete(config);

        log.info("Configuração de gateway {} removida", id);

        return ResponseEntity.noContent().build();
    }

    // ========== Métodos auxiliares ==========

    private void desmarcarOutrosPadrao(UUID oficinaId) {
        repository.findByOficinaIdAndPadraoTrue(oficinaId)
            .ifPresent(c -> {
                c.setPadrao(false);
                repository.save(c);
            });
    }

    private ConfiguracaoGatewayDTO toDTO(ConfiguracaoGateway config) {
        return ConfiguracaoGatewayDTO.builder()
            .id(config.getId())
            .tipoGateway(config.getTipoGateway())
            .tipoGatewayDescricao(config.getTipoGateway().getDescricao())
            .ambiente(config.getAmbiente())
            .ambienteDescricao(config.getAmbiente().getDescricao())
            .ativo(config.getAtivo())
            .padrao(config.getPadrao())
            .configurado(config.isConfiguracaoCompleta())
            .taxaPercentual(config.getTaxaPercentual())
            .taxaFixa(config.getTaxaFixa())
            .webhookUrl(config.getWebhookUrl())
            .statusValidacao(config.getStatusValidacao())
            .dataUltimaValidacao(config.getDataUltimaValidacao())
            .observacoes(config.getObservacoes())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .build();
    }
}
