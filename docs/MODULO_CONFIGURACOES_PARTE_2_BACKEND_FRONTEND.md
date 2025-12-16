# 📋 Especificação Técnica Completa: Módulo de Configurações - PARTE 2

**Versão:** 1.0.0
**Data:** 01/12/2025

---

## 5. Backend - Implementação Java/Spring Boot

### 5.1 Repositories

#### 5.1.1 PreferenciaUsuarioRepository.java

```java
package com.pitstop.configuracao.repository;

import com.pitstop.configuracao.domain.PreferenciaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreferenciaUsuarioRepository extends JpaRepository<PreferenciaUsuario, UUID> {

    /**
     * Busca preferências por ID do usuário.
     */
    Optional<PreferenciaUsuario> findByUsuarioId(UUID usuarioId);

    /**
     * Verifica se existe preferência para um usuário.
     */
    boolean existsByUsuarioId(UUID usuarioId);

    /**
     * Deleta preferências de um usuário.
     */
    void deleteByUsuarioId(UUID usuarioId);
}
```

#### 5.1.2 ConfiguracaoSistemaRepository.java

```java
package com.pitstop.configuracao.repository;

import com.pitstop.configuracao.domain.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, UUID> {

    /**
     * Busca a configuração singleton do sistema.
     * Deve existir apenas 1 registro.
     */
    @Query("SELECT c FROM ConfiguracaoSistema c ORDER BY c.createdAt LIMIT 1")
    Optional<ConfiguracaoSistema> findSingleton();

    /**
     * Conta quantos registros existem (deve ser sempre 1).
     */
    long count();
}
```

#### 5.1.3 IntegracaoExternaRepository.java

```java
package com.pitstop.configuracao.repository;

import com.pitstop.configuracao.domain.IntegracaoExterna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegracaoExternaRepository extends JpaRepository<IntegracaoExterna, UUID> {

    /**
     * Busca integração por tipo.
     */
    Optional<IntegracaoExterna> findByTipo(IntegracaoExterna.TipoIntegracao tipo);

    /**
     * Busca integração por tipo e provedor.
     */
    Optional<IntegracaoExterna> findByTipoAndProvedor(
        IntegracaoExterna.TipoIntegracao tipo,
        String provedor
    );

    /**
     * Lista todas as integrações ativas.
     */
    List<IntegracaoExterna> findByAtiva(Boolean ativa);

    /**
     * Lista todas as integrações de um tipo.
     */
    List<IntegracaoExterna> findByTipo(IntegracaoExterna.TipoIntegracao tipo);

    /**
     * Verifica se existe integração ativa de um tipo.
     */
    boolean existsByTipoAndAtiva(IntegracaoExterna.TipoIntegracao tipo, Boolean ativa);
}
```

#### 5.1.4 LogAuditoriaRepository.java

```java
package com.pitstop.configuracao.repository;

import com.pitstop.configuracao.domain.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    /**
     * Busca logs por usuário.
     */
    Page<LogAuditoria> findByUsuarioId(UUID usuarioId, Pageable pageable);

    /**
     * Busca logs por ação.
     */
    Page<LogAuditoria> findByAcao(String acao, Pageable pageable);

    /**
     * Busca logs por entidade.
     */
    Page<LogAuditoria> findByEntidade(String entidade, Pageable pageable);

    /**
     * Busca logs por período.
     */
    @Query("SELECT l FROM LogAuditoria l WHERE l.createdAt BETWEEN :inicio AND :fim ORDER BY l.createdAt DESC")
    Page<LogAuditoria> findByPeriodo(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        Pageable pageable
    );

    /**
     * Deleta logs antigos (para limpeza automática).
     */
    @Query("DELETE FROM LogAuditoria l WHERE l.createdAt < :dataLimite")
    void deleteByCreatedAtBefore(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Conta logs por usuário em um período.
     */
    @Query("SELECT COUNT(l) FROM LogAuditoria l WHERE l.usuarioId = :usuarioId AND l.createdAt >= :dataInicio")
    long countByUsuarioIdAndCreatedAtAfter(
        @Param("usuarioId") UUID usuarioId,
        @Param("dataInicio") LocalDateTime dataInicio
    );
}
```

---

### 5.2 Services

#### 5.2.1 PreferenciaUsuarioService.java

