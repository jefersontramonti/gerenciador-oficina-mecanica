package com.pitstop.ordemservico.controller;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.notificacao.service.NotificacaoEventPublisher;
import com.pitstop.shared.security.RateLimitService;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller público para aprovação de orçamento pelo cliente.
 * Não requer autenticação - usa token único para validação.
 *
 * @author PitStop Team
 */
@RestController
@RequestMapping("/api/public/orcamento")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orçamento Público", description = "API pública para aprovação de orçamentos pelo cliente")
public class OrcamentoPublicoController {

    private final OrdemServicoRepository ordemServicoRepository;
    private final OficinaRepository oficinaRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoEventPublisher notificacaoEventPublisher;
    private final RateLimitService rateLimitService;

    /**
     * Consulta dados do orçamento pelo token.
     * Permite que o cliente visualize os detalhes antes de aprovar.
     *
     * <p><b>Security:</b> Rate limited to prevent brute-force token guessing.</p>
     *
     * @param token Token de aprovação único
     * @param request HTTP request for IP extraction
     * @return Dados do orçamento
     */
    @GetMapping("/{token}")
    @Operation(summary = "Consultar orçamento", description = "Retorna dados do orçamento para visualização pelo cliente")
    @Transactional(readOnly = true)
    public ResponseEntity<?> consultarOrcamento(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        log.info("Consultando orçamento - IP: {}", maskIp(clientIp));

        // Rate limiting check
        if (!rateLimitService.isOrcamentoRequestAllowed(clientIp)) {
            log.warn("SECURITY: Rate limit exceeded for quote consultation - IP: {}", maskIp(clientIp));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "erro", "RATE_LIMIT_EXCEEDED",
                    "mensagem", "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente."
                ));
        }

        OrdemServico os = ordemServicoRepository.findByTokenAprovacao(token)
            .orElse(null);

        if (os == null) {
            return ResponseEntity.notFound().build();
        }

        // Verifica se token ainda é válido
        if (os.getTokenAprovacaoExpiracao() != null &&
            LocalDateTime.now().isAfter(os.getTokenAprovacaoExpiracao())) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "TOKEN_EXPIRADO",
                "mensagem", "Este link de aprovação expirou. Solicite um novo orçamento à oficina."
            ));
        }

        // Verifica se já foi aprovado
        if (os.getAprovadoPeloCliente() != null && os.getAprovadoPeloCliente()) {
            return ResponseEntity.ok(Map.of(
                "status", "JA_APROVADO",
                "mensagem", "Este orçamento já foi aprovado anteriormente.",
                "numero", os.getNumero(),
                "dataAprovacao", os.getUpdatedAt()
            ));
        }

        // Formatter para datas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Retorna dados do orçamento
        Map<String, Object> response = new HashMap<>();
        response.put("numero", os.getNumero());
        response.put("status", os.getStatus().name());
        response.put("statusDescricao", os.getStatus().getDescricao());
        response.put("problemasRelatados", os.getProblemasRelatados() != null ? os.getProblemasRelatados() : "");
        response.put("diagnostico", os.getDiagnostico() != null ? os.getDiagnostico() : "");

        // Modelo híbrido de mão de obra
        response.put("tipoCobrancaMaoObra", os.getTipoCobrancaMaoObra() != null ? os.getTipoCobrancaMaoObra().name() : "VALOR_FIXO");
        response.put("valorMaoObra", os.getValorMaoObra());
        response.put("tempoEstimadoHoras", os.getTempoEstimadoHoras());
        response.put("limiteHorasAprovado", os.getLimiteHorasAprovado());
        response.put("valorHoraSnapshot", os.getValorHoraSnapshot());

        response.put("valorPecas", os.getValorPecas());
        response.put("valorTotal", os.getValorTotal());
        response.put("descontoPercentual", os.getDescontoPercentual() != null ? os.getDescontoPercentual() : BigDecimal.ZERO);
        response.put("descontoValor", os.getDescontoValor() != null ? os.getDescontoValor() : BigDecimal.ZERO);
        response.put("valorFinal", os.getValorFinal());
        response.put("dataPrevisao", os.getDataPrevisao() != null ? os.getDataPrevisao().format(dateFormatter) : null);
        response.put("dataAbertura", os.getDataAbertura() != null ? os.getDataAbertura().format(dateTimeFormatter) : null);
        response.put("podeAprovar", os.getStatus().name().equals("ORCAMENTO"));

        // Adiciona itens (peças e serviços)
        List<Map<String, Object>> itens = new ArrayList<>();
        if (os.getItens() != null) {
            os.getItens().forEach(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("tipo", item.getTipo().name());
                itemMap.put("tipoDescricao", item.getTipo().name().equals("PECA") ? "Peça" : "Serviço");
                itemMap.put("descricao", item.getDescricao());
                itemMap.put("quantidade", item.getQuantidade());
                itemMap.put("valorUnitario", item.getValorUnitario());
                itemMap.put("valorTotal", item.getValorTotal());
                itens.add(itemMap);
            });
        }
        response.put("itens", itens);

        return ResponseEntity.ok(response);
    }

    /**
     * Aprova o orçamento pelo token.
     * Muda o status da OS de ORCAMENTO para APROVADO.
     *
     * <p><b>Security:</b> Rate limited to prevent abuse.</p>
     *
     * @param token Token de aprovação único
     * @param request HTTP request for IP extraction
     * @return Confirmação da aprovação
     */
    @PostMapping("/{token}/aprovar")
    @Operation(summary = "Aprovar orçamento", description = "Aprova o orçamento e autoriza o início do serviço")
    public ResponseEntity<?> aprovarOrcamento(
            @PathVariable String token,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        log.info("Aprovando orçamento - IP: {}", maskIp(clientIp));

        // Rate limiting check
        if (!rateLimitService.isOrcamentoRequestAllowed(clientIp)) {
            log.warn("SECURITY: Rate limit exceeded for quote approval - IP: {}", maskIp(clientIp));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "erro", "RATE_LIMIT_EXCEEDED",
                    "mensagem", "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente."
                ));
        }

        OrdemServico os = ordemServicoRepository.findByTokenAprovacao(token)
            .orElse(null);

        if (os == null) {
            return ResponseEntity.notFound().build();
        }

        // Valida token
        if (!os.isTokenAprovacaoValido(token)) {
            if (os.getTokenAprovacaoExpiracao() != null &&
                LocalDateTime.now().isAfter(os.getTokenAprovacaoExpiracao())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "erro", "TOKEN_EXPIRADO",
                    "mensagem", "Este link de aprovação expirou. Solicite um novo orçamento à oficina."
                ));
            }

            if (os.getAprovadoPeloCliente() != null && os.getAprovadoPeloCliente()) {
                return ResponseEntity.ok(Map.of(
                    "status", "JA_APROVADO",
                    "mensagem", "Este orçamento já foi aprovado anteriormente.",
                    "numero", os.getNumero()
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                "erro", "TOKEN_INVALIDO",
                "mensagem", "Este link de aprovação não é válido."
            ));
        }

        // Aprova a OS
        try {
            os.aprovar(true);
            ordemServicoRepository.save(os);

            log.info("Orçamento OS #{} aprovado pelo cliente via token", os.getNumero());

            // Envia notificação via WebSocket para atualizar a UI em tempo real
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("tipo", "OS_APROVADA");
            wsMessage.put("osId", os.getId());
            wsMessage.put("numero", os.getNumero());
            wsMessage.put("status", "APROVADO");
            wsMessage.put("mensagem", "OS #" + os.getNumero() + " foi aprovada pelo cliente");
            messagingTemplate.convertAndSend("/topic/os-updates", wsMessage);

            return ResponseEntity.ok(Map.of(
                "status", "APROVADO",
                "mensagem", "Orçamento aprovado com sucesso! A oficina iniciará o serviço em breve.",
                "numero", os.getNumero(),
                "valorFinal", os.getValorFinal()
            ));
        } catch (IllegalStateException e) {
            log.warn("Erro ao aprovar OS #{}: {}", os.getNumero(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "ERRO_APROVACAO",
                "mensagem", e.getMessage()
            ));
        }
    }

    /**
     * Rejeita o orçamento pelo token.
     * Opcional: cliente pode informar motivo.
     *
     * <p><b>Security:</b> Rate limited to prevent abuse.</p>
     *
     * @param token Token de aprovação único
     * @param body Corpo opcional com motivo da rejeição
     * @param request HTTP request for IP extraction
     * @return Confirmação da rejeição
     */
    @PostMapping("/{token}/rejeitar")
    @Operation(summary = "Rejeitar orçamento", description = "Rejeita o orçamento (cliente não autoriza o serviço)")
    @Transactional
    public ResponseEntity<?> rejeitarOrcamento(
        @PathVariable String token,
        @RequestBody(required = false) Map<String, String> body,
        HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        log.info("Rejeitando orçamento - IP: {}", maskIp(clientIp));

        // Rate limiting check
        if (!rateLimitService.isOrcamentoRequestAllowed(clientIp)) {
            log.warn("SECURITY: Rate limit exceeded for quote rejection - IP: {}", maskIp(clientIp));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "erro", "RATE_LIMIT_EXCEEDED",
                    "mensagem", "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente."
                ));
        }

        OrdemServico os = ordemServicoRepository.findByTokenAprovacao(token)
            .orElse(null);

        if (os == null) {
            return ResponseEntity.notFound().build();
        }

        // Verifica se está em status válido para rejeição
        if (!os.getStatus().name().equals("ORCAMENTO")) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "STATUS_INVALIDO",
                "mensagem", "Este orçamento não pode mais ser rejeitado."
            ));
        }

        // Adiciona observação de rejeição
        String motivo = body != null ? body.get("motivo") : null;
        String observacao = "ORÇAMENTO REJEITADO PELO CLIENTE";
        if (motivo != null && !motivo.isBlank()) {
            observacao += ": " + motivo;
        }

        String obsAtual = os.getObservacoes();
        os.setObservacoes(obsAtual != null ? obsAtual + "\n\n" + observacao : observacao);

        // Cancela a OS
        os.cancelar("Orçamento rejeitado pelo cliente");
        ordemServicoRepository.save(os);

        log.info("Orçamento OS #{} rejeitado pelo cliente via token", os.getNumero());

        // Envia notificação via WebSocket para atualizar a UI em tempo real
        Map<String, Object> wsMessage = new HashMap<>();
        wsMessage.put("tipo", "OS_REJEITADA");
        wsMessage.put("osId", os.getId());
        wsMessage.put("numero", os.getNumero());
        wsMessage.put("status", "CANCELADO");
        wsMessage.put("motivo", motivo != null ? motivo : "Não informado");
        wsMessage.put("mensagem", "OS #" + os.getNumero() + " foi rejeitada pelo cliente");
        messagingTemplate.convertAndSend("/topic/os-updates", wsMessage);

        // Publica evento de notificação para a oficina
        try {
            // Obtém dados do cliente e oficina para a notificação
            UUID oficinaId = os.getOficina().getId();
            Oficina oficina = oficinaRepository.findById(oficinaId).orElse(null);
            Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, os.getVeiculoId()).orElse(null);
            Cliente cliente = veiculo != null ?
                clienteRepository.findByOficinaIdAndId(oficinaId, veiculo.getClienteId()).orElse(null) : null;

            if (cliente != null && oficina != null) {
                notificacaoEventPublisher.publicarOSRejeitada(
                    oficinaId,
                    os.getId(),
                    os.getNumero(),
                    cliente.getId(),
                    cliente.getNome(),
                    cliente.getEmail(),
                    cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
                    motivo,
                    oficina.getNomeFantasia()
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the rejection
            log.warn("Erro ao publicar evento de rejeição para OS #{}: {}", os.getNumero(), e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "status", "REJEITADO",
            "mensagem", "Orçamento rejeitado. Agradecemos seu contato.",
            "numero", os.getNumero()
        ));
    }

    // ==================== Helper Methods ====================

    /**
     * Extracts the real client IP address from the request.
     * Handles proxies, load balancers, and CDNs.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        // Check for forwarded headers (reverse proxy, load balancer, CDN)
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP", // Cloudflare
            "True-Client-IP",   // Akamai
            "X-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    /**
     * Masks IP address for logging (privacy).
     * Shows only first two octets for IPv4, first two groups for IPv6.
     *
     * @param ip IP address to mask
     * @return Masked IP address
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }

        // IPv4: 192.168.xxx.xxx
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1] + ".xxx.xxx";
            }
        }

        // IPv6: 2001:db8:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx
        if (ip.contains(":")) {
            String[] parts = ip.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":xxxx:xxxx:xxxx:xxxx:xxxx:xxxx";
            }
        }

        return "xxx.xxx.xxx.xxx";
    }
}
