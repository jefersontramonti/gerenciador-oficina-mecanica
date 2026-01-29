package com.pitstop.financeiro.service;

import com.pitstop.financeiro.domain.CategoriaDespesa;
import com.pitstop.financeiro.domain.Despesa;
import com.pitstop.financeiro.domain.StatusDespesa;
import com.pitstop.financeiro.dto.DespesaDTO;
import com.pitstop.financeiro.repository.DespesaRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciamento de despesas operacionais.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;

    // ==================== CRIAÇÃO PROGRAMÁTICA ====================

    /**
     * Cria despesa automática vinculada a uma movimentação de estoque.
     * Chamado internamente pelo módulo de estoque ao registrar entrada.
     */
    @Transactional
    @CacheEvict(value = {"fluxoCaixa", "dre", "despesasResumo"}, allEntries = true)
    public Despesa criarDespesaEstoque(
            String descricao,
            BigDecimal valor,
            String fornecedor,
            UUID movimentacaoEstoqueId,
            String observacoes
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        Despesa despesa = Despesa.builder()
            .oficina(oficina)
            .categoria(CategoriaDespesa.COMPRA_PECAS)
            .descricao(descricao)
            .valor(valor)
            .dataVencimento(LocalDate.now())
            .fornecedor(fornecedor)
            .observacoes(observacoes)
            .recorrente(false)
            .status(StatusDespesa.PENDENTE)
            .movimentacaoEstoqueId(movimentacaoEstoqueId)
            .build();

        despesa = despesaRepository.save(despesa);
        log.info("Despesa automática criada ID: {} para movimentação estoque: {}",
                 despesa.getId(), movimentacaoEstoqueId);

        return despesa;
    }

    // ==================== CRUD ====================

    /**
     * Cria uma nova despesa.
     */
    @Transactional
    @CacheEvict(value = {"fluxoCaixa", "dre"}, allEntries = true)
    public DespesaDTO.Response criar(DespesaDTO.CreateRequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Criando despesa para oficina {}: {}", oficinaId, request.getDescricao());

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        Despesa despesa = Despesa.builder()
            .oficina(oficina)
            .categoria(request.getCategoria())
            .descricao(request.getDescricao())
            .valor(request.getValor())
            .dataVencimento(request.getDataVencimento())
            .numeroDocumento(request.getNumeroDocumento())
            .fornecedor(request.getFornecedor())
            .observacoes(request.getObservacoes())
            .recorrente(request.getRecorrente() != null ? request.getRecorrente() : false)
            .status(StatusDespesa.PENDENTE)
            .build();

        despesa = despesaRepository.save(despesa);
        log.info("Despesa criada com ID: {}", despesa.getId());

        return toResponse(despesa);
    }

    /**
     * Atualiza uma despesa existente.
     */
    @Transactional
    @CacheEvict(value = {"fluxoCaixa", "dre"}, allEntries = true)
    public DespesaDTO.Response atualizar(UUID id, DespesaDTO.UpdateRequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Atualizando despesa {} da oficina {}", id, oficinaId);

        Despesa despesa = despesaRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        // Não permite alterar despesa paga
        if (despesa.getStatus() == StatusDespesa.PAGA) {
            throw new IllegalStateException("Despesa já paga não pode ser alterada");
        }

        if (request.getCategoria() != null) {
            despesa.setCategoria(request.getCategoria());
        }
        if (request.getDescricao() != null) {
            despesa.setDescricao(request.getDescricao());
        }
        if (request.getValor() != null) {
            despesa.setValor(request.getValor());
        }
        if (request.getDataVencimento() != null) {
            despesa.setDataVencimento(request.getDataVencimento());
        }
        if (request.getNumeroDocumento() != null) {
            despesa.setNumeroDocumento(request.getNumeroDocumento());
        }
        if (request.getFornecedor() != null) {
            despesa.setFornecedor(request.getFornecedor());
        }
        if (request.getObservacoes() != null) {
            despesa.setObservacoes(request.getObservacoes());
        }
        if (request.getRecorrente() != null) {
            despesa.setRecorrente(request.getRecorrente());
        }

        despesa = despesaRepository.save(despesa);
        log.info("Despesa {} atualizada", id);

        return toResponse(despesa);
    }

    /**
     * Registra o pagamento de uma despesa.
     */
    @Transactional
    @CacheEvict(value = {"fluxoCaixa", "dre"}, allEntries = true)
    public DespesaDTO.Response pagar(UUID id, DespesaDTO.PagamentoRequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Registrando pagamento da despesa {} da oficina {}", id, oficinaId);

        Despesa despesa = despesaRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        if (despesa.getStatus() == StatusDespesa.PAGA) {
            throw new IllegalStateException("Despesa já está paga");
        }
        if (despesa.getStatus() == StatusDespesa.CANCELADA) {
            throw new IllegalStateException("Despesa cancelada não pode ser paga");
        }

        despesa.pagar(request.getDataPagamento(), request.getTipoPagamento());
        despesa = despesaRepository.save(despesa);

        log.info("Despesa {} paga em {}", id, request.getDataPagamento());
        return toResponse(despesa);
    }

    /**
     * Cancela uma despesa.
     */
    @Transactional
    @CacheEvict(value = {"fluxoCaixa", "dre"}, allEntries = true)
    public DespesaDTO.Response cancelar(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Cancelando despesa {} da oficina {}", id, oficinaId);

        Despesa despesa = despesaRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        if (despesa.getStatus() == StatusDespesa.PAGA) {
            throw new IllegalStateException("Despesa paga não pode ser cancelada");
        }

        despesa.cancelar();
        despesa = despesaRepository.save(despesa);

        log.info("Despesa {} cancelada", id);
        return toResponse(despesa);
    }

    /**
     * Exclui uma despesa (soft delete via cancelamento).
     */
    @Transactional
    @CacheEvict(value = {"fluxoCaixa", "dre"}, allEntries = true)
    public void excluir(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Excluindo despesa {} da oficina {}", id, oficinaId);

        Despesa despesa = despesaRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        if (despesa.getStatus() == StatusDespesa.PAGA) {
            throw new IllegalStateException("Despesa paga não pode ser excluída");
        }

        // Para despesas vinculadas a movimentação, apenas cancela
        if (despesa.getMovimentacaoEstoqueId() != null) {
            despesa.cancelar();
            despesaRepository.save(despesa);
        } else {
            despesaRepository.delete(despesa);
        }

        log.info("Despesa {} excluída/cancelada", id);
    }

    // ==================== CONSULTAS ====================

    /**
     * Busca despesa por ID.
     */
    @Transactional(readOnly = true)
    public DespesaDTO.Response buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Despesa despesa = despesaRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada"));

        return toResponse(despesa);
    }

    /**
     * Lista despesas com filtros.
     */
    @Transactional(readOnly = true)
    public Page<DespesaDTO.ListItem> listar(
            StatusDespesa status,
            CategoriaDespesa categoria,
            LocalDate dataInicio,
            LocalDate dataFim,
            Pageable pageable
    ) {
        UUID oficinaId = TenantContext.getTenantId();

        Page<Despesa> despesas = despesaRepository.findByFilters(
            oficinaId, status, categoria, dataInicio, dataFim, pageable
        );

        return despesas.map(this::toListItem);
    }

    /**
     * Lista despesas vencidas.
     */
    @Transactional(readOnly = true)
    public List<DespesaDTO.ListItem> listarVencidas() {
        UUID oficinaId = TenantContext.getTenantId();

        List<Despesa> despesas = despesaRepository.findDespesasVencidas(oficinaId, LocalDate.now());

        return despesas.stream()
            .map(this::toListItem)
            .collect(Collectors.toList());
    }

    /**
     * Lista despesas a vencer nos próximos dias.
     */
    @Transactional(readOnly = true)
    public List<DespesaDTO.ListItem> listarAVencer(int dias) {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(dias);

        List<Despesa> despesas = despesaRepository.findDespesasAVencer(oficinaId, hoje, dataLimite);

        return despesas.stream()
            .map(this::toListItem)
            .collect(Collectors.toList());
    }

    /**
     * Retorna resumo das despesas para dashboard.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "despesasResumo", key = "#root.target.getTenantId()")
    public DespesaDTO.Resumo getResumo() {
        UUID oficinaId = TenantContext.getTenantId();
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        BigDecimal totalPendente = despesaRepository.sumDespesasPendentes(oficinaId);

        List<Despesa> vencidas = despesaRepository.findDespesasVencidas(oficinaId, hoje);
        BigDecimal totalVencido = vencidas.stream()
            .map(Despesa::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagoMes = despesaRepository.sumDespesasPagasByPeriodo(oficinaId, inicioMes, fimMes);

        long qtdPendente = despesaRepository.countPendentes(oficinaId);

        List<Despesa> aVencer7Dias = despesaRepository.findDespesasAVencer(oficinaId, hoje, hoje.plusDays(7));

        return DespesaDTO.Resumo.builder()
            .totalPendente(totalPendente != null ? totalPendente : BigDecimal.ZERO)
            .totalVencido(totalVencido)
            .totalPagoMes(totalPagoMes != null ? totalPagoMes : BigDecimal.ZERO)
            .quantidadePendente(qtdPendente)
            .quantidadeVencida((long) vencidas.size())
            .quantidadeAVencer7Dias((long) aVencer7Dias.size())
            .build();
    }

    /**
     * Retorna todas as categorias de despesa disponíveis.
     */
    public List<DespesaDTO.CategoriaInfo> listarCategorias() {
        return Arrays.stream(CategoriaDespesa.values())
            .map(cat -> DespesaDTO.CategoriaInfo.builder()
                .codigo(cat)
                .descricao(cat.getDescricao())
                .grupo(cat.getGrupo())
                .cor(cat.getCor())
                .build())
            .collect(Collectors.toList());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    public UUID getTenantId() {
        return TenantContext.getTenantId();
    }

    private DespesaDTO.Response toResponse(Despesa despesa) {
        LocalDate hoje = LocalDate.now();
        boolean vencida = despesa.getStatus() == StatusDespesa.PENDENTE &&
                          despesa.getDataVencimento() != null &&
                          despesa.getDataVencimento().isBefore(hoje);

        Long diasAtraso = null;
        if (vencida) {
            diasAtraso = ChronoUnit.DAYS.between(despesa.getDataVencimento(), hoje);
        }

        return DespesaDTO.Response.builder()
            .id(despesa.getId())
            .categoria(despesa.getCategoria())
            .categoriaDescricao(despesa.getCategoria().getDescricao())
            .categoriaGrupo(despesa.getCategoria().getGrupo())
            .categoriaCor(despesa.getCategoria().getCor())
            .descricao(despesa.getDescricao())
            .valor(despesa.getValor())
            .dataVencimento(despesa.getDataVencimento())
            .dataPagamento(despesa.getDataPagamento())
            .status(despesa.getStatus())
            .statusDescricao(despesa.getStatus().getDescricao())
            .statusCor(despesa.getStatus().getCor())
            .numeroDocumento(despesa.getNumeroDocumento())
            .fornecedor(despesa.getFornecedor())
            .observacoes(despesa.getObservacoes())
            .recorrente(despesa.getRecorrente())
            .tipoPagamento(despesa.getTipoPagamento())
            .movimentacaoEstoqueId(despesa.getMovimentacaoEstoqueId())
            .createdAt(despesa.getCreatedAt())
            .updatedAt(despesa.getUpdatedAt())
            .vencida(vencida)
            .diasAtraso(diasAtraso)
            .build();
    }

    private DespesaDTO.ListItem toListItem(Despesa despesa) {
        LocalDate hoje = LocalDate.now();
        boolean vencida = despesa.getStatus() == StatusDespesa.PENDENTE &&
                          despesa.getDataVencimento() != null &&
                          despesa.getDataVencimento().isBefore(hoje);

        return DespesaDTO.ListItem.builder()
            .id(despesa.getId())
            .categoria(despesa.getCategoria())
            .categoriaDescricao(despesa.getCategoria().getDescricao())
            .categoriaCor(despesa.getCategoria().getCor())
            .descricao(despesa.getDescricao())
            .valor(despesa.getValor())
            .dataVencimento(despesa.getDataVencimento())
            .dataPagamento(despesa.getDataPagamento())
            .status(despesa.getStatus())
            .statusDescricao(despesa.getStatus().getDescricao())
            .statusCor(despesa.getStatus().getCor())
            .fornecedor(despesa.getFornecedor())
            .recorrente(despesa.getRecorrente())
            .vencida(vencida)
            .build();
    }
}
