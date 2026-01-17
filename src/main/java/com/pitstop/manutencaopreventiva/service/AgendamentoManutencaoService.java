package com.pitstop.manutencaopreventiva.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.manutencaopreventiva.domain.*;
import com.pitstop.manutencaopreventiva.dto.*;
import com.pitstop.manutencaopreventiva.mapper.ManutencaoMapper;
import com.pitstop.manutencaopreventiva.repository.*;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AgendamentoManutencaoService {

    private final AgendamentoManutencaoRepository agendamentoRepository;
    private final PlanoManutencaoRepository planoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final ManutencaoMapper mapper;

    /**
     * Lista agendamentos com filtros.
     */
    @Transactional(readOnly = true)
    public Page<AgendamentoManutencaoResponseDTO> listar(
            UUID veiculoId,
            UUID clienteId,
            StatusAgendamento status,
            LocalDate dataInicio,
            LocalDate dataFim,
            Pageable pageable) {

        UUID oficinaId = TenantContext.getTenantId();
        Page<AgendamentoManutencao> agendamentos = agendamentoRepository.findByFilters(
            oficinaId, veiculoId, clienteId, status, dataInicio, dataFim, pageable
        );
        return agendamentos.map(mapper::toAgendamentoResponse);
    }

    /**
     * Busca agendamento por ID.
     */
    @Transactional(readOnly = true)
    public AgendamentoManutencaoResponseDTO buscarPorId(UUID id) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Lista agendamentos do dia.
     */
    @Transactional(readOnly = true)
    public List<AgendamentoManutencaoResponseDTO> listarAgendamentosDoDia() {
        UUID oficinaId = TenantContext.getTenantId();
        return agendamentoRepository.findAgendamentosDoDia(oficinaId, LocalDate.now())
            .stream()
            .map(mapper::toAgendamentoResponse)
            .toList();
    }

    /**
     * Lista próximos agendamentos.
     */
    @Transactional(readOnly = true)
    public List<AgendamentoManutencaoResponseDTO> listarProximos(int limite) {
        UUID oficinaId = TenantContext.getTenantId();
        return agendamentoRepository.findProximosAgendamentos(
                oficinaId, LocalDate.now(), PageRequest.of(0, limite))
            .stream()
            .map(mapper::toAgendamentoResponse)
            .toList();
    }

    /**
     * Lista eventos para calendário.
     */
    @Transactional(readOnly = true)
    public List<CalendarioEventoDTO> listarCalendario(int mes, int ano) {
        UUID oficinaId = TenantContext.getTenantId();

        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.plusMonths(1).minusDays(1);

        return agendamentoRepository.findByOficinaIdAndDataAgendamentoBetween(oficinaId, inicio, fim)
            .stream()
            .map(mapper::toCalendarioEvento)
            .toList();
    }

    /**
     * Cria agendamento.
     */
    public AgendamentoManutencaoResponseDTO criar(AgendamentoManutencaoRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        // Validar conflito de horário
        if (agendamentoRepository.existsConflitoHorario(
                oficinaId, request.dataAgendamento(), request.horaAgendamento(), null)) {
            throw new IllegalStateException("Já existe agendamento para este horário");
        }

        Veiculo veiculo = veiculoRepository.findById(request.veiculoId())
            .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        PlanoManutencaoPreventiva plano = null;
        if (request.planoId() != null) {
            plano = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado"));
        }

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        AgendamentoManutencao agendamento = AgendamentoManutencao.builder()
            .oficina(oficina)
            .plano(plano)
            .veiculo(veiculo)
            .cliente(cliente)
            .dataAgendamento(request.dataAgendamento())
            .horaAgendamento(request.horaAgendamento())
            .duracaoEstimadaMinutos(request.duracaoEstimadaMinutos() != null ? request.duracaoEstimadaMinutos() : 60)
            .tipoManutencao(request.tipoManutencao())
            .descricao(request.descricao())
            .observacoes(request.observacoes())
            .observacoesInternas(request.observacoesInternas())
            .build();

        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento criado: {} para {} em {}",
            agendamento.getId(), veiculo.getPlaca(), request.dataAgendamento());

        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Atualiza agendamento.
     */
    public AgendamentoManutencaoResponseDTO atualizar(UUID id, AgendamentoManutencaoRequestDTO request) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);

        if (agendamento.getStatus() == StatusAgendamento.REALIZADO ||
            agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new IllegalStateException("Não é possível editar agendamento " + agendamento.getStatus());
        }

        UUID oficinaId = TenantContext.getTenantId();

        // Validar conflito de horário (excluindo o próprio)
        if (agendamentoRepository.existsConflitoHorario(
                oficinaId, request.dataAgendamento(), request.horaAgendamento(), id)) {
            throw new IllegalStateException("Já existe agendamento para este horário");
        }

        agendamento.setDataAgendamento(request.dataAgendamento());
        agendamento.setHoraAgendamento(request.horaAgendamento());
        agendamento.setDuracaoEstimadaMinutos(request.duracaoEstimadaMinutos());
        agendamento.setTipoManutencao(request.tipoManutencao());
        agendamento.setDescricao(request.descricao());
        agendamento.setObservacoes(request.observacoes());
        agendamento.setObservacoesInternas(request.observacoesInternas());

        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento atualizado: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Confirma agendamento (por usuário interno).
     */
    public AgendamentoManutencaoResponseDTO confirmar(UUID id) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        agendamento.confirmar("PRESENCIAL");
        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento confirmado: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Confirma agendamento por token (cliente via link).
     */
    @Transactional
    public AgendamentoManutencaoResponseDTO confirmarPorToken(String token) {
        AgendamentoManutencao agendamento = agendamentoRepository.findByTokenConfirmacao(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (!agendamento.isTokenValido(token)) {
            throw new IllegalStateException("Token expirado ou inválido");
        }

        agendamento.confirmar("LINK");
        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento confirmado via link: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Remarca agendamento.
     */
    public AgendamentoManutencaoResponseDTO remarcar(UUID id, RemarcarAgendamentoRequestDTO request) {
        AgendamentoManutencao agendamentoAntigo = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamentoAntigo);

        UUID oficinaId = TenantContext.getTenantId();

        // Validar conflito de horário
        if (agendamentoRepository.existsConflitoHorario(
                oficinaId, request.novaData(), request.novaHora(), null)) {
            throw new IllegalStateException("Já existe agendamento para este horário");
        }

        AgendamentoManutencao novoAgendamento = agendamentoAntigo.remarcar(
            request.novaData(), request.novaHora(), request.motivo()
        );

        agendamentoRepository.save(agendamentoAntigo);
        novoAgendamento = agendamentoRepository.save(novoAgendamento);

        log.info("Agendamento remarcado: {} -> {}", id, novoAgendamento.getId());
        return mapper.toAgendamentoResponse(novoAgendamento);
    }

    /**
     * Cancela agendamento.
     */
    public AgendamentoManutencaoResponseDTO cancelar(UUID id, String motivo) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        agendamento.cancelar(motivo, null);
        agendamento = agendamentoRepository.save(agendamento);

        log.info("Agendamento cancelado: {}", agendamento.getId());
        return mapper.toAgendamentoResponse(agendamento);
    }

    /**
     * Deleta agendamento.
     */
    public void deletar(UUID id) {
        AgendamentoManutencao agendamento = agendamentoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        validarTenant(agendamento);
        agendamentoRepository.delete(agendamento);

        log.info("Agendamento deletado: {}", id);
    }

    /**
     * Conta agendamentos do dia.
     */
    @Transactional(readOnly = true)
    public long contarAgendamentosDoDia() {
        UUID oficinaId = TenantContext.getTenantId();
        return agendamentoRepository.countAgendamentosDoDia(oficinaId, LocalDate.now());
    }

    private void validarTenant(AgendamentoManutencao agendamento) {
        UUID oficinaId = TenantContext.getTenantId();
        if (!agendamento.getOficina().getId().equals(oficinaId)) {
            throw new ResourceNotFoundException("Agendamento não encontrado");
        }
    }
}
