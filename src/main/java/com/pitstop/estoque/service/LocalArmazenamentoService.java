package com.pitstop.estoque.service;

import com.pitstop.estoque.domain.LocalArmazenamento;
import com.pitstop.estoque.domain.TipoLocal;
import com.pitstop.estoque.dto.CreateLocalArmazenamentoDTO;
import com.pitstop.estoque.dto.UpdateLocalArmazenamentoDTO;
import com.pitstop.estoque.exception.CicloHierarquicoException;
import com.pitstop.estoque.exception.LocalComPecasVinculadasException;
import com.pitstop.estoque.mapper.LocalArmazenamentoMapper;
import com.pitstop.estoque.repository.LocalArmazenamentoRepository;
import com.pitstop.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service para gerenciamento de locais de armazenamento.
 * Implementa regras de negócio complexas para hierarquia e validações.
 *
 * @author PitStop Team
 * @version 1.0
 * @since 2025-11-02
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LocalArmazenamentoService {

    private static final int MAX_PROFUNDIDADE_HIERARQUIA = 10;

    private final LocalArmazenamentoRepository localRepository;
    private final LocalArmazenamentoMapper localMapper;

    /**
     * Cria novo local de armazenamento com validações completas.
     *
     * @param dto dados do local
     * @return local criado
     * @throws IllegalArgumentException se validação falhar
     * @throws CicloHierarquicoException se criaria ciclo
     * @throws ResourceNotFoundException se pai não existir
     */
    public LocalArmazenamento criar(CreateLocalArmazenamentoDTO dto) {
        log.info("Criando local de armazenamento: código={}, tipo={}", dto.codigo(), dto.tipo());

        // 1. Validar código único
        validarCodigoUnico(dto.codigo(), null);

        // 2. Converter DTO para entidade
        LocalArmazenamento local = localMapper.toEntity(dto);

        // 3. Setar localização pai se informada
        if (dto.localizacaoPaiId() != null) {
            LocalArmazenamento pai = buscarPorId(dto.localizacaoPaiId());
            local.setLocalizacaoPai(pai);

            // 4. Validar hierarquia
            validarHierarquia(local, pai);
        } else {
            // 5. Validar se tipo pode ser raiz
            validarPodeSerRaiz(local.getTipo());
        }

        // 6. Persistir
        LocalArmazenamento localSalvo = localRepository.save(local);
        log.info("Local criado com sucesso: id={}, código={}", localSalvo.getId(), localSalvo.getCodigo());

        return localSalvo;
    }

    /**
     * Busca local por ID.
     *
     * @param id ID do local
     * @return local encontrado
     * @throws ResourceNotFoundException se não encontrado
     */
    @Transactional(readOnly = true)
    public LocalArmazenamento buscarPorId(UUID id) {
        return localRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Local de armazenamento não encontrado: " + id));
    }

    /**
     * Busca local por código.
     *
     * @param codigo código do local (será normalizado para uppercase)
     * @return local encontrado
     * @throws ResourceNotFoundException se não encontrado
     */
    @Transactional(readOnly = true)
    public LocalArmazenamento buscarPorCodigo(String codigo) {
        String codigoNormalizado = codigo.trim().toUpperCase();
        return localRepository.findByCodigo(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Local não encontrado: " + codigoNormalizado));
    }

    /**
     * Lista todos os locais ativos.
     *
     * @return lista de locais ativos
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamento> listarTodos() {
        return localRepository.findByAtivoTrue();
    }

    /**
     * Lista locais raiz (sem pai).
     *
     * @return lista de locais raiz
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamento> listarLocaisRaiz() {
        return localRepository.findLocaisRaiz();
    }

    /**
     * Lista locais filhos de um pai específico.
     *
     * @param paiId ID do local pai
     * @return lista de filhos
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamento> listarFilhos(UUID paiId) {
        return localRepository.findByLocalizacaoPaiId(paiId);
    }

    /**
     * Lista locais por tipo.
     *
     * @param tipo tipo do local
     * @return lista de locais do tipo
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamento> listarPorTipo(TipoLocal tipo) {
        return localRepository.findByTipoAndAtivoTrue(tipo);
    }

    /**
     * Busca locais por descrição (parcial).
     *
     * @param descricao texto a buscar
     * @return lista de locais encontrados
     */
    @Transactional(readOnly = true)
    public List<LocalArmazenamento> buscarPorDescricao(String descricao) {
        return localRepository.findByDescricaoContainingIgnoreCaseAndAtivoTrue(descricao);
    }

    /**
     * Atualiza local existente com validações.
     *
     * @param id ID do local
     * @param dto dados atualizados
     * @return local atualizado
     * @throws ResourceNotFoundException se não encontrado
     * @throws CicloHierarquicoException se mudança de pai criaria ciclo
     * @throws IllegalArgumentException se validação falhar
     */
    public LocalArmazenamento atualizar(UUID id, UpdateLocalArmazenamentoDTO dto) {
        log.info("Atualizando local: id={}", id);

        // 1. Buscar local existente
        LocalArmazenamento local = buscarPorId(id);

        // 2. Validar código único (se mudou)
        if (!local.getCodigo().equals(dto.codigo().trim().toUpperCase())) {
            validarCodigoUnico(dto.codigo(), id);
        }

        // 3. Atualizar campos básicos
        localMapper.updateEntityFromDTO(dto, local);

        // 4. Atualizar localização pai se mudou
        UUID novoPaiId = dto.localizacaoPaiId();
        UUID paiAtualId = local.getLocalizacaoPai() != null ? local.getLocalizacaoPai().getId() : null;

        if (!equals(novoPaiId, paiAtualId)) {
            if (novoPaiId != null) {
                LocalArmazenamento novoPai = buscarPorId(novoPaiId);

                // Validar ciclo
                validarNaoCriaCiclo(local.getId(), novoPaiId);

                // Validar hierarquia
                validarHierarquia(local, novoPai);

                local.setLocalizacaoPai(novoPai);
            } else {
                // Removendo pai (tornando raiz)
                validarPodeSerRaiz(local.getTipo());
                local.setLocalizacaoPai(null);
            }
        }

        // 5. Persistir
        LocalArmazenamento localAtualizado = localRepository.save(local);
        log.info("Local atualizado com sucesso: id={}", id);

        return localAtualizado;
    }

    /**
     * Desativa local (soft delete).
     * Sempre permitido.
     *
     * @param id ID do local
     * @throws ResourceNotFoundException se não encontrado
     */
    public void desativar(UUID id) {
        log.info("Desativando local: id={}", id);

        LocalArmazenamento local = buscarPorId(id);
        local.setAtivo(false);
        localRepository.save(local);

        log.info("Local desativado com sucesso: id={}", id);
    }

    /**
     * Reativa local desativado.
     *
     * @param id ID do local
     * @throws ResourceNotFoundException se não encontrado
     */
    public void reativar(UUID id) {
        log.info("Reativando local: id={}", id);

        LocalArmazenamento local = buscarPorId(id);
        local.setAtivo(true);
        localRepository.save(local);

        log.info("Local reativado com sucesso: id={}", id);
    }

    /**
     * Exclui local permanentemente (hard delete).
     * Só permitido se não houver peças vinculadas.
     *
     * @param id ID do local
     * @throws ResourceNotFoundException se não encontrado
     * @throws LocalComPecasVinculadasException se houver peças vinculadas
     */
    public void excluir(UUID id) {
        log.info("Excluindo local permanentemente: id={}", id);

        // 1. Buscar local
        LocalArmazenamento local = buscarPorId(id);

        // 2. Validar se não tem peças vinculadas
        long quantidadePecas = localRepository.countPecasVinculadas(id);
        if (quantidadePecas > 0) {
            throw new LocalComPecasVinculadasException(quantidadePecas);
        }

        // 3. Excluir
        localRepository.delete(local);
        log.info("Local excluído com sucesso: id={}", id);
    }

    // ========== Métodos de Validação ==========

    /**
     * Valida se código é único.
     */
    private void validarCodigoUnico(String codigo, UUID idExcluir) {
        String codigoNormalizado = codigo.trim().toUpperCase();

        boolean existe = idExcluir == null
                ? localRepository.existsByCodigo(codigoNormalizado)
                : localRepository.existsByCodigoAndIdNot(codigoNormalizado, idExcluir);

        if (existe) {
            throw new IllegalArgumentException("Já existe local com o código: " + codigoNormalizado);
        }
    }

    /**
     * Valida se tipo pode ser local raiz.
     */
    private void validarPodeSerRaiz(TipoLocal tipo) {
        if (!tipo.podeSerRaiz()) {
            throw new IllegalArgumentException(
                    String.format("Tipo %s não pode ser local raiz. Deve ter um local pai.", tipo.getDescricao())
            );
        }
    }

    /**
     * Valida hierarquia completa.
     */
    private void validarHierarquia(LocalArmazenamento local, LocalArmazenamento pai) {
        // 1. Validar profundidade máxima
        int nivelPai = pai.getNivel();
        if (nivelPai >= MAX_PROFUNDIDADE_HIERARQUIA - 1) {
            throw new IllegalArgumentException(
                    String.format("Profundidade máxima da hierarquia atingida (%d níveis)", MAX_PROFUNDIDADE_HIERARQUIA)
            );
        }

        // 2. Validar regras de tipo (Decisão 2: A - validar hierarquia)
        if (!local.getTipo().aceitaTipoPai(pai.getTipo())) {
            throw new IllegalArgumentException(
                    String.format("%s não pode estar dentro de %s",
                            local.getTipo().getDescricao(),
                            pai.getTipo().getDescricao())
            );
        }
    }

    /**
     * Valida que mudança de pai não criaria ciclo.
     */
    private void validarNaoCriaCiclo(UUID localId, UUID novoPaiId) {
        if (localId.equals(novoPaiId)) {
            throw new CicloHierarquicoException("Local não pode ser pai de si mesmo");
        }

        boolean criaCiclo = localRepository.verificaCicloHierarquia(localId, novoPaiId);
        if (criaCiclo) {
            throw new CicloHierarquicoException(
                    "Mudança de localização pai criaria ciclo na hierarquia"
            );
        }
    }

    /**
     * Compara dois UUIDs, tratando nulls.
     */
    private boolean equals(UUID a, UUID b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
