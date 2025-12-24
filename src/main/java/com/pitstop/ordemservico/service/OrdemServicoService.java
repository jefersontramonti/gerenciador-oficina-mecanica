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
import com.pitstop.ordemservico.event.OrdemServicoFinalizadaEvent;
import com.pitstop.ordemservico.event.OrdemServicoCanceladaEvent;
import com.pitstop.notificacao.service.NotificacaoEventPublisher;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
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
    private final OficinaRepository oficinaRepository;
    private final OrdemServicoMapper mapper;
    private final ItemOSMapper itemMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.pitstop.financeiro.service.PagamentoService pagamentoService;
    private final NotificacaoEventPublisher notificacaoEventPublisher;

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

        UUID oficinaId = TenantContext.getTenantId();

        // Valida veículo existe
        Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, dto.veiculoId())
            .orElseThrow(() -> new VeiculoNotFoundException(dto.veiculoId()));

        // Valida mecânico existe
        Usuario mecanico = usuarioRepository.findByOficinaIdAndId(oficinaId, dto.usuarioId())
            .orElseThrow(() -> new UsuarioNotFoundException(dto.usuarioId()));

        // Mapeia DTO para entidade
        OrdemServico os = mapper.toEntity(dto);

        // Gera número sequencial (sequence global do PostgreSQL)
        Long numero = repository.getNextNumero();
        os.setNumero(numero);

        // Adiciona itens (se houver)
        if (dto.itens() != null && !dto.itens().isEmpty()) {
            dto.itens().forEach(itemDto -> {
                ItemOS item = itemMapper.toEntity(itemDto);
                // Calcula o valorTotal do item antes de adicionar
                item.setValorTotal(item.calcularValorTotal());
                os.adicionarItem(item);
            });
        }

        // Gera token de aprovação
        os.gerarTokenAprovacao();

        // Primeira salvada - persiste a OS e os itens
        OrdemServico saved = repository.save(os);
        repository.flush();

        // Agora que os itens estão persistidos, recalcula os valores da OS
        saved.recalcularValores();

        // Segunda salvada - persiste os valores calculados
        saved = repository.save(saved);
        repository.flush();

        log.info("OS #{} criada com sucesso - ID: {}, valorPecas={}, valorTotal={}, valorFinal={}",
            saved.getNumero(), saved.getId(), saved.getValorPecas(), saved.getValorTotal(), saved.getValorFinal());

        // Busca cliente para notificacao
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, veiculo.getClienteId())
            .orElse(null);

        // Busca nome da oficina para notificacao
        String nomeOficina = oficinaRepository.findById(oficinaId)
            .map(Oficina::getNomeFantasia)
            .orElse("PitStop");

        // Publica evento de notificacao (assincrono)
        if (cliente != null) {
            notificacaoEventPublisher.publicarOSCriada(
                saved.getId(),
                saved.getNumero(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
                veiculo.getPlacaFormatada(),
                veiculo.getMarca() + " " + veiculo.getModelo(),
                saved.getValorFinal(),
                nomeOficina,
                saved.getTokenAprovacao()
            );
        }

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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndNumero(oficinaId, numero)
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

        UUID oficinaId = TenantContext.getTenantId();
        String statusStr = status != null ? status.name() : null;

        Page<OrdemServico> page = repository.findByFiltros(oficinaId, statusStr, veiculoId, usuarioId, dataInicio, dataFim, pageable);

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

        UUID oficinaId = TenantContext.getTenantId();
        return repository.findHistoricoVeiculo(oficinaId, veiculoId, pageable)
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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.aprovar(Boolean.TRUE.equals(aprovadoPeloCliente));
            repository.save(os);
            log.info("OS #{} aprovada com sucesso", os.getNumero());

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoAprovada(os, oficinaId);
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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.iniciar();
            repository.save(os);
            log.info("OS #{} iniciada com sucesso", os.getNumero());

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoEmAndamento(os, oficinaId);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Finaliza OS (EM_ANDAMENTO/AGUARDANDO_PECA → FINALIZADO).
     * Neste ponto ocorre a baixa automática de peças do estoque via evento.
     *
     * @param id ID da OS
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     * @throws com.pitstop.estoque.exception.EstoqueInsuficienteException se estoque insuficiente (rollback completo)
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void finalizar(UUID id) {
        log.info("Finalizando OS ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            os.finalizar();

            // Dispara evento para baixa automática de estoque (síncrono - mesma transação)
            OrdemServicoFinalizadaEvent event = new OrdemServicoFinalizadaEvent(
                this,
                os.getId(),
                os.getNumero(),
                os.getUsuarioId(),
                os.getItens()
            );
            applicationEventPublisher.publishEvent(event);
            log.debug("Evento OrdemServicoFinalizadaEvent publicado para OS #{}", os.getNumero());

            repository.save(os);
            log.info("OS #{} finalizada com sucesso", os.getNumero());

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoFinalizada(os, oficinaId);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
        // Nota: EstoqueInsuficienteException será propagada causando rollback automático
    }

    /**
     * Entrega veículo ao cliente (FINALIZADO → ENTREGUE).
     * Valida se OS está quitada antes de entregar.
     *
     * @param id ID da OS
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     * @throws OrdemServicoNaoPagaException se OS não estiver quitada
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void entregar(UUID id) {
        log.info("Entregando veículo da OS ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            // Valida se OS está paga (integração com módulo Financeiro)
            boolean quitada = pagamentoService.isOrdemServicoQuitada(id);
            if (!quitada) {
                throw new OrdemServicoNaoPagaException(os.getNumero());
            }

            os.entregar();
            repository.save(os);
            log.info("OS #{} entregue com sucesso", os.getNumero());

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoEntregue(os, oficinaId);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Cancela OS (qualquer status exceto ENTREGUE).
     * Se OS estava FINALIZADA, dispara evento para estorno de estoque.
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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            // Captura status anterior ANTES de cancelar
            StatusOS statusAnterior = os.getStatus();

            os.cancelar(dto.motivo());

            // Dispara evento para estorno de estoque (se OS estava finalizada)
            OrdemServicoCanceladaEvent event = new OrdemServicoCanceladaEvent(
                this,
                os.getId(),
                os.getNumero(),
                os.getUsuarioId(),
                statusAnterior,
                dto.motivo()
            );
            applicationEventPublisher.publishEvent(event);
            log.debug("Evento OrdemServicoCanceladaEvent publicado para OS #{} (status anterior: {})",
                    os.getNumero(), statusAnterior);

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

        UUID oficinaId = TenantContext.getTenantId();
        List<Object[]> results = repository.countByStatus(oficinaId);

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
        UUID oficinaId = TenantContext.getTenantId();
        return repository.calcularFaturamento(oficinaId, dataInicio, dataFim);
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
        UUID oficinaId = TenantContext.getTenantId();
        return repository.calcularTicketMedio(oficinaId, dataInicio, dataFim);
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Monta OrdemServicoResponseDTO com dados relacionados (Veiculo, Cliente, Usuario).
     *
     * @param os entidade OrdemServico
     * @return DTO completo
     */
    private OrdemServicoResponseDTO montarResponse(OrdemServico os) {
        UUID oficinaId = TenantContext.getTenantId();

        // Busca veículo
        Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, os.getVeiculoId())
            .orElseThrow(() -> new VeiculoNotFoundException(os.getVeiculoId()));

        // Busca mecânico
        Usuario mecanico = usuarioRepository.findByOficinaIdAndId(oficinaId, os.getUsuarioId())
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
        UUID oficinaId = TenantContext.getTenantId();

        // Busca cliente do veículo (incluindo inativos para permitir visualizar OS antigas)
        Cliente cliente = clienteRepository.findByOficinaIdAndIdIncludingInactive(oficinaId, veiculo.getClienteId())
            .orElseThrow(() -> new ClienteNotFoundException(veiculo.getClienteId()));

        // Força inicialização dos itens (lazy loading)
        os.getItens().size();

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

    // ===== MÉTODOS AUXILIARES DE NOTIFICAÇÃO =====

    /**
     * Publica notificação de OS aprovada.
     */
    private void publicarNotificacaoAprovada(OrdemServico os, UUID oficinaId) {
        try {
            Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, os.getVeiculoId())
                .orElse(null);
            if (veiculo == null) return;

            Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, veiculo.getClienteId())
                .orElse(null);
            if (cliente == null) return;

            String nomeOficina = oficinaRepository.findById(oficinaId)
                .map(Oficina::getNomeFantasia)
                .orElse("PitStop");

            notificacaoEventPublisher.publicarOSAprovada(
                os.getId(),
                os.getNumero(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
                nomeOficina
            );
        } catch (Exception e) {
            log.warn("Falha ao publicar notificacao de OS aprovada: {}", e.getMessage());
        }
    }

    /**
     * Publica notificação de OS em andamento.
     */
    private void publicarNotificacaoEmAndamento(OrdemServico os, UUID oficinaId) {
        try {
            Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, os.getVeiculoId())
                .orElse(null);
            if (veiculo == null) return;

            Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, veiculo.getClienteId())
                .orElse(null);
            if (cliente == null) return;

            Usuario mecanico = usuarioRepository.findByOficinaIdAndId(oficinaId, os.getUsuarioId())
                .orElse(null);

            String nomeOficina = oficinaRepository.findById(oficinaId)
                .map(Oficina::getNomeFantasia)
                .orElse("PitStop");

            LocalDateTime previsao = os.getDataPrevisao() != null
                ? os.getDataPrevisao().atStartOfDay()
                : null;

            notificacaoEventPublisher.publicarOSEmAndamento(
                os.getId(),
                os.getNumero(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
                veiculo.getPlacaFormatada(),
                veiculo.getMarca() + " " + veiculo.getModelo(),
                mecanico != null ? mecanico.getNome() : "Equipe",
                previsao,
                nomeOficina
            );
        } catch (Exception e) {
            log.warn("Falha ao publicar notificacao de OS em andamento: {}", e.getMessage());
        }
    }

    /**
     * Publica notificação de OS finalizada.
     */
    private void publicarNotificacaoFinalizada(OrdemServico os, UUID oficinaId) {
        try {
            Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, os.getVeiculoId())
                .orElse(null);
            if (veiculo == null) return;

            Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, veiculo.getClienteId())
                .orElse(null);
            if (cliente == null) return;

            String nomeOficina = oficinaRepository.findById(oficinaId)
                .map(Oficina::getNomeFantasia)
                .orElse("PitStop");

            // Monta descrição dos serviços realizados
            String servicosRealizados = os.getItens().stream()
                .map(item -> item.getDescricao())
                .collect(java.util.stream.Collectors.joining(", "));

            notificacaoEventPublisher.publicarOSFinalizada(
                os.getId(),
                os.getNumero(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
                veiculo.getPlacaFormatada(),
                veiculo.getMarca() + " " + veiculo.getModelo(),
                os.getValorFinal(),
                servicosRealizados,
                nomeOficina
            );
        } catch (Exception e) {
            log.warn("Falha ao publicar notificacao de OS finalizada: {}", e.getMessage());
        }
    }

    /**
     * Publica notificação de OS entregue.
     */
    private void publicarNotificacaoEntregue(OrdemServico os, UUID oficinaId) {
        try {
            Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, os.getVeiculoId())
                .orElse(null);
            if (veiculo == null) return;

            Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, veiculo.getClienteId())
                .orElse(null);
            if (cliente == null) return;

            String nomeOficina = oficinaRepository.findById(oficinaId)
                .map(Oficina::getNomeFantasia)
                .orElse("PitStop");

            notificacaoEventPublisher.publicarOSEntregue(
                os.getId(),
                os.getNumero(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getCelular() != null ? cliente.getCelular() : cliente.getTelefone(),
                veiculo.getPlacaFormatada(),
                nomeOficina
            );
        } catch (Exception e) {
            log.warn("Falha ao publicar notificacao de OS entregue: {}", e.getMessage());
        }
    }
}
