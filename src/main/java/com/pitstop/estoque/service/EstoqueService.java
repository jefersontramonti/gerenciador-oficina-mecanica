package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.domain.UnidadeMedida;
import com.pitstop.estoque.exception.CodigoPecaDuplicadoException;
import com.pitstop.estoque.exception.PecaNotFoundException;
import com.pitstop.estoque.repository.PecaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public Peca criar(Peca peca) {
        log.info("Criando nova peça com código: {}", peca.getCodigo());

        // Valida se código já existe
        if (pecaRepository.existsByCodigoAndAtivoTrue(peca.getCodigo())) {
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
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public Peca atualizar(UUID id, Peca pecaAtualizada) {
        log.info("Atualizando peça ID: {}", id);

        Peca pecaExistente = pecaRepository.findById(id)
                .orElseThrow(() -> new PecaNotFoundException(id));

        // Se código foi alterado, valida duplicidade
        if (!pecaExistente.getCodigo().equals(pecaAtualizada.getCodigo())) {
            if (pecaRepository.existsByCodigoAndAtivoTrue(pecaAtualizada.getCodigo())) {
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
    @Cacheable(value = "pecas", key = "#id")
    public Peca buscarPorId(UUID id) {
        log.debug("Buscando peça por ID: {}", id);
        return pecaRepository.findById(id)
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
    @Cacheable(value = "pecas", key = "'codigo:' + #codigo")
    public Peca buscarPorCodigo(String codigo) {
        log.debug("Buscando peça por código: {}", codigo);
        return pecaRepository.findByCodigoAndAtivoTrue(codigo)
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
        log.debug("Listando todas as peças ativas - Página: {}", pageable.getPageNumber());
        return pecaRepository.findByAtivoTrueOrderByDescricaoAsc(pageable);
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
            Pageable pageable
    ) {
        log.debug("Listando peças com filtros - codigo: {}, descricao: {}, marca: {}, unidade: {}, ativo: {}, estoqueBaixo: {}",
                codigo, descricao, marca, unidadeMedida, ativo, estoqueBaixo);
        return pecaRepository.findByFilters(codigo, descricao, marca, unidadeMedida, ativo, estoqueBaixo, pageable);
    }

    /**
     * Lista peças com estoque baixo (quantidadeAtual <= quantidadeMinima).
     *
     * @param pageable paginação
     * @return página de peças com estoque baixo
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "estoqueBaixo")
    public Page<Peca> listarEstoqueBaixo(Pageable pageable) {
        log.debug("Listando peças com estoque baixo");
        return pecaRepository.findEstoqueBaixo(pageable);
    }

    /**
     * Lista peças com estoque zerado.
     *
     * @param pageable paginação
     * @return página de peças sem estoque
     */
    @Transactional(readOnly = true)
    public Page<Peca> listarEstoqueZerado(Pageable pageable) {
        log.debug("Listando peças com estoque zerado");
        return pecaRepository.findEstoqueZerado(pageable);
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
        log.debug("Buscando peças por marca: {}", marca);
        return pecaRepository.findByMarcaContainingIgnoreCaseAndAtivoTrue(marca, pageable);
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
        log.debug("Buscando peças por descrição: {}", descricao);
        return pecaRepository.findByDescricaoContainingIgnoreCaseAndAtivoTrue(descricao, pageable);
    }

    /**
     * Desativa uma peça (soft delete).
     *
     * @param id ID da peça
     * @throws PecaNotFoundException se não encontrada
     */
    @Transactional
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public void desativar(UUID id) {
        log.info("Desativando peça ID: {}", id);

        Peca peca = pecaRepository.findById(id)
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
    @CacheEvict(value = {"pecas", "estoqueBaixo"}, allEntries = true)
    public void reativar(UUID id) {
        log.info("Reativando peça ID: {}", id);

        // Busca incluindo inativas
        Peca peca = pecaRepository.findById(id)
                .orElseThrow(() -> new PecaNotFoundException(id));

        // Valida se código não conflita ao reativar
        if (pecaRepository.existsByCodigoAndAtivoTrue(peca.getCodigo())) {
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
        log.debug("Calculando valor total do inventário");
        BigDecimal valorTotal = pecaRepository.calcularValorTotalInventario();
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
        return pecaRepository.countEstoqueBaixo();
    }

    /**
     * Conta quantas peças estão com estoque zerado.
     *
     * @return quantidade de peças sem estoque
     */
    @Transactional(readOnly = true)
    public long contarEstoqueZerado() {
        return pecaRepository.countEstoqueZerado();
    }

    /**
     * Lista todas as marcas distintas cadastradas.
     *
     * @return lista de marcas
     */
    @Transactional(readOnly = true)
    public List<String> listarMarcas() {
        return pecaRepository.findDistinctMarcas();
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
        log.debug("Listando peças sem localização");
        return pecaRepository.findPecasSemLocalizacao(pageable);
    }

    /**
     * Conta quantas peças estão sem localização.
     *
     * @return quantidade de peças sem localização
     */
    @Transactional(readOnly = true)
    public long contarPecasSemLocalizacao() {
        return pecaRepository.countPecasSemLocalizacao();
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
    @CacheEvict(value = {"pecas"}, allEntries = true)
    public Peca definirLocalizacaoPeca(UUID pecaId, UUID localId) {
        log.info("Definindo localização da peça {} para local {}", pecaId, localId);

        Peca peca = pecaRepository.findById(pecaId)
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
