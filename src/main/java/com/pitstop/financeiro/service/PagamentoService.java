package com.pitstop.financeiro.service;

import com.pitstop.financeiro.domain.Pagamento;
import com.pitstop.financeiro.domain.StatusPagamento;
import com.pitstop.financeiro.domain.TipoPagamento;
import com.pitstop.financeiro.dto.ConfirmarPagamentoDTO;
import com.pitstop.financeiro.dto.PagamentoRequestDTO;
import com.pitstop.financeiro.dto.PagamentoResponseDTO;
import com.pitstop.financeiro.mapper.PagamentoMapper;
import com.pitstop.financeiro.repository.PagamentoRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service para lógica de negócio de pagamentos.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PagamentoMapper pagamentoMapper;

    /**
     * Cria um novo pagamento.
     *
     * @param dto dados do pagamento
     * @return pagamento criado
     */
    @Transactional
    public PagamentoResponseDTO criar(PagamentoRequestDTO dto) {
        log.info("Criando pagamento para OS: {}", dto.ordemServicoId());

        Pagamento pagamento = pagamentoMapper.toEntity(dto);
        pagamento.setStatus(StatusPagamento.PENDENTE);

        Pagamento salvo = pagamentoRepository.save(pagamento);

        log.info("Pagamento criado com sucesso. ID: {}", salvo.getId());
        return pagamentoMapper.toResponseDTO(salvo);
    }

    /**
     * Busca pagamento por ID.
     *
     * @param id ID do pagamento
     * @return pagamento encontrado
     */
    @Transactional(readOnly = true)
    public PagamentoResponseDTO buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        Pagamento pagamento = pagamentoRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com ID: " + id));

        return pagamentoMapper.toResponseDTO(pagamento);
    }

    /**
     * Lista todos os pagamentos com paginação.
     *
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Transactional(readOnly = true)
    public Page<PagamentoResponseDTO> listar(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pagamentoRepository.findByOficinaId(oficinaId, pageable)
            .map(pagamentoMapper::toResponseDTO);
    }

    /**
     * Busca pagamentos por ordem de serviço.
     *
     * @param ordemServicoId ID da OS
     * @return lista de pagamentos
     */
    @Transactional(readOnly = true)
    public List<PagamentoResponseDTO> buscarPorOrdemServico(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return pagamentoRepository.findByOficinaIdAndOrdemServicoId(oficinaId, ordemServicoId)
            .stream()
            .map(pagamentoMapper::toResponseDTO)
            .toList();
    }

    /**
     * Busca pagamentos com filtros.
     *
     * @param tipo tipo de pagamento
     * @param status status
     * @param dataInicio data inicial
     * @param dataFim data final
     * @param pageable paginação
     * @return página de pagamentos
     */
    @Transactional(readOnly = true)
    public Page<PagamentoResponseDTO> buscarComFiltros(
        TipoPagamento tipo,
        StatusPagamento status,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        Pageable pageable
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        String statusStr = status != null ? status.name() : null;

        return pagamentoRepository.findByFiltros(oficinaId, tipo, statusStr, dataInicio, dataFim, pageable)
            .map(pagamentoMapper::toResponseDTO);
    }

    /**
     * Confirma um pagamento (marca como PAGO).
     *
     * @param id ID do pagamento
     * @param dto dados de confirmação
     * @return pagamento atualizado
     */
    @Transactional
    public PagamentoResponseDTO confirmar(UUID id, ConfirmarPagamentoDTO dto) {
        log.info("Confirmando pagamento ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Pagamento pagamento = pagamentoRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com ID: " + id));

        pagamento.confirmar(dto.dataPagamento());

        if (dto.comprovante() != null) {
            pagamento.setComprovante(dto.comprovante());
        }

        Pagamento atualizado = pagamentoRepository.save(pagamento);

        log.info("Pagamento confirmado. ID: {}", atualizado.getId());
        return pagamentoMapper.toResponseDTO(atualizado);
    }

    /**
     * Cancela um pagamento.
     *
     * @param id ID do pagamento
     */
    @Transactional
    public void cancelar(UUID id) {
        log.info("Cancelando pagamento ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Pagamento pagamento = pagamentoRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com ID: " + id));

        pagamento.cancelar();
        pagamentoRepository.save(pagamento);

        log.info("Pagamento cancelado. ID: {}", id);
    }

    /**
     * Estorna um pagamento (reverte pagamento já realizado).
     *
     * @param id ID do pagamento
     */
    @Transactional
    public void estornar(UUID id) {
        log.info("Estornando pagamento ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Pagamento pagamento = pagamentoRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com ID: " + id));

        pagamento.estornar();
        pagamentoRepository.save(pagamento);

        log.info("Pagamento estornado. ID: {}", id);
    }

    /**
     * Calcula total pago de uma OS.
     *
     * @param ordemServicoId ID da OS
     * @return valor total pago
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalPago(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return pagamentoRepository.calcularTotalPago(oficinaId, ordemServicoId);
    }

    /**
     * Calcula total pendente de uma OS.
     *
     * @param ordemServicoId ID da OS
     * @return valor total pendente
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalPendente(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return pagamentoRepository.calcularTotalPendente(oficinaId, ordemServicoId);
    }

    /**
     * Verifica se uma OS está totalmente quitada.
     *
     * @param ordemServicoId ID da OS
     * @return true se quitada
     */
    @Transactional(readOnly = true)
    public boolean isOrdemServicoQuitada(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return pagamentoRepository.isOrdemServicoQuitada(oficinaId, ordemServicoId);
    }

    /**
     * Atualiza status de pagamentos vencidos.
     */
    @Transactional
    public void atualizarVencidos() {
        log.info("Atualizando status de pagamentos vencidos");

        UUID oficinaId = TenantContext.getTenantId();
        Page<Pagamento> vencidos = pagamentoRepository.findVencidos(oficinaId, LocalDate.now(), Pageable.unpaged());

        vencidos.forEach(pagamento -> {
            pagamento.marcarComoVencido();
            pagamentoRepository.save(pagamento);
        });

        log.info("Total de pagamentos marcados como vencidos: {}", vencidos.getTotalElements());
    }
}