```java
package com.pitstop.configuracao.service;

import com.pitstop.configuracao.domain.PreferenciaUsuario;
import com.pitstop.configuracao.dto.PreferenciaUsuarioDTO;
import com.pitstop.configuracao.mapper.PreferenciaUsuarioMapper;
import com.pitstop.configuracao.repository.PreferenciaUsuarioRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service para gerenciar preferências de usuário.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreferenciaUsuarioService {

    private final PreferenciaUsuarioRepository preferenciaRepository;
    private final PreferenciaUsuarioMapper preferenciaMapper;

    /**
     * Busca ou cria preferências de um usuário.
     * Se não existir, cria com valores padrão.
     */
    @Transactional(readOnly = true)
    public PreferenciaUsuarioDTO buscarOuCriar(UUID usuarioId) {
        log.debug("Buscando preferências do usuário: {}", usuarioId);

        PreferenciaUsuario preferencia = preferenciaRepository.findByUsuarioId(usuarioId)
            .orElseGet(() -> criarPreferenciasPadrao(usuarioId));

        return preferenciaMapper.toDTO(preferencia);
    }

    /**
     * Atualiza preferências de um usuário.
     */
    @Transactional
    public PreferenciaUsuarioDTO atualizar(UUID usuarioId, PreferenciaUsuarioDTO dto) {
        log.info("Atualizando preferências do usuário: {}", usuarioId);

        PreferenciaUsuario preferencia = preferenciaRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Preferências não encontradas para usuário: " + usuarioId
            ));

        // Atualiza campos
        preferencia.setTema(dto.tema());
        preferencia.setIdioma(dto.idioma());
        preferencia.setDensidade(dto.densidade());
        preferencia.setDashboardStyle(dto.dashboardStyle());
        preferencia.setNotifEmail(dto.notifEmail());
        preferencia.setNotifPush(dto.notifPush());
        preferencia.setNotifWhatsApp(dto.notifWhatsApp());

        PreferenciaUsuario atualizada = preferenciaRepository.save(preferencia);

        log.info("Preferências atualizadas para usuário: {}", usuarioId);
        return preferenciaMapper.toDTO(atualizada);
    }

    /**
     * Restaura preferências padrão de um usuário.
     */
    @Transactional
    public PreferenciaUsuarioDTO restaurarPadrao(UUID usuarioId) {
        log.info("Restaurando preferências padrão do usuário: {}", usuarioId);

        // Deleta preferências existentes
        preferenciaRepository.deleteByUsuarioId(usuarioId);

        // Cria novas com valores padrão
        PreferenciaUsuario preferencia = criarPreferenciasPadrao(usuarioId);

        log.info("Preferências restauradas para usuário: {}", usuarioId);
        return preferenciaMapper.toDTO(preferencia);
    }

    /**
     * Cria preferências com valores padrão.
     */
    private PreferenciaUsuario criarPreferenciasPadrao(UUID usuarioId) {
        PreferenciaUsuario preferencia = PreferenciaUsuario.builder()
            .usuarioId(usuarioId)
            .build(); // Usa valores @Builder.Default

        return preferenciaRepository.save(preferencia);
    }
}
```

#### 5.2.2 ConfiguracaoSistemaService.java

