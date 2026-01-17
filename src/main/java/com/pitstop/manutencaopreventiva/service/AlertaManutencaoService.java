package com.pitstop.manutencaopreventiva.service;

import com.pitstop.manutencaopreventiva.domain.AlertaManutencao;
import com.pitstop.manutencaopreventiva.domain.CanalNotificacao;
import com.pitstop.manutencaopreventiva.domain.StatusAlerta;
import com.pitstop.manutencaopreventiva.domain.TipoAlerta;
import com.pitstop.manutencaopreventiva.repository.AlertaManutencaoRepository;
import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.notificacao.service.EmailService;
import com.pitstop.notificacao.service.TelegramService;
import com.pitstop.notificacao.service.WhatsAppService;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço responsável por processar e enviar alertas de manutenção preventiva.
 *
 * Integra com os serviços de notificação existentes (WhatsApp, Email, Telegram)
 * para enviar alertas aos clientes sobre manutenções próximas ou vencidas.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertaManutencaoService {

    private final AlertaManutencaoRepository alertaRepository;
    private final ConfiguracaoNotificacaoRepository configuracaoRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final TelegramService telegramService;

    private static final int INTERVALO_RETRY_MINUTOS = 30;

    /**
     * Processa todos os alertas pendentes prontos para envio.
     * Deve ser chamado pelo scheduler periodicamente.
     *
     * @return Número de alertas processados
     */
    @Transactional
    public int processarAlertasPendentes() {
        log.info("Iniciando processamento de alertas pendentes...");

        List<AlertaManutencao> alertasPendentes = alertaRepository.findParaEnvio(LocalDateTime.now());

        int enviados = 0;
        int falhas = 0;

        for (AlertaManutencao alerta : alertasPendentes) {
            try {
                // Define contexto do tenant
                TenantContext.setTenantId(alerta.getOficina().getId());

                if (enviarAlerta(alerta)) {
                    enviados++;
                } else {
                    falhas++;
                }
            } catch (Exception e) {
                log.error("Erro ao processar alerta {}: {}", alerta.getId(), e.getMessage());
                alerta.marcarComoFalhou(e.getMessage(), INTERVALO_RETRY_MINUTOS);
                alertaRepository.save(alerta);
                falhas++;
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Processamento concluído. Enviados: {}, Falhas: {}", enviados, falhas);
        return enviados;
    }

    /**
     * Envia um alerta específico de forma assíncrona.
     *
     * @param alertaId ID do alerta a enviar
     */
    @Async
    @Transactional
    public void enviarAlertaAsync(UUID alertaId) {
        AlertaManutencao alerta = alertaRepository.findById(alertaId).orElse(null);
        if (alerta == null) {
            log.warn("Alerta não encontrado: {}", alertaId);
            return;
        }

        try {
            TenantContext.setTenantId(alerta.getOficina().getId());
            enviarAlerta(alerta);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Envia um alerta individual.
     *
     * @param alerta Alerta a ser enviado
     * @return true se enviado com sucesso
     */
    @Transactional
    public boolean enviarAlerta(AlertaManutencao alerta) {
        if (!alerta.prontoParaEnvio()) {
            log.debug("Alerta {} não está pronto para envio", alerta.getId());
            return false;
        }

        UUID oficinaId = alerta.getOficina().getId();

        // Verifica configuração de notificações da oficina
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null) {
            log.warn("Configuração de notificação não encontrada para oficina {}", oficinaId);
            alerta.marcarComoFalhou("Configuração de notificação não encontrada", INTERVALO_RETRY_MINUTOS);
            alertaRepository.save(alerta);
            return false;
        }

        // Modo simulação
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Alerta {} seria enviado via {} para {}",
                alerta.getId(), alerta.getCanal(), alerta.getDestinatario());
            alerta.marcarComoEnviado(Map.of("simulacao", true));
            alertaRepository.save(alerta);
            return true;
        }

        // Verifica horário comercial
        if (!config.podeEnviarAgora()) {
            log.debug("Fora do horário comercial. Alerta {} será agendado.", alerta.getId());
            // Não marca como falha, apenas deixa pendente para próximo horário
            return false;
        }

        // Prepara variáveis para template
        Map<String, Object> variaveis = prepararVariaveis(alerta);

        // Determina evento baseado no tipo de alerta
        EventoNotificacao evento = mapearEvento(alerta.getTipoAlerta());

        try {
            HistoricoNotificacao historico = switch (alerta.getCanal()) {
                case WHATSAPP -> enviarViaWhatsApp(alerta, evento, variaveis, config);
                case EMAIL -> enviarViaEmail(alerta, evento, variaveis);
                case TELEGRAM -> enviarViaTelegram(alerta, evento, variaveis, config);
                case SMS -> {
                    log.warn("Canal SMS não implementado");
                    yield null;
                }
                case PUSH, INTERNO -> {
                    log.warn("Canal {} não implementado para alertas de manutenção", alerta.getCanal());
                    yield null;
                }
            };

            if (historico != null && historico.getStatus() == StatusNotificacao.ENVIADO) {
                alerta.marcarComoEnviado(Map.of(
                    "historicoId", historico.getId().toString(),
                    "idExterno", historico.getIdExterno() != null ? historico.getIdExterno() : ""
                ));
                alertaRepository.save(alerta);
                log.info("Alerta {} enviado com sucesso via {}", alerta.getId(), alerta.getCanal());
                return true;
            } else {
                String erro = historico != null ? historico.getErroMensagem() : "Erro desconhecido";
                alerta.marcarComoFalhou(erro, INTERVALO_RETRY_MINUTOS);
                alertaRepository.save(alerta);
                log.warn("Falha ao enviar alerta {} via {}: {}", alerta.getId(), alerta.getCanal(), erro);
                return false;
            }
        } catch (Exception e) {
            log.error("Exceção ao enviar alerta {} via {}: {}", alerta.getId(), alerta.getCanal(), e.getMessage());
            alerta.marcarComoFalhou(e.getMessage(), INTERVALO_RETRY_MINUTOS);
            alertaRepository.save(alerta);
            return false;
        }
    }

    /**
     * Envia alerta via WhatsApp.
     */
    private HistoricoNotificacao enviarViaWhatsApp(
        AlertaManutencao alerta,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        ConfiguracaoNotificacao config
    ) {
        if (!config.getWhatsappHabilitado() || !config.temEvolutionApiConfigurada()) {
            log.warn("WhatsApp não configurado para oficina {}", alerta.getOficina().getId());
            return null;
        }

        return whatsAppService.enviar(
            alerta.getDestinatario(),
            alerta.getCliente().getNome(),
            alerta.getMensagem(),
            evento,
            variaveis,
            null, // ordemServicoId
            alerta.getCliente().getId(),
            null  // usuarioId (automático)
        );
    }

    /**
     * Envia alerta via Email.
     */
    private HistoricoNotificacao enviarViaEmail(
        AlertaManutencao alerta,
        EventoNotificacao evento,
        Map<String, Object> variaveis
    ) {
        return emailService.enviarComHistorico(
            alerta.getDestinatario(),
            alerta.getCliente().getNome(),
            alerta.getTitulo(),
            alerta.getMensagem(),
            evento,
            variaveis,
            null, // ordemServicoId
            alerta.getCliente().getId(),
            null  // usuarioId (automático)
        );
    }

    /**
     * Envia alerta via Telegram.
     */
    private HistoricoNotificacao enviarViaTelegram(
        AlertaManutencao alerta,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        ConfiguracaoNotificacao config
    ) {
        if (!config.getTelegramHabilitado() || !config.temTelegramConfigurado()) {
            log.warn("Telegram não configurado para oficina {}", alerta.getOficina().getId());
            return null;
        }

        // Telegram usa chatId da configuração ou do cliente
        String chatId = config.getTelegramChatId();

        return telegramService.enviar(
            chatId,
            alerta.getCliente().getNome(),
            alerta.getMensagem(),
            evento,
            variaveis,
            null, // ordemServicoId
            alerta.getCliente().getId(),
            null  // usuarioId (automático)
        );
    }

    /**
     * Mapeia TipoAlerta para EventoNotificacao.
     */
    private EventoNotificacao mapearEvento(TipoAlerta tipoAlerta) {
        return switch (tipoAlerta) {
            case PROXIMIDADE -> EventoNotificacao.MANUTENCAO_PROXIMA;
            case VENCIDO -> EventoNotificacao.MANUTENCAO_VENCIDA;
            case LEMBRETE_AGENDAMENTO -> EventoNotificacao.LEMBRETE_AGENDAMENTO;
            case CONFIRMACAO -> EventoNotificacao.CONFIRMACAO_AGENDAMENTO;
        };
    }

    /**
     * Prepara variáveis para uso nos templates.
     */
    private Map<String, Object> prepararVariaveis(AlertaManutencao alerta) {
        Map<String, Object> variaveis = new HashMap<>();

        // Cliente
        if (alerta.getCliente() != null) {
            variaveis.put("nomeCliente", alerta.getCliente().getNome());
        }

        // Veículo
        if (alerta.getVeiculo() != null) {
            variaveis.put("veiculoPlaca", alerta.getVeiculo().getPlacaFormatada());
            variaveis.put("veiculoModelo", alerta.getVeiculo().getModelo());
            variaveis.put("veiculoMarca", alerta.getVeiculo().getMarca());
        }

        // Plano
        if (alerta.getPlano() != null) {
            variaveis.put("tipoManutencao", alerta.getPlano().getTipoManutencao());
            if (alerta.getPlano().getProximaPrevisaoData() != null) {
                variaveis.put("dataPrevisao", alerta.getPlano().getProximaPrevisaoData().toString());
            }
            if (alerta.getPlano().getProximaPrevisaoKm() != null) {
                variaveis.put("kmPrevisao", alerta.getPlano().getProximaPrevisaoKm().toString());
            }
        }

        // Oficina
        if (alerta.getOficina() != null) {
            variaveis.put("nomeOficina", alerta.getOficina().getNomeFantasia() != null
                ? alerta.getOficina().getNomeFantasia()
                : alerta.getOficina().getRazaoSocial());
        }

        // Dados extras do alerta
        if (alerta.getDadosExtras() != null) {
            variaveis.putAll(alerta.getDadosExtras());
        }

        return variaveis;
    }

    /**
     * Cancela um alerta pendente.
     *
     * @param alertaId ID do alerta
     */
    @Transactional
    public void cancelarAlerta(UUID alertaId) {
        AlertaManutencao alerta = alertaRepository.findById(alertaId).orElse(null);
        if (alerta != null && alerta.getStatus() == StatusAlerta.PENDENTE) {
            alerta.cancelar();
            alertaRepository.save(alerta);
            log.info("Alerta {} cancelado", alertaId);
        }
    }

    /**
     * Retorna estatísticas de alertas.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obterEstatisticas(UUID oficinaId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pendentes", alertaRepository.countByOficinaIdAndStatus(oficinaId, StatusAlerta.PENDENTE));
        stats.put("enviados", alertaRepository.countByOficinaIdAndStatus(oficinaId, StatusAlerta.ENVIADO));
        stats.put("falharam", alertaRepository.countByOficinaIdAndStatus(oficinaId, StatusAlerta.FALHOU));
        return stats;
    }
}
