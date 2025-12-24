package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.event.OrdemServicoEvent;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publisher de eventos de notificacao.
 *
 * Esta classe e injetada nos services de dominio (OrdemServicoService, etc.)
 * para publicar eventos de notificacao de forma assincrona.
 *
 * @author PitStop Team
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificacaoEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publica evento de OS criada.
     */
    public void publicarOSCriada(
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String veiculoModelo,
        BigDecimal valorTotal,
        String nomeOficina,
        String tokenAprovacao
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        OrdemServicoEvent event = OrdemServicoEvent.osCriada(
            this,
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

        // Adiciona link de aprovação
        if (tokenAprovacao != null) {
            event.comDadoExtra("linkAprovacao",
                "http://localhost:5173/orcamento/aprovar?token=" + tokenAprovacao);
        }

        eventPublisher.publishEvent(event);
        log.debug("Evento OS_CRIADA publicado para OS #{}", numeroOS);
    }

    /**
     * Publica evento de OS aprovada.
     */
    public void publicarOSAprovada(
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String nomeOficina
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        OrdemServicoEvent event = OrdemServicoEvent.osAprovada(
            this,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            nomeOficina
        );

        eventPublisher.publishEvent(event);
        log.debug("Evento OS_APROVADA publicado para OS #{}", numeroOS);
    }

    /**
     * Publica evento de OS em andamento.
     */
    public void publicarOSEmAndamento(
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
        UUID oficinaId = TenantContext.getTenantId();

        OrdemServicoEvent event = OrdemServicoEvent.osEmAndamento(
            this,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            veiculoPlaca,
            veiculoModelo,
            nomeMecanico,
            previsao,
            nomeOficina
        );

        eventPublisher.publishEvent(event);
        log.debug("Evento OS_EM_ANDAMENTO publicado para OS #{}", numeroOS);
    }

    /**
     * Publica evento de OS finalizada.
     */
    public void publicarOSFinalizada(
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
        UUID oficinaId = TenantContext.getTenantId();

        OrdemServicoEvent event = OrdemServicoEvent.osFinalizada(
            this,
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
            servicosRealizados,
            nomeOficina
        );

        eventPublisher.publishEvent(event);
        log.debug("Evento OS_FINALIZADA publicado para OS #{}", numeroOS);
    }

    /**
     * Publica evento de OS entregue.
     */
    public void publicarOSEntregue(
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String veiculoPlaca,
        String nomeOficina
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        OrdemServicoEvent event = OrdemServicoEvent.osEntregue(
            this,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            veiculoPlaca,
            nomeOficina
        );

        eventPublisher.publishEvent(event);
        log.debug("Evento OS_ENTREGUE publicado para OS #{}", numeroOS);
    }

    /**
     * Publica evento generico de OS.
     */
    public void publicarEventoOS(
        EventoNotificacao evento,
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
        UUID oficinaId = TenantContext.getTenantId();

        OrdemServicoEvent event = new OrdemServicoEvent(
            this,
            evento,
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

        eventPublisher.publishEvent(event);
        log.debug("Evento {} publicado para OS #{}", evento, numeroOS);
    }
}