```java
package com.pitstop.configuracao.service;

import com.pitstop.configuracao.domain.ConfiguracaoSistema;
import com.pitstop.configuracao.dto.*;
import com.pitstop.configuracao.mapper.ConfiguracaoSistemaMapper;
import com.pitstop.configuracao.repository.ConfiguracaoSistemaRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service para gerenciar configurações globais do sistema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoSistemaService {

    private final ConfiguracaoSistemaRepository configuracaoRepository;
    private final ConfiguracaoSistemaMapper configuracaoMapper;
    private final AuditoriaService auditoriaService;

    /**
     * Busca a configuração singleton do sistema.
     * Se não existir, cria uma com valores padrão.
     */
    @Transactional(readOnly = true)
    public ConfiguracaoSistemaDTO buscarConfiguracao() {
        log.debug("Buscando configuração do sistema");

        ConfiguracaoSistema configuracao = configuracaoRepository.findSingleton()
            .orElseGet(this::criarConfiguracaoPadrao);

        return configuracaoMapper.toDTO(configuracao);
    }

    /**
     * Atualiza configurações de Ordem de Serviço.
     */
    @Transactional
    public ConfiguracaoOSDTO atualizarOS(ConfiguracaoOSDTO dto) {
        log.info("Atualizando configurações de Ordem de Serviço");

        ConfiguracaoSistema configuracao = buscarSingleton();
        var dadosAntes = configuracao.getOrdemServico();

        // Atualiza
        configuracao.setOrdemServico(configuracaoMapper.toEntity(dto));
        ConfiguracaoSistema atualizada = configuracaoRepository.save(configuracao);

        // Auditoria
        auditoriaService.registrarAlteracao(
            "UPDATE_CONFIG_OS",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            dadosAntes,
            atualizada.getOrdemServico()
        );

        log.info("Configurações de OS atualizadas");
        return configuracaoMapper.osToDTO(atualizada.getOrdemServico());
    }

    /**
     * Atualiza configurações de Estoque.
     */
    @Transactional
    public ConfiguracaoEstoqueDTO atualizarEstoque(ConfiguracaoEstoqueDTO dto) {
        log.info("Atualizando configurações de Estoque");

        ConfiguracaoSistema configuracao = buscarSingleton();
        var dadosAntes = configuracao.getEstoque();

        configuracao.setEstoque(configuracaoMapper.toEntity(dto));
        ConfiguracaoSistema atualizada = configuracaoRepository.save(configuracao);

        auditoriaService.registrarAlteracao(
            "UPDATE_CONFIG_ESTOQUE",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            dadosAntes,
            atualizada.getEstoque()
        );

        log.info("Configurações de Estoque atualizadas");
        return configuracaoMapper.estoqueToDTO(atualizada.getEstoque());
    }

    /**
     * Atualiza configurações Financeiras.
     */
    @Transactional
    public ConfiguracaoFinanceiroDTO atualizarFinanceiro(ConfiguracaoFinanceiroDTO dto) {
        log.info("Atualizando configurações Financeiras");

        ConfiguracaoSistema configuracao = buscarSingleton();
        var dadosAntes = configuracao.getFinanceiro();

        configuracao.setFinanceiro(configuracaoMapper.toEntity(dto));
        ConfiguracaoSistema atualizada = configuracaoRepository.save(configuracao);

        auditoriaService.registrarAlteracao(
            "UPDATE_CONFIG_FINANCEIRO",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            dadosAntes,
            atualizada.getFinanceiro()
        );

        log.info("Configurações Financeiras atualizadas");
        return configuracaoMapper.financeiroToDTO(atualizada.getFinanceiro());
    }

    /**
     * Atualiza configurações de Notificação.
     */
    @Transactional
    public ConfiguracaoNotificacaoDTO atualizarNotificacao(ConfiguracaoNotificacaoDTO dto) {
        log.info("Atualizando configurações de Notificação");

        ConfiguracaoSistema configuracao = buscarSingleton();
        var dadosAntes = configuracao.getNotificacao();

        configuracao.setNotificacao(configuracaoMapper.toEntity(dto));
        ConfiguracaoSistema atualizada = configuracaoRepository.save(configuracao);

        auditoriaService.registrarAlteracao(
            "UPDATE_CONFIG_NOTIFICACAO",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            dadosAntes,
            atualizada.getNotificacao()
        );

        log.info("Configurações de Notificação atualizadas");
        return configuracaoMapper.notificacaoToDTO(atualizada.getNotificacao());
    }

    /**
     * Atualiza configurações de Segurança.
     */
    @Transactional
    public ConfiguracaoSegurancaDTO atualizarSeguranca(ConfiguracaoSegurancaDTO dto) {
        log.info("Atualizando configurações de Segurança");

        ConfiguracaoSistema configuracao = buscarSingleton();
        var dadosAntes = configuracao.getSeguranca();

        configuracao.setSeguranca(configuracaoMapper.toEntity(dto));
        ConfiguracaoSistema atualizada = configuracaoRepository.save(configuracao);

        auditoriaService.registrarAlteracao(
            "UPDATE_CONFIG_SEGURANCA",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            dadosAntes,
            atualizada.getSeguranca()
        );

        log.info("Configurações de Segurança atualizadas");
        return configuracaoMapper.segurancaToDTO(atualizada.getSeguranca());
    }

    /**
     * Atualiza configurações de Sistema.
     */
    @Transactional
    public ConfiguracaoSistemaTecnicoDTO atualizarSistema(ConfiguracaoSistemaTecnicoDTO dto) {
        log.info("Atualizando configurações de Sistema");

        ConfiguracaoSistema configuracao = buscarSingleton();
        var dadosAntes = configuracao.getSistema();

        configuracao.setSistema(configuracaoMapper.toEntity(dto));
        ConfiguracaoSistema atualizada = configuracaoRepository.save(configuracao);

        auditoriaService.registrarAlteracao(
            "UPDATE_CONFIG_SISTEMA",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            dadosAntes,
            atualizada.getSistema()
        );

        log.info("Configurações de Sistema atualizadas");
        return configuracaoMapper.sistemaToDTO(atualizada.getSistema());
    }

    /**
     * Restaura configurações padrão (todas as seções).
     */
    @Transactional
    public ConfiguracaoSistemaDTO restaurarPadrao() {
        log.warn("Restaurando configurações padrão do sistema");

        ConfiguracaoSistema configuracao = buscarSingleton();

        // Cria nova configuração com valores padrão
        ConfiguracaoSistema novaPadrao = ConfiguracaoSistema.builder().build();

        // Mantém ID e timestamps
        novaPadrao.setId(configuracao.getId());
        novaPadrao.setCreatedAt(configuracao.getCreatedAt());

        ConfiguracaoSistema atualizada = configuracaoRepository.save(novaPadrao);

        auditoriaService.registrarAlteracao(
            "RESTORE_DEFAULT_CONFIG",
            "CONFIGURACAO_SISTEMA",
            configuracao.getId(),
            configuracao,
            atualizada
        );

        log.info("Configurações padrão restauradas");
        return configuracaoMapper.toDTO(atualizada);
    }

    // ===== Private Methods =====

    private ConfiguracaoSistema buscarSingleton() {
        return configuracaoRepository.findSingleton()
            .orElseThrow(() -> new ResourceNotFoundException(
                "Configuração do sistema não encontrada"
            ));
    }

    private ConfiguracaoSistema criarConfiguracaoPadrao() {
        log.info("Criando configuração padrão do sistema (singleton)");

        // Verifica se já existe (evitar duplicação)
        long count = configuracaoRepository.count();
        if (count > 0) {
            throw new IllegalStateException(
                "Já existe uma configuração do sistema. Violação de singleton."
            );
        }

        ConfiguracaoSistema configuracao = ConfiguracaoSistema.builder().build();
        return configuracaoRepository.save(configuracao);
    }
}
```

#### 5.2.3 IntegracaoExternaService.java

