package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.UnidadeMedida;
import com.pitstop.estoque.exception.CodigoPecaDuplicadoException;
import com.pitstop.estoque.exception.PecaNotFoundException;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciamento do catálogo de peças.
 * Responsável por operações CRUD e consultas de peças do estoque.
 *
 * <p><strong>Importante:</strong> Este service NÃO modifica quantidadeAtual diretamente.
 * Atualizações de estoque devem ser feitas via MovimentacaoEstoqueService.</p>
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueService {

    private final PecaRepository pecaRepository;

    /**
     * Cria uma nova peça no catálogo.
     *
     * @param peca dados da peça a criar
     * @return peça criada com ID gerado
     * @throws CodigoPecaDuplicadoException se código já existe
     * @throws IllegalStateException se validações falharem
     */
    @Transactional
    public Peca criar(Peca peca) {
        log.info("Criando nova peça com código: {}", peca.getCodigo());

        UUID oficinaId = TenantContext.getTenantId();
        // Valida se código já existe
        if (pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(oficinaId, peca.getCodigo())) {
            log.warn("Tentativa de criar peça com código duplicado: {}", peca.getCodigo());
            throw new CodigoPecaDuplicadoException(peca.getCodigo());
        }

        // Define quantidade inicial como 0 se não informada
        if (peca.getQuantidadeAtual() == null) {
            peca.setQuantidadeAtual(0);
        }

        Peca pecaSalva = pecaRepository.save(peca);
        log.info("Peça criada com sucesso - ID: {}, Código: {}", pecaSalva.getId(), pecaSalva.getCodigo());

        return pecaSalva;
    }

    /**
     * Atualiza dados de uma peça existente.
     *
     * <p><strong>Atenção:</strong> NÃO atualiza quantidadeAtual - use MovimentacaoEstoqueService para isso.</p>
     *
     * @param id ID da peça
     * @param pecaAtualizada dados atualizados
     * @return peça atualizada
     * @throws PecaNotFoundException se peça não existe
     * @throws CodigoPecaDuplicadoException se novo código já existe
     */
    @Transactional
    public Peca atualizar(UUID id, Peca pecaAtualizada) {
        log.info("Atualizando peça ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Peca pecaExistente = pecaRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> new PecaNotFoundException(id));

        // Se código foi alterado, valida duplicidade
        if (!pecaExistente.getCodigo().equals(pecaAtualizada.getCodigo())) {
            if (pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(oficinaId, pecaAtualizada.getCodigo())) {
                log.warn("Tentativa de alterar código para um já existente: {}", pecaAtualizada.getCodigo());
                throw new CodigoPecaDuplicadoException(pecaAtualizada.getCodigo());
            }
        }

        // Atualiza apenas campos editáveis (NÃO atualiza quantidadeAtual nem localArmazenamento)
        pecaExistente.setCodigo(pecaAtualizada.getCodigo());
        pecaExistente.setDescricao(pecaAtualizada.getDescricao());
        pecaExistente.setMarca(pecaAtualizada.getMarca());
        pecaExistente.setAplicacao(pecaAtualizada.getAplicacao());
        // localArmazenamento é atualizado via definirLocalizacaoPeca()
        pecaExistente.setUnidadeMedida(pecaAtualizada.getUnidadeMedida());
        pecaExistente.setQuantidadeMinima(pecaAtualizada.getQuantidadeMinima());
        pecaExistente.setValorCusto(pecaAtualizada.getValorCusto());
        pecaExistente.setValorVenda(pecaAtualizada.getValorVenda());
        // quantidadeAtual NÃO é atualizado aqui!

        Peca pecaSalva = pecaRepository.save(pecaExistente);
        log.info("Peça atualizada com sucesso - ID: {}, Código: {}", pecaSalva.getId(), pecaSalva.getCodigo());

        return pecaSalva;
    }

    /**
     * Busca peça por ID.
     *
     * @param id ID da peça
     * @return peça encontrada
     * @throws PecaNotFoundException se não encontrada
     */
    @Transactional(readOnly = true)
    public Peca buscarPorId(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> new PecaNotFoundException(id));
    }

    /**
     * Busca peça por código (SKU).
     *
     * @param codigo código único da peça
     * @return peça encontrada
     * @throws PecaNotFoundException se não encontrada
     */
    @Transactional(readOnly = true)
    public Peca buscarPorCodigo(String codigo) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findByOficinaIdAndCodigoAndAtivoTrue(oficinaId, codigo)
                .orElseThrow(() -> new PecaNotFoundException(codigo));
    }

    /**
     * Lista todas as peças ativas com paginação.
     *
     * @param pageable paginação e ordenação
     * @return página de peças
     */
    @Transactional(readOnly = true)
    public Page<Peca> listarTodas(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findByOficinaIdAndAtivoTrueOrderByDescricaoAsc(oficinaId, pageable);
    }

    /**
     * Lista peças com filtros múltiplos.
     *
     * @param codigo filtro por código (opcional)
     * @param descricao filtro por descrição (opcional)
     * @param marca filtro por marca (opcional)
     * @param unidadeMedida filtro por unidade (opcional)
     * @param ativo filtro por status ativo (opcional)
     * @param estoqueBaixo filtro por estoque baixo (opcional)
     * @param localArmazenamentoId filtro por local de armazenamento (opcional)
     * @param pageable paginação
     * @return página de peças filtradas
     */
    @Transactional(readOnly = true)
    public Page<Peca> listarComFiltros(
            String codigo,
            String descricao,
            String marca,
            UnidadeMedida unidadeMedida,
            Boolean ativo,
            Boolean estoqueBaixo,
            UUID localArmazenamentoId,
            Pageable pageable
    ) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findByFilters(oficinaId, codigo, descricao, marca, unidadeMedida, ativo, estoqueBaixo, localArmazenamentoId, pageable);
    }

    /**
     * Lista peças com estoque baixo (quantidadeAtual <= quantidadeMinima).
     *
     * @param pageable paginação
     * @return página de peças com estoque baixo
     */
    @Transactional(readOnly = true)
    public Page<Peca> listarEstoqueBaixo(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findEstoqueBaixo(oficinaId, pageable);
    }

    /**
     * Lista peças com estoque zerado.
     *
     * @param pageable paginação
     * @return página de peças sem estoque
     */
    @Transactional(readOnly = true)
    public Page<Peca> listarEstoqueZerado(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findEstoqueZerado(oficinaId, pageable);
    }

    /**
     * Busca peças por marca.
     *
     * @param marca marca das peças
     * @param pageable paginação
     * @return página de peças da marca
     */
    @Transactional(readOnly = true)
    public Page<Peca> buscarPorMarca(String marca, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findByOficinaIdAndMarcaContainingIgnoreCaseAndAtivoTrue(oficinaId, marca, pageable);
    }

    /**
     * Busca peças por descrição (busca parcial).
     *
     * @param descricao texto a buscar
     * @param pageable paginação
     * @return página de peças encontradas
     */
    @Transactional(readOnly = true)
    public Page<Peca> buscarPorDescricao(String descricao, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findByOficinaIdAndDescricaoContainingIgnoreCaseAndAtivoTrue(oficinaId, descricao, pageable);
    }

    /**
     * Desativa uma peça (soft delete).
     *
     * @param id ID da peça
     * @throws PecaNotFoundException se não encontrada
     */
    @Transactional
    public void desativar(UUID id) {
        log.info("Desativando peça ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Peca peca = pecaRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> new PecaNotFoundException(id));

        peca.desativar();
        pecaRepository.save(peca);

        log.info("Peça desativada com sucesso - ID: {}, Código: {}", id, peca.getCodigo());
    }

    /**
     * Reativa uma peça desativada.
     *
     * @param id ID da peça
     * @throws PecaNotFoundException se não encontrada
     */
    @Transactional
    public void reativar(UUID id) {
        log.info("Reativando peça ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        // Busca incluindo inativas
        Peca peca = pecaRepository.findByOficinaIdAndId(oficinaId, id)
                .orElseThrow(() -> new PecaNotFoundException(id));

        // Valida se código não conflita ao reativar
        if (pecaRepository.existsByOficinaIdAndCodigoAndAtivoTrue(oficinaId, peca.getCodigo())) {
            log.warn("Não é possível reativar peça - código {} já está em uso", peca.getCodigo());
            throw new CodigoPecaDuplicadoException(peca.getCodigo());
        }

        peca.reativar();
        pecaRepository.save(peca);

        log.info("Peça reativada com sucesso - ID: {}, Código: {}", id, peca.getCodigo());
    }

    /**
     * Calcula o valor total do inventário (soma de quantidadeAtual * valorCusto).
     *
     * @return valor total em estoque
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularValorTotalInventario() {
        UUID oficinaId = TenantContext.getTenantId();
        BigDecimal valorTotal = pecaRepository.calcularValorTotalInventario(oficinaId);
        log.info("Valor total do inventário: R$ {}", valorTotal);
        return valorTotal;
    }

    /**
     * Conta quantas peças estão com estoque baixo.
     *
     * @return quantidade de peças com alerta
     */
    @Transactional(readOnly = true)
    public long contarEstoqueBaixo() {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.countEstoqueBaixo(oficinaId);
    }

    /**
     * Conta quantas peças estão com estoque zerado.
     *
     * @return quantidade de peças sem estoque
     */
    @Transactional(readOnly = true)
    public long contarEstoqueZerado() {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.countEstoqueZerado(oficinaId);
    }

    /**
     * Lista todas as marcas distintas cadastradas.
     *
     * @return lista de marcas
     */
    @Transactional(readOnly = true)
    public List<String> listarMarcas() {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findDistinctMarcas(oficinaId);
    }

    // ========== LOCATION MANAGEMENT ==========

    /**
     * Lista peças sem localização física definida.
     * Útil para identificar peças que precisam ser organizadas no estoque.
     *
     * @param pageable paginação
     * @return página de peças sem localização
     */
    @Transactional(readOnly = true)
    public Page<Peca> listarPecasSemLocalizacao(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.findPecasSemLocalizacao(oficinaId, pageable);
    }

    /**
     * Conta quantas peças estão sem localização.
     *
     * @return quantidade de peças sem localização
     */
    @Transactional(readOnly = true)
    public long contarPecasSemLocalizacao() {
        UUID oficinaId = TenantContext.getTenantId();
        return pecaRepository.countPecasSemLocalizacao(oficinaId);
    }

    /**
     * Define ou altera a localização física de uma peça.
     * Se localId for NULL, remove a localização atual.
     *
     * @param pecaId ID da peça
     * @param localId ID do local de armazenamento (NULL para remover)
     * @return peça atualizada
     * @throws PecaNotFoundException se peça não encontrada
     */
    @Transactional
    public Peca definirLocalizacaoPeca(UUID pecaId, UUID localId) {
        log.info("Definindo localização da peça {} para local {}", pecaId, localId);

        UUID oficinaId = TenantContext.getTenantId();
        Peca peca = pecaRepository.findByOficinaIdAndId(oficinaId, pecaId)
                .orElseThrow(() -> new PecaNotFoundException(pecaId));

        // LocalArmazenamento será validado no controller/service de locais
        // Aqui apenas setamos o ID - a FK garante integridade
        if (localId != null) {
            com.pitstop.estoque.domain.LocalArmazenamento local =
                    new com.pitstop.estoque.domain.LocalArmazenamento();
            local.setId(localId);
            peca.setLocalArmazenamento(local);
            log.info("Localização definida para peça {}", pecaId);
        } else {
            peca.setLocalArmazenamento(null);
            log.info("Localização removida da peça {}", pecaId);
        }

        return pecaRepository.save(peca);
    }
}
