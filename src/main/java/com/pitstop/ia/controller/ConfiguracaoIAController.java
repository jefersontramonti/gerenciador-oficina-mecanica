package com.pitstop.ia.controller;

import com.pitstop.ia.dto.AtualizarApiKeyRequest;
import com.pitstop.ia.dto.ConfiguracaoIARequest;
import com.pitstop.ia.dto.ConfiguracaoIAResponse;
import com.pitstop.ia.service.ConfiguracaoIAService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para gerenciar configurações de IA por oficina.
 */
@RestController
@RequestMapping("/api/configuracao-ia")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Configuração IA", description = "Gerenciamento de configurações de IA (Anthropic)")
@SecurityRequirement(name = "bearer-jwt")
public class ConfiguracaoIAController {

    private final ConfiguracaoIAService service;

    /**
     * Busca a configuração de IA da oficina atual.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar configuração", description = "Retorna configuração de IA da oficina atual")
    public ResponseEntity<ConfiguracaoIAResponse> buscarConfiguracao() {
        log.debug("GET /api/configuracao-ia - Buscando configuração de IA");
        ConfiguracaoIAResponse response = service.buscarConfiguracao();
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza configurações de IA.
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar configuração", description = "Atualiza configurações de IA da oficina")
    public ResponseEntity<ConfiguracaoIAResponse> atualizarConfiguracao(
            @Valid @RequestBody ConfiguracaoIARequest request
    ) {
        log.info("PUT /api/configuracao-ia - Atualizando configuração de IA");
        ConfiguracaoIAResponse response = service.atualizarConfiguracao(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza a API key da Anthropic.
     */
    @PutMapping("/api-key")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Operation(summary = "Atualizar API key", description = "Atualiza a API key da Anthropic (criptografada)")
    public ResponseEntity<ConfiguracaoIAResponse> atualizarApiKey(
            @Valid @RequestBody AtualizarApiKeyRequest request
    ) {
        log.info("PUT /api/configuracao-ia/api-key - Atualizando API key");
        ConfiguracaoIAResponse response = service.atualizarApiKey(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a API key configurada.
     */
    @DeleteMapping("/api-key")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Operation(summary = "Remover API key", description = "Remove a API key configurada")
    public ResponseEntity<ConfiguracaoIAResponse> removerApiKey() {
        log.info("DELETE /api/configuracao-ia/api-key - Removendo API key");
        ConfiguracaoIAResponse response = service.removerApiKey();
        return ResponseEntity.ok(response);
    }

    /**
     * Testa a conexão com a API da Anthropic.
     */
    @PostMapping("/testar-conexao")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Testar conexão", description = "Testa a conexão com a API da Anthropic")
    public ResponseEntity<Map<String, Object>> testarConexao() {
        log.info("POST /api/configuracao-ia/testar-conexao - Testando conexão");
        boolean conexaoOk = service.testarConexao();
        return ResponseEntity.ok(Map.of(
                "sucesso", conexaoOk,
                "mensagem", conexaoOk ? "Conexão com Anthropic estabelecida com sucesso" : "Falha na conexão. Verifique a API key."
        ));
    }
}