```java
package com.pitstop.configuracao.service;

import com.pitstop.configuracao.domain.IntegracaoExterna;
import com.pitstop.configuracao.dto.IntegracaoExternaDTO;
import com.pitstop.configuracao.dto.TesteIntegracaoRequest;
import com.pitstop.configuracao.dto.TesteIntegracaoResponse;
import com.pitstop.configuracao.mapper.IntegracaoExternaMapper;
import com.pitstop.configuracao.repository.IntegracaoExternaRepository;
import com.pitstop.configuracao.service.integracao.*;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service para gerenciar integrações externas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegracaoExternaService {

    private final IntegracaoExternaRepository integracaoRepository;
    private final IntegracaoExternaMapper integracaoMapper;
    private final EmailIntegrationService emailService;
    private final WhatsAppIntegrationService whatsappService;
    private final TelegramIntegrationService telegramService;
    private final MercadoPagoIntegrationService mercadoPagoService;
    private final AuditoriaService auditoriaService;

    /**
     * Lista todas as integrações.
     */
    @Transactional(readOnly = true)
    public List<IntegracaoExternaDTO> listarTodas() {
        return integracaoRepository.findAll()
            .stream()
            .map(integracaoMapper::toDTO)
            .toList();
    }

    /**
     * Busca integração por ID.
     */
    @Transactional(readOnly = true)
    public IntegracaoExternaDTO buscarPorId(UUID id) {
        IntegracaoExterna integracao = integracaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Integração não encontrada com ID: " + id
            ));

        return integracaoMapper.toDTO(integracao);
    }

    /**
     * Busca integração por tipo.
     */
    @Transactional(readOnly = true)
    public IntegracaoExternaDTO buscarPorTipo(IntegracaoExterna.TipoIntegracao tipo) {
        IntegracaoExterna integracao = integracaoRepository.findByTipo(tipo)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Integração não encontrada para tipo: " + tipo
            ));

        return integracaoMapper.toDTO(integracao);
    }

    /**
     * Cria ou atualiza uma integração.
     */
    @Transactional
    public IntegracaoExternaDTO salvarIntegracao(IntegracaoExternaDTO dto) {
        log.info("Salvando integração do tipo: {} - provedor: {}", dto.tipo(), dto.provedor());

        IntegracaoExterna integracao;

        if (dto.id() != null) {
            // Atualizar existente
            integracao = integracaoRepository.findById(dto.id())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Integração não encontrada com ID: " + dto.id()
                ));

            var dadosAntes = integracao.getConfiguracao();

            // Atualiza campos
            integracao.setAtiva(dto.ativa());
            integracao.setProvedor(dto.provedor());
            integracao.setConfiguracao(dto.configuracao()); // TODO: Criptografar

            auditoriaService.registrarAlteracao(
                "UPDATE_INTEGRACAO",
                "INTEGRACAO_EXTERNA",
                integracao.getId(),
                dadosAntes,
                integracao.getConfiguracao()
            );

        } else {
            // Criar nova
            integracao = integracaoMapper.toEntity(dto);
            // TODO: Criptografar configuracao antes de salvar
        }

        IntegracaoExterna salva = integracaoRepository.save(integracao);

        log.info("Integração salva. ID: {}, Tipo: {}", salva.getId(), salva.getTipo());
        return integracaoMapper.toDTO(salva);
    }

    /**
     * Ativa/Desativa uma integração.
     */
    @Transactional
    public void alternarAtivacao(UUID id) {
        IntegracaoExterna integracao = integracaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Integração não encontrada com ID: " + id
            ));

        boolean novoStatus = !integracao.getAtiva();
        integracao.setAtiva(novoStatus);

        integracaoRepository.save(integracao);

        log.info("Integração {} {}", id, novoStatus ? "ativada" : "desativada");
    }

    /**
     * Testa uma integração (envia mensagem de teste).
     */
    @Transactional
    public TesteIntegracaoResponse testarIntegracao(TesteIntegracaoRequest request) {
        log.info("Testando integração do tipo: {}", request.tipo());

        IntegracaoExterna integracao = integracaoRepository.findByTipo(request.tipo())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Integração não encontrada para tipo: " + request.tipo()
            ));

        TesteIntegracaoResponse response;

        try {
            switch (request.tipo()) {
                case EMAIL -> response = emailService.testarConexao(integracao);
                case WHATSAPP -> response = whatsappService.testarConexao(integracao);
                case TELEGRAM -> response = telegramService.testarConexao(integracao);
                case MERCADOPAGO -> response = mercadoPagoService.testarConexao(integracao);
                default -> throw new IllegalArgumentException("Tipo de integração não suportado");
            }

            // Atualiza status
            integracao.setStatus(
                response.sucesso() ? IntegracaoExterna.StatusIntegracao.OK : IntegracaoExterna.StatusIntegracao.ERROR
            );
            integracao.setUltimaConexao(LocalDateTime.now());
            integracao.setErro(response.sucesso() ? null : response.mensagem());

            integracaoRepository.save(integracao);

            log.info("Teste de integração {} concluído: {}", request.tipo(), response.sucesso());
            return response;

        } catch (Exception e) {
            log.error("Erro ao testar integração {}: {}", request.tipo(), e.getMessage(), e);

            integracao.setStatus(IntegracaoExterna.StatusIntegracao.ERROR);
            integracao.setErro(e.getMessage());
            integracaoRepository.save(integracao);

            return new TesteIntegracaoResponse(
                false,
                "Erro ao testar integração: " + e.getMessage(),
                null
            );
        }
    }

    /**
     * Deleta uma integração.
     */
    @Transactional
    public void deletar(UUID id) {
        log.info("Deletando integração ID: {}", id);

        IntegracaoExterna integracao = integracaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Integração não encontrada com ID: " + id
            ));

        auditoriaService.registrarAlteracao(
            "DELETE_INTEGRACAO",
            "INTEGRACAO_EXTERNA",
            id,
            integracao,
            null
        );

        integracaoRepository.delete(integracao);

        log.info("Integração deletada: {}", id);
    }
}
```

