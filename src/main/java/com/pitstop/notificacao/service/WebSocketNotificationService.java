package com.pitstop.notificacao.service;

import com.pitstop.notificacao.event.OrdemServicoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço responsável por enviar notificações via WebSocket para atualização em tempo real da UI.
 *
 * Escuta eventos de domínio (OrdemServicoEvent, etc.) e envia mensagens para o frontend
 * através de WebSocket/STOMP, permitindo que a interface seja atualizada automaticamente.
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Escuta eventos de Ordem de Serviço e envia notificação via WebSocket.
     */
    @EventListener
    @Async
    public void handleOrdemServicoEvent(OrdemServicoEvent event) {
        try {
            String tipo = mapEventoToTipo(event.getTipoEvento().name());

            Map<String, Object> notification = new HashMap<>();
            notification.put("tipo", tipo);
            notification.put("titulo", getTitulo(event));
            notification.put("mensagem", getMensagem(event));
            notification.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));

            // Dados adicionais para o frontend
            Map<String, Object> dados = new HashMap<>();
            dados.put("id", event.getOrdemServicoId());
            dados.put("numero", event.getNumeroOS());
            dados.put("clienteNome", event.getNomeCliente());
            if (event.getVeiculoPlaca() != null) {
                dados.put("veiculoPlaca", event.getVeiculoPlaca());
            }
            if (event.getValorTotal() != null) {
                dados.put("valorTotal", event.getValorTotal());
            }
            notification.put("dados", dados);

            // Envia para o tópico broadcast (todos os usuários da oficina)
            messagingTemplate.convertAndSend("/topic/os-updates", notification);

            // Também envia para a fila do usuário específico (se tiver userId)
            // messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);

            log.debug("WebSocket notification sent for event: {} - OS #{}", tipo, event.getNumeroOS());

        } catch (Exception e) {
            log.error("Error sending WebSocket notification for OS event: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia notificação genérica de atualização do dashboard.
     */
    public void notifyDashboardUpdate(UUID oficinaId) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("tipo", "DASHBOARD_UPDATE");
            notification.put("titulo", "Dashboard atualizado");
            notification.put("mensagem", "Novos dados disponíveis");
            notification.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));

            messagingTemplate.convertAndSend("/topic/dashboard-updates", notification);
            log.debug("Dashboard update notification sent for oficina: {}", oficinaId);

        } catch (Exception e) {
            log.error("Error sending dashboard update notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de alerta de estoque.
     */
    public void notifyStockAlert(UUID oficinaId, UUID pecaId, String pecaNome, int quantidadeAtual, int quantidadeMinima) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("tipo", "STOCK_ALERT");
            notification.put("titulo", "Alerta de Estoque Baixo");
            notification.put("mensagem", String.format("%s está com estoque baixo (%d/%d)", pecaNome, quantidadeAtual, quantidadeMinima));
            notification.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));

            Map<String, Object> dados = new HashMap<>();
            dados.put("id", pecaId);
            dados.put("nome", pecaNome);
            dados.put("quantidadeAtual", quantidadeAtual);
            dados.put("quantidadeMinima", quantidadeMinima);
            notification.put("dados", dados);

            messagingTemplate.convertAndSend("/topic/estoque-alerts", notification);
            log.debug("Stock alert notification sent for peca: {}", pecaNome);

        } catch (Exception e) {
            log.error("Error sending stock alert notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de pagamento recebido.
     */
    public void notifyPaymentReceived(UUID oficinaId, UUID pagamentoId, Long osNumero, String valorFormatado) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("tipo", "PAYMENT_RECEIVED");
            notification.put("titulo", "Pagamento Recebido");
            notification.put("mensagem", String.format("Pagamento de %s recebido para OS #%d", valorFormatado, osNumero));
            notification.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));

            Map<String, Object> dados = new HashMap<>();
            dados.put("id", pagamentoId);
            dados.put("osNumero", osNumero);
            dados.put("valor", valorFormatado);
            notification.put("dados", dados);

            messagingTemplate.convertAndSend("/topic/os-updates", notification);
            log.debug("Payment received notification sent for OS #{}}", osNumero);

        } catch (Exception e) {
            log.error("Error sending payment notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de plano de manutenção.
     */
    public void notifyManutencaoPlanoUpdate(UUID oficinaId, UUID planoId, String tipo, String mensagem) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("tipo", tipo); // PLANO_CREATED, PLANO_UPDATED, PLANO_EXECUTED, ALERTA_GERADO
            notification.put("titulo", "Manutenção Preventiva");
            notification.put("mensagem", mensagem);
            notification.put("timestamp", LocalDateTime.now().format(DATETIME_FORMATTER));

            Map<String, Object> dados = new HashMap<>();
            dados.put("id", planoId);
            notification.put("dados", dados);

            messagingTemplate.convertAndSend("/topic/os-updates", notification);
            log.debug("Manutencao plano notification sent: {} - {}", tipo, planoId);

        } catch (Exception e) {
            log.error("Error sending manutencao notification: {}", e.getMessage(), e);
        }
    }

    // ==================== Private Helper Methods ====================

    private String mapEventoToTipo(String evento) {
        return switch (evento) {
            case "OS_CRIADA" -> "OS_CREATED";
            case "OS_APROVADA" -> "OS_APROVADA";
            case "OS_REJEITADA" -> "OS_STATUS_CHANGED";
            case "OS_EM_ANDAMENTO" -> "OS_STATUS_CHANGED";
            case "OS_FINALIZADA" -> "OS_STATUS_CHANGED";
            case "OS_ENTREGUE" -> "OS_STATUS_CHANGED";
            case "OS_ATUALIZADA" -> "OS_UPDATED";
            default -> "OS_UPDATED";
        };
    }

    private String getTitulo(OrdemServicoEvent event) {
        return switch (event.getTipoEvento().name()) {
            case "OS_CRIADA" -> "Nova Ordem de Serviço";
            case "OS_APROVADA" -> "Orçamento Aprovado";
            case "OS_REJEITADA" -> "Orçamento Rejeitado";
            case "OS_EM_ANDAMENTO" -> "Serviço Iniciado";
            case "OS_FINALIZADA" -> "Serviço Finalizado";
            case "OS_ENTREGUE" -> "Veículo Entregue";
            default -> "Atualização de OS";
        };
    }

    private String getMensagem(OrdemServicoEvent event) {
        String nomeCliente = event.getNomeCliente() != null ? event.getNomeCliente() : "Cliente";
        Long numero = event.getNumeroOS();

        return switch (event.getTipoEvento().name()) {
            case "OS_CRIADA" -> String.format("OS #%d criada para %s", numero, nomeCliente);
            case "OS_APROVADA" -> String.format("OS #%d foi aprovada pelo cliente", numero);
            case "OS_REJEITADA" -> String.format("OS #%d foi rejeitada pelo cliente", numero);
            case "OS_EM_ANDAMENTO" -> String.format("Serviço da OS #%d foi iniciado", numero);
            case "OS_FINALIZADA" -> String.format("OS #%d foi finalizada", numero);
            case "OS_ENTREGUE" -> String.format("Veículo da OS #%d foi entregue", numero);
            default -> String.format("OS #%d foi atualizada", numero);
        };
    }
}
