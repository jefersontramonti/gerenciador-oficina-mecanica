package com.pitstop.manutencaopreventiva.scheduler;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.manutencaopreventiva.domain.*;
import com.pitstop.manutencaopreventiva.repository.*;
import com.pitstop.manutencaopreventiva.service.AlertaManutencaoService;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.domain.TipoCobrancaMaoObra;
import com.pitstop.ordemservico.dto.CreateOrdemServicoDTO;
import com.pitstop.ordemservico.dto.OrdemServicoResponseDTO;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.ordemservico.service.OrdemServicoService;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import com.pitstop.veiculo.domain.Veiculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Jobs agendados para manutenção preventiva.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ManutencaoPreventivaScheduler {

    private final PlanoManutencaoRepository planoRepository;
    private final AlertaManutencaoRepository alertaRepository;
    private final AgendamentoManutencaoRepository agendamentoRepository;
    private final ConfiguracaoManutencaoRepository configuracaoRepository;
    private final ClienteRepository clienteRepository;
    private final AlertaManutencaoService alertaManutencaoService;
    private final OrdemServicoService ordemServicoService;
    private final OrdemServicoRepository ordemServicoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Verifica planos próximos a vencer e gera alertas.
     * Executa todo dia às 8h.
     *
     * IMPORTANTE: Usa o campo antecedenciaDias de cada plano para determinar
     * quando enviar o alerta (não um valor fixo).
     *
     * Se o plano tiver notificações agendadas manualmente, o alerta automático
     * NÃO será enviado (o usuário controla quando notificar).
     */
    @Scheduled(cron = "${manutencao.jobs.verificar-pendentes.cron:0 0 8 * * ?}")
    @Transactional
    public void verificarManutencoesPendentes() {
        log.info("Iniciando verificação de manutenções pendentes...");

        LocalDate hoje = LocalDate.now();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);

        // Busca todos os planos ativos
        List<PlanoManutencaoPreventiva> planosAtivos = planoRepository.findAll().stream()
            .filter(p -> p.getStatus() == StatusPlanoManutencao.ATIVO)
            .filter(p -> p.getAtivo())
            .filter(p -> p.getProximaPrevisaoData() != null)
            // Se tem notificações agendadas manualmente, não envia automático
            .filter(p -> p.getAgendamentosNotificacao() == null || p.getAgendamentosNotificacao().isEmpty())
            // Não enviou alerta recentemente (últimas 24h)
            .filter(p -> p.getUltimoAlertaEnviadoEm() == null || p.getUltimoAlertaEnviadoEm().isBefore(ontem))
            .toList();

        int alertasGerados = 0;
        for (PlanoManutencaoPreventiva plano : planosAtivos) {
            try {
                // Usa a antecedência configurada no plano (padrão 15 dias se não definido)
                int antecedenciaDias = plano.getAntecedenciaDias() != null ? plano.getAntecedenciaDias() : 15;
                LocalDate dataLimiteAlerta = hoje.plusDays(antecedenciaDias);

                // Verifica se a próxima manutenção está dentro da janela de antecedência
                if (!plano.getProximaPrevisaoData().isAfter(dataLimiteAlerta)) {
                    if (gerarAlertaParaPlano(plano, TipoAlerta.PROXIMIDADE)) {
                        alertasGerados++;
                        log.debug("Alerta gerado para plano {} (antecedência: {} dias)",
                            plano.getId(), antecedenciaDias);
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao gerar alerta para plano {}: {}", plano.getId(), e.getMessage());
            }
        }

        log.info("Verificação concluída. Alertas gerados: {}", alertasGerados);
    }

    /**
     * Verifica planos vencidos e gera alertas de vencimento.
     * Executa todo dia às 8h30.
     */
    @Scheduled(cron = "${manutencao.jobs.verificar-vencidos.cron:0 30 8 * * ?}")
    @Transactional
    public void verificarManutencaoVencidas() {
        log.info("Iniciando verificação de manutenções vencidas...");

        LocalDate hoje = LocalDate.now();
        LocalDateTime seteDiasAtras = LocalDateTime.now().minusDays(7);

        // Buscar planos vencidos que não receberam alerta de vencimento recentemente
        List<PlanoManutencaoPreventiva> planosVencidos = planoRepository.findAll().stream()
            .filter(p -> p.getStatus() == StatusPlanoManutencao.ATIVO)
            .filter(p -> p.isVencido())
            .filter(p -> p.getUltimoAlertaEnviadoEm() == null ||
                        p.getUltimoAlertaEnviadoEm().isBefore(seteDiasAtras))
            .toList();

        int alertasGerados = 0;
        for (PlanoManutencaoPreventiva plano : planosVencidos) {
            try {
                if (gerarAlertaParaPlano(plano, TipoAlerta.VENCIDO)) {
                    alertasGerados++;
                }
            } catch (Exception e) {
                log.error("Erro ao gerar alerta de vencimento para plano {}: {}", plano.getId(), e.getMessage());
            }
        }

        log.info("Verificação de vencidos concluída. Alertas gerados: {}", alertasGerados);
    }

    /**
     * Envia lembretes para agendamentos do dia.
     * Executa todo dia às 7h.
     */
    @Scheduled(cron = "${manutencao.jobs.lembretes-agendamentos.cron:0 0 7 * * ?}")
    @Transactional
    public void enviarLembretesAgendamentos() {
        log.info("Iniciando envio de lembretes de agendamentos...");

        List<AgendamentoManutencao> agendamentos = agendamentoRepository.findParaEnviarLembrete(LocalDate.now());

        int lembretesEnviados = 0;
        for (AgendamentoManutencao agendamento : agendamentos) {
            try {
                gerarAlertaLembrete(agendamento);
                agendamento.setLembreteEnviado(true);
                agendamento.setLembreteEnviadoEm(LocalDateTime.now());
                agendamentoRepository.save(agendamento);
                lembretesEnviados++;
            } catch (Exception e) {
                log.error("Erro ao enviar lembrete para agendamento {}: {}", agendamento.getId(), e.getMessage());
            }
        }

        log.info("Envio de lembretes concluído. Lembretes enviados: {}", lembretesEnviados);
    }

    /**
     * Atualiza status dos planos vencidos.
     * Executa todo dia à 1h.
     */
    @Scheduled(cron = "${manutencao.jobs.atualizar-status.cron:0 0 1 * * ?}")
    @Transactional
    public void atualizarStatusPlanos() {
        log.info("Iniciando atualização de status dos planos...");

        List<PlanoManutencaoPreventiva> planosAtivos = planoRepository.findAll().stream()
            .filter(p -> p.getStatus() == StatusPlanoManutencao.ATIVO)
            .filter(p -> p.getAtivo())
            .toList();

        int planosAtualizados = 0;
        for (PlanoManutencaoPreventiva plano : planosAtivos) {
            if (plano.isVencido()) {
                // Não muda automaticamente para VENCIDO, apenas mantém ATIVO
                // O status VENCIDO é informativo e pode ser visto na listagem
                plano.calcularProximaManutencao();
                planoRepository.save(plano);
                planosAtualizados++;
            }
        }

        log.info("Atualização de status concluída. Planos atualizados: {}", planosAtualizados);
    }

    /**
     * Processa e envia alertas pendentes.
     * Executa a cada 30 minutos.
     *
     * Este job é responsável por enviar os alertas gerados pelos outros jobs
     * via WhatsApp, Email e Telegram.
     */
    @Scheduled(cron = "${manutencao.jobs.processar-alertas.cron:0 */30 * * * ?}")
    public void processarEEnviarAlertas() {
        log.info("Iniciando processamento e envio de alertas de manutenção...");

        try {
            int enviados = alertaManutencaoService.processarAlertasPendentes();
            log.info("Processamento concluído. Alertas enviados: {}", enviados);
        } catch (Exception e) {
            log.error("Erro ao processar alertas: {}", e.getMessage(), e);
        }
    }

    /**
     * Processa notificações agendadas pelo usuário.
     * Executa a cada 5 minutos para verificar se há notificações a enviar.
     */
    @Scheduled(cron = "${manutencao.jobs.notificacoes-agendadas.cron:0 */5 * * * ?}")
    @Transactional
    public void processarNotificacoesAgendadas() {
        log.info("Verificando notificações agendadas para envio...");

        LocalDateTime agora = LocalDateTime.now();
        // Janela de 5 minutos antes e depois da hora atual
        LocalDateTime inicio = agora.minusMinutes(5);
        LocalDateTime fim = agora.plusMinutes(5);

        List<PlanoManutencaoPreventiva> planosComNotificacoes = planoRepository.findAll().stream()
            .filter(p -> p.getStatus() == StatusPlanoManutencao.ATIVO)
            .filter(p -> p.getAtivo())
            .filter(p -> p.getAgendamentosNotificacao() != null && !p.getAgendamentosNotificacao().isEmpty())
            .toList();

        int notificacoesEnviadas = 0;

        for (PlanoManutencaoPreventiva plano : planosComNotificacoes) {
            List<AgendamentoNotificacao> agendamentos = plano.getAgendamentosNotificacao();

            for (AgendamentoNotificacao agendamento : agendamentos) {
                // Verifica se já foi enviado
                if (Boolean.TRUE.equals(agendamento.getEnviado())) {
                    continue;
                }

                LocalDateTime dataHoraAgendada = agendamento.getDataHora();
                if (dataHoraAgendada == null) {
                    continue;
                }

                // Verifica se está na janela de envio
                if (!dataHoraAgendada.isBefore(inicio) || dataHoraAgendada.isAfter(fim)) {
                    // Ainda não está na hora ou já passou muito tempo
                    if (dataHoraAgendada.isAfter(fim)) {
                        continue; // Ainda não chegou a hora
                    }
                    if (dataHoraAgendada.isBefore(inicio.minusMinutes(30))) {
                        // Passou mais de 30 min, marca como erro
                        agendamento.marcarComoFalha("Horário de envio perdido");
                        continue;
                    }
                }

                try {
                    // Envia a notificação criando uma OS
                    if (enviarNotificacaoAgendada(plano, agendamento)) {
                        agendamento.marcarComoEnviado();
                        notificacoesEnviadas++;
                        log.info("Notificação agendada enviada para plano {}", plano.getId());
                    }
                } catch (Exception e) {
                    log.error("Erro ao enviar notificação agendada para plano {}: {}",
                        plano.getId(), e.getMessage());
                    agendamento.marcarComoFalha(e.getMessage());
                }
            }

            // Salva as atualizações no plano
            planoRepository.save(plano);
        }

        log.info("Processamento de notificações agendadas concluído. Enviadas: {}", notificacoesEnviadas);
    }

    /**
     * Envia notificação para um agendamento específico, criando OS e disparando alertas.
     */
    private boolean enviarNotificacaoAgendada(PlanoManutencaoPreventiva plano, AgendamentoNotificacao agendamento) {
        Veiculo veiculo = plano.getVeiculo();
        if (veiculo == null) {
            return false;
        }

        UUID clienteId = veiculo.getClienteId();
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            return false;
        }

        TenantContext.setTenantId(plano.getOficina().getId());

        try {
            // Cria OS automaticamente
            OrdemServicoResponseDTO osResponse = criarOrdemServicoParaManutencao(plano, veiculo, TipoAlerta.PROXIMIDADE);

            if (osResponse != null) {
                log.info("OS {} criada via notificação agendada para plano {}",
                    osResponse.numero(), plano.getId());

                OrdemServico os = ordemServicoRepository.findById(osResponse.id()).orElse(null);
                if (os != null) {
                    plano.registrarExecucao(null, null, os);
                }
            }

            plano.setUltimoAlertaEnviadoEm(LocalDateTime.now());
            return true;
        } catch (Exception e) {
            log.error("Erro ao processar notificação agendada: {}", e.getMessage(), e);
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Gera alerta para um plano de manutenção E cria OS automaticamente.
     * A OS criada dispara notificações via WhatsApp, Email e Telegram.
     */
    private boolean gerarAlertaParaPlano(PlanoManutencaoPreventiva plano, TipoAlerta tipoAlerta) {
        // Verifica se já existe alerta pendente
        if (alertaRepository.existsAlertaPendente(plano.getId(), tipoAlerta)) {
            return false;
        }

        Veiculo veiculo = plano.getVeiculo();
        if (veiculo == null) {
            return false;
        }

        UUID clienteId = veiculo.getClienteId();
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            return false;
        }

        // Define contexto do tenant para a criação da OS
        TenantContext.setTenantId(plano.getOficina().getId());

        try {
            // ========== CRIA ORDEM DE SERVIÇO AUTOMATICAMENTE ==========
            OrdemServicoResponseDTO osResponse = criarOrdemServicoParaManutencao(plano, veiculo, tipoAlerta);

            if (osResponse != null) {
                log.info("OS {} criada automaticamente para manutenção {} do plano {}",
                    osResponse.numero(), tipoAlerta, plano.getId());

                // Vincula a OS ao plano
                OrdemServico os = ordemServicoRepository.findById(osResponse.id()).orElse(null);
                if (os != null) {
                    plano.registrarExecucao(null, null, os);
                }
            }
            // A OS já dispara notificações automaticamente (OS_CRIADA)
            // Não precisamos mais gerar alertas separados para os canais

            plano.setUltimoAlertaEnviadoEm(LocalDateTime.now());
            planoRepository.save(plano);

            return true;
        } catch (Exception e) {
            log.error("Erro ao criar OS para plano {}: {}", plano.getId(), e.getMessage(), e);
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Cria uma Ordem de Serviço automaticamente para manutenção preventiva.
     * A OS dispara notificações via WhatsApp, Email e Telegram.
     */
    private OrdemServicoResponseDTO criarOrdemServicoParaManutencao(
            PlanoManutencaoPreventiva plano,
            Veiculo veiculo,
            TipoAlerta tipoAlerta) {

        // Busca um usuário ADMIN ou GERENTE da oficina para ser o responsável
        Usuario responsavel = usuarioRepository.findByOficinaIdAndAtivoTrue(plano.getOficina().getId())
            .stream()
            .filter(u -> u.getPerfil().name().equals("ADMIN") || u.getPerfil().name().equals("GERENTE"))
            .findFirst()
            .orElse(null);

        if (responsavel == null) {
            log.warn("Nenhum usuário ADMIN/GERENTE encontrado para oficina {}", plano.getOficina().getId());
            return null;
        }

        // Monta descrição baseada no tipo de alerta
        String tipoMensagem = tipoAlerta == TipoAlerta.VENCIDO
            ? "⚠️ MANUTENÇÃO VENCIDA"
            : "🔧 Manutenção Preventiva";

        String problemasRelatados = String.format(
            "%s: %s\n\n" +
            "Veículo: %s %s (%s)\n" +
            "Previsão: %s\n\n" +
            "Esta ordem de serviço foi criada automaticamente pelo sistema de manutenção preventiva.",
            tipoMensagem,
            plano.getTipoManutencao(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getPlacaFormatada(),
            plano.getProximaPrevisaoData() != null ? plano.getProximaPrevisaoData().toString() : "A definir"
        );

        // Cria a OS com valor estimado do plano (se disponível)
        BigDecimal valorMaoObra = plano.getValorEstimado() != null
            ? plano.getValorEstimado()
            : BigDecimal.ZERO;

        CreateOrdemServicoDTO createDTO = new CreateOrdemServicoDTO(
            veiculo.getId(),
            responsavel.getId(),
            problemasRelatados,
            LocalDate.now().plusDays(7), // Previsão: 7 dias
            TipoCobrancaMaoObra.VALOR_FIXO,
            valorMaoObra,
            null, // tempoEstimadoHoras
            null, // limiteHorasAprovado
            null, // descontoPercentual
            null, // descontoValor
            "Manutenção Preventiva: " + plano.getTipoManutencao(),
            "Gerado automaticamente - " + tipoAlerta.name(),
            null  // itens
        );

        return ordemServicoService.criar(createDTO);
    }

    /**
     * Gera alerta de lembrete para agendamento.
     */
    private void gerarAlertaLembrete(AgendamentoManutencao agendamento) {
        Cliente cliente = agendamento.getCliente();
        Veiculo veiculo = agendamento.getVeiculo();

        if (cliente == null || veiculo == null) {
            return;
        }

        // WhatsApp
        if (cliente.getTelefone() != null && !cliente.getTelefone().isBlank()) {
            String mensagem = String.format(
                "🔔 Lembrete!\n\n%s, sua manutenção é hoje às %s!\n\n%s - %s\n\nNos vemos em breve! 😊",
                cliente.getNome(),
                agendamento.getHoraAgendamento(),
                agendamento.getTipoManutencao(),
                veiculo.getPlacaFormatada()
            );

            AlertaManutencao alerta = AlertaManutencao.builder()
                .oficina(agendamento.getOficina())
                .plano(agendamento.getPlano())
                .veiculo(veiculo)
                .cliente(cliente)
                .tipoAlerta(TipoAlerta.LEMBRETE_AGENDAMENTO)
                .canal(CanalNotificacao.WHATSAPP)
                .destinatario(cliente.getTelefone())
                .titulo("Lembrete de Agendamento")
                .mensagem(mensagem)
                .build();

            alertaRepository.save(alerta);
        }
    }

    private String getDestinatario(Cliente cliente, CanalNotificacao canal) {
        return switch (canal) {
            case WHATSAPP, SMS -> cliente.getTelefone();
            case EMAIL -> cliente.getEmail();
            case TELEGRAM -> "TELEGRAM"; // Placeholder - chatId vem da ConfiguracaoNotificacao
            default -> null;
        };
    }

    private String getTitulo(TipoAlerta tipo) {
        return switch (tipo) {
            case PROXIMIDADE -> "Manutenção Preventiva Próxima";
            case VENCIDO -> "Manutenção Preventiva Vencida";
            case LEMBRETE_AGENDAMENTO -> "Lembrete de Agendamento";
            case CONFIRMACAO -> "Confirmação de Agendamento";
        };
    }

    private String gerarMensagem(PlanoManutencaoPreventiva plano, Cliente cliente, Veiculo veiculo, TipoAlerta tipo) {
        if (tipo == TipoAlerta.PROXIMIDADE) {
            return String.format(
                "Olá %s! 🔧\n\nSeu %s (%s) está próximo da %s!\n\n📅 Previsão: %s\n\nAgende agora sua manutenção.",
                cliente.getNome(),
                veiculo.getDescricaoCompleta(),
                veiculo.getPlacaFormatada(),
                plano.getTipoManutencao(),
                plano.getProximaPrevisaoData() != null ? plano.getProximaPrevisaoData().toString() : "Em breve"
            );
        } else if (tipo == TipoAlerta.VENCIDO) {
            return String.format(
                "⚠️ Atenção %s!\n\nA %s do seu %s está VENCIDA!\n\nÉ importante realizar o quanto antes.\n\nAgende sua manutenção.",
                cliente.getNome(),
                plano.getTipoManutencao(),
                veiculo.getDescricaoCompleta()
            );
        }

        return "";
    }
}