#### 5.2.4 AuditoriaService.java

```java
package com.pitstop.configuracao.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitstop.configuracao.domain.LogAuditoria;
import com.pitstop.configuracao.repository.LogAuditoriaRepository;
import com.pitstop.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service para gerenciar logs de auditoria.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final LogAuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Registra uma ação de auditoria.
     */
    @Transactional
    public void registrar(
        String acao,
        String entidade,
        UUID entidadeId,
        String observacao
    ) {
        registrarAlteracao(acao, entidade, entidadeId, null, null, observacao);
    }

    /**
     * Registra uma alteração em uma entidade.
     */
    @Transactional
    public void registrarAlteracao(
        String acao,
        String entidade,
        UUID entidadeId,
        Object dadosAntes,
        Object dadosDepois
    ) {
        registrarAlteracao(acao, entidade, entidadeId, dadosAntes, dadosDepois, null);
    }

    /**
     * Registra uma alteração completa.
     */
    @Transactional
    public void registrarAlteracao(
        String acao,
        String entidade,
        UUID entidadeId,
        Object dadosAntes,
        Object dadosDepois,
        String observacao
    ) {
        try {
            // Obtém informações do usuário logado
            var usuario = SecurityUtils.getCurrentUser();

            // Obtém informações da requisição HTTP
            var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = requestAttributes != null ? requestAttributes.getRequest() : null;

            String ipAddress = null;
            String userAgent = null;

            if (request != null) {
                ipAddress = obterIpReal(request);
                userAgent = request.getHeader("User-Agent");
            }

            // Converte objetos para Map (JSON)
            Map<String, Object> dadosAntesMap = dadosAntes != null
                ? objectMapper.convertValue(dadosAntes, Map.class)
                : null;

            Map<String, Object> dadosDepoisMap = dadosDepois != null
                ? objectMapper.convertValue(dadosDepois, Map.class)
                : null;

            // Cria log
            LogAuditoria log = LogAuditoria.builder()
                .usuarioId(usuario != null ? usuario.getId() : null)
                .usuarioNome(usuario != null ? usuario.getNome() : "Sistema")
                .acao(acao)
                .entidade(entidade)
                .entidadeId(entidadeId)
                .dadosAntes(dadosAntesMap)
                .dadosDepois(dadosDepoisMap)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .observacao(observacao)
                .build();

            auditoriaRepository.save(log);

            log.debug("Log de auditoria registrado: {} - {} - {}", acao, entidade, entidadeId);

        } catch (Exception e) {
            log.error("Erro ao registrar log de auditoria: {}", e.getMessage(), e);
            // Não propaga erro para não interromper operação principal
        }
    }

    /**
     * Lista logs de auditoria com paginação.
     */
    @Transactional(readOnly = true)
    public Page<LogAuditoria> listar(Pageable pageable) {
        return auditoriaRepository.findAll(pageable);
    }

    /**
     * Busca logs por período.
     */
    @Transactional(readOnly = true)
    public Page<LogAuditoria> buscarPorPeriodo(
        LocalDateTime inicio,
        LocalDateTime fim,
        Pageable pageable
    ) {
        return auditoriaRepository.findByPeriodo(inicio, fim, pageable);
    }

    /**
     * Limpa logs antigos (tarefa agendada).
     */
    @Transactional
    public void limparLogsAntigos(int diasRetencao) {
        log.info("Limpando logs de auditoria com mais de {} dias", diasRetencao);

        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasRetencao);
        auditoriaRepository.deleteByCreatedAtBefore(dataLimite);

        log.info("Logs antigos removidos");
    }

    // ===== Private Methods =====

    /**
     * Obtém o IP real do cliente (considera proxy/load balancer).
     */
    private String obterIpReal(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Se múltiplos IPs (proxy chain), pega o primeiro
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
```

---

### 5.3 Controllers

#### 5.3.1 PreferenciaUsuarioController.java

