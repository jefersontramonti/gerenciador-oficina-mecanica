package com.pitstop.notificacao.service;

import com.pitstop.notificacao.domain.EventoNotificacao;
import com.pitstop.notificacao.event.OrdemServicoEvent;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
     * URL base do frontend para geração de links (ex: aprovação de orçamento).
     * Configurável via variável de ambiente ou application.properties.
     */
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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

        // Adiciona link de aprovação (usando URL configurável)
        if (tokenAprovacao != null) {
            String linkAprovacao = frontendUrl + "/orcamento/aprovar?token=" + tokenAprovacao;
            event.comDadoExtra("linkAprovacao", linkAprovacao);
            log.debug("Link de aprovação gerado: {}", linkAprovacao);
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
     * Publica evento de OS rejeitada.
     * Nota: Aceita oficinaId explícito pois pode ser chamado de endpoint público.
     */
    public void publicarOSRejeitada(
        UUID oficinaId,
        UUID ordemServicoId,
        Long numeroOS,
        UUID clienteId,
        String nomeCliente,
        String emailCliente,
        String telefoneCliente,
        String motivoRejeicao,
        String nomeOficina
    ) {
        OrdemServicoEvent event = OrdemServicoEvent.osRejeitada(
            this,
            oficinaId,
            ordemServicoId,
            numeroOS,
            clienteId,
            nomeCliente,
            emailCliente,
            telefoneCliente,
            motivoRejeicao,
            nomeOficina
        );

        eventPublisher.publishEvent(event);
        log.debug("Evento OS_REJEITADA publicado para OS #{}", numeroOS);
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
