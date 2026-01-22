package com.pitstop.manutencaopreventiva.service;

import com.pitstop.manutencaopreventiva.domain.AgendamentoManutencao;
import com.pitstop.manutencaopreventiva.domain.StatusAgendamento;
import com.pitstop.manutencaopreventiva.dto.AgendamentoPublicoDTO;
import com.pitstop.manutencaopreventiva.repository.AgendamentoManutencaoRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.veiculo.domain.Veiculo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Serviço público para confirmação de agendamentos via token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoPublicService {

    private final AgendamentoManutencaoRepository agendamentoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, String> TIPO_MANUTENCAO_DESCRICAO = Map.ofEntries(
        Map.entry("TROCA_OLEO", "Troca de Óleo"),
        Map.entry("REVISAO", "Revisão"),
        Map.entry("ALINHAMENTO", "Alinhamento"),
        Map.entry("BALANCEAMENTO", "Balanceamento"),
        Map.entry("FREIOS", "Freios"),
        Map.entry("SUSPENSAO", "Suspensão"),
        Map.entry("AR_CONDICIONADO", "Ar Condicionado"),
        Map.entry("CORREIA_DENTADA", "Correia Dentada"),
        Map.entry("FILTROS", "Filtros"),
        Map.entry("OUTROS", "Outros")
    );

    /**
     * Busca agendamento por token para exibição pública.
     */
    @Transactional(readOnly = true)
    public AgendamentoPublicoDTO buscarPorToken(String token) {
        AgendamentoManutencao agendamento = agendamentoRepository.findByTokenConfirmacao(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado ou link expirado"));

        // Verifica se já foi confirmado
        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            return AgendamentoPublicoDTO.jaConfirmado(
                "Este agendamento já foi confirmado em " +
                (agendamento.getConfirmadoEm() != null
                    ? agendamento.getConfirmadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))
                    : "")
            );
        }

        // Verifica se foi cancelado
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            return AgendamentoPublicoDTO.erro("Este agendamento foi cancelado.");
        }

        // Verifica se foi realizado
        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            return AgendamentoPublicoDTO.erro("Este agendamento já foi realizado.");
        }

        // Verifica se token expirou
        if (!agendamento.isTokenValido(token)) {
            return AgendamentoPublicoDTO.erro("O link de confirmação expirou. Entre em contato com a oficina.");
        }

        Veiculo veiculo = agendamento.getVeiculo();
        Oficina oficina = agendamento.getOficina();

        String tipoManutencaoDesc = TIPO_MANUTENCAO_DESCRICAO.getOrDefault(
            agendamento.getTipoManutencao(),
            agendamento.getTipoManutencao()
        );

        String statusDesc = getStatusDescricao(agendamento.getStatus());

        String oficinaEndereco = null;
        if (oficina.getEndereco() != null) {
            var end = oficina.getEndereco();
            oficinaEndereco = String.format("%s, %s - %s, %s",
                end.getLogradouro() != null ? end.getLogradouro() : "",
                end.getNumero() != null ? end.getNumero() : "S/N",
                end.getBairro() != null ? end.getBairro() : "",
                end.getCidade() != null ? end.getCidade() : ""
            );
        }

        return new AgendamentoPublicoDTO(
            "OK",
            null,
            agendamento.getStatus() == StatusAgendamento.AGENDADO,
            agendamento.getDataAgendamento(),
            agendamento.getDataAgendamento().format(DATE_FORMATTER),
            agendamento.getHoraAgendamento(),
            agendamento.getHoraAgendamento().format(TIME_FORMATTER),
            agendamento.getDuracaoEstimadaMinutos(),
            agendamento.getTipoManutencao(),
            tipoManutencaoDesc,
            agendamento.getDescricao(),
            agendamento.getObservacoes(),
            agendamento.getStatus(),
            statusDesc,
            veiculo.getPlacaFormatada(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getAno(),
            oficina.getNomeFantasia() != null ? oficina.getNomeFantasia() : oficina.getRazaoSocial(),
            null, // Oficina não tem telefone direto
            oficinaEndereco
        );
    }

    /**
     * Confirma agendamento via token.
     */
    @Transactional
    public Map<String, Object> confirmar(String token) {
        AgendamentoManutencao agendamento = agendamentoRepository.findByTokenConfirmacao(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        Map<String, Object> response = new HashMap<>();

        // Já confirmado
        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            response.put("status", "JA_CONFIRMADO");
            response.put("mensagem", "Este agendamento já foi confirmado anteriormente.");
            return response;
        }

        // Token expirado
        if (!agendamento.isTokenValido(token)) {
            response.put("status", "ERRO");
            response.put("mensagem", "O link de confirmação expirou. Entre em contato com a oficina.");
            return response;
        }

        // Não pode confirmar (status inválido)
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            response.put("status", "ERRO");
            response.put("mensagem", "Este agendamento não pode mais ser confirmado.");
            return response;
        }

        // Confirmar
        agendamento.confirmar("LINK");
        agendamentoRepository.save(agendamento);

        log.info("Agendamento {} confirmado via link pelo cliente", agendamento.getId());

        String dataFormatada = agendamento.getDataAgendamento().format(DATE_FORMATTER);
        String horaFormatada = agendamento.getHoraAgendamento().format(TIME_FORMATTER);

        response.put("status", "CONFIRMADO");
        response.put("mensagem", String.format(
            "Agendamento confirmado com sucesso! Esperamos você no dia %s às %s.",
            dataFormatada, horaFormatada
        ));
        response.put("dataAgendamento", dataFormatada);
        response.put("horaAgendamento", horaFormatada);

        return response;
    }

    /**
     * Rejeita agendamento via token (cliente não pode comparecer).
     */
    @Transactional
    public Map<String, Object> rejeitar(String token, String motivo) {
        AgendamentoManutencao agendamento = agendamentoRepository.findByTokenConfirmacao(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        Map<String, Object> response = new HashMap<>();

        // Token expirado
        if (!agendamento.isTokenValido(token)) {
            response.put("status", "ERRO");
            response.put("mensagem", "O link expirou. Entre em contato com a oficina.");
            return response;
        }

        // Já cancelado
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            response.put("status", "JA_CANCELADO");
            response.put("mensagem", "Este agendamento já foi cancelado.");
            return response;
        }

        // Já confirmado - pode ser que queira cancelar depois de confirmar
        // Vamos permitir cancelar se ainda não foi realizado

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO) {
            response.put("status", "ERRO");
            response.put("mensagem", "Este agendamento já foi realizado e não pode ser cancelado.");
            return response;
        }

        // Cancelar
        String motivoFinal = motivo != null && !motivo.isBlank()
            ? "Cliente via link: " + motivo
            : "Cancelado pelo cliente via link";
        agendamento.cancelar(motivoFinal, null);
        agendamentoRepository.save(agendamento);

        log.info("Agendamento {} rejeitado/cancelado via link pelo cliente. Motivo: {}",
            agendamento.getId(), motivoFinal);

        response.put("status", "CANCELADO");
        response.put("mensagem", "Agendamento cancelado. Se desejar reagendar, entre em contato com a oficina.");

        return response;
    }

    private String getStatusDescricao(StatusAgendamento status) {
        return switch (status) {
            case AGENDADO -> "Aguardando Confirmação";
            case CONFIRMADO -> "Confirmado";
            case REMARCADO -> "Remarcado";
            case CANCELADO -> "Cancelado";
            case REALIZADO -> "Realizado";
        };
    }
}