```java
package com.pitstop.configuracao.controller;

import com.pitstop.configuracao.dto.PreferenciaUsuarioDTO;
import com.pitstop.configuracao.service.PreferenciaUsuarioService;
import com.pitstop.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/configuracoes/preferencias")
@RequiredArgsConstructor
@Tag(name = "Preferências de Usuário", description = "Configurações pessoais do usuário logado")
public class PreferenciaUsuarioController {

    private final PreferenciaUsuarioService preferenciaService;

    /**
     * Busca preferências do usuário logado.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar preferências", description = "Retorna preferências do usuário logado")
    public ResponseEntity<PreferenciaUsuarioDTO> buscarMinhasPreferencias() {
        UUID usuarioId = SecurityUtils.getCurrentUserId();
        PreferenciaUsuarioDTO preferencias = preferenciaService.buscarOuCriar(usuarioId);
        return ResponseEntity.ok(preferencias);
    }

    /**
     * Atualiza preferências do usuário logado.
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar preferências")
    public ResponseEntity<PreferenciaUsuarioDTO> atualizarMinhasPreferencias(
        @Valid @RequestBody PreferenciaUsuarioDTO dto
    ) {
        UUID usuarioId = SecurityUtils.getCurrentUserId();
        PreferenciaUsuarioDTO atualizada = preferenciaService.atualizar(usuarioId, dto);
        return ResponseEntity.ok(atualizada);
    }

    /**
     * Restaura preferências padrão do usuário logado.
     */
    @PostMapping("/restaurar-padrao")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Restaurar preferências padrão")
    public ResponseEntity<PreferenciaUsuarioDTO> restaurarPadrao() {
        UUID usuarioId = SecurityUtils.getCurrentUserId();
        PreferenciaUsuarioDTO restaurada = preferenciaService.restaurarPadrao(usuarioId);
        return ResponseEntity.ok(restaurada);
    }

    /**
     * ADMIN: Busca preferências de outro usuário.
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar preferências de outro usuário (ADMIN)")
    public ResponseEntity<PreferenciaUsuarioDTO> buscarPreferenciasDeUsuario(
        @PathVariable UUID usuarioId
    ) {
        PreferenciaUsuarioDTO preferencias = preferenciaService.buscarOuCriar(usuarioId);
        return ResponseEntity.ok(preferencias);
    }
}
```

#### 5.3.2 ConfiguracaoSistemaController.java

```java
package com.pitstop.configuracao.controller;

import com.pitstop.configuracao.dto.*;
import com.pitstop.configuracao.service.ConfiguracaoSistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracoes/sistema")
@RequiredArgsConstructor
@Tag(name = "Configurações do Sistema", description = "Configurações globais (ADMIN/GERENTE)")
public class ConfiguracaoSistemaController {

    private final ConfiguracaoSistemaService configuracaoService;

    /**
     * Busca todas as configurações do sistema.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar todas as configurações")
    public ResponseEntity<ConfiguracaoSistemaDTO> buscarConfiguracoes() {
        ConfiguracaoSistemaDTO configuracao = configuracaoService.buscarConfiguracao();
        return ResponseEntity.ok(configuracao);
    }

    // ===== Ordem de Serviço =====

    @PutMapping("/ordem-servico")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar configurações de Ordem de Serviço")
    public ResponseEntity<ConfiguracaoOSDTO> atualizarOS(
        @Valid @RequestBody ConfiguracaoOSDTO dto
    ) {
        ConfiguracaoOSDTO atualizada = configuracaoService.atualizarOS(dto);
        return ResponseEntity.ok(atualizada);
    }

    // ===== Estoque =====

    @PutMapping("/estoque")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar configurações de Estoque")
    public ResponseEntity<ConfiguracaoEstoqueDTO> atualizarEstoque(
        @Valid @RequestBody ConfiguracaoEstoqueDTO dto
    ) {
        ConfiguracaoEstoqueDTO atualizada = configuracaoService.atualizarEstoque(dto);
        return ResponseEntity.ok(atualizada);
    }

    // ===== Financeiro =====

    @PutMapping("/financeiro")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar configurações Financeiras")
    public ResponseEntity<ConfiguracaoFinanceiroDTO> atualizarFinanceiro(
        @Valid @RequestBody ConfiguracaoFinanceiroDTO dto
    ) {
        ConfiguracaoFinanceiroDTO atualizada = configuracaoService.atualizarFinanceiro(dto);
        return ResponseEntity.ok(atualizada);
    }

    // ===== Notificação =====

    @PutMapping("/notificacao")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(summary = "Atualizar configurações de Notificação")
    public ResponseEntity<ConfiguracaoNotificacaoDTO> atualizarNotificacao(
        @Valid @RequestBody ConfiguracaoNotificacaoDTO dto
    ) {
        ConfiguracaoNotificacaoDTO atualizada = configuracaoService.atualizarNotificacao(dto);
        return ResponseEntity.ok(atualizada);
    }

    // ===== Segurança =====

    @PutMapping("/seguranca")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar configurações de Segurança (ADMIN)")
    public ResponseEntity<ConfiguracaoSegurancaDTO> atualizarSeguranca(
        @Valid @RequestBody ConfiguracaoSegurancaDTO dto
    ) {
        ConfiguracaoSegurancaDTO atualizada = configuracaoService.atualizarSeguranca(dto);
        return ResponseEntity.ok(atualizada);
    }

    // ===== Sistema =====

    @PutMapping("/tecnico")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar configurações Técnicas (ADMIN)")
    public ResponseEntity<ConfiguracaoSistemaTecnicoDTO> atualizarSistema(
        @Valid @RequestBody ConfiguracaoSistemaTecnicoDTO dto
    ) {
        ConfiguracaoSistemaTecnicoDTO atualizada = configuracaoService.atualizarSistema(dto);
        return ResponseEntity.ok(atualizada);
    }

    // ===== Restaurar Padrão =====

    @PostMapping("/restaurar-padrao")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Restaurar configurações padrão (ADMIN) - CUIDADO!")
    public ResponseEntity<ConfiguracaoSistemaDTO> restaurarPadrao() {
        ConfiguracaoSistemaDTO restaurada = configuracaoService.restaurarPadrao();
        return ResponseEntity.ok(restaurada);
    }
}
```

