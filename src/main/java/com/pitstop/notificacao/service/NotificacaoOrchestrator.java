package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.dto.EnviarNotificacaoRequest;
import com.pitstop.notificacao.dto.EnviarNotificacaoResponse;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.ordemservico.service.OrdemServicoPDFService;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orquestrador central de notificacoes.
 *
 * Responsavel por:
 * - Verificar configuracoes da oficina
 * - Rotear para os canais corretos
 * - Aplicar regras de horario comercial
 * - Gerenciar fallback entre canais
 * - Registrar historico
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoOrchestrator {

    private final ConfiguracaoNotificacaoRepository configuracaoRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final TelegramService telegramService;
    private final TemplateService templateService;
    private final OrdemServicoPDFService ordemServicoPDFService;
    private final TempFileService tempFileService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Envia notificacao para todos os canais configurados para o evento.
     *
     * @param request Dados da notificacao
     * @return Resultado do envio
     */
    @Transactional
    public EnviarNotificacaoResponse enviar(EnviarNotificacaoRequest request) {
        UUID oficinaId = TenantContext.getTenantId();

        // Busca configuracao
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null) {
            return EnviarNotificacaoResponse.falha("Configuracao de notificacao nao encontrada");
        }

        // Determina canais a usar
        List<TipoNotificacao> canais = determinarCanais(config, request);
        if (canais.isEmpty()) {
            return EnviarNotificacaoResponse.falha("Nenhum canal habilitado para este evento");
        }

        // Verifica horario comercial
        boolean foraHorario = !config.podeEnviarAgora() && !Boolean.TRUE.equals(request.forcarEnvio());

        // Envia para cada canal
        List<EnviarNotificacaoResponse.ResultadoCanal> resultados = new ArrayList<>();
        boolean algumSucesso = false;

        for (TipoNotificacao canal : canais) {
            EnviarNotificacaoResponse.ResultadoCanal resultado = enviarPorCanal(
                canal, config, request, foraHorario
            );
            resultados.add(resultado);

            if (resultado.status() == StatusNotificacao.ENVIADO) {
                algumSucesso = true;
            }
        }

        // Tenta fallback se todos falharam
        if (!algumSucesso && config.getCanalFallback() != null && !canais.contains(config.getCanalFallback())) {
            log.info("Tentando fallback via {}", config.getCanalFallback());
            EnviarNotificacaoResponse.ResultadoCanal fallbackResult = enviarPorCanal(
                config.getCanalFallback(), config, request, foraHorario
            );
            resultados.add(fallbackResult);

            if (fallbackResult.status() == StatusNotificacao.ENVIADO) {
                algumSucesso = true;
            }
        }

        // Monta resposta
        if (foraHorario) {
            return EnviarNotificacaoResponse.agendado(resultados);
        } else if (algumSucesso) {
            boolean todasSucesso = resultados.stream()
                .allMatch(r -> r.status() == StatusNotificacao.ENVIADO);
            return todasSucesso
                ? EnviarNotificacaoResponse.sucesso(resultados)
                : EnviarNotificacaoResponse.parcial(resultados);
        } else {
            return EnviarNotificacaoResponse.parcial(resultados);
        }
    }

    /**
     * Envia notificacao de forma assincrona.
     */
    @Async
    public void enviarAsync(EnviarNotificacaoRequest request) {
        enviar(request);
    }

    /**
     * Envia notificacao para um evento de OS.
     * Nota: Este metodo NAO e @Async porque o caller (OrdemServicoEventListener) ja e async.
     *
     * <p>Para eventos OS_ENTREGUE, gera e anexa o PDF da ordem de servico automaticamente.</p>
     *
     * @param evento Evento que disparou
     * @param destinatarioEmail Email do cliente
     * @param destinatarioTelefone Telefone do cliente
     * @param nomeDestinatario Nome do cliente
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS
     * @param clienteId ID do cliente
     */
    @Transactional
    public void notificarEventoOS(
        EventoNotificacao evento,
        String destinatarioEmail,
        String destinatarioTelefone,
        String nomeDestinatario,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        log.info("Processando notificacao de evento {} para OS {}", evento, ordemServicoId);

        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null) {
            log.warn("Configuracao de notificacao nao encontrada para oficina {}", oficinaId);
            return;
        }

        // Verifica delay configurado para o evento
        int delayMinutos = config.getDelayParaEvento(evento);
        if (delayMinutos > 0) {
            log.info("Evento {} tem delay de {} minutos configurado", evento, delayMinutos);
            // Em producao, usaria um scheduler/job para agendar
            // Por ora, apenas loga
        }

        // Gera PDF para eventos OS_ENTREGUE
        byte[] pdfBytes = null;
        String pdfUrl = null;
        String pdfFileName = null;

        if (evento == EventoNotificacao.OS_ENTREGUE && ordemServicoId != null) {
            try {
                pdfBytes = ordemServicoPDFService.gerarPDF(ordemServicoId);
                String numeroOS = variaveis.get("numeroOS") != null
                    ? variaveis.get("numeroOS").toString()
                    : ordemServicoId.toString().substring(0, 8);
                pdfFileName = "OS-" + numeroOS + ".pdf";

                // Armazena PDF temporariamente para WhatsApp/Telegram
                String token = tempFileService.storePdf(pdfBytes, pdfFileName);
                pdfUrl = baseUrl + "/api/public/files/" + token;

                log.info("PDF gerado para OS {}: {} ({} bytes)", ordemServicoId, pdfFileName, pdfBytes.length);
            } catch (Exception e) {
                log.error("Erro ao gerar PDF para OS {}: {}", ordemServicoId, e.getMessage());
                // Continua com notificacao sem PDF
            }
        }

        // Envia por email se habilitado
        if (config.isEventoHabilitado(evento, TipoNotificacao.EMAIL) && destinatarioEmail != null) {
            try {
                var template = templateService.obterTemplate(
                    oficinaId,
                    evento.getTemplatePadrao(),
                    TipoNotificacao.EMAIL
                );
                String assunto = templateService.processarAssunto(template, variaveis);
                String corpo = templateService.processarCorpo(template, variaveis);

                // Se tem PDF, envia com anexo e historico
                if (pdfBytes != null && evento == EventoNotificacao.OS_ENTREGUE) {
                    emailService.enviarComPdfEHistorico(
                        destinatarioEmail,
                        nomeDestinatario,
                        assunto,
                        corpo,
                        pdfBytes,
                        pdfFileName,
                        evento,
                        variaveis,
                        ordemServicoId,
                        clienteId,
                        null // automatico
                    );
                    log.info("Email com PDF enviado para {} (evento: {})", destinatarioEmail, evento);
                } else {
                    // Envia com historico
                    emailService.enviarComHistorico(
                        destinatarioEmail,
                        nomeDestinatario,
                        assunto,
                        corpo,
                        evento,
                        variaveis,
                        ordemServicoId,
                        clienteId,
                        null // automatico
                    );
                    log.info("Email enviado para {} (evento: {})", destinatarioEmail, evento);
                }
            } catch (Exception e) {
                log.error("Erro ao enviar email para {}: {}", destinatarioEmail, e.getMessage());
            }
        }

        // Envia por WhatsApp se habilitado
        if (config.isEventoHabilitado(evento, TipoNotificacao.WHATSAPP) && destinatarioTelefone != null) {
            try {
                // Se tem PDF, envia documento
                if (pdfUrl != null && evento == EventoNotificacao.OS_ENTREGUE) {
                    String legenda = "Ola " + nomeDestinatario + "! Segue o comprovante da sua Ordem de Servico.";
                    whatsAppService.enviarDocumento(
                        destinatarioTelefone,
                        nomeDestinatario,
                        pdfUrl,
                        pdfFileName,
                        legenda,
                        evento,
                        variaveis,
                        ordemServicoId,
                        clienteId
                    );
                    log.info("WhatsApp com PDF enviado para {} (evento: {})", destinatarioTelefone, evento);
                } else {
                    whatsAppService.enviarComTemplate(
                        destinatarioTelefone,
                        nomeDestinatario,
                        evento,
                        variaveis,
                        ordemServicoId,
                        clienteId,
                        null // automatico
                    );
                    log.info("WhatsApp enviado para {} (evento: {})", destinatarioTelefone, evento);
                }
            } catch (Exception e) {
                log.error("Erro ao enviar WhatsApp para {}: {}", destinatarioTelefone, e.getMessage());
            }
        }

        // Envia por Telegram SEMPRE (se configurado globalmente)
        // Telegram é sempre enviado junto com Email e WhatsApp
        if (config.getTelegramHabilitado() && config.temTelegramConfigurado()) {
            try {
                // Se tem PDF em bytes, envia documento diretamente
                if (pdfBytes != null && evento == EventoNotificacao.OS_ENTREGUE) {
                    String legenda = "*Comprovante OS #" + variaveis.get("numeroOS") + "*\n\n" +
                        "Olá " + nomeDestinatario + "!\n" +
                        "Segue em anexo o comprovante da sua Ordem de Serviço.\n\n" +
                        "Obrigado pela preferência!\n" +
                        "_" + variaveis.get("nomeOficina") + "_";
                    telegramService.enviarDocumentoBytes(
                        pdfBytes,
                        pdfFileName,
                        nomeDestinatario,
                        legenda,
                        evento,
                        variaveis,
                        ordemServicoId,
                        clienteId
                    );
                    log.info("Telegram com PDF enviado via bytes (evento: {})", evento);
                } else {
                    telegramService.enviarComTemplate(
                        null, // usa chat_id configurado
                        nomeDestinatario,
                        evento,
                        variaveis,
                        ordemServicoId,
                        clienteId,
                        null // automatico
                    );
                    log.info("Telegram enviado (evento: {})", evento);
                }
            } catch (Exception e) {
                log.error("Erro ao enviar Telegram: {}", e.getMessage());
            }
        }
    }

    /**
     * Processa notificacoes agendadas.
     * Deve ser chamado por um scheduler periodico.
     */
    @Transactional
    public void processarAgendadas() {
        // Esta funcao seria chamada por um @Scheduled
        log.debug("Processando notificacoes agendadas...");
        // Implementar busca e envio de notificacoes agendadas
    }

    /**
     * Processa reenvio de notificacoes que falharam.
     * Deve ser chamado por um scheduler periodico.
     */
    @Transactional
    public void processarReenvios() {
        // Esta funcao seria chamada por um @Scheduled
        log.debug("Processando reenvios de notificacoes...");
        // Implementar busca e reenvio de notificacoes com falha
    }

    // ===== METODOS PRIVADOS =====

    private List<TipoNotificacao> determinarCanais(
        ConfiguracaoNotificacao config,
        EnviarNotificacaoRequest request
    ) {
        List<TipoNotificacao> canais = new ArrayList<>();

        // Se canal especifico foi solicitado
        if (request.canal() != null) {
            if (isCanalHabilitado(config, request.canal(), request.evento())) {
                canais.add(request.canal());
            }
            return canais;
        }

        // Verifica cada canal
        for (TipoNotificacao canal : TipoNotificacao.values()) {
            if (isCanalHabilitado(config, canal, request.evento())) {
                canais.add(canal);
            }
        }

        return canais;
    }

    private boolean isCanalHabilitado(
        ConfiguracaoNotificacao config,
        TipoNotificacao canal,
        EventoNotificacao evento
    ) {
        // Verifica se canal esta globalmente habilitado
        boolean globalHabilitado = switch (canal) {
            case EMAIL -> config.getEmailHabilitado();
            case WHATSAPP -> config.getWhatsappHabilitado() && config.temEvolutionApiConfigurada();
            case SMS -> config.getSmsHabilitado();
            case TELEGRAM -> config.getTelegramHabilitado() && config.temTelegramConfigurado();
        };

        if (!globalHabilitado) {
            return false;
        }

        // Verifica configuracao especifica do evento
        return config.isEventoHabilitado(evento, canal);
    }

    private EnviarNotificacaoResponse.ResultadoCanal enviarPorCanal(
        TipoNotificacao canal,
        ConfiguracaoNotificacao config,
        EnviarNotificacaoRequest request,
        boolean agendar
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Se deve agendar
        if (agendar) {
            LocalDateTime agendadoPara = calcularProximoHorario(config);
            // Aqui criaria o historico com status AGENDADO
            UUID historicoId = UUID.randomUUID(); // Placeholder
            return EnviarNotificacaoResponse.ResultadoCanal.agendado(canal, historicoId, agendadoPara);
        }

        // Modo simulacao
        if (config.getModoSimulacao() && !Boolean.TRUE.equals(request.ignorarSimulacao())) {
            log.info("[SIMULACAO] {} para {}: evento {}",
                canal, request.destinatario(), request.evento());
            UUID historicoId = UUID.randomUUID(); // Placeholder
            return EnviarNotificacaoResponse.ResultadoCanal.simulado(canal, historicoId);
        }

        // Renderiza mensagem
        var template = templateService.obterTemplate(
            oficinaId,
            request.evento().getTemplatePadrao(),
            canal
        );
        String mensagem = templateService.processarCorpo(template, request.variaveis());

        try {
            return switch (canal) {
                case EMAIL -> enviarEmail(request, mensagem, oficinaId);
                case WHATSAPP -> enviarWhatsApp(request, mensagem);
                case TELEGRAM -> enviarTelegram(request, mensagem);
                case SMS -> EnviarNotificacaoResponse.ResultadoCanal.falha(
                    canal, null, "SMS nao implementado"
                );
            };
        } catch (Exception e) {
            log.error("Erro ao enviar via {}: {}", canal, e.getMessage());
            return EnviarNotificacaoResponse.ResultadoCanal.falha(canal, null, e.getMessage());
        }
    }

    private EnviarNotificacaoResponse.ResultadoCanal enviarEmail(
        EnviarNotificacaoRequest request,
        String corpo,
        UUID oficinaId
    ) {
        String assunto = renderizarAssunto(request.evento(), request.variaveis(), oficinaId);

        // Envia com historico para rastreamento
        HistoricoNotificacao historico = emailService.enviarComHistorico(
            request.destinatario(),
            request.nomeDestinatario(),
            assunto,
            corpo,
            request.evento(),
            request.variaveis(),
            request.ordemServicoId(),
            request.clienteId(),
            request.usuarioId()
        );

        if (historico.getStatus() == StatusNotificacao.ENVIADO) {
            return EnviarNotificacaoResponse.ResultadoCanal.sucesso(
                TipoNotificacao.EMAIL,
                historico.getId(),
                historico.getIdExterno()
            );
        } else {
            return EnviarNotificacaoResponse.ResultadoCanal.falha(
                TipoNotificacao.EMAIL,
                historico.getId(),
                historico.getErroMensagem()
            );
        }
    }

    private EnviarNotificacaoResponse.ResultadoCanal enviarWhatsApp(
        EnviarNotificacaoRequest request,
        String mensagem
    ) {
        HistoricoNotificacao historico = whatsAppService.enviar(
            request.destinatario(),
            request.nomeDestinatario(),
            mensagem,
            request.evento(),
            request.variaveis(),
            request.ordemServicoId(),
            request.clienteId(),
            request.usuarioId()
        );

        if (historico.getStatus() == StatusNotificacao.ENVIADO) {
            return EnviarNotificacaoResponse.ResultadoCanal.sucesso(
                TipoNotificacao.WHATSAPP,
                historico.getId(),
                historico.getIdExterno()
            );
        } else {
            return EnviarNotificacaoResponse.ResultadoCanal.falha(
                TipoNotificacao.WHATSAPP,
                historico.getId(),
                historico.getErroMensagem()
            );
        }
    }

    private EnviarNotificacaoResponse.ResultadoCanal enviarTelegram(
        EnviarNotificacaoRequest request,
        String mensagem
    ) {
        HistoricoNotificacao historico = telegramService.enviar(
            request.destinatario(), // chatId
            request.nomeDestinatario(),
            mensagem,
            request.evento(),
            request.variaveis(),
            request.ordemServicoId(),
            request.clienteId(),
            request.usuarioId()
        );

        if (historico.getStatus() == StatusNotificacao.ENVIADO) {
            return EnviarNotificacaoResponse.ResultadoCanal.sucesso(
                TipoNotificacao.TELEGRAM,
                historico.getId(),
                historico.getIdExterno()
            );
        } else {
            return EnviarNotificacaoResponse.ResultadoCanal.falha(
                TipoNotificacao.TELEGRAM,
                historico.getId(),
                historico.getErroMensagem()
            );
        }
    }

    private String renderizarAssunto(
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID oficinaId
    ) {
        String assuntoPadrao = evento.getTemplatePadrao().getSubject();
        // Substitui variaveis simples no assunto
        String assunto = assuntoPadrao;
        for (Map.Entry<String, Object> entry : variaveis.entrySet()) {
            assunto = assunto.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return assunto;
    }

    private LocalDateTime calcularProximoHorario(ConfiguracaoNotificacao config) {
        LocalDateTime agora = LocalDateTime.now();
        java.time.LocalTime horarioInicio = config.getHorarioInicio();

        if (agora.toLocalTime().isBefore(horarioInicio)) {
            return agora.toLocalDate().atTime(horarioInicio);
        }

        java.time.LocalDate proximoDia = agora.toLocalDate().plusDays(1);
        while (!isDiaPermitido(proximoDia, config)) {
            proximoDia = proximoDia.plusDays(1);
        }

        return proximoDia.atTime(horarioInicio);
    }

    private boolean isDiaPermitido(java.time.LocalDate data, ConfiguracaoNotificacao config) {
        java.time.DayOfWeek dia = data.getDayOfWeek();
        if (dia == java.time.DayOfWeek.SATURDAY && !config.getEnviarSabados()) {
            return false;
        }
        if (dia == java.time.DayOfWeek.SUNDAY && !config.getEnviarDomingos()) {
            return false;
        }
        return true;
    }
}
