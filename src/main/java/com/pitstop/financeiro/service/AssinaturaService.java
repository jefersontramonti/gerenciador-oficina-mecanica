package com.pitstop.financeiro.service;

import com.pitstop.cliente.domain.Cliente;
import com.pitstop.cliente.repository.ClienteRepository;
import com.pitstop.financeiro.domain.*;
import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.repository.*;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciamento de assinaturas e cobranças recorrentes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final PlanoAssinaturaRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final FaturaAssinaturaRepository faturaRepository;
    private final ClienteRepository clienteRepository;

    // ========== PLANOS DE ASSINATURA ==========

    /**
     * Lista planos da oficina.
     */
    @Transactional(readOnly = true)
    public List<PlanoAssinaturaDTO> listarPlanos() {
        UUID oficinaId = TenantContext.getTenantId();
        return planoRepository.findByOficinaId(oficinaId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Lista planos ativos para seleção.
     */
    @Transactional(readOnly = true)
    public List<PlanoAssinaturaDTO> listarPlanosAtivos() {
        UUID oficinaId = TenantContext.getTenantId();
        return planoRepository.findAtivosByOficinaId(oficinaId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Busca plano por ID.
     */
    @Transactional(readOnly = true)
    public PlanoAssinaturaDTO buscarPlano(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        PlanoAssinatura plano = planoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));
        return toDTO(plano);
    }

    /**
     * Cria novo plano.
     */
    @Transactional
    public PlanoAssinaturaDTO criarPlano(PlanoAssinaturaDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();

        // Verificar nome duplicado
        if (planoRepository.existsByOficinaIdAndNome(oficinaId, dto.getNome(), null)) {
            throw new IllegalArgumentException("Já existe um plano com este nome");
        }

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        PlanoAssinatura plano = PlanoAssinatura.builder()
            .oficina(oficina)
            .nome(dto.getNome())
            .descricao(dto.getDescricao())
            .valor(dto.getValor())
            .periodicidade(dto.getPeriodicidade() != null ? dto.getPeriodicidade() : PeriodicidadeAssinatura.MENSAL)
            .servicosIncluidos(dto.getServicosIncluidos())
            .limiteOsMes(dto.getLimiteOsMes())
            .descontoPecas(dto.getDescontoPecas())
            .descontoMaoObra(dto.getDescontoMaoObra())
            .ativo(true)
            .build();

        plano = planoRepository.save(plano);
        log.info("Plano criado: {} - {}", plano.getId(), plano.getNome());

        return toDTO(plano);
    }

    /**
     * Atualiza plano existente.
     */
    @Transactional
    public PlanoAssinaturaDTO atualizarPlano(UUID id, PlanoAssinaturaDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();

        PlanoAssinatura plano = planoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));

        // Verificar nome duplicado
        if (planoRepository.existsByOficinaIdAndNome(oficinaId, dto.getNome(), id)) {
            throw new IllegalArgumentException("Já existe um plano com este nome");
        }

        plano.setNome(dto.getNome());
        plano.setDescricao(dto.getDescricao());
        plano.setValor(dto.getValor());
        plano.setPeriodicidade(dto.getPeriodicidade());
        plano.setServicosIncluidos(dto.getServicosIncluidos());
        plano.setLimiteOsMes(dto.getLimiteOsMes());
        plano.setDescontoPecas(dto.getDescontoPecas());
        plano.setDescontoMaoObra(dto.getDescontoMaoObra());

        if (dto.getAtivo() != null) {
            plano.setAtivo(dto.getAtivo());
        }

        plano = planoRepository.save(plano);
        log.info("Plano atualizado: {} - {}", plano.getId(), plano.getNome());

        return toDTO(plano);
    }

    /**
     * Desativa um plano.
     */
    @Transactional
    public void desativarPlano(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        PlanoAssinatura plano = planoRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));

        plano.setAtivo(false);
        planoRepository.save(plano);
        log.info("Plano desativado: {}", id);
    }

    // ========== ASSINATURAS ==========

    /**
     * Lista assinaturas da oficina.
     */
    @Transactional(readOnly = true)
    public Page<AssinaturaDTO> listarAssinaturas(StatusAssinatura status, UUID planoId, String busca, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return assinaturaRepository.findByFiltros(oficinaId, status, planoId, busca, pageable)
            .map(this::toDTO);
    }

    /**
     * Busca assinatura por ID.
     */
    @Transactional(readOnly = true)
    public AssinaturaDTO buscarAssinatura(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        Assinatura assinatura = assinaturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + id));
        return toDTO(assinatura);
    }

    /**
     * Cria nova assinatura.
     */
    @Transactional
    public AssinaturaDTO criarAssinatura(AssinaturaDTO.CreateAssinaturaDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();

        // Verificar se cliente já tem assinatura ativa
        if (assinaturaRepository.existsAssinaturaAtivaByClienteId(dto.getClienteId())) {
            throw new IllegalArgumentException("Cliente já possui uma assinatura ativa");
        }

        // Buscar cliente
        Cliente cliente = clienteRepository.findByOficinaIdAndId(oficinaId, dto.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + dto.getClienteId()));

        // Buscar plano
        PlanoAssinatura plano = planoRepository.findByIdAndOficinaId(dto.getPlanoId(), oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + dto.getPlanoId()));

        if (!plano.getAtivo()) {
            throw new IllegalArgumentException("Plano selecionado não está ativo");
        }

        Oficina oficina = new Oficina();
        oficina.setId(oficinaId);

        // Calcular primeiro vencimento
        Integer diaVencimento = dto.getDiaVencimento() != null ? dto.getDiaVencimento() : 10;
        LocalDate dataInicio = dto.getDataInicio();
        LocalDate primeiroVencimento = calcularProximoVencimento(dataInicio, diaVencimento, plano.getPeriodicidade());

        Assinatura assinatura = Assinatura.builder()
            .oficina(oficina)
            .cliente(cliente)
            .plano(plano)
            .status(StatusAssinatura.ATIVA)
            .dataInicio(dataInicio)
            .dataFim(dto.getDataFim())
            .dataProximoVencimento(primeiroVencimento)
            .valorAtual(dto.getValorCustomizado() != null ? dto.getValorCustomizado() : plano.getValor())
            .diaVencimento(diaVencimento)
            .toleranciaDias(5)
            .osUtilizadasMes(0)
            .mesReferencia(LocalDate.now().withDayOfMonth(1))
            .build();

        assinatura = assinaturaRepository.save(assinatura);
        log.info("Assinatura criada: {} - Cliente: {}", assinatura.getId(), cliente.getNome());

        // Gerar primeira fatura
        gerarFatura(assinatura);

        return toDTO(assinatura);
    }

    /**
     * Pausa uma assinatura.
     */
    @Transactional
    public AssinaturaDTO pausarAssinatura(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Assinatura assinatura = assinaturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + id));

        assinatura.pausar();
        assinatura = assinaturaRepository.save(assinatura);
        log.info("Assinatura pausada: {}", id);

        return toDTO(assinatura);
    }

    /**
     * Reativa uma assinatura.
     */
    @Transactional
    public AssinaturaDTO reativarAssinatura(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        Assinatura assinatura = assinaturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + id));

        assinatura.reativar();
        assinatura = assinaturaRepository.save(assinatura);
        log.info("Assinatura reativada: {}", id);

        return toDTO(assinatura);
    }

    /**
     * Cancela uma assinatura.
     */
    @Transactional
    public AssinaturaDTO cancelarAssinatura(UUID id, AssinaturaDTO.CancelarAssinaturaDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();

        Assinatura assinatura = assinaturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Assinatura não encontrada: " + id));

        assinatura.cancelar(dto.getMotivo());
        assinatura = assinaturaRepository.save(assinatura);
        log.info("Assinatura cancelada: {} - Motivo: {}", id, dto.getMotivo());

        return toDTO(assinatura);
    }

    // ========== FATURAS ==========

    /**
     * Lista faturas da oficina.
     */
    @Transactional(readOnly = true)
    public Page<FaturaAssinaturaDTO> listarFaturas(StatusFaturaAssinatura status, UUID assinaturaId, Pageable pageable) {
        UUID oficinaId = TenantContext.getTenantId();
        return faturaRepository.findByFiltros(oficinaId, status, assinaturaId, null, pageable)
            .map(this::toDTO);
    }

    /**
     * Busca fatura por ID.
     */
    @Transactional(readOnly = true)
    public FaturaAssinaturaDTO buscarFatura(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();
        FaturaAssinatura fatura = faturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + id));
        return toDTO(fatura);
    }

    /**
     * Lista faturas de uma assinatura.
     */
    @Transactional(readOnly = true)
    public List<FaturaAssinaturaDTO> listarFaturasAssinatura(UUID assinaturaId) {
        return faturaRepository.findByAssinaturaId(assinaturaId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Registra pagamento manual de fatura.
     */
    @Transactional
    public FaturaAssinaturaDTO registrarPagamento(UUID id, FaturaAssinaturaDTO.RegistrarPagamentoDTO dto) {
        UUID oficinaId = TenantContext.getTenantId();

        FaturaAssinatura fatura = faturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + id));

        if (!fatura.podeSerPaga()) {
            throw new IllegalStateException("Fatura não pode ser paga (status atual: " + fatura.getStatus() + ")");
        }

        fatura.registrarPagamento(null, "MANUAL");
        if (dto != null && dto.getObservacao() != null) {
            fatura.setObservacoes(dto.getObservacao());
        }

        fatura = faturaRepository.save(fatura);

        // Reativar assinatura se estava inadimplente
        Assinatura assinatura = fatura.getAssinatura();
        if (assinatura.isInadimplente()) {
            assinatura.reativar();
            assinaturaRepository.save(assinatura);
        }

        log.info("Pagamento registrado manualmente: Fatura {} - Assinatura {}", fatura.getNumeroFatura(), assinatura.getId());

        return toDTO(fatura);
    }

    /**
     * Cancela uma fatura.
     */
    @Transactional
    public FaturaAssinaturaDTO cancelarFatura(UUID id, String observacao) {
        UUID oficinaId = TenantContext.getTenantId();

        FaturaAssinatura fatura = faturaRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + id));

        if (fatura.getStatus() == StatusFaturaAssinatura.PAGA) {
            throw new IllegalStateException("Fatura já paga não pode ser cancelada");
        }

        fatura.cancelar(observacao);
        fatura = faturaRepository.save(fatura);
        log.info("Fatura cancelada: {}", fatura.getNumeroFatura());

        return toDTO(fatura);
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Gera fatura para uma assinatura.
     */
    @Transactional
    public FaturaAssinatura gerarFatura(Assinatura assinatura) {
        LocalDate mesReferencia = assinatura.getDataProximoVencimento().withDayOfMonth(1);

        // Verificar se já existe fatura para este mês
        if (faturaRepository.existsByAssinaturaIdAndMesReferencia(assinatura.getId(), mesReferencia)) {
            log.warn("Fatura já existe para assinatura {} no mês {}", assinatura.getId(), mesReferencia);
            return null;
        }

        Long numero = faturaRepository.getNextNumeroFatura();
        String numeroFatura = String.format("FAT-%d", numero);

        FaturaAssinatura fatura = FaturaAssinatura.builder()
            .oficina(assinatura.getOficina())
            .assinatura(assinatura)
            .numeroFatura(numeroFatura)
            .mesReferencia(mesReferencia)
            .valor(assinatura.getValorAtual())
            .status(StatusFaturaAssinatura.PENDENTE)
            .dataVencimento(assinatura.getDataProximoVencimento())
            .descricao(String.format("Assinatura %s - %s", assinatura.getPlano().getNome(),
                mesReferencia.format(DateTimeFormatter.ofPattern("MM/yyyy"))))
            .build();

        fatura = faturaRepository.save(fatura);
        log.info("Fatura gerada: {} para assinatura {}", numeroFatura, assinatura.getId());

        return fatura;
    }

    /**
     * Calcula próxima data de vencimento.
     */
    private LocalDate calcularProximoVencimento(LocalDate dataBase, int diaVencimento, PeriodicidadeAssinatura periodicidade) {
        LocalDate proximoVencimento = dataBase.withDayOfMonth(Math.min(diaVencimento, dataBase.lengthOfMonth()));

        if (!proximoVencimento.isAfter(dataBase)) {
            proximoVencimento = proximoVencimento.plusDays(periodicidade.getDiasIntervalo());
        }

        return proximoVencimento;
    }

    // ========== CONVERSÃO PARA DTO ==========

    private PlanoAssinaturaDTO toDTO(PlanoAssinatura plano) {
        long totalAssinantes = planoRepository.countAssinaturasAtivasByPlanoId(plano.getId());

        return PlanoAssinaturaDTO.builder()
            .id(plano.getId())
            .nome(plano.getNome())
            .descricao(plano.getDescricao())
            .valor(plano.getValor())
            .periodicidade(plano.getPeriodicidade())
            .servicosIncluidos(plano.getServicosIncluidos())
            .limiteOsMes(plano.getLimiteOsMes())
            .descontoPecas(plano.getDescontoPecas())
            .descontoMaoObra(plano.getDescontoMaoObra())
            .ativo(plano.getAtivo())
            .totalAssinantes((int) totalAssinantes)
            .receitaMensalEstimada(plano.getValor().multiply(BigDecimal.valueOf(totalAssinantes)))
            .createdAt(plano.getCreatedAt())
            .updatedAt(plano.getUpdatedAt())
            .build();
    }

    private AssinaturaDTO toDTO(Assinatura assinatura) {
        long diasAteVencimento = ChronoUnit.DAYS.between(LocalDate.now(), assinatura.getDataProximoVencimento());

        return AssinaturaDTO.builder()
            .id(assinatura.getId())
            .clienteId(assinatura.getCliente().getId())
            .clienteNome(assinatura.getCliente().getNome())
            .clienteTelefone(assinatura.getCliente().getTelefone())
            .planoId(assinatura.getPlano().getId())
            .planoNome(assinatura.getPlano().getNome())
            .planoPeriodicidade(assinatura.getPlano().getPeriodicidade())
            .status(assinatura.getStatus())
            .dataInicio(assinatura.getDataInicio())
            .dataFim(assinatura.getDataFim())
            .dataProximoVencimento(assinatura.getDataProximoVencimento())
            .valorAtual(assinatura.getValorAtual())
            .diaVencimento(assinatura.getDiaVencimento())
            .toleranciaDias(assinatura.getToleranciaDias())
            .osUtilizadasMes(assinatura.getOsUtilizadasMes())
            .limiteOsMes(assinatura.getPlano().getLimiteOsMes())
            .mesReferencia(assinatura.getMesReferencia())
            .motivoCancelamento(assinatura.getMotivoCancelamento())
            .dataCancelamento(assinatura.getDataCancelamento())
            .gatewaySubscriptionId(assinatura.getGatewaySubscriptionId())
            .proximaCobranca(assinatura.getValorAtual())
            .dentroDoLimite(!assinatura.atingiuLimiteOs())
            .diasAteVencimento((int) diasAteVencimento)
            .createdAt(assinatura.getCreatedAt())
            .updatedAt(assinatura.getUpdatedAt())
            .build();
    }

    private FaturaAssinaturaDTO toDTO(FaturaAssinatura fatura) {
        long diasAteVencimento = ChronoUnit.DAYS.between(LocalDate.now(), fatura.getDataVencimento());

        return FaturaAssinaturaDTO.builder()
            .id(fatura.getId())
            .assinaturaId(fatura.getAssinatura().getId())
            .clienteNome(fatura.getAssinatura().getCliente().getNome())
            .planoNome(fatura.getAssinatura().getPlano().getNome())
            .numeroFatura(fatura.getNumeroFatura())
            .mesReferencia(fatura.getMesReferencia())
            .valor(fatura.getValor())
            .status(fatura.getStatus())
            .dataVencimento(fatura.getDataVencimento())
            .dataPagamento(fatura.getDataPagamento())
            .gatewayPaymentId(fatura.getGatewayPaymentId())
            .gatewayPaymentStatus(fatura.getGatewayPaymentStatus())
            .linkPagamento(fatura.getLinkPagamento())
            .descricao(fatura.getDescricao())
            .observacoes(fatura.getObservacoes())
            .diasAteVencimento((int) diasAteVencimento)
            .vencida(fatura.isVencida())
            .createdAt(fatura.getCreatedAt())
            .updatedAt(fatura.getUpdatedAt())
            .build();
    }
}
