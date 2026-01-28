package com.pitstop.manutencaopreventiva.mapper;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.manutencaopreventiva.domain.*;
import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.veiculo.domain.Veiculo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Mapper para conversão entre entidades e DTOs do módulo de manutenção preventiva.
 */
@Component
public class ManutencaoMapper {

    // ==================== PLANO ====================

    public PlanoManutencaoResponseDTO toPlanoResponse(PlanoManutencaoPreventiva plano) {
        if (plano == null) return null;

        Veiculo veiculo = plano.getVeiculo();
        TemplateManutencao template = plano.getTemplate();

        Integer diasParaVencer = null;
        Boolean proximoAVencer = false;
        Boolean vencido = false;

        if (plano.getProximaPrevisaoData() != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), plano.getProximaPrevisaoData());
            diasParaVencer = (int) dias;
            proximoAVencer = dias <= (plano.getAntecedenciaDias() != null ? plano.getAntecedenciaDias() : 15) && dias > 0;
            vencido = dias < 0;
        }

        return new PlanoManutencaoResponseDTO(
            plano.getId(),
            mapVeiculoResumo(veiculo),
            mapTemplateResumo(template),
            plano.getNome(),
            plano.getDescricao(),
            plano.getTipoManutencao(),
            plano.getCriterio(),
            plano.getIntervaloDias(),
            plano.getIntervaloKm(),
            plano.getAntecedenciaDias(),
            plano.getAntecedenciaKm(),
            plano.getCanaisNotificacao(),
            plano.getUltimaExecucaoData(),
            plano.getUltimaExecucaoKm(),
            plano.getProximaPrevisaoData(),
            plano.getProximaPrevisaoKm(),
            plano.getStatus(),
            plano.getMotivoPausa(),
            proximoAVencer,
            vencido,
            diasParaVencer,
            plano.getChecklist(),
            plano.getPecasSugeridas(),
            plano.getValorEstimado(),
            plano.getAgendamentosNotificacao(),
            plano.getCreatedAt(),
            plano.getUpdatedAt()
        );
    }

    private PlanoManutencaoResponseDTO.VeiculoResumoDTO mapVeiculoResumo(Veiculo veiculo) {
        if (veiculo == null) return null;

        String clienteNome = null;
        // Cliente name would be fetched separately if needed

        return new PlanoManutencaoResponseDTO.VeiculoResumoDTO(
            veiculo.getId(),
            veiculo.getPlaca(),
            veiculo.getPlacaFormatada(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getAno(),
            veiculo.getQuilometragem(),
            clienteNome
        );
    }

    private PlanoManutencaoResponseDTO.TemplateResumoDTO mapTemplateResumo(TemplateManutencao template) {
        if (template == null) return null;

        return new PlanoManutencaoResponseDTO.TemplateResumoDTO(
            template.getId(),
            template.getNome(),
            template.getTipoManutencao()
        );
    }

    // ==================== TEMPLATE ====================

    public TemplateManutencaoResponseDTO toTemplateResponse(TemplateManutencao template) {
        if (template == null) return null;

        return new TemplateManutencaoResponseDTO(
            template.getId(),
            template.getOficina() != null ? template.getOficina().getId() : null,
            template.isGlobal(),
            template.getNome(),
            template.getDescricao(),
            template.getTipoManutencao(),
            template.getIntervaloDias(),
            template.getIntervaloKm(),
            template.getCriterio(),
            template.getAntecedenciaDias(),
            template.getAntecedenciaKm(),
            template.getChecklist(),
            template.getPecasSugeridas(),
            template.getValorEstimado(),
            template.getTempoEstimadoMinutos(),
            template.getAtivo(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    // ==================== AGENDAMENTO ====================

    public AgendamentoManutencaoResponseDTO toAgendamentoResponse(AgendamentoManutencao agendamento) {
        return toAgendamentoResponse(agendamento, null);
    }

    /**
     * Converte AgendamentoManutencao para DTO incluindo feedback de notificação.
     *
     * @param agendamento O agendamento
     * @param notificacaoFeedback Feedback sobre as notificações (opcional)
     * @return DTO de resposta
     */
    public AgendamentoManutencaoResponseDTO toAgendamentoResponse(
            AgendamentoManutencao agendamento,
            NotificacaoFeedbackDTO notificacaoFeedback) {
        if (agendamento == null) return null;

        return new AgendamentoManutencaoResponseDTO(
            agendamento.getId(),
            mapPlanoResumo(agendamento.getPlano()),
            mapVeiculoResumoAgendamento(agendamento.getVeiculo()),
            mapClienteResumo(agendamento.getCliente()),
            agendamento.getDataAgendamento(),
            agendamento.getHoraAgendamento(),
            agendamento.getDuracaoEstimadaMinutos(),
            agendamento.getTipoManutencao(),
            agendamento.getDescricao(),
            agendamento.getStatus(),
            agendamento.getConfirmadoEm(),
            agendamento.getConfirmadoVia(),
            agendamento.getCanceladoEm(),
            agendamento.getMotivoCancelamento(),
            agendamento.getRealizadoEm(),
            agendamento.getOrdemServico() != null ? agendamento.getOrdemServico().getId() : null,
            agendamento.getLembreteEnviado(),
            agendamento.getObservacoes(),
            agendamento.isHoje(),
            agendamento.isPassado(),
            agendamento.getCreatedAt(),
            notificacaoFeedback
        );
    }

    private AgendamentoManutencaoResponseDTO.PlanoResumoDTO mapPlanoResumo(PlanoManutencaoPreventiva plano) {
        if (plano == null) return null;

        return new AgendamentoManutencaoResponseDTO.PlanoResumoDTO(
            plano.getId(),
            plano.getNome(),
            plano.getTipoManutencao()
        );
    }

    private AgendamentoManutencaoResponseDTO.VeiculoResumoDTO mapVeiculoResumoAgendamento(Veiculo veiculo) {
        if (veiculo == null) return null;

        return new AgendamentoManutencaoResponseDTO.VeiculoResumoDTO(
            veiculo.getId(),
            veiculo.getPlaca(),
            veiculo.getPlacaFormatada(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getAno()
        );
    }

    private AgendamentoManutencaoResponseDTO.ClienteResumoDTO mapClienteResumo(Cliente cliente) {
        if (cliente == null) return null;

        return new AgendamentoManutencaoResponseDTO.ClienteResumoDTO(
            cliente.getId(),
            cliente.getNome(),
            cliente.getCelular(),
            cliente.getEmail()
        );
    }

    // ==================== CALENDÁRIO ====================

    public CalendarioEventoDTO toCalendarioEvento(AgendamentoManutencao agendamento) {
        if (agendamento == null) return null;

        Veiculo veiculo = agendamento.getVeiculo();
        Cliente cliente = agendamento.getCliente();

        LocalDateTime inicio = agendamento.getDataHoraAgendamento();
        LocalDateTime fim = inicio.plusMinutes(
            agendamento.getDuracaoEstimadaMinutos() != null ? agendamento.getDuracaoEstimadaMinutos() : 60
        );

        String cor = switch (agendamento.getStatus()) {
            case AGENDADO -> "#3B82F6"; // blue
            case CONFIRMADO -> "#10B981"; // green
            case REMARCADO -> "#F59E0B"; // amber
            case CANCELADO -> "#EF4444"; // red
            case REALIZADO -> "#6B7280"; // gray
        };

        String titulo = String.format("%s - %s",
            agendamento.getTipoManutencao(),
            veiculo != null ? veiculo.getPlacaFormatada() : "");

        return new CalendarioEventoDTO(
            agendamento.getId(),
            titulo,
            agendamento.getDescricao(),
            inicio,
            fim,
            agendamento.getStatus(),
            cor,
            veiculo != null ? veiculo.getId() : null,
            veiculo != null ? veiculo.getPlacaFormatada() : null,
            veiculo != null ? veiculo.getDescricaoCompleta() : null,
            cliente != null ? cliente.getId() : null,
            cliente != null ? cliente.getNome() : null,
            agendamento.getTipoManutencao()
        );
    }
}