#### 5.3.3 IntegracaoExternaController.java

```java
package com.pitstop.configuracao.controller;

import com.pitstop.configuracao.domain.IntegracaoExterna;
import com.pitstop.configuracao.dto.IntegracaoExternaDTO;
import com.pitstop.configuracao.dto.TesteIntegracaoRequest;
import com.pitstop.configuracao.dto.TesteIntegracaoResponse;
import com.pitstop.configuracao.service.IntegracaoExternaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/configuracoes/integracoes")
@RequiredArgsConstructor
@Tag(name = "Integrações Externas", description = "Email, WhatsApp, Telegram, Mercado Pago (ADMIN)")
public class IntegracaoExternaController {

    private final IntegracaoExternaService integracaoService;

    /**
     * Lista todas as integrações.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar integrações")
    public ResponseEntity<List<IntegracaoExternaDTO>> listarTodas() {
        List<IntegracaoExternaDTO> integracoes = integracaoService.listarTodas();
        return ResponseEntity.ok(integracoes);
    }

    /**
     * Busca integração por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar integração por ID")
    public ResponseEntity<IntegracaoExternaDTO> buscarPorId(@PathVariable UUID id) {
        IntegracaoExternaDTO integracao = integracaoService.buscarPorId(id);
        return ResponseEntity.ok(integracao);
    }

    /**
     * Busca integração por tipo.
     */
    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar integração por tipo")
    public ResponseEntity<IntegracaoExternaDTO> buscarPorTipo(
        @PathVariable IntegracaoExterna.TipoIntegracao tipo
    ) {
        IntegracaoExternaDTO integracao = integracaoService.buscarPorTipo(tipo);
        return ResponseEntity.ok(integracao);
    }

    /**
     * Salva integração (criar ou atualizar).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar ou atualizar integração")
    public ResponseEntity<IntegracaoExternaDTO> salvar(
        @Valid @RequestBody IntegracaoExternaDTO dto
    ) {
        IntegracaoExternaDTO salva = integracaoService.salvarIntegracao(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    /**
     * Ativa/desativa integração.
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ativar/desativar integração")
    public ResponseEntity<Void> alternarAtivacao(@PathVariable UUID id) {
        integracaoService.alternarAtivacao(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Testa integração (envia mensagem de teste).
     */
    @PostMapping("/testar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Testar integração", description = "Envia mensagem de teste e valida credenciais")
    public ResponseEntity<TesteIntegracaoResponse> testar(
        @Valid @RequestBody TesteIntegracaoRequest request
    ) {
        TesteIntegracaoResponse response = integracaoService.testarIntegracao(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deleta integração.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deletar integração")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        integracaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

### 5.4 DTOs

#### 5.4.1 PreferenciaUsuarioDTO.java

```java
package com.pitstop.configuracao.dto;

import com.pitstop.configuracao.domain.PreferenciaUsuario.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PreferenciaUsuarioDTO(
    UUID id,
    @NotNull UUID usuarioId,
    @NotNull Tema tema,
    @NotNull Idioma idioma,
    @NotNull Densidade densidade,
    @NotNull DashboardStyle dashboardStyle,
    @NotNull Boolean notifEmail,
    @NotNull Boolean notifPush,
    @NotNull Boolean notifWhatsApp
) {
}
```

#### 5.4.2 ConfiguracaoSistemaDTO.java

```java
package com.pitstop.configuracao.dto;

import java.util.UUID;

/**
 * DTO completo da configuração do sistema (todas as seções).
 */
public record ConfiguracaoSistemaDTO(
    UUID id,
    ConfiguracaoOSDTO ordemServico,
    ConfiguracaoEstoqueDTO estoque,
    ConfiguracaoFinanceiroDTO financeiro,
    ConfiguracaoNotificacaoDTO notificacao,
    ConfiguracaoSegurancaDTO seguranca,
    ConfiguracaoSistemaTecnicoDTO sistema
) {
}
```

#### 5.4.3 ConfiguracaoOSDTO.java

```java
package com.pitstop.configuracao.dto;

