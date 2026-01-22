package com.pitstop.saas.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.user.UserClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.user.User;
import com.pitstop.saas.domain.ConfiguracaoGateway;
import com.pitstop.saas.domain.TipoGateway;
import com.pitstop.saas.dto.ConfiguracaoGatewayDTO;
import com.pitstop.saas.dto.ConfiguracaoGatewayRequestDTO;
import com.pitstop.saas.repository.SaasConfigGatewayRepository;
import com.pitstop.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing payment gateway configuration.
 * Only SUPER_ADMIN can access these methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoGatewayService {

    private final SaasConfigGatewayRepository repository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Get all gateway configurations.
     */
    @Transactional(readOnly = true)
    public List<ConfiguracaoGatewayDTO> listarTodos() {
        return repository.findAll().stream()
            .map(ConfiguracaoGatewayDTO::fromEntity)
            .toList();
    }

    /**
     * Get configuration for a specific gateway type.
     */
    @Transactional(readOnly = true)
    public ConfiguracaoGatewayDTO getByTipo(TipoGateway tipo) {
        return repository.findByTipo(tipo)
            .map(ConfiguracaoGatewayDTO::fromEntity)
            .orElse(null);
    }

    /**
     * Get or create configuration for Mercado Pago.
     */
    @Transactional(readOnly = true)
    public ConfiguracaoGatewayDTO getMercadoPago() {
        Optional<ConfiguracaoGateway> config = repository.findByTipo(TipoGateway.MERCADO_PAGO);

        if (config.isPresent()) {
            return ConfiguracaoGatewayDTO.fromEntity(config.get());
        }

        // Return empty config
        return new ConfiguracaoGatewayDTO(
            null,
            TipoGateway.MERCADO_PAGO,
            TipoGateway.MERCADO_PAGO.getNome(),
            false,
            true,
            null,
            null,
            false,
            baseUrl + "/api/webhooks/mercadopago/fatura",
            null,
            null,
            null,
            false,
            null
        );
    }

    /**
     * Save or update gateway configuration.
     */
    @Transactional
    public ConfiguracaoGatewayDTO salvar(ConfiguracaoGatewayRequestDTO request, UUID usuarioId) {
        ConfiguracaoGateway config = repository.findByTipo(request.tipo())
            .orElse(ConfiguracaoGateway.builder()
                .tipo(request.tipo())
                .sandbox(true)
                .ativo(false)
                .build());

        // Update fields only if provided (don't overwrite with null)
        if (request.ativo() != null) {
            config.setAtivo(request.ativo());
        }
        if (request.sandbox() != null) {
            config.setSandbox(request.sandbox());
        }
        if (request.hasAccessToken()) {
            config.setAccessToken(request.accessToken());
        }
        if (request.hasPublicKey()) {
            config.setPublicKey(request.publicKey());
        }
        if (request.hasWebhookSecret()) {
            config.setWebhookSecret(request.webhookSecret());
        }

        // Set webhook URL
        config.setWebhookUrl(baseUrl + "/api/webhooks/mercadopago/fatura");
        config.setUpdatedBy(usuarioId);

        config = repository.save(config);

        log.info("Gateway {} configuration updated by user {}", request.tipo(), usuarioId);

        return ConfiguracaoGatewayDTO.fromEntity(config);
    }

    /**
     * Validate gateway credentials by making a test API call.
     */
    @Transactional
    public ConfiguracaoGatewayDTO validarCredenciais(TipoGateway tipo, UUID usuarioId) {
        ConfiguracaoGateway config = repository.findByTipo(tipo)
            .orElseThrow(() -> new BusinessException("Configuração não encontrada para: " + tipo));

        if (config.getAccessToken() == null || config.getAccessToken().isBlank()) {
            config.marcarValidacao(false, "Access Token não configurado");
            config.setUpdatedBy(usuarioId);
            repository.save(config);
            return ConfiguracaoGatewayDTO.fromEntity(config);
        }

        try {
            // Test credentials by getting user info
            MercadoPagoConfig.setAccessToken(config.getAccessToken());
            UserClient userClient = new UserClient();
            User user = userClient.get();

            String mensagem = String.format("Conectado como: %s (%s)",
                user.getNickname(), user.getEmail());

            config.marcarValidacao(true, mensagem);
            log.info("Mercado Pago credentials validated successfully: {}", user.getEmail());

        } catch (MPApiException e) {
            String mensagem = "Erro de API: " + e.getMessage();
            if (e.getStatusCode() == 401) {
                mensagem = "Access Token inválido ou expirado";
            }
            config.marcarValidacao(false, mensagem);
            log.warn("Mercado Pago validation failed: {}", e.getMessage());

        } catch (MPException e) {
            config.marcarValidacao(false, "Erro de conexão: " + e.getMessage());
            log.error("Mercado Pago connection error", e);
        }

        config.setUpdatedBy(usuarioId);
        repository.save(config);

        return ConfiguracaoGatewayDTO.fromEntity(config);
    }

    /**
     * Get active Mercado Pago configuration for payment processing.
     * Used by FaturaMercadoPagoService.
     */
    @Transactional(readOnly = true)
    public Optional<ConfiguracaoGateway> getConfiguracaoAtiva(TipoGateway tipo) {
        return repository.findByTipoAndAtivoTrue(tipo);
    }

    /**
     * Check if Mercado Pago is configured and active.
     */
    public boolean isMercadoPagoConfigurado() {
        return repository.findByTipoAndAtivoTrue(TipoGateway.MERCADO_PAGO)
            .map(ConfiguracaoGateway::isConfigurado)
            .orElse(false);
    }
}
