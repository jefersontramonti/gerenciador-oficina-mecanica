package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.saas.domain.Plano;
import com.pitstop.saas.dto.UsoLimitesDTO;
import com.pitstop.saas.exception.LimiteOsMesExcedidoException;
import com.pitstop.saas.exception.LimiteUsuariosExcedidoException;
import com.pitstop.saas.repository.PlanoRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Service for validating and checking plan limits.
 *
 * <p>Validates user and service order limits based on the workshop's subscription plan.</p>
 *
 * @author PitStop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanoLimiteService {

    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final PlanoRepository planoRepository;

    /**
     * Validates if workshop can create a new user based on plan limits.
     *
     * @param oficinaId ID da oficina (tenant)
     * @throws LimiteUsuariosExcedidoException if limit is reached
     */
    public void validarLimiteUsuarios(UUID oficinaId) {
        Oficina oficina = getOficina(oficinaId);
        Plano plano = obterPlano(oficina);

        // -1 means unlimited
        if (plano.getLimiteUsuarios() == null || plano.getLimiteUsuarios() == -1) {
            log.debug("Oficina {} tem usuários ilimitados (plano {})", oficinaId, plano.getNome());
            return;
        }

        long usuariosAtivos = usuarioRepository.countByOficinaIdAndAtivoTrue(oficinaId);

        if (usuariosAtivos >= plano.getLimiteUsuarios()) {
            log.warn("Oficina {} atingiu limite de usuários: {}/{} (plano {})",
                oficinaId, usuariosAtivos, plano.getLimiteUsuarios(), plano.getNome());
            throw new LimiteUsuariosExcedidoException(
                plano.getNome(),
                plano.getLimiteUsuarios(),
                usuariosAtivos
            );
        }

        log.debug("Oficina {} pode criar usuário: {}/{} (plano {})",
            oficinaId, usuariosAtivos, plano.getLimiteUsuarios(), plano.getNome());
    }

    /**
     * Validates if workshop can create a new service order based on monthly plan limits.
     *
     * @param oficinaId ID da oficina (tenant)
     * @throws LimiteOsMesExcedidoException if monthly limit is reached
     */
    public void validarLimiteOsMes(UUID oficinaId) {
        Oficina oficina = getOficina(oficinaId);
        Plano plano = obterPlano(oficina);

        // -1 means unlimited
        if (plano.getLimiteOsMes() == null || plano.getLimiteOsMes() == -1) {
            log.debug("Oficina {} tem OS ilimitadas (plano {})", oficinaId, plano.getNome());
            return;
        }

        // Get current month boundaries
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX);

        long osNoMes = ordemServicoRepository.countByOficinaIdAndDataAberturaBetweenExcluindoCanceladas(
            oficinaId, inicioMes, fimMes
        );

        if (osNoMes >= plano.getLimiteOsMes()) {
            log.warn("Oficina {} atingiu limite de OS/mês: {}/{} (plano {})",
                oficinaId, osNoMes, plano.getLimiteOsMes(), plano.getNome());
            throw new LimiteOsMesExcedidoException(
                plano.getNome(),
                plano.getLimiteOsMes(),
                osNoMes
            );
        }

        log.debug("Oficina {} pode criar OS: {}/{} este mês (plano {})",
            oficinaId, osNoMes, plano.getLimiteOsMes(), plano.getNome());
    }

    /**
     * Returns current usage statistics vs plan limits.
     *
     * @param oficinaId ID da oficina (tenant)
     * @return DTO with usage information
     */
    public UsoLimitesDTO obterUsoAtual(UUID oficinaId) {
        Oficina oficina = getOficina(oficinaId);
        Plano plano = obterPlano(oficina);

        // Count active users
        long usuariosAtivos = usuarioRepository.countByOficinaIdAndAtivoTrue(oficinaId);

        // Get current month boundaries
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth()).atTime(LocalTime.MAX);

        // Count OS this month (excluding canceled)
        long osNoMes = ordemServicoRepository.countByOficinaIdAndDataAberturaBetweenExcluindoCanceladas(
            oficinaId, inicioMes, fimMes
        );

        int limiteUsuarios = plano.getLimiteUsuarios() != null ? plano.getLimiteUsuarios() : -1;
        int limiteOsMes = plano.getLimiteOsMes() != null ? plano.getLimiteOsMes() : -1;

        return UsoLimitesDTO.builder()
            .planoNome(plano.getNome())
            .planoCodigo(plano.getCodigo())
            .limiteUsuarios(limiteUsuarios)
            .usuariosAtivos(usuariosAtivos)
            .percentualUsuarios(calcularPercentual(usuariosAtivos, limiteUsuarios))
            .usuariosIlimitados(limiteUsuarios == -1)
            .limiteOsMes(limiteOsMes)
            .osNoMes(osNoMes)
            .percentualOsMes(calcularPercentual(osNoMes, limiteOsMes))
            .osIlimitadas(limiteOsMes == -1)
            .mesReferencia(hoje.getMonthValue())
            .anoReferencia(hoje.getYear())
            .build();
    }

    // =====================================
    // PRIVATE HELPERS
    // =====================================

    private Oficina getOficina(UUID oficinaId) {
        return oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada: " + oficinaId));
    }

    /**
     * Gets the plan for the workshop.
     * Uses the enum PlanoAssinatura name to lookup in planos table.
     */
    private Plano obterPlano(Oficina oficina) {
        if (oficina.getPlano() == null) {
            throw new ResourceNotFoundException("Oficina não possui plano configurado: " + oficina.getId());
        }

        // Lookup plan by code (enum name)
        String codigoPlano = oficina.getPlano().name();
        return planoRepository.findByCodigo(codigoPlano)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Plano não encontrado na tabela planos: " + codigoPlano +
                ". Execute a migration para criar os planos base."
            ));
    }

    private double calcularPercentual(long usado, int limite) {
        if (limite == -1) return 0.0; // unlimited
        if (limite == 0) return 100.0;
        return Math.round((usado * 100.0 / limite) * 10.0) / 10.0; // 1 decimal place
    }
}
