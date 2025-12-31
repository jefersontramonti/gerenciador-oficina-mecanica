package com.pitstop.cliente.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.domain.TipoCliente;
import com.pitstop.cliente.dto.ClienteMapper;
import com.pitstop.cliente.dto.ClienteResponse;
import com.pitstop.cliente.dto.CreateClienteRequest;
import com.pitstop.cliente.dto.UpdateClienteRequest;
import com.pitstop.cliente.exception.ClienteNotFoundException;
import com.pitstop.cliente.exception.ClienteValidationException;
import com.pitstop.cliente.exception.CpfCnpjAlreadyExistsException;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.shared.validation.CpfCnpjUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço de negócio para gerenciamento de clientes.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Validações de regras de negócio</li>
 *   <li>Coordenação entre repository e DTOs via mapper</li>
 *   <li>Gerenciamento de cache (invalidação em operações de escrita)</li>
 *   <li>Log estruturado de operações</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    /**
     * Cria um novo cliente no sistema.
     *
     * <p>Validações realizadas:</p>
     * <ul>
     *   <li>CPF/CNPJ único no sistema</li>
     *   <li>Compatibilidade entre tipo e documento (tamanho)</li>
     * </ul>
     *
     * @param request dados do cliente a ser criado
     * @return cliente criado
     * @throws CpfCnpjAlreadyExistsException se CPF/CNPJ já cadastrado
     * @throws ClienteValidationException se documento incompatível com tipo
     */
    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public ClienteResponse create(CreateClienteRequest request) {
        log.info("Criando novo cliente: tipo={}, cpfCnpj={}", request.getTipo(), request.getCpfCnpj());

        UUID oficinaId = TenantContext.getTenantId();

        // Valida unicidade do CPF/CNPJ
        if (clienteRepository.existsByOficinaIdAndCpfCnpj(oficinaId, request.getCpfCnpj())) {
            log.warn("Tentativa de criar cliente com CPF/CNPJ duplicado: {}", request.getCpfCnpj());
            throw new CpfCnpjAlreadyExistsException(request.getCpfCnpj());
        }

        // Valida dígitos verificadores do CPF/CNPJ
        if (!CpfCnpjUtils.isValid(request.getCpfCnpj())) {
            log.warn("CPF/CNPJ com dígitos verificadores inválidos: {}", request.getCpfCnpj());
            throw new ClienteValidationException("CPF/CNPJ inválido - os dígitos verificadores não conferem");
        }

        // Valida compatibilidade entre tipo e documento
        boolean isCpf = CpfCnpjUtils.isCpf(request.getCpfCnpj());
        if (request.getTipo() == TipoCliente.PESSOA_FISICA && !isCpf) {
            throw new ClienteValidationException("Pessoa Física deve informar CPF (11 dígitos)");
        }
        if (request.getTipo() == TipoCliente.PESSOA_JURIDICA && isCpf) {
            throw new ClienteValidationException("Pessoa Jurídica deve informar CNPJ (14 dígitos)");
        }

        Cliente cliente = clienteMapper.toEntity(request);
        Cliente savedCliente = clienteRepository.save(cliente);

        log.info("Cliente criado com sucesso: id={}, nome={}", savedCliente.getId(), savedCliente.getNome());
        return clienteMapper.toResponse(savedCliente);
    }

    /**
     * Busca cliente por ID (incluindo inativos).
     *
     * <p>IMPORTANTE: Usa findByIdIncludingInactive para permitir acesso a clientes desativados.
     * Isso é necessário para permitir visualização e reativação de clientes inativos.</p>
     *
     * <p>Resultado cacheado por 24 horas (TTL definido em CacheConfig).</p>
     *
     * @param id identificador único do cliente
     * @return dados do cliente (ativo ou inativo)
     * @throws ClienteNotFoundException se cliente não encontrado
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "clientes", key = "#id")
    public ClienteResponse findById(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        // Usa query customizada que ignora @Where clause para permitir buscar clientes inativos
        Cliente cliente = clienteRepository.findByOficinaIdAndIdIncludingInactive(oficinaId, id)
            .orElseThrow(() -> new ClienteNotFoundException(id));

        return clienteMapper.toResponse(cliente);
    }

    /**
     * Busca cliente por CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ formatado
     * @return dados do cliente
     * @throws ClienteNotFoundException se cliente não encontrado
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "clientes", key = "#cpfCnpj")
    public ClienteResponse findByCpfCnpj(String cpfCnpj) {
        UUID oficinaId = TenantContext.getTenantId();
        Cliente cliente = clienteRepository.findByOficinaIdAndCpfCnpj(oficinaId, cpfCnpj)
            .orElseThrow(() -> new ClienteNotFoundException(cpfCnpj));

        return clienteMapper.toResponse(cliente);
    }

    /**
     * Lista todos os clientes ativos com paginação.
     *
     * @param pageable configuração de paginação e ordenação
     * @return página de clientes
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> findAll(Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return clienteRepository.findByOficinaId(oficinaId, pageable)
            .map(clienteMapper::toResponse);
    }

    /**
     * Busca clientes por tipo com paginação.
     *
     * @param tipo tipo de cliente (PESSOA_FISICA ou PESSOA_JURIDICA)
     * @param pageable configuração de paginação
     * @return página de clientes do tipo especificado
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> findByTipo(TipoCliente tipo, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return clienteRepository.findByOficinaIdAndTipo(oficinaId, tipo, pageable)
            .map(clienteMapper::toResponse);
    }

    /**
     * Busca clientes por nome (parcial, case-insensitive).
     *
     * @param nome termo de busca
     * @param pageable configuração de paginação
     * @return página de clientes encontrados
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> findByNome(String nome, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return clienteRepository.findByOficinaIdAndNomeContainingIgnoreCase(oficinaId, nome, pageable)
            .map(clienteMapper::toResponse);
    }

    /**
     * Busca avançada com múltiplos filtros.
     *
     * @param nome filtro de nome (opcional)
     * @param tipo filtro de tipo (opcional)
     * @param cidade filtro de cidade (opcional)
     * @param estado filtro de UF (opcional)
     * @param pageable configuração de paginação
     * @return página de clientes filtrados
     */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> findByFiltros(String nome, TipoCliente tipo, Boolean ativo, String cidade, String estado, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        // Converte enum para string para evitar erro BYTEA do PostgreSQL
        String tipoStr = (tipo != null) ? tipo.name() : null;

        return clienteRepository.findByFiltros(oficinaId, nome, tipoStr, ativo, cidade, estado, pageable)
            .map(clienteMapper::toResponse);
    }

    /**
     * Atualiza dados de um cliente existente.
     *
     * <p>Nota: CPF/CNPJ e tipo não podem ser alterados.</p>
     *
     * @param id identificador do cliente
     * @param request dados atualizados
     * @return cliente atualizado
     * @throws ClienteNotFoundException se cliente não encontrado
     */
    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public ClienteResponse update(UUID id, UpdateClienteRequest request) {
        log.info("Atualizando cliente: id={}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ClienteNotFoundException(id));

        clienteMapper.updateEntityFromDto(request, cliente);
        Cliente updatedCliente = clienteRepository.save(cliente);

        log.info("Cliente atualizado com sucesso: id={}, nome={}", updatedCliente.getId(), updatedCliente.getNome());
        return clienteMapper.toResponse(updatedCliente);
    }

    /**
     * Realiza soft delete de um cliente (marca como inativo).
     *
     * <p>Cliente inativo não aparece em buscas padrão devido ao {@code @Where} na entidade.</p>
     *
     * @param id identificador do cliente
     * @throws ClienteNotFoundException se cliente não encontrado
     */
    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public void delete(UUID id) {
        log.info("Desativando cliente (soft delete): id={}", id);

        UUID oficinaId = TenantContext.getTenantId();
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new ClienteNotFoundException(id));

        cliente.desativar();
        clienteRepository.save(cliente);

        log.info("Cliente desativado com sucesso: id={}", id);
    }

    /**
     * Reativa um cliente previamente desativado.
     *
     * @param id identificador do cliente
     * @throws ClienteNotFoundException se cliente não encontrado
     */
    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public ClienteResponse reativar(UUID id) {
        log.info("Reativando cliente: id={}", id);

        UUID oficinaId = TenantContext.getTenantId();
        // Busca ignorando o filtro @Where para encontrar clientes inativos
        Cliente cliente = clienteRepository.findByOficinaIdAndIdIncludingInactive(oficinaId, id)
            .orElseThrow(() -> new ClienteNotFoundException(id));

        cliente.reativar();
        Cliente reativadoCliente = clienteRepository.save(cliente);

        log.info("Cliente reativado com sucesso: id={}", id);
        return clienteMapper.toResponse(reativadoCliente);
    }

    /**
     * Retorna lista de estados (UF) únicos presentes nos endereços de clientes.
     * Útil para popular filtros dinâmicos.
     *
     * @return lista de siglas de estados
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "clientes", key = "'estados'")
    public List<String> findEstados() {
        UUID oficinaId = TenantContext.getTenantId();
        return clienteRepository.findDistinctEstados(oficinaId);
    }

    /**
     * Retorna lista de cidades únicas presentes nos endereços de clientes.
     * Útil para popular filtros dinâmicos.
     *
     * @return lista de nomes de cidades
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "clientes", key = "'cidades'")
    public List<String> findCidades() {
        UUID oficinaId = TenantContext.getTenantId();
        return clienteRepository.findDistinctCidades(oficinaId);
    }

    /**
     * Conta total de clientes ativos por tipo.
     *
     * @param tipo tipo de cliente
     * @return quantidade de clientes
     */
    @Transactional(readOnly = true)
    public long countByTipo(TipoCliente tipo) {
        UUID oficinaId = TenantContext.getTenantId();
        return clienteRepository.countByOficinaIdAndTipo(oficinaId, tipo);
    }
}
