package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.ConfiguracaoNotificacao;
import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.domain.HistoricoNotificacao;
import com.pitstop.notificacao.domain.StatusNotificacao;
import com.pitstop.notificacao.domain.TipoNotificacao;
import com.pitstop.notificacao.integration.evolution.EvolutionApiClient;
import com.pitstop.notificacao.integration.evolution.EvolutionConfig;
import com.pitstop.notificacao.integration.evolution.EvolutionInstanceStatus;
import com.pitstop.notificacao.integration.evolution.EvolutionSendResult;
import com.pitstop.notificacao.repository.ConfiguracaoNotificacaoRepository;
import com.pitstop.notificacao.repository.HistoricoNotificacaoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Servico para envio de notificacoes via WhatsApp.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppService {

    private final EvolutionApiClient evolutionApiClient;
    private final ConfiguracaoNotificacaoRepository configuracaoRepository;
    private final HistoricoNotificacaoRepository historicoRepository;
    private final TemplateService templateService;

    /**
     * Envia mensagem de WhatsApp de forma sincrona.
     *
     * @param numero Numero do destinatario
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
        String numero,
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
            TipoNotificacao.WHATSAPP,
            numero,
            nomeDestinatario,
            null, // WhatsApp nao tem assunto
            mensagem,
            variaveis,
            null, // templateId
            ordemServicoId,
            clienteId,
            usuarioId
        );

        // Valida configuracao
        if (config == null) {
            historico.marcarComoFalha(
                "Configuracao de notificacao nao encontrada. Acesse Configuracoes > Notificacoes para configurar.",
                "CONFIG_NOT_FOUND"
            );
            return historicoRepository.save(historico);
        }

        if (!config.getWhatsappHabilitado()) {
            historico.marcarComoFalha(
                "WhatsApp nao esta habilitado para esta oficina. Ative em Configuracoes > Notificacoes > WhatsApp.",
                "WHATSAPP_DISABLED"
            );
            return historicoRepository.save(historico);
        }

        if (!config.temEvolutionApiConfigurada()) {
            historico.marcarComoFalha(
                "Evolution API nao esta configurada. Configure a URL da API, nome da instancia e API Key em Configuracoes > Notificacoes > WhatsApp.",
                "EVOLUTION_NOT_CONFIGURED"
            );
            return historicoRepository.save(historico);
        }

        // Verifica horario comercial
        if (!config.podeEnviarAgora()) {
            // Agenda para proximo horario disponivel com mensagem clara
            String motivo = gerarMotivoAgendamento(config);
            log.info("WhatsApp agendado para {}: {}", numero, motivo);
            historico.agendar(calcularProximoHorario(config));
            historico.setMotivoAgendamento(motivo);
            return historicoRepository.save(historico);
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] WhatsApp para {}: {}", numero, mensagem);
            historico.marcarComoEnviado("SIMULADO-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Envia via Evolution API
        EvolutionConfig evolutionConfig = EvolutionConfig.from(config);
        EvolutionSendResult result = evolutionApiClient.enviarTexto(evolutionConfig, numero, mensagem);

        if (result.sucesso()) {
            historico.marcarComoEnviado(result.messageId());
            historico.setRespostaApi(Map.of("response", result.respostaJson()));
        } else {
            historico.marcarComoFalha(result.erroMensagem(), result.erroCodigo());
            if (result.respostaJson() != null) {
                historico.setRespostaApi(Map.of("error", result.respostaJson()));
            }
        }

        return historicoRepository.save(historico);
    }

    /**
     * Envia mensagem de WhatsApp de forma assincrona.
     */
    @Async
    @Transactional
    public void enviarAsync(
        String numero,
        String nomeDestinatario,
        String mensagem,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        enviar(numero, nomeDestinatario, mensagem, evento, variaveis, ordemServicoId, clienteId, usuarioId);
    }

    /**
     * Envia mensagem usando template.
     *
     * @param numero Numero do destinatario
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
        String numero,
        String nomeDestinatario,
        EventoNotificacao evento,
        Map<String, Object> variaveis,
        UUID ordemServicoId,
        UUID clienteId,
        UUID usuarioId
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        // Renderiza o template
        var template = templateService.obterTemplate(
            oficinaId,
            evento.getTemplatePadrao(),
            TipoNotificacao.WHATSAPP
        );
        String mensagem = templateService.processarCorpo(template, variaveis);

        return enviar(
            numero,
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
     * Verifica status da conexao WhatsApp de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return Status da instancia
     */
    public EvolutionInstanceStatus verificarConexao(UUID oficinaId) {
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null || !config.temEvolutionApiConfigurada()) {
            return new EvolutionInstanceStatus(false, "not_configured", false, "Evolution API nao configurada");
        }

        EvolutionConfig evolutionConfig = EvolutionConfig.from(config);
        return evolutionApiClient.verificarStatus(evolutionConfig);
    }

    /**
     * Gera QR Code para conectar WhatsApp de uma oficina.
     *
     * @param oficinaId ID da oficina
     * @return QR Code em base64 ou null
     */
    public String gerarQrCode(UUID oficinaId) {
        ConfiguracaoNotificacao config = configuracaoRepository
            .findByOficinaIdAndAtivoTrue(oficinaId)
            .orElse(null);

        if (config == null || !config.temEvolutionApiConfigurada()) {
            return null;
        }

        EvolutionConfig evolutionConfig = EvolutionConfig.from(config);
        return evolutionApiClient.gerarQrCode(evolutionConfig);
    }

    /**
     * Envia documento/arquivo via WhatsApp.
     *
     * @param numero Numero do destinatario
     * @param nomeDestinatario Nome do destinatario
     * @param documentoUrl URL publica do documento
     * @param nomeArquivo Nome do arquivo
     * @param legenda Legenda/mensagem opcional
     * @param evento Evento que disparou
     * @param variaveis Variaveis do template
     * @param ordemServicoId ID da OS relacionada
     * @param clienteId ID do cliente
     * @return Historico da notificacao
     */
    @Transactional
    public HistoricoNotificacao enviarDocumento(
        String numero,
        String nomeDestinatario,
        String documentoUrl,
        String nomeArquivo,
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
            TipoNotificacao.WHATSAPP,
            numero,
            nomeDestinatario,
            null,
            legenda != null ? legenda : "Documento: " + nomeArquivo,
            variaveis,
            null,
            ordemServicoId,
            clienteId,
            null
        );

        // Valida configuracao
        if (config == null) {
            historico.marcarComoFalha(
                "Configuracao de notificacao nao encontrada. Acesse Configuracoes > Notificacoes para configurar.",
                "CONFIG_NOT_FOUND"
            );
            return historicoRepository.save(historico);
        }

        if (!config.getWhatsappHabilitado()) {
            historico.marcarComoFalha(
                "WhatsApp nao esta habilitado para esta oficina. Ative em Configuracoes > Notificacoes > WhatsApp.",
                "WHATSAPP_DISABLED"
            );
            return historicoRepository.save(historico);
        }

        if (!config.temEvolutionApiConfigurada()) {
            historico.marcarComoFalha(
                "Evolution API nao esta configurada. Configure a URL da API, nome da instancia e API Key em Configuracoes > Notificacoes > WhatsApp.",
                "EVOLUTION_NOT_CONFIGURED"
            );
            return historicoRepository.save(historico);
        }

        // Verifica horario comercial
        if (!config.podeEnviarAgora()) {
            String motivo = gerarMotivoAgendamento(config);
            log.info("WhatsApp documento agendado para {}: {}", numero, motivo);
            historico.agendar(calcularProximoHorario(config));
            historico.setMotivoAgendamento(motivo);
            return historicoRepository.save(historico);
        }

        // Modo simulacao
        if (config.getModoSimulacao()) {
            log.info("[SIMULACAO] WhatsApp documento para {}: {} ({})", numero, nomeArquivo, documentoUrl);
            historico.marcarComoEnviado("SIMULADO-DOC-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Envia documento via Evolution API
        EvolutionConfig evolutionConfig = EvolutionConfig.from(config);
        EvolutionSendResult result = evolutionApiClient.enviarDocumento(
            evolutionConfig,
            numero,
            documentoUrl,
            nomeArquivo,
            legenda
        );

        if (result.sucesso()) {
            historico.marcarComoEnviado(result.messageId());
            historico.setRespostaApi(Map.of("response", result.respostaJson()));
            log.info("Documento WhatsApp enviado para {}: {}", numero, nomeArquivo);
        } else {
            historico.marcarComoFalha(result.erroMensagem(), result.erroCodigo());
            if (result.respostaJson() != null) {
                historico.setRespostaApi(Map.of("error", result.respostaJson()));
            }
            log.error("Erro ao enviar documento WhatsApp para {}: {}", numero, result.erroMensagem());
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
            log.info("[SIMULACAO] Reenvio WhatsApp para {}", historico.getDestinatario());
            historico.marcarComoEnviado("SIMULADO-REENVIO-" + UUID.randomUUID());
            return historicoRepository.save(historico);
        }

        // Reenvia
        EvolutionConfig evolutionConfig = EvolutionConfig.from(config);
        EvolutionSendResult result = evolutionApiClient.enviarTexto(
            evolutionConfig,
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
     * Gera uma mensagem clara explicando por que a notificacao foi agendada.
     */
    private String gerarMotivoAgendamento(ConfiguracaoNotificacao config) {
        java.time.LocalTime agora = java.time.LocalTime.now();
        java.time.DayOfWeek diaSemana = java.time.LocalDate.now().getDayOfWeek();

        // Verifica se e domingo
        if (diaSemana == java.time.DayOfWeek.SUNDAY && !config.getEnviarDomingos()) {
            return "Envio bloqueado aos domingos conforme suas configuracoes. " +
                   "Para enviar agora, desative 'Aplicar horario comercial' ou habilite 'Enviar aos domingos' " +
                   "em Configuracoes > Notificacoes > Politicas de Envio.";
        }

        // Verifica se e sabado
        if (diaSemana == java.time.DayOfWeek.SATURDAY && !config.getEnviarSabados()) {
            return "Envio bloqueado aos sabados conforme suas configuracoes. " +
                   "Para enviar agora, desative 'Aplicar horario comercial' ou habilite 'Enviar aos sabados' " +
                   "em Configuracoes > Notificacoes > Politicas de Envio.";
        }

        // Verifica horario comercial
        java.time.LocalTime inicio = config.getHorarioInicio();
        java.time.LocalTime fim = config.getHorarioFim();

        if (agora.isBefore(inicio)) {
            return String.format(
                "Fora do horario comercial (antes das %s). " +
                "Horario configurado: %s as %s. " +
                "Para enviar agora, desative 'Aplicar horario comercial' em Configuracoes > Notificacoes > Politicas de Envio.",
                inicio, inicio, fim
            );
        }

        if (agora.isAfter(fim)) {
            return String.format(
                "Fora do horario comercial (apos %s). " +
                "Horario configurado: %s as %s. " +
                "Para enviar agora, desative 'Aplicar horario comercial' em Configuracoes > Notificacoes > Politicas de Envio.",
                fim, inicio, fim
            );
        }

        return "Agendado conforme politicas de envio configuradas.";
    }
}
