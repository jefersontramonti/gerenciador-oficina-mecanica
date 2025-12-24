package com.pitstop.ordemservico.controller;

import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Consulta dados do orçamento pelo token.
     * Permite que o cliente visualize os detalhes antes de aprovar.
     *
     * @param token Token de aprovação único
     * @return Dados do orçamento
     */
    @GetMapping("/{token}")
    @Operation(summary = "Consultar orçamento", description = "Retorna dados do orçamento para visualização pelo cliente")
    @Transactional(readOnly = true)
    public ResponseEntity<?> consultarOrcamento(@PathVariable String token) {
        log.info("Consultando orçamento pelo token");

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
        response.put("valorMaoObra", os.getValorMaoObra());
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
     * @param token Token de aprovação único
     * @return Confirmação da aprovação
     */
    @PostMapping("/{token}/aprovar")
    @Operation(summary = "Aprovar orçamento", description = "Aprova o orçamento e autoriza o início do serviço")
    public ResponseEntity<?> aprovarOrcamento(@PathVariable String token) {
        log.info("Aprovando orçamento pelo token");

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
     * @param token Token de aprovação único
     * @param body Corpo opcional com motivo da rejeição
     * @return Confirmação da rejeição
     */
    @PostMapping("/{token}/rejeitar")
    @Operation(summary = "Rejeitar orçamento", description = "Rejeita o orçamento (cliente não autoriza o serviço)")
    public ResponseEntity<?> rejeitarOrcamento(
        @PathVariable String token,
        @RequestBody(required = false) Map<String, String> body
    ) {
        log.info("Rejeitando orçamento pelo token");

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

        return ResponseEntity.ok(Map.of(
            "status", "REJEITADO",
            "mensagem", "Orçamento rejeitado. Agradecemos seu contato.",
            "numero", os.getNumero()
        ));
    }
}
