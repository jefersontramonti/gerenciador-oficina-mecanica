package com.pitstop.ordemservico.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.exception.ClienteNotFoundException;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.ordemservico.domain.HistoricoStatusOS;
import com.pitstop.ordemservico.domain.ItemOS;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.domain.OrigemPeca;
import com.pitstop.ordemservico.domain.StatusOS;
import com.pitstop.ordemservico.domain.TipoCobrancaMaoObra;
import com.pitstop.ordemservico.domain.TipoItem;
import com.pitstop.ordemservico.dto.*;
import com.pitstop.estoque.domain.Peca;
import com.pitstop.estoque.exception.PecaNotFoundException;
import com.pitstop.estoque.exception.EstoqueInsuficienteException;
import com.pitstop.estoque.repository.PecaRepository;
import com.pitstop.ordemservico.exception.*;
import com.pitstop.ordemservico.mapper.ItemOSMapper;
import com.pitstop.ordemservico.mapper.OrdemServicoMapper;
import com.pitstop.ordemservico.repository.HistoricoStatusOSRepository;
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
import com.pitstop.saas.service.PlanoLimiteService;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.shared.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final HistoricoStatusOSRepository historicoStatusRepository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final OficinaRepository oficinaRepository;
    private final PecaRepository pecaRepository;
    private final OrdemServicoMapper mapper;
    private final ItemOSMapper itemMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.pitstop.financeiro.service.PagamentoService pagamentoService;
    private final NotificacaoEventPublisher notificacaoEventPublisher;
    private final AsyncPdfMailService asyncPdfMailService;
    private final PlanoLimiteService planoLimiteService;

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

        // Validar limite de OS/mês do plano
        planoLimiteService.validarLimiteOsMes(oficinaId);

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

        // Configura tipo de cobrança de mão de obra
        os.setTipoCobrancaMaoObra(dto.tipoCobrancaMaoObra());

        if (dto.tipoCobrancaMaoObra() == TipoCobrancaMaoObra.VALOR_FIXO) {
            // VALOR_FIXO: valorMaoObra já vem definido no DTO
            os.setValorMaoObra(dto.valorMaoObra() != null ? dto.valorMaoObra() : BigDecimal.ZERO);
            log.debug("OS #{} configurada com VALOR_FIXO: R$ {}", numero, os.getValorMaoObra());
        } else {
            // POR_HORA: configura estimativas e captura valor/hora da oficina
            os.setTempoEstimadoHoras(dto.tempoEstimadoHoras());
            os.setLimiteHorasAprovado(dto.limiteHorasAprovado());
            os.setValorMaoObra(BigDecimal.ZERO); // Será calculado na finalização

            // Captura valor/hora atual da oficina (snapshot)
            Oficina oficina = oficinaRepository.findById(oficinaId)
                .orElseThrow(() -> new IllegalStateException("Oficina não encontrada: " + oficinaId));
            os.setValorHoraSnapshot(oficina.getValorHora());

            log.debug("OS #{} configurada com POR_HORA: estimativa {}h, limite {}h, R$ {}/h",
                numero, dto.tempoEstimadoHoras(), dto.limiteHorasAprovado(), oficina.getValorHora());
        }

        // Adiciona itens (se houver)
        if (dto.itens() != null && !dto.itens().isEmpty()) {
            for (CreateItemOSDTO itemDto : dto.itens()) {
                ItemOS item = itemMapper.toEntity(itemDto);

                // Processa origem da peça (se for item do tipo PECA)
                if (item.getTipo() == TipoItem.PECA && itemDto.origemPeca() != null) {
                    item.setOrigemPeca(itemDto.origemPeca());
                    processarOrigemPeca(item, oficinaId);
                }

                // Calcula o valorTotal do item antes de adicionar
                item.setValorTotal(item.calcularValorTotal());
                os.adicionarItem(item);
            }
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

        // Registra histórico de criação
        registrarHistoricoStatus(saved, null, StatusOS.ORCAMENTO, "Ordem de serviço criada");

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

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndNumero(oficinaId, numero)
            .orElseThrow(() -> new OrdemServicoNotFoundException(numero));

        return montarResponse(os);
    }

    /**
     * Lista OS com filtros opcionais e paginação.
     * OTIMIZADO: Usa query nativa com JOINs para evitar N+1 queries.
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

        UUID oficinaId = TenantContext.getTenantId();
        String statusStr = status != null ? status.name() : null;

        // Usa query otimizada que traz todos os dados em uma única consulta
        Page<Object[]> page = repository.findByFiltrosOptimized(
            oficinaId, statusStr, veiculoId, usuarioId, dataInicio, dataFim, pageable
        );

        return page.map(this::mapFromNativeQuery);
    }

    /**
     * Mapeia resultado da query nativa para DTO.
     * Índices conforme SELECT da query findByFiltrosOptimized:
     * 0:id, 1:numero, 2:status, 3:data_abertura, 4:data_previsao, 5:data_finalizacao,
     * 6:data_entrega, 7:valor_mao_obra, 8:valor_pecas, 9:valor_total, 10:desconto_percentual,
     * 11:valor_final, 12:problemas_relatados, 13:diagnostico, 14:observacoes,
     * 15:veiculo_id, 16:usuario_id, 17:veiculo_placa, 18:veiculo_marca, 19:veiculo_modelo,
     * 20:veiculo_ano, 21:veiculo_cor, 22:cliente_id, 23:cliente_nome, 24:cliente_telefone,
     * 25:cliente_email, 26:mecanico_nome
     */
    private OrdemServicoResponseDTO mapFromNativeQuery(Object[] row) {
        UUID osId = (UUID) row[0];
        Long numero = row[1] != null ? ((Number) row[1]).longValue() : null;
        StatusOS osStatus = row[2] != null ? StatusOS.valueOf((String) row[2]) : null;

        // Datas
        LocalDateTime dataAbertura = row[3] != null ? ((java.sql.Timestamp) row[3]).toLocalDateTime() : null;
        LocalDate dataPrevisao = row[4] != null ? ((java.sql.Date) row[4]).toLocalDate() : null;
        LocalDateTime dataFinalizacao = row[5] != null ? ((java.sql.Timestamp) row[5]).toLocalDateTime() : null;
        LocalDateTime dataEntrega = row[6] != null ? ((java.sql.Timestamp) row[6]).toLocalDateTime() : null;

        // Valores
        BigDecimal valorMaoObra = row[7] != null ? (BigDecimal) row[7] : BigDecimal.ZERO;
        BigDecimal valorPecas = row[8] != null ? (BigDecimal) row[8] : BigDecimal.ZERO;
        BigDecimal valorTotal = row[9] != null ? (BigDecimal) row[9] : BigDecimal.ZERO;
        BigDecimal descontoPercentual = row[10] != null ? (BigDecimal) row[10] : BigDecimal.ZERO;
        BigDecimal valorFinal = row[11] != null ? (BigDecimal) row[11] : BigDecimal.ZERO;

        // Textos
        String problemasRelatados = (String) row[12];
        String diagnostico = (String) row[13];
        String observacoes = (String) row[14];

        // IDs
        UUID veiculoIdVal = (UUID) row[15];
        UUID usuarioIdVal = (UUID) row[16];

        // Veiculo
        VeiculoResumoDTO veiculoDto = new VeiculoResumoDTO(
            veiculoIdVal,
            (String) row[17], // placa
            (String) row[18], // marca
            (String) row[19], // modelo
            row[20] != null ? ((Number) row[20]).intValue() : null, // ano
            (String) row[21]  // cor
        );

        // Cliente
        UUID clienteId = (UUID) row[22];
        ClienteResumoDTO clienteDto = clienteId != null ? new ClienteResumoDTO(
            clienteId,
            (String) row[23], // nome
            null, // cpfCnpj - não retornamos na listagem
            (String) row[24], // telefone
            (String) row[25]  // email
        ) : null;

        // Mecânico
        UsuarioResumoDTO mecanicoDto = usuarioIdVal != null ? new UsuarioResumoDTO(
            usuarioIdVal,
            (String) row[26], // nome
            null, // email
            null  // perfil
        ) : null;

        // Calcula desconto em valor
        BigDecimal descontoValor = valorTotal.multiply(descontoPercentual)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        return new OrdemServicoResponseDTO(
            osId,
            numero,
            osStatus,
            veiculoDto,
            clienteDto,
            mecanicoDto,
            dataAbertura,
            dataPrevisao,
            dataFinalizacao,
            dataEntrega,
            problemasRelatados,
            diagnostico,
            observacoes,
            null, // tipoCobrancaMaoObra - não retornamos na listagem básica
            valorMaoObra,
            null, // tempoEstimadoHoras - não retornamos na listagem
            null, // limiteHorasAprovado - não retornamos na listagem
            null, // horasTrabalhadas - não retornamos na listagem
            null, // valorHoraSnapshot - não retornamos na listagem
            valorPecas,
            valorTotal,
            descontoPercentual,
            descontoValor,
            valorFinal,
            null, // aprovadoPeloCliente - não retornamos na listagem básica
            null, // itens - não carregamos na listagem
            null, // createdAt
            null  // updatedAt
        );
    }

    /**
     * Busca histórico de OS de um veículo.
     *
     * @param veiculoId ID do veículo
     * @param pageable configuração de paginação
     * @return página de OS do veículo
     */
    public Page<OrdemServicoResponseDTO> buscarHistoricoVeiculo(UUID veiculoId, Pageable pageable) {

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
            StatusOS statusAnterior = os.getStatus();
            os.aprovar(Boolean.TRUE.equals(aprovadoPeloCliente));
            repository.save(os);
            log.info("OS #{} aprovada com sucesso", os.getNumero());

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.APROVADO,
                Boolean.TRUE.equals(aprovadoPeloCliente) ? "Orçamento aprovado pelo cliente" : "Orçamento aprovado");

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
            StatusOS statusAnterior = os.getStatus();
            os.iniciar();
            repository.save(os);
            log.info("OS #{} iniciada com sucesso", os.getNumero());

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.EM_ANDAMENTO, "Execução do serviço iniciada");

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoEmAndamento(os, oficinaId);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Coloca OS em aguardando peça.
     *
     * <p>Transição: EM_ANDAMENTO → AGUARDANDO_PECA</p>
     *
     * @param id ID da OS
     * @param descricaoPeca descrição da peça aguardada
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void aguardarPeca(UUID id, String descricaoPeca) {
        log.info("Colocando OS ID: {} em aguardando peça: {}", id, descricaoPeca);

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            StatusOS statusAnterior = os.getStatus();
            os.aguardarPeca(descricaoPeca);
            repository.save(os);
            log.info("OS #{} em aguardando peça", os.getNumero());

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.AGUARDANDO_PECA,
                "Aguardando peça: " + descricaoPeca);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Retoma execução de OS que estava aguardando peça.
     *
     * <p>Transição: AGUARDANDO_PECA → EM_ANDAMENTO</p>
     *
     * @param id ID da OS
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public void retomarExecucao(UUID id) {
        log.info("Retomando execução da OS ID: {}", id);

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        try {
            StatusOS statusAnterior = os.getStatus();
            os.retomarExecucao();
            repository.save(os);
            log.info("OS #{} execução retomada", os.getNumero());

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.EM_ANDAMENTO, "Peça recebida - Execução retomada");

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoEmAndamento(os, oficinaId);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
    }

    /**
     * Finaliza OS com informações de horas trabalhadas (modelo POR_HORA).
     *
     * <p>Este método deve ser usado quando a OS utiliza cobrança POR_HORA,
     * pois permite informar as horas efetivamente trabalhadas.</p>
     *
     * @param id ID da OS
     * @param dto dados de finalização (horas trabalhadas, observações)
     * @return DTO de resposta com OS finalizada
     * @throws OrdemServicoNotFoundException se não encontrada
     * @throws TransicaoStatusInvalidaException se transição inválida
     * @throws LimiteHorasExcedidoException se horas trabalhadas excedem limite aprovado
     * @throws com.pitstop.estoque.exception.EstoqueInsuficienteException se estoque insuficiente
     */
    @Transactional
    @CacheEvict(value = {"ordemServico", "osCountByStatus"}, allEntries = true)
    public OrdemServicoResponseDTO finalizar(UUID id, FinalizarOSDTO dto) {
        log.info("Finalizando OS ID: {} com {} horas trabalhadas", id, dto.horasTrabalhadas());

        UUID oficinaId = TenantContext.getTenantId();
        OrdemServico os = repository.findByOficinaIdAndId(oficinaId, id)
            .orElseThrow(() -> new OrdemServicoNotFoundException(id));

        // Captura status antes da mudança
        StatusOS statusAnterior = os.getStatus();

        // Valida status
        if (statusAnterior != StatusOS.EM_ANDAMENTO && statusAnterior != StatusOS.AGUARDANDO_PECA) {
            throw new TransicaoStatusInvalidaException(
                "Apenas OS em andamento ou aguardando peça pode ser finalizada. Status atual: " + statusAnterior);
        }

        // Se cobrança por hora, valida e calcula mão de obra
        if (os.getTipoCobrancaMaoObra() == TipoCobrancaMaoObra.POR_HORA) {
            BigDecimal horas = dto.horasTrabalhadas();

            // Valida limite aprovado pelo cliente
            if (horas.compareTo(os.getLimiteHorasAprovado()) > 0) {
                throw new LimiteHorasExcedidoException(horas, os.getLimiteHorasAprovado());
            }

            os.setHorasTrabalhadas(horas);
            // valorMaoObra será recalculado em recalcularValores()

            log.info("OS #{}: {} horas trabalhadas (limite: {}h) × R$ {}/h",
                os.getNumero(), horas, os.getLimiteHorasAprovado(), os.getValorHoraSnapshot());
        }

        // Adiciona observações finais (se informadas)
        if (dto.observacoesFinais() != null && !dto.observacoesFinais().isBlank()) {
            String obsAtual = os.getObservacoes() != null ? os.getObservacoes() + "\n\n" : "";
            os.setObservacoes(obsAtual + "[Finalização] " + dto.observacoesFinais());
        }

        // Recalcula valores (agora com horas trabalhadas definidas)
        os.recalcularValores();

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

            OrdemServico saved = repository.save(os);

            log.info("OS #{} finalizada com sucesso - Mão de obra: R$ {}, Total: R$ {}",
                saved.getNumero(), saved.getValorMaoObra(), saved.getValorFinal());

            // Registra histórico
            String obsHistorico = dto.horasTrabalhadas() != null
                ? String.format("Serviço finalizado - %.1f horas trabalhadas", dto.horasTrabalhadas())
                : "Serviço finalizado";
            registrarHistoricoStatus(saved, statusAnterior, StatusOS.FINALIZADO, obsHistorico);

            // Publica evento de notificacao (assincrono)
            publicarNotificacaoFinalizada(saved, oficinaId);

            return montarResponse(saved);
        } catch (IllegalStateException e) {
            throw new TransicaoStatusInvalidaException(e.getMessage());
        }
        // Nota: EstoqueInsuficienteException será propagada causando rollback automático
    }

    /**
     * Finaliza OS sem informar horas (modelo VALOR_FIXO).
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
            StatusOS statusAnterior = os.getStatus();
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

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.FINALIZADO, "Serviço finalizado");

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

            StatusOS statusAnterior = os.getStatus();
            os.entregar();
            repository.save(os);
            log.info("OS #{} entregue com sucesso", os.getNumero());

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.ENTREGUE, "Veículo entregue ao cliente");

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

            // Registra histórico
            registrarHistoricoStatus(os, statusAnterior, StatusOS.CANCELADO,
                "Cancelamento: " + dto.motivo());
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
            response.tipoCobrancaMaoObra(),
            response.valorMaoObra(),
            response.tempoEstimadoHoras(),
            response.limiteHorasAprovado(),
            response.horasTrabalhadas(),
            response.valorHoraSnapshot(),
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
     * Publica notificação de OS finalizada e envia PDF por email.
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

            // Envia PDF por email de forma assíncrona (não bloqueia a resposta)
            asyncPdfMailService.enviarPdfFinalizacaoAsync(
                os.getId(),
                os.getNumero(),
                cliente,
                veiculo,
                os.getValorFinal(),
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

    // ===== MÉTODOS AUXILIARES DE PROCESSAMENTO DE ITENS =====

    /**
     * Processa origem da peça, validando estoque quando aplicável.
     *
     * <p>Para peças do ESTOQUE, valida se a peça existe e se há quantidade suficiente.
     * Para peças AVULSA e CLIENTE, apenas registra log informativo.</p>
     *
     * @param item item da OS sendo processado
     * @param oficinaId ID da oficina atual
     * @throws PecaNotFoundException se peça do estoque não existir
     * @throws EstoqueInsuficienteException se não houver quantidade suficiente
     */
    private void processarOrigemPeca(ItemOS item, UUID oficinaId) {
        if (item.getOrigemPeca() == OrigemPeca.ESTOQUE) {
            // Peça do estoque - valida existência e disponibilidade
            if (item.getPecaId() == null) {
                throw new IllegalStateException("Peça do estoque requer pecaId");
            }

            Peca peca = pecaRepository.findByOficinaIdAndId(oficinaId, item.getPecaId())
                .orElseThrow(() -> new PecaNotFoundException(item.getPecaId()));

            // Verifica disponibilidade (baixa real ocorre na finalização)
            if (peca.getQuantidadeAtual() < item.getQuantidade()) {
                throw new EstoqueInsuficienteException(
                    peca.getId(),
                    peca.getCodigo(),
                    peca.getDescricao(),
                    item.getQuantidade(),
                    peca.getQuantidadeAtual()
                );
            }

            log.debug("Peça do ESTOQUE: {} ({} unidades) - Estoque disponível: {}",
                peca.getDescricao(), item.getQuantidade(), peca.getQuantidadeAtual());

        } else if (item.getOrigemPeca() == OrigemPeca.AVULSA) {
            // Peça comprada externamente - não afeta estoque
            log.debug("Peça AVULSA adicionada: {} ({} unidades) - R$ {}",
                item.getDescricao(), item.getQuantidade(), item.getValorUnitario());

        } else if (item.getOrigemPeca() == OrigemPeca.CLIENTE) {
            // Peça fornecida pelo cliente - não afeta estoque
            log.debug("Peça do CLIENTE adicionada: {} ({} unidades)",
                item.getDescricao(), item.getQuantidade());
        }
    }

    // ===== HISTÓRICO DE STATUS =====

    /**
     * Busca o histórico de mudanças de status de uma OS.
     *
     * @param ordemServicoId ID da OS
     * @return lista de histórico de status em ordem cronológica
     */
    public List<HistoricoStatusOSDTO> buscarHistoricoStatus(UUID ordemServicoId) {
        UUID oficinaId = TenantContext.getTenantId();

        // Verifica se a OS existe
        repository.findByOficinaIdAndId(oficinaId, ordemServicoId)
            .orElseThrow(() -> new OrdemServicoNotFoundException(ordemServicoId));

        return historicoStatusRepository.findByOrdemServicoId(oficinaId, ordemServicoId)
            .stream()
            .map(HistoricoStatusOSDTO::fromEntity)
            .toList();
    }

    /**
     * Registra uma mudança de status no histórico.
     *
     * @param os Ordem de Serviço
     * @param statusAnterior Status antes da mudança (null se for criação)
     * @param statusNovo Novo status
     * @param observacao Observação opcional (ex: motivo de cancelamento)
     */
    private void registrarHistoricoStatus(OrdemServico os, StatusOS statusAnterior, StatusOS statusNovo, String observacao) {
        try {
            // Obtém usuário atual do contexto de segurança
            UUID usuarioId = null;
            String usuarioNome = "Sistema";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                usuarioId = userDetails.getUserId();
                usuarioNome = userDetails.getUsuario().getNome();
            }

            HistoricoStatusOS historico = HistoricoStatusOS.builder()
                .oficina(os.getOficina())
                .ordemServico(os)
                .statusAnterior(statusAnterior)
                .statusNovo(statusNovo)
                .usuarioId(usuarioId)
                .usuarioNome(usuarioNome)
                .observacao(observacao)
                .build();

            historicoStatusRepository.save(historico);

            log.debug("Histórico de status registrado para OS #{}: {} → {}",
                os.getNumero(),
                statusAnterior != null ? statusAnterior : "CRIAÇÃO",
                statusNovo);

        } catch (Exception e) {
            // Log do erro mas não interrompe o fluxo principal
            log.warn("Falha ao registrar histórico de status para OS #{}: {}", os.getNumero(), e.getMessage());
        }
    }
}
