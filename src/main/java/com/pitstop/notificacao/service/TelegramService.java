package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.integration.telegram.TelegramApiClient;
import com.pitstop.notificacao.integration.telegram.TelegramApiClient.TelegramButton;
import com.pitstop.notificacao.integration.telegram.TelegramConfig;
import com.pitstop.notificacao.integration.telegram.TelegramSendResult;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.notificacao.repository.HistoricoNotificacaoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servico para envio de notificacoes via Telegram.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramApiClient telegramApiClient;
    private final ConfiguracaoNotificacaoRepository configuracaoRepository;
    private final HistoricoNotificacaoRepository historicoRepository;
    private final TemplateService templateService;

    /**
     * Envia mensagem de Telegram de forma sincrona.
     *
     * @param chatId Chat ID do destinatario (pode ser diferente do configurado)
     * @param nomeDestinatario Nome do destinatario
     * @param mensagem Mensagem a enviar
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS relacionada
     * @param clienteId ID do cliente
     * @param usuarioId ID do usuario que disparou (null = automatico)
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviar(
        String chatId,
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Busca configuracao da oficina
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        // Cria registro de historico
        HistoricoNotificacao historico = HistoricoNotificacao.criar(
            oficinaId,
            evento,
            TipoNotificacao.TELEGRAM,
            chatId != null ? chatId : (config != null ? config.getTelegramChatId() : null),
            nomeDestinatario,
            null, // Telegram nao tem assunto
            mensagem,
            variaveis,
            null, // templateId
            ordemServicoId,
            clienteId,
            usuarioId
        );

        // Valida configuracao
        if (config == null) {
            historico.marcarComoFalha("Configuracao de notificacao nao encontrada", "CONFIG_NOT_FOUND");
            return historicoRepository.save(historico);
        }

        if (!config.getTelegramHabilitado()) {
            historico.marcarComoFalha("Telegram nao esta habilitado para esta oficina. Ative em Configuracoes > Notificacoes.", "TELEGRAM_DISABLED");
            return historicoRepository.save(historico);
        }

        if (!config.temTelegramConfigurado()) {
            historico.marcarComoFalha("Telegram nao configurado. Informe o Bot Token e Chat ID em Configuracoes > Notificacoes > Telegram.", "TELEGRAM_NOT_CONFIGURED");
            return historicoRepository.save(historico);
        }

        // Verifica horario comercial
        if (!config.podeEnviarAgora()) {
            String motivo = gerarMotivoAgendamento(config);
            java.time.LocalDateTime proximoHorario = calcularProximoHorario(config);
            historico.agendar(proximoHorario);
            historico.setMotivoAgendamento(motivo);
            log.info("Telegram agendado para {}: {}", proximoHorario, motivo);
            return historicoRepository.save(historico);
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Telegram para {}: {}", chatId, mensagem);
            historico.marcarComoEnviado("SIMULADO-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Determina o chat ID a usar
        String targetChatId = chatId != null ? chatId : config.getTelegramChatId();

        // Envia via Telegram API
        TelegramConfig telegramConfig = TelegramConfig.from(config);

        // Validacao extra de seguranca
        if (telegramConfig == null || !telegramConfig.isValid()) {
            historico.marcarComoFalha("Configuracao do Telegram invalida ou incompleta", "INVALID_CONFIG");
            return historicoRepository.save(historico);
        }

        try {
            TelegramSendResult result;

            // Verifica se ha link de aprovacao nas variaveis para usar botao inline
            String linkAprovacao = extrairLinkAprovacao(variaveis);
            if (linkAprovacao != null && !linkAprovacao.isBlank()) {
                // Telegram requer HTTPS para botoes de URL inline
                // Em desenvolvimento com HTTP, enviamos o link como texto
                boolean usarBotaoInline = linkAprovacao.startsWith("https://");

                if (usarBotaoInline) {
                    // Remove o link do texto (sera mostrado no botao)
                    String mensagemSemLink = removerLinkDoTexto(mensagem, linkAprovacao);

                    // Cria botao inline para aprovar
                    List<List<TelegramButton>> botoes = List.of(
                        List.of(TelegramButton.url("Aprovar Orcamento", linkAprovacao))
                    );

                    result = telegramApiClient.enviarComBotoes(telegramConfig, targetChatId, mensagemSemLink, botoes);
                    log.info("Telegram enviado com botao de aprovacao para chat {}", targetChatId);
                } else {
                    // HTTP URL - envia como texto simples (desenvolvimento)
                    log.info("Link de aprovacao nao e HTTPS, enviando como texto: {}", linkAprovacao);
                    result = telegramApiClient.enviarTexto(telegramConfig, targetChatId, mensagem);
                }
            } else {
                result = telegramApiClient.enviarTexto(telegramConfig, targetChatId, mensagem);
            }

            if (result.sucesso()) {
                historico.marcarComoEnviado(result.messageId());
                if (result.respostaJson() != null) {
                    historico.setRespostaApi(Map.of("response", result.respostaJson()));
                }
            } else {
                String erroMsg = result.erroMensagem() != null ? result.erroMensagem() : "Erro desconhecido ao enviar mensagem";
                historico.marcarComoFalha(erroMsg, result.erroCodigo());
                if (result.respostaJson() != null) {
                    historico.setRespostaApi(Map.of("error", result.respostaJson()));
                }
                log.error("Falha ao enviar Telegram para {}: {} (codigo: {})", targetChatId, erroMsg, result.erroCodigo());
            }
        } catch (Exception e) {
            log.error("Erro ao enviar Telegram: {}", e.getMessage(), e);
            String erroMsg = e.getMessage() != null ? e.getMessage() : "Erro de conexao com Telegram API";
            historico.marcarComoFalha(erroMsg, "EXCEPTION");
        }

        return historicoRepository.save(historico);
    }

    /**
     * Envia mensagem de Telegram para o chat padrao configurado.
     */
    @Transactional
    public HistoricoNotificacao enviar(
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        return enviar(null, nomeDestinatario, mensagem, evento, variaveis, ordemServicoId, clienteId, usuarioId);
    }

    /**
     * Envia mensagem de Telegram de forma assincrona.
     */
    @Async
    @Transactional
    public void enviarAsync(
        String chatId,
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        enviar(chatId, nomeDestinatario, mensagem, evento, variaveis, ordemServicoId, clienteId, usuarioId);
    }

    /**
     * Envia mensagem usando template.
     *
     * @param chatId Chat ID do destinatario (null = usa configuracao)
     * @param nomeDestinatario Nome do destinatario
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS
     * @param clienteId ID do cliente
     * @param usuarioId ID do usuario
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviarComTemplate(
        String chatId,
        String nomeDestinatario,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Renderiza o template (usa o template de WhatsApp que tem formato similar)
        var template = templateService.obterTemplate(
            oficinaId,
            evento.getTemplatePadrao(),
            TipoNotificacao.WHATSAPP // Telegram usa formato similar ao WhatsApp
        );
        String mensagem = templateService.processarCorpo(template, variaveis);

        return enviar(
            chatId,
            nomeDestinatario,
            mensagem,
            evento,
            variaveis,
            ordemServicoId,
            clienteId,
            usuarioId
        );
    }

    /**
     * Verifica se o bot esta funcionando.
     *
     * @param oficinaId ID da oficina
     * @return true se o bot responde corretamente
     */
    public boolean verificarConexao(UUID oficinaId) {
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null || !config.temTelegramConfigurado()) {
            return false;
        }

        TelegramConfig telegramConfig = TelegramConfig.from(config);
        return telegramApiClient.verificarConexao(telegramConfig);
    }

    /**
     * Obtem informacoes do bot configurado.
     *
     * @param oficinaId ID da oficina
     * @return Informacoes do bot ou null
     */
    public TelegramApiClient.TelegramBotInfo getBotInfo(UUID oficinaId) {
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null || !config.temTelegramConfigurado()) {
            return null;
        }

        TelegramConfig telegramConfig = TelegramConfig.from(config);
        return telegramApiClient.getMe(telegramConfig);
    }

    /**
     * Envia documento/arquivo via Telegram.
     *
     * @param documentoUrl URL publica do documento
     * @param nomeDestinatario Nome do destinatario
     * @param legenda Legenda/mensagem opcional
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS relacionada
     * @param clienteId ID do cliente
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviarDocumento(
        String documentoUrl,
        String nomeDestinatario,
        String legenda,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Busca configuracao da oficina
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        // Cria registro de historico
        HistoricoNotificacao historico = HistoricoNotificacao.criar(
            oficinaId,
            evento,
            TipoNotificacao.TELEGRAM,
            config != null ? config.getTelegramChatId() : null,
            nomeDestinatario,
            null,
            legenda != null ? legenda : "Documento enviado",
            variaveis,
            null,
            ordemServicoId,
            clienteId,
            null
        );

        // Valida configuracao
        if (config == null) {
            historico.marcarComoFalha("Configuracao de notificacao nao encontrada", "CONFIG_NOT_FOUND");
            return historicoRepository.save(historico);
        }

        if (!config.getTelegramHabilitado()) {
            historico.marcarComoFalha("Telegram nao esta habilitado para esta oficina. Ative em Configuracoes > Notificacoes.", "TELEGRAM_DISABLED");
            return historicoRepository.save(historico);
        }

        if (!config.temTelegramConfigurado()) {
            historico.marcarComoFalha("Telegram nao configurado. Informe o Bot Token e Chat ID em Configuracoes > Notificacoes > Telegram.", "TELEGRAM_NOT_CONFIGURED");
            return historicoRepository.save(historico);
        }

        // Verifica horario comercial
        if (!config.podeEnviarAgora()) {
            String motivo = gerarMotivoAgendamento(config);
            java.time.LocalDateTime proximoHorario = calcularProximoHorario(config);
            historico.agendar(proximoHorario);
            historico.setMotivoAgendamento(motivo);
            log.info("Telegram documento agendado para {}: {}", proximoHorario, motivo);
            return historicoRepository.save(historico);
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Telegram documento: {} ({})", legenda, documentoUrl);
            historico.marcarComoEnviado("SIMULADO-DOC-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Envia documento via Telegram API
        TelegramConfig telegramConfig = TelegramConfig.from(config);

        // Validacao extra de seguranca
        if (telegramConfig == null || !telegramConfig.isValid()) {
            historico.marcarComoFalha("Configuracao do Telegram invalida ou incompleta", "INVALID_CONFIG");
            return historicoRepository.save(historico);
        }

        try {
            TelegramSendResult result = telegramApiClient.enviarDocumento(
                telegramConfig,
                documentoUrl,
                legenda
            );

            if (result.sucesso()) {
                historico.marcarComoEnviado(result.messageId());
                if (result.respostaJson() != null) {
                    historico.setRespostaApi(Map.of("response", result.respostaJson()));
                }
                log.info("Documento Telegram enviado: {}", legenda);
            } else {
                String erroMsg = result.erroMensagem() != null ? result.erroMensagem() : "Erro desconhecido ao enviar documento";
                historico.marcarComoFalha(erroMsg, result.erroCodigo());
                if (result.respostaJson() != null) {
                    historico.setRespostaApi(Map.of("error", result.respostaJson()));
                }
                log.error("Erro ao enviar documento Telegram: {}", erroMsg);
            }
        } catch (Exception e) {
            log.error("Erro ao enviar documento Telegram: {}", e.getMessage(), e);
            String erroMsg = e.getMessage() != null ? e.getMessage() : "Erro de conexao com Telegram API";
            historico.marcarComoFalha(erroMsg, "EXCEPTION");
        }

        return historicoRepository.save(historico);
    }

    /**
     * Envia documento/arquivo via Telegram usando bytes diretamente.
     * Este metodo faz upload direto do arquivo, nao requer URL publica.
     *
     * @param documentBytes Conteudo do documento em bytes
     * @param fileName Nome do arquivo
     * @param nomeDestinatario Nome do destinatario
     * @param legenda Legenda/mensagem opcional
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS relacionada
     * @param clienteId ID do cliente
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviarDocumentoBytes(
        byte[] documentBytes,
        String fileName,
        String nomeDestinatario,
        String legenda,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Busca configuracao da oficina
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        // Cria registro de historico
        HistoricoNotificacao historico = HistoricoNotificacao.criar(
            oficinaId,
            evento,
            TipoNotificacao.TELEGRAM,
            config != null ? config.getTelegramChatId() : null,
            nomeDestinatario,
            null,
            legenda != null ? legenda : "Documento: " + fileName,
            variaveis,
            null,
            ordemServicoId,
            clienteId,
            null
        );

        // Valida configuracao
        if (config == null) {
            historico.marcarComoFalha("Configuracao de notificacao nao encontrada", "CONFIG_NOT_FOUND");
            return historicoRepository.save(historico);
        }

        if (!config.getTelegramHabilitado()) {
            historico.marcarComoFalha("Telegram nao esta habilitado para esta oficina", "TELEGRAM_DISABLED");
            return historicoRepository.save(historico);
        }

        if (!config.temTelegramConfigurado()) {
            historico.marcarComoFalha("Telegram nao configurado (Bot Token ou Chat ID ausente)", "TELEGRAM_NOT_CONFIGURED");
            return historicoRepository.save(historico);
        }

        // Verifica horario comercial
        if (!config.podeEnviarAgora()) {
            String motivo = gerarMotivoAgendamento(config);
            java.time.LocalDateTime proximoHorario = calcularProximoHorario(config);
            historico.agendar(proximoHorario);
            historico.setMotivoAgendamento(motivo);
            log.info("Telegram documento agendado para {}: {}", proximoHorario, motivo);
            return historicoRepository.save(historico);
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Telegram documento bytes: {} ({} bytes)", fileName, documentBytes.length);
            historico.marcarComoEnviado("SIMULADO-DOC-BYTES-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Envia documento via Telegram API
        TelegramConfig telegramConfig = TelegramConfig.from(config);

        // Validacao extra de seguranca
        if (telegramConfig == null || !telegramConfig.isValid()) {
            historico.marcarComoFalha("Configuracao do Telegram invalida ou incompleta", "INVALID_CONFIG");
            return historicoRepository.save(historico);
        }

        try {
            TelegramSendResult result = telegramApiClient.enviarDocumentoBytes(
                telegramConfig,
                documentBytes,
                fileName,
                legenda
            );

            if (result.sucesso()) {
                historico.marcarComoEnviado(result.messageId());
                if (result.respostaJson() != null) {
                    historico.setRespostaApi(Map.of("response", result.respostaJson()));
                }
                log.info("Documento Telegram enviado via bytes: {} ({} bytes)", fileName, documentBytes.length);
            } else {
                String erroMsg = result.erroMensagem() != null ? result.erroMensagem() : "Erro desconhecido ao enviar documento";
                historico.marcarComoFalha(erroMsg, result.erroCodigo());
                if (result.respostaJson() != null) {
                    historico.setRespostaApi(Map.of("error", result.respostaJson()));
                }
                log.error("Erro ao enviar documento Telegram via bytes: {}", erroMsg);
            }
        } catch (Exception e) {
            log.error("Erro ao enviar documento Telegram via bytes: {}", e.getMessage(), e);
            String erroMsg = e.getMessage() != null ? e.getMessage() : "Erro de conexao com Telegram API";
            historico.marcarComoFalha(erroMsg, "EXCEPTION");
        }

        return historicoRepository.save(historico);
    }

    /**
     * Reenvia uma notificacao que falhou.
     *
     * @param historicoId ID do historico
     * @return Historico atualizado
     */
    @Transactional
    public HistoricoNotificacao reenviar(UUID historicoId) {
        HistoricoNotificacao historico = historicoRepository.findById(historicoId)
            .orElseThrow(() -> new IllegalArgumentException("Historico nao encontrado"));

        if (!historico.getStatus().permiteReenvio()) {
            throw new IllegalStateException("Esta notificacao nao pode ser reenviada. Status: " + historico.getStatus());
        }

        UUID oficinaId = historico.getOficinaId();
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElseThrow(() -> new IllegalStateException("Configuracao nao encontrada"));

        if (!historico.podeRetentar(config.getMaxTentativasReenvio())) {
            throw new IllegalStateException("Numero maximo de tentativas atingido");
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] Reenvio Telegram para {}", historico.getDestinatario());
            historico.marcarComoEnviado("SIMULADO-REENVIO-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Reenvia
        TelegramConfig telegramConfig = TelegramConfig.from(config);
        TelegramSendResult result = telegramApiClient.enviarTexto(
            telegramConfig,
            historico.getDestinatario(),
            historico.getMensagem()
        );

        if (result.sucesso()) {
            historico.marcarComoEnviado(result.messageId());
        } else {
            historico.marcarComoFalha(result.erroMensagem(), result.erroCodigo());
        }

        return historicoRepository.save(historico);
    }

    // ===== METODOS AUXILIARES =====

    private java.time.LocalDateTime calcularProximoHorario(ConfiguracaoNotificacao config) {
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        java.time.LocalTime horarioInicio = config.getHorarioInicio();

        // Se ainda hoje, no horario de inicio
        if (agora.toLocalTime().isBefore(horarioInicio)) {
            return agora.toLocalDate().atTime(horarioInicio);
        }

        // Proximo dia util
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

    /**
     * Gera mensagem explicativa do motivo do agendamento.
     */
    private String gerarMotivoAgendamento(ConfiguracaoNotificacao config) {
        java.time.LocalTime agora = java.time.LocalTime.now();
        java.time.DayOfWeek diaSemana = java.time.LocalDate.now().getDayOfWeek();

        // Verifica se e domingo
        if (diaSemana == java.time.DayOfWeek.SUNDAY && !config.getEnviarDomingos()) {
            return "Envio bloqueado aos domingos. Desative 'Aplicar horario comercial' ou habilite 'Enviar aos domingos' nas configuracoes.";
        }

        // Verifica se e sabado
        if (diaSemana == java.time.DayOfWeek.SATURDAY && !config.getEnviarSabados()) {
            return "Envio bloqueado aos sabados. Desative 'Aplicar horario comercial' ou habilite 'Enviar aos sabados' nas configuracoes.";
        }

        // Verifica horario
        java.time.LocalTime inicio = config.getHorarioInicio();
        java.time.LocalTime fim = config.getHorarioFim();

        if (inicio != null && fim != null) {
            if (agora.isBefore(inicio)) {
                return String.format("Fora do horario comercial. Horario permitido: %s - %s. Desative 'Aplicar horario comercial' para enviar agora.",
                    inicio.toString(), fim.toString());
            }
            if (agora.isAfter(fim)) {
                return String.format("Fora do horario comercial. Horario permitido: %s - %s. Desative 'Aplicar horario comercial' para enviar agora.",
                    inicio.toString(), fim.toString());
            }
        }

        return "Fora do horario comercial configurado. Desative 'Aplicar horario comercial' nas configuracoes para enviar agora.";
    }

    /**
     * Extrai o link de aprovacao das variaveis.
     *
     * @param variaveis Mapa de variaveis do template
     * @return URL do link de aprovacao ou null
     */
    private String extrairLinkAprovacao(Map<String, Object> variaveis) {
        if (variaveis == null) {
            return null;
        }

        Object link = variaveis.get("linkAprovacao");
        if (link != null && link instanceof String linkStr && !linkStr.isBlank()) {
            return linkStr;
        }

        return null;
    }

    /**
     * Remove o link de aprovacao do texto da mensagem.
     * O link sera mostrado como botao inline ao inves de texto.
     *
     * @param mensagem Mensagem original
     * @param linkAprovacao Link a ser removido
     * @return Mensagem sem o link
     */
    private String removerLinkDoTexto(String mensagem, String linkAprovacao) {
        if (mensagem == null || linkAprovacao == null) {
            return mensagem;
        }

        // Remove o link e qualquer texto "Link: " ou similar antes dele
        String resultado = mensagem
            .replace("Link: " + linkAprovacao, "")
            .replace("link: " + linkAprovacao, "")
            .replace(linkAprovacao, "")
            .replaceAll("\\n\\n+", "\n\n") // Remove linhas vazias duplicadas
            .trim();

        // Adiciona instrucao sobre o botao
        if (!resultado.endsWith("\n")) {
            resultado += "\n";
        }
        resultado += "\nClique no botao abaixo para aprovar:";

        return resultado;
    }
}
