package com.pitstop.ordemservico.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.exception.ClienteNotFoundException;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.dto.*;
import com.pitstop.ordemservico.exception.*;
import com.pitstop.ordemservico.mapper.ItemOSMapper;
import com.pitstop.ordemservico.mapper.OrdemServicoMapper;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.exception.UsuarioNotFoundException;
import com.pitstop.usuario.repository.UsuarioRepository;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.exception.VeiculoNotFoundException;
import com.pitstop.veiculo.repository.VeiculoRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de Ordens de Serviço.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>CRUD completo de OS</li>
 *   <li>Gerenciamento de transições de status (máquina de estados)</li>
 *   <li>Cálculos financeiros</li>
 *   <li>Validações de regras de negócio</li>
 *   <li>Cache de consultas frequentes</li>
 * </ul>
 *
 * @author PitStop Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrdemServicoService {

    private final OrdemServicoRepository repository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final OrdemServicoMapper mapper;
    private final ItemOSMapper itemMapper;

    // ===== CREATE =====

    /**
     * Cria nova Ordem de Serviço no status ORCAMENTO.
     *
     * @param dto dados da OS
     * @return DTO de resposta com OS criada
     * @throws VeiculoNotFoundException se veículo não existir
     * @throws UsuarioNotFoundException se mecânico não existir
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public OrdemServicoResponseDTO criar(CreateOrdemServicoDTO dto) {
        log.info("Criando nova OS para veículo ID: {}, mecânico ID: {}", dto.veiculoId(), dto.usuarioId());

        // Valida veículo existe
        Veiculo veiculo = veiculoRepository.findById(dto.veiculoId())
            .orElseThrow(() -> new VeiculoNotFoundException(dto.veiculoId()));

        // Valida mecânico existe
        Usuario mecanico = usuarioRepository.findById(dto.usuarioId())
            .orElseThrow(() -> new UsuarioNotFoundException(dto.usuarioId()));

        // Mapeia DTO para entidade
        OrdemServico os = mapper.toEntity(dto);

        // Gera número sequencial
        Long numero = repository.getNextNumero();
        os.setNumero(numero);

        // Adiciona itens (se houver)
        if (dto.itens() != null && !dto.itens().isEmpty()) {
            dto.itens().forEach(itemDto -> {
                ItemOS item = itemMapper.toEntity(itemDto);
                os.adicionarItem(item);
            });
        }

        // Recalcula valores
        os.recalcularValores();

        // Salva
        OrdemServico saved = repository.save(os);

        log.info("OS #{} criada com sucesso - ID: {}", saved.getNumero(), saved.getId());

        // Retorna response com dados relacionados
        return montarResponse(saved, veiculo, mecanico);
    }

    // ===== READ =====

    /**
     * Busca OS por ID.
     *
     * @param id ID da OS
     * @return DTO de resposta
     * @throws OrdemServicoNotFoundException se não encontrada
     */
    public OrdemServicoResponseDTO buscarPorId(UUID id) {
        log.debug("Buscando OS por ID: {}", id);

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        return montarResponse(os);
    }

    /**
     * Busca OS por número sequencial.
     *
     * @param numero número da OS
     * @return DTO de resposta
     * @throws OrdemServicoNotFoundException se não encontrada
     */
    public OrdemServicoResponseDTO buscarPorNumero(Long numero) {
        log.debug("Buscando OS por número: {}", numero);

        OrdemServico os = repository.findByNumero(numero)
            .orElseThrow(() -> new OrdemServicoNotFoundException(numero));

        return montarResponse(os);
    }

    /**
     * Lista OS com filtros opcionais e paginação.
     *
     * @param status status da OS (opcional)
     * @param veiculoId ID do veículo (opcional)
     * @param usuarioId ID do mecânico (opcional)
     * @param dataInicio data inicial do período (opcional)
     * @param dataFim data final do período (opcional)
     * @param pageable configuração de paginação e ordenação
     * @return página de OS
     */
    public Page<OrdemServicoResponseDTO> listar(
        StatusOS status,
        UUID veiculoId,
        UUID usuarioId,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        Pageable pageable
    ) {
        log.debug("Listando OS com filtros - Status: {}, Veículo: {}, Usuário: {}", status, veiculoId, usuarioId);

        String statusStr = status != null ? status.name() : null;

        Page<OrdemServico> page = repository.findByFiltros(statusStr, veiculoId, usuarioId, dataInicio, dataFim, pageable);

        return page.map(this::montarResponse);
    }

    /**
     * Busca histórico de OS de um veículo.
     *
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação
     * @return página de OS do veículo
     */
    public Page<OrdemServicoResponseDTO> buscarHistoricoVeiculo(UUID veiculoId, Pageable pageable) {
        log.debug("Buscando histórico de OS do veículo ID: {}", veiculoId);

        return repository.findHistoricoVeiculo(veiculoId, pageable)
            .map(this::montarResponse);
    }

    // ===== UPDATE =====

    /**
     * Atualiza OS existente.
     * Apenas OS em status ORCAMENTO ou APROVADO podem ser editadas.
     *
     * @param id ID da OS
     * @param dto dados para atualização
     * @return DTO de resposta atualizado
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws OrdemServicoNaoEditavelException se não estiver editável
     */
    @Transactional
    @CacheEvict(value = "ordemServico", key = "#id")
    public OrdemServicoResponseDTO atualizar(UUID id, UpdateOrdemServicoDTO dto) {
        log.info("Atualizando OS ID: {}", id);

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        // Valida se pode editar
        if (!os.getStatus().isEditavel()) {
            throw new OrdemServicoNaoEditavelException(os.getStatus());
        }

        // Atualiza campos básicos
        mapper.updateEntityFromDto(dto, os);

        // Atualiza itens (se fornecidos)
        if (dto.itens() != null) {
            os.limparItens();
            dto.itens().forEach(itemDto -> {
                ItemOS item = itemMapper.toEntity(itemDto);
                os.adicionarItem(item);
            });
        }

        // Recalcula valores
        os.recalcularValores();

        OrdemServico saved = repository.save(os);

        log.info("OS #{} atualizada com sucesso", saved.getNumero());

        return montarResponse(saved);
    }

    // ===== STATUS TRANSITIONS =====

    /**
     * Aprova orçamento (ORCAMENTO → APROVADO).
     *
     * @param id ID da OS
     * @param aprovadoPeloCliente indicador de aprovação do cliente
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se não estiver em status ORCAMENTO
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void aprovar(UUID id, Boolean aprovadoPeloCliente) {
        log.info("Aprovando OS ID: {}, aprovado pelo cliente: {}", id, aprovadoPeloCliente);

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.aprovar(Boolean.TRUE.equals(aprovadoPeloCliente));
            repository.save(os);
            log.info("OS #{} aprovada com sucesso", os.getNumero());
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Inicia execução (APROVADO → EM_ANDAMENTO).
     *
     * @param id ID da OS
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void iniciar(UUID id) {
        log.info("Iniciando execução da OS ID: {}", id);

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.iniciar();
            repository.save(os);
            log.info("OS #{} iniciada com sucesso", os.getNumero());
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Finaliza OS (EM_ANDAMENTO/AGUARDANDO_PECA → FINALIZADO).
     * Neste ponto ocorre a baixa automática de peças do estoque.
     *
     * @param id ID da OS
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void finalizar(UUID id) {
        log.info("Finalizando OS ID: {}", id);

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.finalizar();

            // TODO: Integrar com EstoqueService para baixa de peças
            // List<ItemOS> pecas = os.getItens().stream()
            //     .filter(item -> item.getTipo() == TipoItem.PECA)
            //     .toList();
            // estoqueService.baixarPecas(pecas);

            repository.save(os);
            log.info("OS #{} finalizada com sucesso", os.getNumero());
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Entrega veículo ao cliente (FINALIZADO → ENTREGUE).
     *
     * @param id ID da OS
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void entregar(UUID id) {
        log.info("Entregando veículo da OS ID: {}", id);

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            // TODO: Validar se OS está paga (integração com módulo Financeiro)
            // if (!financeiroService.isOSPaga(id)) {
            //     throw new OrdemServicoNaoPagaException(id);
            // }

            os.entregar();
            repository.save(os);
            log.info("OS #{} entregue com sucesso", os.getNumero());
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Cancela OS (qualquer status exceto ENTREGUE).
     *
     * @param id ID da OS
     * @param dto dados do cancelamento (motivo obrigatório)
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se não puder ser cancelada
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void cancelar(UUID id, CancelarOrdemServicoDTO dto) {
        log.info("Cancelando OS ID: {}, motivo: {}", id, dto.motivo());

        OrdemServico os = repository.findById(id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.cancelar(dto.motivo());
            repository.save(os);
            log.info("OS #{} cancelada com sucesso", os.getNumero());
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    // ===== DASHBOARD & STATISTICS =====

    /**
     * Conta OS agrupadas por status (para dashboard).
     *
     * @return mapa com status e quantidade
     */
    @Cacheable(value = "osCountByStatus")
    public Map<StatusOS, Long> contarPorStatus() {
        log.debug("Contando OS por status");

        List<Object[]> results = repository.countByStatus();

        return results.stream()
            .collect(Collectors.toMap(
                row -> (StatusOS) row[0],
                row -> (Long) row[1]
            ));
    }

    /**
     * Calcula faturamento total no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return valor total faturado
     */
    public BigDecimal calcularFaturamento(LocalDateTime dataInicio, LocalDateTime dataFim) {
        log.debug("Calculando faturamento entre {} e {}", dataInicio, dataFim);
        return repository.calcularFaturamento(dataInicio, dataFim);
    }

    /**
     * Calcula ticket médio no período.
     *
     * @param dataInicio data inicial
     * @param dataFim data final
     * @return ticket médio
     */
    public BigDecimal calcularTicketMedio(LocalDateTime dataInicio, LocalDateTime dataFim) {
        log.debug("Calculando ticket médio entre {} e {}", dataInicio, dataFim);
        return repository.calcularTicketMedio(dataInicio, dataFim);
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Monta OrdemServicoResponseDTO com dados relacionados (Veiculo, Cliente, Usuario).
     *
     * @param os entidade OrdemServico
     * @return DTO completo
     */
    private OrdemServicoResponseDTO montarResponse(OrdemServico os) {
        // Busca veículo
        Veiculo veiculo = veiculoRepository.findById(os.getVeiculoId())
            .orElseThrow(() -> new VeiculoNotFoundException(os.getVeiculoId()));

        // Busca mecânico
        Usuario mecanico = usuarioRepository.findById(os.getUsuarioId())
            .orElseThrow(() -> new UsuarioNotFoundException(os.getUsuarioId()));

        return montarResponse(os, veiculo, mecanico);
    }

    /**
     * Monta OrdemServicoResponseDTO com entidades já carregadas.
     *
     * @param os entidade OrdemServico
     * @param veiculo entidade Veiculo
     * @param mecanico entidade Usuario (mecânico)
     * @return DTO completo
     */
    private OrdemServicoResponseDTO montarResponse(OrdemServico os, Veiculo veiculo, Usuario mecanico) {
        // Busca cliente do veículo
        Cliente cliente = clienteRepository.findById(veiculo.getClienteId())
            .orElseThrow(() -> new ClienteNotFoundException(veiculo.getClienteId()));

        // Mapeia básico
        OrdemServicoResponseDTO response = mapper.toResponse(os);

        // Preenche dados relacionados
        VeiculoResumoDTO veiculoResumo = mapper.toVeiculoResumo(
            veiculo.getId(),
            veiculo.getPlacaFormatada(),
            veiculo.getMarca(),
            veiculo.getModelo(),
            veiculo.getAno(),
            veiculo.getCor()
        );

        ClienteResumoDTO clienteResumo = mapper.toClienteResumo(
            cliente.getId(),
            cliente.getNome(),
            cliente.getCpfCnpj(),
            cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
            cliente.getEmail()
        );

        UsuarioResumoDTO mecanicoResumo = mapper.toUsuarioResumo(
            mecanico.getId(),
            mecanico.getNome(),
            mecanico.getEmail(),
            mecanico.getPerfil().name()
        );

        // Retorna novo DTO com dados relacionados
        return new OrdemServicoResponseDTO(
            response.id(),
            response.numero(),
            response.status(),
            veiculoResumo,
            clienteResumo,
            mecanicoResumo,
            response.dataAbertura(),
            response.dataPrevisao(),
            response.dataFinalizacao(),
            response.dataEntrega(),
            response.problemasRelatados(),
            response.diagnostico(),
            response.observacoes(),
            response.valorMaoObra(),
            response.valorPecas(),
            response.valorTotal(),
            response.descontoPercentual(),
            response.descontoValor(),
            response.valorFinal(),
            response.aprovadoPeloCliente(),
            response.itens(),
            response.createdAt(),
            response.updatedAt()
        );
    }
}
