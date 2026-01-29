package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.MovimentacaoEstoque;
import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.TipoMovimentacao;
import com.pitstop.estoque.exception.EstoqueInsuficienteException;
import com.pitstop.estoque.exception.MovimentacaoInvalidaException;
import com.pitstop.estoque.exception.PecaNotFoundException;
import com.pitstop.estoque.repository.MovimentacaoEstoqueRepository;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.financeiro.service.DespesaService;
import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.domain.OrigemPeca;
import com.pitstop.ordemservico.domain.TipoItem;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciamento de movimentações de estoque.
 * Responsável por registrar entradas, saídas, ajustes e baixas automáticas por OS.
 *
 * <p><strong>Características críticas:</strong></p>
 * <ul>
 *   <li>Todas as operações são transacionais (ACID)</li>
 *   <li>Usa pessimistic locking em operações de atualização de estoque</li>
 *   <li>Movimentações são imutáveis (audit trail)</li>
 *   <li>Valida estoque disponível antes de permitir saídas</li>
 * </ul>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final PecaRepository pecaRepository;
    private final DespesaService despesaService;

    /**
     * Registra uma entrada de estoque (compra, devolução de fornecedor, etc).
     *
     * @param pecaId ID da peça
     * @param quantidade quantidade a adicionar
     * @param valorUnitario valor unitário no momento da entrada
     * @param usuarioId ID do usuário responsável
     * @param motivo descrição do motivo
     * @param observacao observações adicionais (opcional)
     * @return movimentação registrada
     * @throws PecaNotFoundException se peça não existe
     */
    @Transactional
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public MovimentacaoEstoque registrarEntrada(
            UUID pecaId,
            Integer quantidade,
            BigDecimal valorUnitario,
            UUID usuarioId,
            String motivo,
            String observacao
    ) {
        log.info("Registrando ENTRADA de estoque - Peça ID: {}, Quantidade: {}", pecaId, quantidade);

        UUID oficinaId = TenantContext.getTenantId();
        // Busca peça com lock pessimista
        Peca peca = pecaRepository.findByOficinaIdAndIdForUpdate(oficinaId, pecaId)
                .orElseThrow(() -> new PecaNotFoundException(pecaId));

        Integer quantidadeAnterior = peca.getQuantidadeAtual();
        Integer quantidadeNova = quantidadeAnterior + quantidade;

        // Atualiza estoque da peça
        peca.setQuantidadeAtual(quantidadeNova);
        pecaRepository.save(peca);

        // Cria registro de movimentação
        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .pecaId(pecaId)
                .usuarioId(usuarioId)
                .tipo(TipoMovimentacao.ENTRADA)
                .quantidade(quantidade)
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeNova)
                .valorUnitario(valorUnitario)
                .motivo(motivo)
                .observacao(observacao)
                .dataMovimentacao(LocalDateTime.now())
                .build();

        MovimentacaoEstoque movimentacaoSalva = movimentacaoRepository.save(movimentacao);

        // Gerar despesa automática para compra de peças
        try {
            String descricaoDespesa = String.format("Compra de peça: %s - %s (x%d)",
                    peca.getCodigo(),
                    peca.getNome() != null ? peca.getNome() : peca.getDescricao(),
                    quantidade);

            despesaService.criarDespesaEstoque(
                    descricaoDespesa,
                    movimentacaoSalva.getValorTotal(),
                    peca.getFornecedorPrincipal(),
                    movimentacaoSalva.getId(),
                    motivo
            );

            log.info("Despesa automática gerada para entrada de estoque - Peça: {}, Valor: {}",
                    peca.getCodigo(), movimentacaoSalva.getValorTotal());
        } catch (Exception e) {
            log.error("Erro ao gerar despesa automática para entrada de estoque: {}", e.getMessage(), e);
        }

        log.info("ENTRADA registrada com sucesso - Peça: {} ({}), Estoque: {} -> {}",
                peca.getCodigo(), peca.getDescricao(), quantidadeAnterior, quantidadeNova);

        return movimentacaoSalva;
    }

    /**
     * Registra uma saída manual de estoque (venda avulsa, uso interno, etc).
     *
     * @param pecaId ID da peça
     * @param quantidade quantidade a retirar
     * @param valorUnitario valor unitário no momento da saída
     * @param usuarioId ID do usuário responsável
     * @param motivo descrição do motivo
     * @param observacao observações adicionais (opcional)
     * @return movimentação registrada
     * @throws PecaNotFoundException se peça não existe
     * @throws EstoqueInsuficienteException se não há estoque suficiente
     */
    @Transactional
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public MovimentacaoEstoque registrarSaida(
            UUID pecaId,
            Integer quantidade,
            BigDecimal valorUnitario,
            UUID usuarioId,
            String motivo,
            String observacao
    ) {
        log.info("Registrando SAÍDA de estoque - Peça ID: {}, Quantidade: {}", pecaId, quantidade);

        UUID oficinaId = TenantContext.getTenantId();
        // Busca peça com lock pessimista
        Peca peca = pecaRepository.findByOficinaIdAndIdForUpdate(oficinaId, pecaId)
                .orElseThrow(() -> new PecaNotFoundException(pecaId));

        Integer quantidadeAnterior = peca.getQuantidadeAtual();

        // Valida estoque disponível
        if (!peca.temEstoqueDisponivel(quantidade)) {
            log.error("Estoque insuficiente para saída - Peça: {}, Requerido: {}, Disponível: {}",
                    peca.getCodigo(), quantidade, quantidadeAnterior);
            throw new EstoqueInsuficienteException(
                    pecaId, peca.getCodigo(), peca.getDescricao(), quantidade, quantidadeAnterior
            );
        }

        Integer quantidadeNova = quantidadeAnterior - quantidade;

        // Atualiza estoque da peça
        peca.setQuantidadeAtual(quantidadeNova);
        pecaRepository.save(peca);

        // Cria registro de movimentação
        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .pecaId(pecaId)
                .usuarioId(usuarioId)
                .tipo(TipoMovimentacao.SAIDA)
                .quantidade(quantidade)
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeNova)
                .valorUnitario(valorUnitario)
                .motivo(motivo)
                .observacao(observacao)
                .dataMovimentacao(LocalDateTime.now())
                .build();

        MovimentacaoEstoque movimentacaoSalva = movimentacaoRepository.save(movimentacao);

        log.info("SAÍDA registrada com sucesso - Peça: {} ({}), Estoque: {} -> {}",
                peca.getCodigo(), peca.getDescricao(), quantidadeAnterior, quantidadeNova);

        return movimentacaoSalva;
    }

    /**
     * Registra um ajuste de inventário (correção de estoque).
     *
     * @param pecaId ID da peça
     * @param quantidadeNova nova quantidade correta (valor absoluto)
     * @param valorUnitario valor unitário atual
     * @param usuarioId ID do usuário responsável
     * @param motivo descrição do motivo (inventário físico, perda, etc)
     * @param observacao observações adicionais (opcional)
     * @return movimentação registrada
     * @throws PecaNotFoundException se peça não existe
     */
    @Transactional
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public MovimentacaoEstoque registrarAjuste(
            UUID pecaId,
            Integer quantidadeNova,
            BigDecimal valorUnitario,
            UUID usuarioId,
            String motivo,
            String observacao
    ) {
        log.info("Registrando AJUSTE de estoque - Peça ID: {}, Nova quantidade: {}", pecaId, quantidadeNova);

        UUID oficinaId = TenantContext.getTenantId();
        // Busca peça com lock pessimista
        Peca peca = pecaRepository.findByOficinaIdAndIdForUpdate(oficinaId, pecaId)
                .orElseThrow(() -> new PecaNotFoundException(pecaId));

        Integer quantidadeAnterior = peca.getQuantidadeAtual();
        Integer diferenca = Math.abs(quantidadeNova - quantidadeAnterior);

        if (diferenca == 0) {
            throw new MovimentacaoInvalidaException(
                    "Ajuste inválido: quantidade nova é igual à atual (" + quantidadeAnterior + ")"
            );
        }

        // Atualiza estoque da peça
        peca.setQuantidadeAtual(quantidadeNova);
        pecaRepository.save(peca);

        // Cria registro de movimentação
        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .pecaId(pecaId)
                .usuarioId(usuarioId)
                .tipo(TipoMovimentacao.AJUSTE)
                .quantidade(diferenca)
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeNova)
                .valorUnitario(valorUnitario)
                .motivo(motivo)
                .observacao(observacao)
                .dataMovimentacao(LocalDateTime.now())
                .build();

        MovimentacaoEstoque movimentacaoSalva = movimentacaoRepository.save(movimentacao);

        String tipoAjuste = quantidadeNova > quantidadeAnterior ? "AUMENTOU" : "DIMINUIU";
        log.info("AJUSTE registrado com sucesso - Peça: {} ({}), Estoque {} de {} para {} (diferença: {})",
                peca.getCodigo(), peca.getDescricao(), tipoAjuste, quantidadeAnterior, quantidadeNova, diferenca);

        return movimentacaoSalva;
    }

    /**
     * Baixa estoque automaticamente ao finalizar uma Ordem de Serviço.
     * Este método é chamado pelo EstoqueEventListener quando OS é finalizada.
     *
     * <p><strong>Comportamento crítico:</strong></p>
     * <ul>
     *   <li>Processa apenas itens do tipo PECA com pecaId preenchido</li>
     *   <li>Valida estoque de TODAS as peças antes de baixar (atomicidade)</li>
     *   <li>Se QUALQUER peça tiver estoque insuficiente, lança exception e faz rollback</li>
     *   <li>Cria movimentações do tipo BAIXA_OS vinculadas à OS</li>
     * </ul>
     *
     * @param ordemServicoId ID da OS finalizada
     * @param itens lista de itens da OS
     * @param usuarioId ID do usuário que finalizou a OS
     * @return lista de movimentações criadas
     * @throws EstoqueInsuficienteException se alguma peça não tem estoque suficiente
     */
    @Transactional
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public List<MovimentacaoEstoque> baixarEstoquePorOS(
            UUID ordemServicoId,
            List<ItemOS> itens,
            UUID usuarioId
    ) {
        log.info("Iniciando baixa automática de estoque para OS ID: {}", ordemServicoId);

        UUID oficinaId = TenantContext.getTenantId();

        // Filtra apenas itens do tipo PECA com origem ESTOQUE (peças do inventário)
        // Peças AVULSA e CLIENTE não afetam o estoque da oficina
        List<ItemOS> itensPeca = itens.stream()
                .filter(item -> item.getTipo() == TipoItem.PECA)
                .filter(item -> item.getOrigemPeca() == OrigemPeca.ESTOQUE)
                .filter(item -> item.getPecaId() != null)
                .toList();

        if (itensPeca.isEmpty()) {
            log.info("OS {} não possui itens de peça do ESTOQUE para baixar", ordemServicoId);
            return List.of();
        }

        // Conta peças por origem para log informativo
        long totalPecas = itens.stream()
                .filter(item -> item.getTipo() == TipoItem.PECA)
                .count();
        long pecasAvulsas = itens.stream()
                .filter(item -> item.getTipo() == TipoItem.PECA)
                .filter(item -> item.getOrigemPeca() == OrigemPeca.AVULSA)
                .count();
        long pecasCliente = itens.stream()
                .filter(item -> item.getTipo() == TipoItem.PECA)
                .filter(item -> item.getOrigemPeca() == OrigemPeca.CLIENTE)
                .count();

        log.info("OS {}: {} peças do ESTOQUE para baixar (total: {} peças, {} avulsas, {} do cliente)",
                ordemServicoId, itensPeca.size(), totalPecas, pecasAvulsas, pecasCliente);

        // FASE 1: Valida estoque de TODAS as peças ANTES de baixar
        for (ItemOS item : itensPeca) {
            Peca peca = pecaRepository.findByOficinaIdAndId(oficinaId, item.getPecaId())
                    .orElseThrow(() -> new PecaNotFoundException(item.getPecaId()));

            if (!peca.temEstoqueDisponivel(item.getQuantidade())) {
                log.error("Estoque insuficiente ao finalizar OS {} - Peça: {}, Requerido: {}, Disponível: {}",
                        ordemServicoId, peca.getCodigo(), item.getQuantidade(), peca.getQuantidadeAtual());
                throw new EstoqueInsuficienteException(
                        peca.getId(),
                        peca.getCodigo(),
                        peca.getDescricao(),
                        item.getQuantidade(),
                        peca.getQuantidadeAtual()
                );
            }
        }

        // FASE 2: Todas validações OK - processa baixas atomicamente
        List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();

        for (ItemOS item : itensPeca) {
            Peca peca = pecaRepository.findByOficinaIdAndIdForUpdate(oficinaId, item.getPecaId())
                    .orElseThrow(() -> new PecaNotFoundException(item.getPecaId()));

            Integer quantidadeAnterior = peca.getQuantidadeAtual();
            Integer quantidadeNova = quantidadeAnterior - item.getQuantidade();

            // Atualiza estoque
            peca.setQuantidadeAtual(quantidadeNova);
            pecaRepository.save(peca);

            // Cria movimentação vinculada à OS
            MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                    .pecaId(peca.getId())
                    .ordemServicoId(ordemServicoId)
                    .usuarioId(usuarioId)
                    .tipo(TipoMovimentacao.BAIXA_OS)
                    .quantidade(item.getQuantidade())
                    .quantidadeAnterior(quantidadeAnterior)
                    .quantidadeAtual(quantidadeNova)
                    .valorUnitario(item.getValorUnitario())
                    .motivo(String.format("Baixa automática - OS finalizada"))
                    .observacao(String.format("Item: %s", item.getDescricao()))
                    .dataMovimentacao(LocalDateTime.now())
                    .build();

            MovimentacaoEstoque movimentacaoSalva = movimentacaoRepository.save(movimentacao);
            movimentacoes.add(movimentacaoSalva);

            log.info("Estoque baixado - Peça: {} ({}), Qtd: {}, Estoque: {} -> {}",
                    peca.getCodigo(), peca.getDescricao(), item.getQuantidade(), quantidadeAnterior, quantidadeNova);
        }

        log.info("Baixa automática de estoque concluída com sucesso - OS: {}, {} movimentações criadas",
                ordemServicoId, movimentacoes.size());

        return movimentacoes;
    }

    /**
     * Estorna estoque quando uma OS finalizada é cancelada.
     * Reverte as movimentações do tipo BAIXA_OS criando movimentações do tipo DEVOLUCAO.
     *
     * @param ordemServicoId ID da OS cancelada
     * @param usuarioId ID do usuário que cancelou
     * @return lista de movimentações de estorno criadas
     */
    @Transactional
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public List<MovimentacaoEstoque> estornarEstoquePorOS(UUID ordemServicoId, UUID usuarioId) {
        log.info("Iniciando estorno de estoque para OS cancelada ID: {}", ordemServicoId);

        UUID oficinaId = TenantContext.getTenantId();
        // Busca movimentações de baixa vinculadas à OS
        List<MovimentacaoEstoque> movimentacoesBaixa = movimentacaoRepository
                .findByOficinaIdAndOrdemServicoIdOrderByCreatedAtDesc(oficinaId, ordemServicoId).stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.BAIXA_OS)
                .toList();

        if (movimentacoesBaixa.isEmpty()) {
            log.warn("Nenhuma movimentação de baixa encontrada para OS {}", ordemServicoId);
            return List.of();
        }

        log.info("Estornando {} movimentações de baixa", movimentacoesBaixa.size());

        List<MovimentacaoEstoque> estornos = new ArrayList<>();

        for (MovimentacaoEstoque baixa : movimentacoesBaixa) {
            Peca peca = pecaRepository.findByOficinaIdAndIdForUpdate(oficinaId, baixa.getPecaId())
                    .orElseThrow(() -> new PecaNotFoundException(baixa.getPecaId()));

            Integer quantidadeAnterior = peca.getQuantidadeAtual();
            Integer quantidadeNova = quantidadeAnterior + baixa.getQuantidade(); // Devolve ao estoque

            // Atualiza estoque
            peca.setQuantidadeAtual(quantidadeNova);
            pecaRepository.save(peca);

            // Cria movimentação de devolução
            MovimentacaoEstoque estorno = MovimentacaoEstoque.builder()
                    .pecaId(peca.getId())
                    .ordemServicoId(ordemServicoId)
                    .usuarioId(usuarioId)
                    .tipo(TipoMovimentacao.DEVOLUCAO)
                    .quantidade(baixa.getQuantidade())
                    .quantidadeAnterior(quantidadeAnterior)
                    .quantidadeAtual(quantidadeNova)
                    .valorUnitario(baixa.getValorUnitario())
                    .motivo("Estorno - OS cancelada")
                    .observacao(String.format("Estorno da movimentação ID: %s", baixa.getId()))
                    .dataMovimentacao(LocalDateTime.now())
                    .build();

            MovimentacaoEstoque estornoSalvo = movimentacaoRepository.save(estorno);
            estornos.add(estornoSalvo);

            log.info("Estoque estornado - Peça: {} ({}), Qtd: {}, Estoque: {} -> {}",
                    peca.getCodigo(), peca.getDescricao(), baixa.getQuantidade(), quantidadeAnterior, quantidadeNova);
        }

        log.info("Estorno de estoque concluído - OS: {}, {} devoluções criadas", ordemServicoId, estornos.size());

        return estornos;
    }

    // ========== QUERIES ==========

    /**
     * Busca histórico de movimentações de uma peça.
     *
     * @param pecaId ID da peça
     * @param pageable paginação
     * @return página de movimentações
     */
    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> buscarHistoricoPeca(UUID pecaId, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return movimentacaoRepository.findByOficinaIdAndPecaIdOrderByDataMovimentacaoDesc(oficinaId, pecaId, pageable);
    }

    /**
     * Busca movimentações por filtros.
     *
     * @param pecaId ID da peça (opcional)
     * @param tipo tipo da movimentação (opcional)
     * @param dataInicio data inicial (opcional)
     * @param dataFim data final (opcional)
     * @param usuarioId ID do usuário (opcional)
     * @param pageable paginação
     * @return página de movimentações filtradas
     */
    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> buscarComFiltros(
            UUID pecaId,
            TipoMovimentacao tipo,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            UUID usuarioId,
            Pageable pageable
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        return movimentacaoRepository.findByFilters(oficinaId, pecaId, tipo, dataInicio, dataFim, usuarioId, pageable);
    }

    /**
     * Busca movimentações de uma OS específica.
     *
     * @param ordemServicoId ID da OS
     * @return lista de movimentações
     */
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> buscarPorOS(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();
        return movimentacaoRepository.findByOficinaIdAndOrdemServicoIdOrderByCreatedAtDesc(oficinaId, ordemServicoId);
    }
}
