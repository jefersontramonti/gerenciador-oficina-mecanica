package com.pitstop.notificacao.controller;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.TemplateCustomizado;
import com.pitstop.notificacao.domain.TemplateNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.TemplateCustomizadoDTO;
import com.pitstop.notificacao.dto.TemplateCustomizadoRequest;
import com.pitstop.notificacao.repository.TemplateCustomizadoRepository;
import com.pitstop.notificacao.service.TemplateService;
import com.pitstop.shared.security.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de templates de notificacao.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/notificacoes/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Templates de Notificacao", description = "Gerenciamento de templates customizados")
@SecurityRequirement(name = "bearer-jwt")
public class TemplateController {

    private final TemplateCustomizadoRepository repository;
    private final TemplateService templateService;

    /**
     * Lista todos os templates da oficina.
     * GET /api/notificacoes/templates
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar templates", description = "Lista todos os templates da oficina")
    public ResponseEntity<List<TemplateCustomizadoDTO>> listar() {
        UUID oficinaId = TenantContext.getTenantId();

        List<TemplateCustomizado> templates = repository.findByOficinaIdAndAtivoTrueOrderByTipoTemplate(oficinaId);
        List<TemplateCustomizadoDTO> dtos = templates.stream()
            .map(TemplateCustomizadoDTO::fromEntity)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca template por tipo e canal.
     * GET /api/notificacoes/templates/{tipoTemplate}/{tipoNotificacao}
     */
    @GetMapping("/{tipoTemplate}/{tipoNotificacao}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Buscar template", description = "Busca template por tipo e canal")
    public ResponseEntity<TemplateCustomizadoDTO> buscar(
        @PathVariable TemplateNotificacao tipoTemplate,
        @PathVariable TipoNotificacao tipoNotificacao
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Usa o TemplateService para buscar com fallback
        TemplateCustomizado template = templateService.obterTemplate(oficinaId, tipoTemplate, tipoNotificacao);
        return ResponseEntity.ok(TemplateCustomizadoDTO.fromEntity(template));
    }

