package com.pitstop.notificacao.event;

import com.pitstop.notificacao.domain.EventoNotificacao;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de aplicacao para mudancas em Ordem de Servico.
 *
 * @author PitStop Team
 */
@Getter
public class OrdemServicoEvent extends ApplicationEvent {

    // Formatadores para padrão brasileiro
    private static final DateTimeFormatter DATE_FORMAT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final EventoNotificacao tipoEvento;
    private final UUID oficinaId;
    private final UUID ordemServicoId;
    private final Long numeroOS;
    private final UUID clienteId;
    private final String nomeCliente;
    private final String emailCliente;
    private final String telefoneCliente;
    private final String veiculoPlaca;
    private final String veiculoModelo;
    private final BigDecimal valorTotal;
    private final String nomeOficina;
    private final Map<String, Object> dadosExtras;

    public OrdemServicoEvent(
        Object source,
        EventoNotificacao tipoEvento,
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String veiculoModelo,
        BigDecimal valorTotal,
        String nomeOficina
    ) {
        super(source);
        this.tipoEvento = tipoEvento;
        this.oficinaId = oficinaId;
        this.ordemServicoId = ordemServicoId;
        this.numeroOS = numeroOS;
        this.clienteId = clienteId;
        this.nomeCliente = nomeCliente;
        this.emailCliente = emailCliente;
        this.telefoneCliente = telefoneCliente;
        this.veiculoPlaca = veiculoPlaca;
        this.veiculoModelo = veiculoModelo;
        this.valorTotal = valorTotal;
        this.nomeOficina = nomeOficina;
        this.dadosExtras = new HashMap<>();
    }

    /**
     * Adiciona dado extra ao evento.
     */
    public OrdemServicoEvent comDadoExtra(String chave, Object valor) {
        this.dadosExtras.put(chave, valor);
        return this;
    }

    /**
     * Converte para mapa de variaveis para template.
     */
    public Map<String, Object> toVariaveis() {
        Map<String, Object> variaveis = new HashMap<>();
        variaveis.put("numeroOS", numeroOS);
        variaveis.put("nomeCliente", nomeCliente);
        variaveis.put("veiculoPlaca", veiculoPlaca);
        variaveis.put("veiculoModelo", veiculoModelo);
        variaveis.put("valorTotal", valorTotal != null ? valorTotal.toString() : "0.00");
        variaveis.put("nomeOficina", nomeOficina);
        variaveis.put("dataEvento", LocalDateTime.now().format(DATETIME_FORMAT_BR));

        // Adiciona dados extras
        variaveis.putAll(dadosExtras);

        return variaveis;
    }

    // ===== FACTORY METHODS =====

    /**
     * Cria evento de OS criada.
     */
    public static OrdemServicoEvent osCriada(
        Object source,
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String veiculoModelo,
        BigDecimal valorTotal,
        String nomeOficina
    ) {
        return new OrdemServicoEvent(
            source,
            EventoNotificacao.OS_CRIADA,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            veiculoPlaca,
            veiculoModelo,
            valorTotal,
            nomeOficina
        );
    }

    /**
     * Cria evento de OS aprovada.
     */
    public static OrdemServicoEvent osAprovada(
        Object source,
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String nomeOficina
    ) {
        return new OrdemServicoEvent(
            source,
            EventoNotificacao.OS_APROVADA,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            null, null, null,
            nomeOficina
        ).comDadoExtra("dataAprovacao", LocalDateTime.now().format(DATETIME_FORMAT_BR));
    }

    /**
     * Cria evento de OS em andamento.
     */
    public static OrdemServicoEvent osEmAndamento(
        Object source,
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String veiculoModelo,
        String nomeMecanico,
        LocalDateTime previsao,
        String nomeOficina
    ) {
        return new OrdemServicoEvent(
            source,
            EventoNotificacao.OS_EM_ANDAMENTO,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            veiculoPlaca,
            veiculoModelo,
            null,
            nomeOficina
        )
        .comDadoExtra("mecanico", nomeMecanico)
        .comDadoExtra("dataPrevisao", previsao != null ? previsao.format(DATE_FORMAT_BR) : "A definir");
    }

    /**
     * Cria evento de OS finalizada.
     */
    public static OrdemServicoEvent osFinalizada(
        Object source,
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String veiculoModelo,
        BigDecimal valorTotal,
        String servicosRealizados,
        String nomeOficina
    ) {
        return new OrdemServicoEvent(
            source,
            EventoNotificacao.OS_FINALIZADA,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            veiculoPlaca,
            veiculoModelo,
            valorTotal,
            nomeOficina
        ).comDadoExtra("servicosRealizados", servicosRealizados);
    }

    /**
     * Cria evento de OS entregue.
     */
    public static OrdemServicoEvent osEntregue(
        Object source,
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String nomeOficina
    ) {
        return new OrdemServicoEvent(
            source,
            EventoNotificacao.OS_ENTREGUE,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            veiculoPlaca,
            null, null,
            nomeOficina
        ).comDadoExtra("dataEntrega", LocalDateTime.now().format(DATETIME_FORMAT_BR));
    }
}