import com.pitstop.configuracao.domain.ConfiguracaoOS.FormatoNumeroOS;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ConfiguracaoOSDTO(
    @NotNull FormatoNumeroOS formatoNumero,
    @Size(max = 10) String prefixo,
    @NotNull @Min(1) Long proximoNumero,
    @NotNull Boolean aprovacaoObrigatoria,
    @NotNull Boolean exigirPagamentoEntrega,
    @NotNull Boolean permitirCancelamentoAposInicio,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal descontoMaximoPercentual,
    @NotNull @Min(1) Integer prazoConclusaoDias,
    @NotNull Boolean alertaAtrasadasEmail,
    @NotNull Boolean alertaAtrasadasSistema,
    @NotNull @Min(1) Integer alertaAguardandoPecaDias,
    @NotNull Boolean exigirProblemasRelatados,
    @NotNull Boolean exigirDiagnostico,
    @NotNull Boolean permitirEdicaoFinalizada
) {
}
```

*(Continua com os outros DTOs... o documento está ficando extenso)*

---

## 6. Frontend - Implementação React/TypeScript

### 6.1 Estrutura de Diretórios

```
frontend/src/features/configuracoes/
├── components/
│   ├── ConfiguracoesSidebar.tsx
│   ├── ConfiguracaoHeader.tsx
│   ├── sections/
│   │   ├── PerfilSection.tsx
│   │   ├── OficinaSection.tsx
│   │   ├── OrdemServicoSection.tsx
│   │   ├── EstoqueSection.tsx
│   │   ├── FinanceiroSection.tsx
│   │   ├── NotificacoesSection.tsx
│   │   ├── IntegracoesSection.tsx
│   │   ├── SegurancaSection.tsx
│   │   └── SistemaSection.tsx
│   └── shared/
│       ├── SectionTitle.tsx
│       ├── FormField.tsx
│       ├── ToggleSwitch.tsx
│       └── SaveButton.tsx
├── hooks/
│   ├── usePreferenciasUsuario.ts
│   ├── useConfiguracaoSistema.ts
│   ├── useIntegracoes.ts
│   └── useAuditoria.ts
├── services/
│   ├── preferenciasService.ts
│   ├── configuracaoService.ts
│   └── integracoesService.ts
├── types/
│   ├── preferencias.types.ts
│   ├── configuracao.types.ts
│   └── integracao.types.ts
├── utils/
│   └── validation.ts
└── pages/
    └── ConfiguracoesPage.tsx
```

### 6.2 Types

#### 6.2.1 configuracao.types.ts

```typescript
// frontend/src/features/configuracoes/types/configuracao.types.ts

export enum Tema {
  LIGHT = 'LIGHT',
  DARK = 'DARK',
  AUTO = 'AUTO',
}

export enum Idioma {
  PT_BR = 'PT_BR',
  EN_US = 'EN_US',
}

export enum Densidade {
  COMPACT = 'COMPACT',
  NORMAL = 'NORMAL',
}

export enum DashboardStyle {
  CARDS = 'CARDS',
  MINIMAL = 'MINIMAL',
}

export interface PreferenciaUsuario {
  id: string;
  usuarioId: string;
  tema: Tema;
  idioma: Idioma;
  densidade: Densidade;
  dashboardStyle: DashboardStyle;
  notifEmail: boolean;
  notifPush: boolean;
  notifWhatsApp: boolean;
}

export enum FormatoNumeroOS {
  SEQUENCIAL = 'SEQUENCIAL',
  COM_ANO = 'COM_ANO',
  COM_MES = 'COM_MES',
}

export interface ConfiguracaoOS {
  formatoNumero: FormatoNumeroOS;
  prefixo: string;
  proximoNumero: number;
  aprovacaoObrigatoria: boolean;
  exigirPagamentoEntrega: boolean;
  permitirCancelamentoAposInicio: boolean;
  descontoMaximoPercentual: number;
  prazoConclusaoDias: number;
  alertaAtrasadasEmail: boolean;
  alertaAtrasadasSistema: boolean;
  alertaAguardandoPecaDias: number;
  exigirProblemasRelatados: boolean;
  exigirDiagnostico: boolean;
  permitirEdicaoFinalizada: boolean;
}

export enum CriterioAlerta {
  QUANTIDADE_MINIMA = 'QUANTIDADE_MINIMA',
  PERCENTUAL_MAXIMO = 'PERCENTUAL_MAXIMO',
}

export enum FrequenciaInventario {
  MENSAL = 'MENSAL',
  TRIMESTRAL = 'TRIMESTRAL',
  SEMESTRAL = 'SEMESTRAL',
  ANUAL = 'ANUAL',
}

export interface ConfiguracaoEstoque {
  criterioAlerta: CriterioAlerta;
  antecedenciaAlertaDias: number;
  alertaEmail: boolean;
  alertaSistema: boolean;
  alertaWhatsApp: boolean;
  exigirMotivoAjuste: boolean;
  permitirEstoqueNegativo: boolean;
  confirmacaoDuplaSaida: boolean;
  obrigarLocalizacao: boolean;
  niveisHierarquia: number;
  frequenciaInventario: FrequenciaInventario;
}

// ... (continuar com outros tipos)

export interface ConfiguracaoSistema {
  id: string;
  ordemServico: ConfiguracaoOS;
  estoque: ConfiguracaoEstoque;
  financeiro: ConfiguracaoFinanceiro;
  notificacao: ConfiguracaoNotificacao;
  seguranca: ConfiguracaoSeguranca;
  sistema: ConfiguracaoSistemaTecnico;
}
```

---

**[Documento continua...]**

Este documento está ficando muito extenso. Criei a estrutura completa do backend (entidades, repositories, services, controllers, DTOs) e o início do frontend.

Quer que eu:
1. Continue com o Frontend completo (hooks, components, pages)?
2. Pule para as **Integrações Externas** (WhatsApp, Telegram, Email, Mercado Pago)?
3. Ou prefere que eu finalize criando um terceiro arquivo com as integrações?

Diga-me como prefere proceder!