    /**
     * Cria ou atualiza template.
     * PUT /api/notificacoes/templates/{tipoTemplate}/{tipoNotificacao}
     */
    @PutMapping("/{tipoTemplate}/{tipoNotificacao}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Transactional
    @Operation(summary = "Salvar template", description = "Cria ou atualiza um template customizado")
    public ResponseEntity<TemplateCustomizadoDTO> salvar(
        @PathVariable TemplateNotificacao tipoTemplate,
        @PathVariable TipoNotificacao tipoNotificacao,
        @Valid @RequestBody TemplateCustomizadoRequest request
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("PUT /api/notificacoes/templates/{}/{}", tipoTemplate, tipoNotificacao);

        // Busca template existente ou cria novo
        Optional<TemplateCustomizado> existente = repository
            .findByOficinaIdAndTipoTemplateAndTipoNotificacaoAndAtivoTrue(oficinaId, tipoTemplate, tipoNotificacao);

        TemplateCustomizado template;
        if (existente.isPresent()) {
            template = existente.get();
            log.debug("Atualizando template existente: {}", template.getId());
        } else {
            template = new TemplateCustomizado();
            template.setOficinaId(oficinaId);
            template.setTipoTemplate(tipoTemplate);
            template.setTipoNotificacao(tipoNotificacao);
            log.debug("Criando novo template para oficina: {}", oficinaId);
        }

        // Atualiza campos
        template.setAssunto(request.assunto());
        template.setCorpo(request.corpo());
        template.setAtivo(request.ativo() != null ? request.ativo() : true);
        template.setCategoria(request.categoria());
        template.setTags(request.tags());
        template.setObservacoes(request.observacoes());

        // Define variaveis disponiveis baseado no tipo de template
        template.setVariaveisDisponiveis(getVariaveisParaTemplate(tipoTemplate));

        template = repository.save(template);
        log.info("Template salvo: {} - {} para oficina {}", tipoTemplate, tipoNotificacao, oficinaId);

        return ResponseEntity.ok(TemplateCustomizadoDTO.fromEntity(template));
    }

    /**
     * Remove template customizado (restaura padrao).
     * DELETE /api/notificacoes/templates/{tipoTemplate}/{tipoNotificacao}
     */
    @DeleteMapping("/{tipoTemplate}/{tipoNotificacao}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Transactional
    @Operation(summary = "Remover template", description = "Remove template customizado (restaura padrao)")
    public ResponseEntity<Map<String, String>> remover(
        @PathVariable TemplateNotificacao tipoTemplate,
        @PathVariable TipoNotificacao tipoNotificacao
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("DELETE /api/notificacoes/templates/{}/{}", tipoTemplate, tipoNotificacao);

        Optional<TemplateCustomizado> existente = repository
            .findByOficinaIdAndTipoTemplateAndTipoNotificacaoAndAtivoTrue(oficinaId, tipoTemplate, tipoNotificacao);

        if (existente.isPresent()) {
            TemplateCustomizado template = existente.get();
            template.setAtivo(false); // Soft delete
            repository.save(template);
            log.info("Template removido (soft delete): {}", template.getId());
            return ResponseEntity.ok(Map.of("message", "Template removido. O template padrao sera usado."));
        }

        return ResponseEntity.ok(Map.of("message", "Template ja esta usando o padrao."));
    }

    /**
     * Lista tipos de templates disponiveis com suas variaveis.
     * GET /api/notificacoes/templates/tipos
     */
    @GetMapping("/tipos")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar tipos", description = "Lista tipos de templates disponiveis")
    public ResponseEntity<List<TemplateInfo>> listarTipos() {
        List<TemplateInfo> tipos = Arrays.stream(TemplateNotificacao.values())
            .map(t -> new TemplateInfo(
                t.name(),
                t.getTemplateId(),
                t.getSubject(),
                getVariaveisParaTemplateAsMap(t)
            ))
            .toList();
        return ResponseEntity.ok(tipos);
    }

    /**
     * Lista canais disponiveis.
     * GET /api/notificacoes/templates/canais
     */
    @GetMapping("/canais")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Listar canais", description = "Lista canais de notificacao disponiveis")
    public ResponseEntity<List<CanalInfo>> listarCanais() {
        List<CanalInfo> canais = List.of(
            new CanalInfo("WHATSAPP", "WhatsApp", true, "Mensagens curtas com emojis"),
            new CanalInfo("TELEGRAM", "Telegram", true, "Mensagens curtas com emojis e formatacao"),
            new CanalInfo("EMAIL", "E-mail", false, "Mensagens detalhadas com HTML"),
            new CanalInfo("SMS", "SMS", false, "Mensagens muito curtas (limite 160 caracteres)")
        );
        return ResponseEntity.ok(canais);
    }

    /**
     * Preview de template com variaveis de exemplo.
     * POST /api/notificacoes/templates/preview
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Operation(summary = "Preview template", description = "Renderiza preview do template com dados de exemplo")
    public ResponseEntity<PreviewResponse> preview(
        @RequestBody PreviewRequest request
    ) {
        // Dados de exemplo para preview
        Map<String, Object> dadosExemplo = new HashMap<>();
        dadosExemplo.put("cliente_nome", "Carlos Almeida");
        dadosExemplo.put("nomeCliente", "Carlos Almeida");
        dadosExemplo.put("placa", "ABC1D23");
        dadosExemplo.put("veiculoPlaca", "ABC1D23");
        dadosExemplo.put("modelo", "Toyota Corolla 2019");
        dadosExemplo.put("veiculoModelo", "Toyota Corolla 2019");
        dadosExemplo.put("ordem_id", "OS-10458");
        dadosExemplo.put("numeroOS", "10458");
        dadosExemplo.put("valor_total", "R$ 1.280,00");
        dadosExemplo.put("valorTotal", "1.280,00");
        dadosExemplo.put("data_previsao", "27/12/2025");
        dadosExemplo.put("dataPrevisao", "27/12/2025");
        dadosExemplo.put("status", "Em andamento");
        dadosExemplo.put("nome_oficina", "Auto Center PitStop");
        dadosExemplo.put("nomeOficina", "Auto Center PitStop");
        dadosExemplo.put("telefone_oficina", "(11) 99999-9999");
        dadosExemplo.put("link_aprovacao", "https://pitstop.com/aprovar/abc123");
        dadosExemplo.put("linkAprovacao", "https://pitstop.com/aprovar/abc123");
        dadosExemplo.put("mecanico", "Joao Silva");

        // Processa corpo
        String corpoRenderizado = request.corpo();
        for (Map.Entry<String, Object> entry : dadosExemplo.entrySet()) {
            corpoRenderizado = corpoRenderizado.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
            corpoRenderizado = corpoRenderizado.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }

        // Processa assunto (se fornecido)
        String assuntoRenderizado = request.assunto();
        if (assuntoRenderizado != null) {
            for (Map.Entry<String, Object> entry : dadosExemplo.entrySet()) {
                assuntoRenderizado = assuntoRenderizado.replace("{{" + entry.getKey() + "}}", entry.getValue().toString());
                assuntoRenderizado = assuntoRenderizado.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }

        return ResponseEntity.ok(new PreviewResponse(assuntoRenderizado, corpoRenderizado));
    }

    /**
     * Restaura template para o padrao do sistema.
     * POST /api/notificacoes/templates/{tipoTemplate}/{tipoNotificacao}/restaurar
     */
    @PostMapping("/{tipoTemplate}/{tipoNotificacao}/restaurar")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'GERENTE')")
    @Transactional
    @Operation(summary = "Restaurar padrao", description = "Restaura template para o padrao do sistema")
    public ResponseEntity<TemplateCustomizadoDTO> restaurarPadrao(
        @PathVariable TemplateNotificacao tipoTemplate,
        @PathVariable TipoNotificacao tipoNotificacao
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("POST /api/notificacoes/templates/{}/{}/restaurar", tipoTemplate, tipoNotificacao);

        // Remove template customizado (soft delete)
        Optional<TemplateCustomizado> existente = repository
            .findByOficinaIdAndTipoTemplateAndTipoNotificacaoAndAtivoTrue(oficinaId, tipoTemplate, tipoNotificacao);

        if (existente.isPresent()) {
            TemplateCustomizado template = existente.get();
            template.setAtivo(false);
            repository.save(template);
            log.info("Template customizado desativado: {}", template.getId());
        }

        // Retorna template padrao/hardcoded
        TemplateCustomizado padrao = templateService.obterTemplate(null, tipoTemplate, tipoNotificacao);
        return ResponseEntity.ok(TemplateCustomizadoDTO.fromEntity(padrao));
    }

    // ===== METODOS AUXILIARES =====

    private String getVariaveisParaTemplate(TemplateNotificacao tipo) {
        Map<String, String> vars = getVariaveisParaTemplateAsMap(tipo);
        // Converte para JSON simples
        return "[" + vars.keySet().stream()
            .map(k -> "\"" + k + "\"")
            .collect(Collectors.joining(",")) + "]";
    }

    private Map<String, String> getVariaveisParaTemplateAsMap(TemplateNotificacao tipo) {
        Map<String, String> vars = new LinkedHashMap<>();

        // Variaveis comuns
        vars.put("nomeOficina", "Nome da oficina");
        vars.put("telefoneOficina", "Telefone da oficina");

        // Variaveis por tipo
        switch (tipo) {
            case OFICINA_WELCOME, OFICINA_ACTIVATED, OFICINA_SUSPENDED -> {
                // Apenas variaveis comuns
            }
            case TRIAL_EXPIRING -> vars.put("diasRestantes", "Dias restantes do trial");
            case TRIAL_EXPIRED -> { /* Sem variaveis adicionais */ }
            case PAYMENT_OVERDUE -> {
                vars.put("valor", "Valor em atraso");
                vars.put("dataVencimento", "Data de vencimento");
                vars.put("diasAtraso", "Dias em atraso");
            }
            case PAYMENT_CONFIRMED, PAYMENT_PENDING -> {
                vars.put("valor", "Valor do pagamento");
                vars.put("referencia", "Referencia do pagamento");
            }
            case OS_CREATED, OS_WAITING_APPROVAL, OS_APPROVED, OS_IN_PROGRESS,
                 OS_WAITING_PART, OS_COMPLETED, OS_DELIVERED -> {
                vars.put("nomeCliente", "Nome do cliente");
                vars.put("numeroOS", "Numero da OS");
                vars.put("veiculoModelo", "Modelo do veiculo");
                vars.put("veiculoPlaca", "Placa do veiculo");
                vars.put("valorTotal", "Valor total");
                vars.put("dataPrevisao", "Data de previsao");
                vars.put("mecanico", "Nome do mecanico");
                vars.put("linkAprovacao", "Link para aprovacao");
            }
            case REMINDER_PICKUP -> {
                vars.put("nomeCliente", "Nome do cliente");
                vars.put("veiculoPlaca", "Placa do veiculo");
                vars.put("diasEsperando", "Dias aguardando retirada");
            }
            case REMINDER_MAINTENANCE -> {
                vars.put("nomeCliente", "Nome do cliente");
                vars.put("veiculoModelo", "Modelo do veiculo");
                vars.put("veiculoPlaca", "Placa do veiculo");
                vars.put("quilometragemAtual", "Quilometragem atual");
                vars.put("proximaRevisao", "Data da proxima revisao");
            }
            case TEST -> vars.put("mensagem", "Mensagem de teste");
            default -> { }
        }

        return vars;
    }

    // ===== RECORDS AUXILIARES =====

    public record TemplateInfo(
        String codigo,
        String templateId,
        String assuntoPadrao,
        Map<String, String> variaveisDisponiveis
    ) {}

    public record CanalInfo(
        String codigo,
        String nome,
        boolean suportaEmoji,
        String descricao
    ) {}

    public record PreviewRequest(
        String assunto,
        String corpo
    ) {}

    public record PreviewResponse(
        String assunto,
        String corpo
    ) {}
}
