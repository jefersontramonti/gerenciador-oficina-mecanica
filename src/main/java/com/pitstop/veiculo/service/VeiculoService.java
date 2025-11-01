package com.pitstop.veiculo.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.exception.ClienteNotFoundException;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.dto.*;
import com.pitstop.veiculo.exception.PlacaJaExisteException;
import com.pitstop.veiculo.exception.VeiculoNotFoundException;
import com.pitstop.veiculo.mapper.VeiculoMapper;
import com.pitstop.veiculo.repository.VeiculoRepository;
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
 * Serviço de negócio para gerenciamento de veículos.
 *
 * <p>Responsável por:</p>
 * <ul>
 *   <li>Validações de regras de negócio (placa única, cliente existente)</li>
 *   <li>Coordenação entre repository e DTOs via mapper</li>
 *   <li>Gerenciamento de cache (invalidação em operações de escrita)</li>
 *   <li>Log estruturado de operações</li>
 *   <li>Enriquecimento de responses com dados do cliente</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteRepository clienteRepository;
    private final VeiculoMapper veiculoMapper;

    /**
     * Cria um novo veículo no sistema.
     *
     * <p>Validações realizadas:</p>
     * <ul>
     *   <li>Cliente existe no sistema</li>
     *   <li>Placa única no sistema (normalizada)</li>
     *   <li>Ano válido (1900 até ano atual + 1)</li>
     * </ul>
     *
     * @param request dados do veículo a ser criado
     * @return veículo criado
     * @throws ClienteNotFoundException se cliente não encontrado
     * @throws PlacaJaExisteException se placa já cadastrada
     */
    @Transactional
    @CacheEvict(value = "veiculos", allEntries = true)
    public VeiculoResponseDTO create(VeiculoRequestDTO request) {
        log.info("Criando novo veículo: placa={}, clienteId={}", request.getPlaca(), request.getClienteId());

        // Valida se cliente existe
        Cliente cliente = clienteRepository.findById(request.getClienteId())
            .orElseThrow(() -> new ClienteNotFoundException(request.getClienteId()));

        // Normaliza placa antes de validar unicidade
        String placaNormalizada = request.getPlaca().trim().toUpperCase().replace("-", "");

        // Valida unicidade da placa
        if (veiculoRepository.existsByPlaca(placaNormalizada)) {
            log.warn("Tentativa de criar veículo com placa duplicada: {}", placaNormalizada);
            throw new PlacaJaExisteException(request.getPlaca());
        }

        Veiculo veiculo = veiculoMapper.toEntity(request);
        Veiculo savedVeiculo = veiculoRepository.save(veiculo);

        log.info("Veículo criado com sucesso: id={}, placa={}", savedVeiculo.getId(), savedVeiculo.getPlaca());

        VeiculoResponseDTO response = veiculoMapper.toResponse(savedVeiculo);
        enrichWithClienteData(response, cliente);

        return response;
    }

    /**
     * Busca veículo por ID.
     *
     * <p>Resultado cacheado por 24 horas (TTL definido em CacheConfig).</p>
     *
     * @param id identificador único do veículo
     * @return dados do veículo
     * @throws VeiculoNotFoundException se veículo não encontrado
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "veiculos", key = "#id")
    public VeiculoResponseDTO findById(UUID id) {
        log.debug("Buscando veículo por ID: {}", id);

        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNotFoundException(id));

        VeiculoResponseDTO response = veiculoMapper.toResponse(veiculo);
        enrichWithClienteData(response, veiculo.getClienteId());

        return response;
    }

    /**
     * Busca veículo por placa.
     *
     * @param placa placa do veículo (com ou sem hífen)
     * @return dados do veículo
     * @throws VeiculoNotFoundException se veículo não encontrado
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "veiculos", key = "#placa")
    public VeiculoResponseDTO findByPlaca(String placa) {
        log.debug("Buscando veículo por placa: {}", placa);

        String placaNormalizada = placa.trim().toUpperCase().replace("-", "");

        Veiculo veiculo = veiculoRepository.findByPlaca(placaNormalizada)
            .orElseThrow(() -> new VeiculoNotFoundException(placa));

        VeiculoResponseDTO response = veiculoMapper.toResponse(veiculo);
        enrichWithClienteData(response, veiculo.getClienteId());

        return response;
    }

    /**
     * Lista todos os veículos com paginação.
     *
     * @param pageable configuração de paginação e ordenação
     * @return página de veículos
     */
    @Transactional(readOnly = true)
    public Page<VeiculoResponseDTO> findAll(Pageable pageable) {
        log.debug("Listando veículos: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        return veiculoRepository.findAll(pageable)
            .map(veiculo -> {
                VeiculoResponseDTO response = veiculoMapper.toResponse(veiculo);
                enrichWithClienteData(response, veiculo.getClienteId());
                return response;
            });
    }

    /**
     * Busca veículos de um cliente específico.
     *
     * @param clienteId ID do cliente
     * @param pageable configuração de paginação
     * @return página de veículos do cliente
     * @throws ClienteNotFoundException se cliente não encontrado
     */
    @Transactional(readOnly = true)
    public Page<VeiculoResponseDTO> findByClienteId(UUID clienteId, Pageable pageable) {
        log.debug("Buscando veículos do cliente: {}", clienteId);

        // Valida se cliente existe
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new ClienteNotFoundException(clienteId));

        return veiculoRepository.findByClienteId(clienteId, pageable)
            .map(veiculo -> {
                VeiculoResponseDTO response = veiculoMapper.toResponse(veiculo);
                enrichWithClienteData(response, cliente);
                return response;
            });
    }

    /**
     * Busca avançada com múltiplos filtros.
     *
     * @param clienteId filtro de cliente (opcional)
     * @param placa filtro de placa parcial (opcional)
     * @param marca filtro de marca (opcional)
     * @param modelo filtro de modelo (opcional)
     * @param ano filtro de ano (opcional)
     * @param pageable configuração de paginação
     * @return página de veículos filtrados
     */
    @Transactional(readOnly = true)
    public Page<VeiculoResponseDTO> findByFiltros(UUID clienteId, String placa, String marca, String modelo, Integer ano, Pageable pageable) {
        log.debug("Busca com filtros: clienteId={}, placa={}, marca={}, modelo={}, ano={}", clienteId, placa, marca, modelo, ano);

        // Normaliza placa se fornecida
        String placaNormalizada = (placa != null) ? placa.trim().toUpperCase().replace("-", "") : null;

        return veiculoRepository.findByFiltros(clienteId, placaNormalizada, marca, modelo, ano, pageable)
            .map(veiculo -> {
                VeiculoResponseDTO response = veiculoMapper.toResponse(veiculo);
                enrichWithClienteData(response, veiculo.getClienteId());
                return response;
            });
    }

    /**
     * Atualiza dados de um veículo existente.
     *
     * <p>Nota: Placa e clienteId não podem ser alterados.</p>
     *
     * @param id identificador do veículo
     * @param request dados atualizados
     * @return veículo atualizado
     * @throws VeiculoNotFoundException se veículo não encontrado
     */
    @Transactional
    @CacheEvict(value = "veiculos", allEntries = true)
    public VeiculoResponseDTO update(UUID id, VeiculoUpdateDTO request) {
        log.info("Atualizando veículo: id={}", id);

        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNotFoundException(id));

        veiculoMapper.updateEntityFromDto(request, veiculo);
        Veiculo updatedVeiculo = veiculoRepository.save(veiculo);

        log.info("Veículo atualizado com sucesso: id={}, placa={}", updatedVeiculo.getId(), updatedVeiculo.getPlaca());

        VeiculoResponseDTO response = veiculoMapper.toResponse(updatedVeiculo);
        enrichWithClienteData(response, updatedVeiculo.getClienteId());

        return response;
    }

    /**
     * Atualiza apenas a quilometragem do veículo.
     *
     * @param id identificador do veículo
     * @param request nova quilometragem
     * @return veículo atualizado
     * @throws VeiculoNotFoundException se veículo não encontrado
     */
    @Transactional
    @CacheEvict(value = "veiculos", allEntries = true)
    public VeiculoResponseDTO updateQuilometragem(UUID id, QuilometragemUpdateDTO request) {
        log.info("Atualizando quilometragem do veículo: id={}, novaQuilometragem={}", id, request.getQuilometragem());

        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNotFoundException(id));

        veiculo.atualizarQuilometragem(request.getQuilometragem());
        Veiculo updatedVeiculo = veiculoRepository.save(veiculo);

        log.info("Quilometragem atualizada com sucesso: id={}, quilometragem={}", updatedVeiculo.getId(), updatedVeiculo.getQuilometragem());

        VeiculoResponseDTO response = veiculoMapper.toResponse(updatedVeiculo);
        enrichWithClienteData(response, updatedVeiculo.getClienteId());

        return response;
    }

    /**
     * Remove um veículo do sistema.
     *
     * <p>TODO: Implementar validação de ordens de serviço vinculadas antes de permitir deleção.
     * Deve retornar erro se veículo possui ordens de serviço cadastradas.</p>
     *
     * @param id identificador do veículo
     * @throws VeiculoNotFoundException se veículo não encontrado
     */
    @Transactional
    @CacheEvict(value = "veiculos", allEntries = true)
    public void delete(UUID id) {
        log.info("Removendo veículo: id={}", id);

        Veiculo veiculo = veiculoRepository.findById(id)
            .orElseThrow(() -> new VeiculoNotFoundException(id));

        // TODO: Validar se veículo não possui ordens de serviço vinculadas
        // if (ordemServicoRepository.existsByVeiculoId(id)) {
        //     throw new VeiculoComOrdensException(id);
        // }

        veiculoRepository.delete(veiculo);
        log.info("Veículo removido com sucesso: id={}", id);
    }

    /**
     * Retorna lista de marcas únicas presentes nos veículos cadastrados.
     * Útil para popular filtros dinâmicos.
     *
     * @return lista de nomes de marcas
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "veiculos", key = "'marcas'")
    public List<String> findMarcas() {
        log.debug("Buscando lista de marcas");
        return veiculoRepository.findDistinctMarcas();
    }

    /**
     * Retorna lista de modelos únicos presentes nos veículos cadastrados.
     * Útil para popular filtros dinâmicos.
     *
     * @return lista de nomes de modelos
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "veiculos", key = "'modelos'")
    public List<String> findModelos() {
        log.debug("Buscando lista de modelos");
        return veiculoRepository.findDistinctModelos();
    }

    /**
     * Retorna lista de anos únicos presentes nos veículos cadastrados.
     * Útil para popular filtros dinâmicos.
     *
     * @return lista de anos
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "veiculos", key = "'anos'")
    public List<Integer> findAnos() {
        log.debug("Buscando lista de anos");
        return veiculoRepository.findDistinctAnos();
    }

    /**
     * Conta total de veículos de um cliente.
     *
     * @param clienteId ID do cliente
     * @return quantidade de veículos
     */
    @Transactional(readOnly = true)
    public long countByClienteId(UUID clienteId) {
        log.debug("Contando veículos do cliente: {}", clienteId);
        return veiculoRepository.countByClienteId(clienteId);
    }

    /**
     * Enriquece VeiculoResponseDTO com dados do cliente.
     *
     * @param response DTO de resposta
     * @param clienteId ID do cliente
     */
    private void enrichWithClienteData(VeiculoResponseDTO response, UUID clienteId) {
        clienteRepository.findById(clienteId).ifPresent(cliente ->
            enrichWithClienteData(response, cliente)
        );
    }

    /**
     * Enriquece VeiculoResponseDTO com dados do cliente.
     *
     * @param response DTO de resposta
     * @param cliente entidade Cliente
     */
    private void enrichWithClienteData(VeiculoResponseDTO response, Cliente cliente) {
        String telefone = (cliente.getCelular() != null && !cliente.getCelular().isBlank())
            ? cliente.getCelular()
            : cliente.getTelefone();

        response.setCliente(veiculoMapper.toClienteResumo(
            cliente.getId(),
            cliente.getNome(),
            cliente.getCpfCnpj(),
            telefone
        ));
    }
}
