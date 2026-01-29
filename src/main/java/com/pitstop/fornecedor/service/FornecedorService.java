package com.pitstop.fornecedor.service;

import com.pitstop.fornecedor.domain.Fornecedor;
import com.pitstop.fornecedor.domain.TipoFornecedor;
import com.pitstop.fornecedor.dto.*;
import com.pitstop.fornecedor.exception.CpfCnpjFornecedorDuplicadoException;
import com.pitstop.fornecedor.exception.FornecedorNotFoundException;
import com.pitstop.fornecedor.repository.FornecedorRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de negócio para gerenciamento de fornecedores.
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorMapper fornecedorMapper;

    @Transactional
    @CacheEvict(value = "fornecedores", allEntries = true)
    public FornecedorResponse create(CreateFornecedorRequest request) {
        log.info("Criando novo fornecedor: {}", request.getNomeFantasia());

        UUID oficinaId = TenantContext.getTenantId();

        // Valida unicidade do CPF/CNPJ (se informado)
        if (request.getCpfCnpj() != null && !request.getCpfCnpj().isBlank()) {
            if (fornecedorRepository.existsByOficinaIdAndCpfCnpj(oficinaId, request.getCpfCnpj())) {
                log.warn("Tentativa de criar fornecedor com CPF/CNPJ duplicado: {}", request.getCpfCnpj());
                throw new CpfCnpjFornecedorDuplicadoException(request.getCpfCnpj());
            }
        }

        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        Fornecedor saved = fornecedorRepository.save(fornecedor);

        log.info("Fornecedor criado com sucesso: id={}, nome={}", saved.getId(), saved.getNomeFantasia());
        return fornecedorMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "fornecedores", key = "#id")
    public FornecedorResponse findById(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        Fornecedor fornecedor = fornecedorRepository.findByOficinaIdAndIdIncludingInactive(oficinaId, id)
            .orElseThrow(() -> new FornecedorNotFoundException(id));
        return fornecedorMapper.toResponse(fornecedor);
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponse> findAll(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return fornecedorRepository.findByOficinaId(oficinaId, pageable)
            .map(fornecedorMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<FornecedorResponse> findByFiltros(String nome, TipoFornecedor tipo, String cidade, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        String tipoStr = (tipo != null) ? tipo.name() : null;
        // Native query já tem ORDER BY fixo; remover sort do Pageable para evitar "column not found"
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return fornecedorRepository.findByFiltros(oficinaId, nome, tipoStr, cidade, unsorted)
            .map(fornecedorMapper::toResponse);
    }

    /**
     * Lista todos os fornecedores ativos (resumo) para uso em selects/autocomplete.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "fornecedores", key = "'resumo'")
    public List<FornecedorResumoResponse> findAllResumo() {
        UUID oficinaId = TenantContext.getTenantId();
        return fornecedorRepository.findAllByOficinaIdOrderByNomeFantasia(oficinaId)
            .stream()
            .map(fornecedorMapper::toResumoResponse)
            .toList();
    }

    @Transactional
    @CacheEvict(value = "fornecedores", allEntries = true)
    public FornecedorResponse update(UUID id, UpdateFornecedorRequest request) {
        log.info("Atualizando fornecedor: id={}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Fornecedor fornecedor = fornecedorRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new FornecedorNotFoundException(id));

        // Valida unicidade do CPF/CNPJ se alterado
        if (request.getCpfCnpj() != null && !request.getCpfCnpj().isBlank()) {
            if (fornecedorRepository.existsByOficinaIdAndCpfCnpjAndIdNot(oficinaId, request.getCpfCnpj(), id)) {
                log.warn("Tentativa de alterar CPF/CNPJ para um já existente: {}", request.getCpfCnpj());
                throw new CpfCnpjFornecedorDuplicadoException(request.getCpfCnpj());
            }
        }

        fornecedorMapper.updateEntityFromDto(request, fornecedor);
        Fornecedor updated = fornecedorRepository.save(fornecedor);

        log.info("Fornecedor atualizado com sucesso: id={}, nome={}", updated.getId(), updated.getNomeFantasia());
        return fornecedorMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "fornecedores", allEntries = true)
    public void delete(UUID id) {
        log.info("Desativando fornecedor (soft delete): id={}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Fornecedor fornecedor = fornecedorRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new FornecedorNotFoundException(id));

        fornecedor.desativar();
        fornecedorRepository.save(fornecedor);

        log.info("Fornecedor desativado com sucesso: id={}", id);
    }

    @Transactional
    @CacheEvict(value = "fornecedores", allEntries = true)
    public FornecedorResponse reativar(UUID id) {
        log.info("Reativando fornecedor: id={}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Fornecedor fornecedor = fornecedorRepository.findByOficinaIdAndIdIncludingInactive(oficinaId, id)
            .orElseThrow(() -> new FornecedorNotFoundException(id));

        fornecedor.reativar();
        Fornecedor reativado = fornecedorRepository.save(fornecedor);

        log.info("Fornecedor reativado com sucesso: id={}", id);
        return fornecedorMapper.toResponse(reativado);
    }
}
