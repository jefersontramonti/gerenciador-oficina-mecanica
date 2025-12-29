package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.*;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.dto.AcaoMassaInadimplenciaRequest.AcaoInadimplencia;
import com.pitstop.saas.dto.InadimplenciaDashboardDTO.InadimplenciaFaixaDTO;
import com.pitstop.saas.dto.OficinaInadimplenteDTO.FaturaVencidaResumoDTO;
import com.pitstop.saas.repository.AcordoRepository;
import com.pitstop.saas.repository.FaturaRepository;
import com.pitstop.saas.repository.ParcelaAcordoRepository;
import com.pitstop.shared.audit.service.AuditService;
import com.pitstop.shared.exception.BusinessException;
import com.pitstop.shared.exception.ResourceNotFoundException;
import com.pitstop.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing defaults and payment agreements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InadimplenciaService {

    private final FaturaRepository faturaRepository;
    private final AcordoRepository acordoRepository;
    private final ParcelaAcordoRepository parcelaRepository;
    private final OficinaRepository oficinaRepository;
    private final AuditService auditService;

    // =====================================
    // DASHBOARD
    // =====================================

    /**
     * Get dashboard overview of defaults.
     */
    @Transactional(readOnly = true)
    public InadimplenciaDashboardDTO getDashboard() {
        LocalDate hoje = LocalDate.now();

        // Get all overdue invoices
        List<Fatura> faturasVencidas = faturaRepository.findPendentesVencidas(hoje);

        // Calculate totals
        BigDecimal valorTotal = faturasVencidas.stream()
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<UUID> oficinasComAtraso = faturasVencidas.stream()
            .map(f -> f.getOficina().getId())
            .collect(Collectors.toSet());

        // Group by days overdue
        Map<String, InadimplenciaFaixaDTO> porFaixa = calcularFaixasAtraso(faturasVencidas, hoje);

        // Top 10 defaulting workshops
        List<OficinaInadimplenteDTO> top10 = getTop10Inadimplentes(faturasVencidas, hoje);

        // Recovery metrics
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        BigDecimal valorRecuperado = acordoRepository.sumValorRecuperado(inicioMes);
        long acordosAtivos = acordoRepository.countByStatus(StatusAcordo.ATIVO);
        BigDecimal valorEmAcordos = acordoRepository.sumValorAcordadoAtivo();

        return new InadimplenciaDashboardDTO(
            valorTotal,
            oficinasComAtraso.size(),
            faturasVencidas.size(),
            porFaixa,
            top10,
            valorRecuperado != null ? valorRecuperado : BigDecimal.ZERO,
            (int) acordosAtivos,
            valorEmAcordos != null ? valorEmAcordos : BigDecimal.ZERO
        );
    }

    private Map<String, InadimplenciaFaixaDTO> calcularFaixasAtraso(List<Fatura> faturas, LocalDate hoje) {
        Map<String, List<Fatura>> agrupadas = faturas.stream()
            .collect(Collectors.groupingBy(f -> {
                long dias = ChronoUnit.DAYS.between(f.getDataVencimento(), hoje);
                if (dias <= 30) return "1-30";
                if (dias <= 60) return "31-60";
                if (dias <= 90) return "61-90";
                return "90+";
            }));

        Map<String, InadimplenciaFaixaDTO> result = new LinkedHashMap<>();
        for (String faixa : Arrays.asList("1-30", "31-60", "61-90", "90+")) {
            List<Fatura> lista = agrupadas.getOrDefault(faixa, List.of());
            Set<UUID> oficinas = lista.stream()
                .map(f -> f.getOficina().getId())
                .collect(Collectors.toSet());
            BigDecimal valor = lista.stream()
                .map(Fatura::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.put(faixa, new InadimplenciaFaixaDTO(
                faixa + " dias",
                lista.size(),
                oficinas.size(),
                valor
            ));
        }
        return result;
    }

    private List<OficinaInadimplenteDTO> getTop10Inadimplentes(List<Fatura> faturas, LocalDate hoje) {
        // Group by workshop
        Map<UUID, List<Fatura>> porOficina = faturas.stream()
            .collect(Collectors.groupingBy(f -> f.getOficina().getId()));

        return porOficina.entrySet().stream()
            .map(entry -> {
                UUID oficinaId = entry.getKey();
                List<Fatura> faturasDaOficina = entry.getValue();
                Oficina oficina = faturasDaOficina.get(0).getOficina();

                BigDecimal valorTotal = faturasDaOficina.stream()
                    .map(Fatura::getValorTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                LocalDate maisAntiga = faturasDaOficina.stream()
                    .map(Fatura::getDataVencimento)
                    .min(LocalDate::compareTo)
                    .orElse(hoje);

                int diasAtrasoMaior = (int) ChronoUnit.DAYS.between(maisAntiga, hoje);

                // Check for active agreement
                boolean temAcordo = acordoRepository.hasActiveAgreement(oficinaId);
                BigDecimal valorAcordo = BigDecimal.ZERO;
                if (temAcordo) {
                    valorAcordo = acordoRepository.findByOficinaIdAndStatus(oficinaId, StatusAcordo.ATIVO)
                        .map(Acordo::getValorAcordado)
                        .orElse(BigDecimal.ZERO);
                }

                List<FaturaVencidaResumoDTO> faturasList = faturasDaOficina.stream()
                    .sorted(Comparator.comparing(Fatura::getDataVencimento))
                    .limit(5)
                    .map(f -> new FaturaVencidaResumoDTO(
                        f.getId(),
                        f.getNumero(),
                        f.getValorTotal(),
                        f.getDataVencimento(),
                        (int) ChronoUnit.DAYS.between(f.getDataVencimento(), hoje),
                        f.getMesReferenciaFormatado()
                    ))
                    .toList();

                return new OficinaInadimplenteDTO(
                    oficinaId,
                    oficina.getNomeFantasia(),
                    oficina.getCnpjCpf(),
                    oficina.getContato() != null ? oficina.getContato().getEmail() : null,
                    oficina.getContato() != null ? oficina.getContato().getTelefoneCelular() : null,
                    oficina.getPlano() != null ? oficina.getPlano().name() : "N/A",
                    faturasDaOficina.size(),
                    valorTotal,
                    maisAntiga,
                    diasAtrasoMaior,
                    0, // TODO: Track notifications sent
                    null, // TODO: Track last notification
                    temAcordo,
                    valorAcordo,
                    oficina.getUltimoAcesso(),
                    !temAcordo, // Can notify if no agreement
                    oficina.getStatus() == StatusOficina.ATIVA && !temAcordo, // Can suspend if active
                    oficina.getStatus() != StatusOficina.CANCELADA && !temAcordo, // Can cancel
                    faturasList
                );
            })
            .sorted((a, b) -> b.valorTotalDevido().compareTo(a.valorTotalDevido()))
            .limit(10)
            .toList();
    }

    // =====================================
    // LIST DEFAULTERS
    // =====================================

    /**
     * Get paginated list of defaulting workshops.
     */
    @Transactional(readOnly = true)
    public Page<OficinaInadimplenteDTO> listarInadimplentes(Pageable pageable) {
        LocalDate hoje = LocalDate.now();

        // Get workshops with overdue invoices
        List<Fatura> faturasVencidas = faturaRepository.findPendentesVencidas(hoje);

        Map<UUID, List<Fatura>> porOficina = faturasVencidas.stream()
            .collect(Collectors.groupingBy(f -> f.getOficina().getId()));

        List<OficinaInadimplenteDTO> inadimplentes = porOficina.entrySet().stream()
            .map(entry -> mapToOficinaInadimplenteDTO(entry.getKey(), entry.getValue(), hoje))
            .sorted((a, b) -> b.valorTotalDevido().compareTo(a.valorTotalDevido()))
            .toList();

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), inadimplentes.size());

        if (start > inadimplentes.size()) {
            return Page.empty(pageable);
        }

        return new org.springframework.data.domain.PageImpl<>(
            inadimplentes.subList(start, end),
            pageable,
            inadimplentes.size()
        );
    }

    private OficinaInadimplenteDTO mapToOficinaInadimplenteDTO(UUID oficinaId, List<Fatura> faturas, LocalDate hoje) {
        Oficina oficina = faturas.get(0).getOficina();

        BigDecimal valorTotal = faturas.stream()
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate maisAntiga = faturas.stream()
            .map(Fatura::getDataVencimento)
            .min(LocalDate::compareTo)
            .orElse(hoje);

        int diasAtrasoMaior = (int) ChronoUnit.DAYS.between(maisAntiga, hoje);

        boolean temAcordo = acordoRepository.hasActiveAgreement(oficinaId);
        BigDecimal valorAcordo = BigDecimal.ZERO;
        if (temAcordo) {
            valorAcordo = acordoRepository.findByOficinaIdAndStatus(oficinaId, StatusAcordo.ATIVO)
                .map(Acordo::getValorAcordado)
                .orElse(BigDecimal.ZERO);
        }

        List<FaturaVencidaResumoDTO> faturasList = faturas.stream()
            .sorted(Comparator.comparing(Fatura::getDataVencimento))
            .map(f -> new FaturaVencidaResumoDTO(
                f.getId(),
                f.getNumero(),
                f.getValorTotal(),
                f.getDataVencimento(),
                (int) ChronoUnit.DAYS.between(f.getDataVencimento(), hoje),
                f.getMesReferenciaFormatado()
            ))
            .toList();

        return new OficinaInadimplenteDTO(
            oficinaId,
            oficina.getNomeFantasia(),
            oficina.getCnpjCpf(),
            oficina.getContato() != null ? oficina.getContato().getEmail() : null,
            oficina.getContato() != null ? oficina.getContato().getTelefoneCelular() : null,
            oficina.getPlano() != null ? oficina.getPlano().name() : "N/A",
            faturas.size(),
            valorTotal,
            maisAntiga,
            diasAtrasoMaior,
            0,
            null,
            temAcordo,
            valorAcordo,
            oficina.getUltimoAcesso(),
            !temAcordo,
            oficina.getStatus() == StatusOficina.ATIVA && !temAcordo,
            oficina.getStatus() != StatusOficina.CANCELADA && !temAcordo,
            faturasList
        );
    }

    // =====================================
    // MASS ACTIONS
    // =====================================

    /**
     * Execute mass action on workshops.
     */
    @Transactional
    public AcaoMassaResultDTO executarAcaoMassa(AcaoMassaInadimplenciaRequest request) {
        log.info("Executando ação em massa: {} para {} oficinas",
            request.getAcao(), request.getOficinaIds().size());

        List<AcaoMassaResultDTO.ResultadoIndividualDTO> resultados = new ArrayList<>();
        int sucesso = 0;
        int falha = 0;

        for (UUID oficinaId : request.getOficinaIds()) {
            try {
                Oficina oficina = oficinaRepository.findById(oficinaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada: " + oficinaId));

                switch (request.getAcao()) {
                    case NOTIFICAR, NOTIFICAR_URGENTE -> {
                        // TODO: Implement notification sending
                        log.info("Notificação enviada para oficina: {}", oficina.getNomeFantasia());
                    }
                    case SUSPENDER -> {
                        if (oficina.getStatus() != StatusOficina.ATIVA) {
                            throw new BusinessException("Oficina não está ativa");
                        }
                        oficina.setStatus(StatusOficina.SUSPENSA);
                        oficinaRepository.save(oficina);
                        auditService.log("INADIMPLENCIA", "SUSPENDER", oficinaId,
                            "Oficina suspensa por inadimplência");
                    }
                    case REATIVAR -> {
                        if (oficina.getStatus() != StatusOficina.SUSPENSA) {
                            throw new BusinessException("Oficina não está suspensa");
                        }
                        oficina.setStatus(StatusOficina.ATIVA);
                        oficinaRepository.save(oficina);
                        auditService.log("INADIMPLENCIA", "REATIVAR", oficinaId,
                            "Oficina reativada");
                    }
                    case CANCELAR -> {
                        if (oficina.getStatus() == StatusOficina.CANCELADA) {
                            throw new BusinessException("Oficina já está cancelada");
                        }
                        oficina.setStatus(StatusOficina.CANCELADA);
                        oficinaRepository.save(oficina);
                        auditService.log("INADIMPLENCIA", "CANCELAR", oficinaId,
                            "Oficina cancelada por inadimplência");
                    }
                }

                resultados.add(new AcaoMassaResultDTO.ResultadoIndividualDTO(
                    oficinaId, oficina.getNomeFantasia(), true, "Sucesso"
                ));
                sucesso++;

            } catch (Exception e) {
                log.error("Erro ao executar ação para oficina {}: {}", oficinaId, e.getMessage());
                resultados.add(new AcaoMassaResultDTO.ResultadoIndividualDTO(
                    oficinaId, null, false, e.getMessage()
                ));
                falha++;
            }
        }

        return new AcaoMassaResultDTO(
            request.getOficinaIds().size(),
            sucesso,
            falha,
            resultados
        );
    }

    // =====================================
    // AGREEMENTS
    // =====================================

    /**
     * Create a payment agreement.
     */
    @Transactional
    public AcordoDTO criarAcordo(UUID oficinaId, CriarAcordoRequest request) {
        log.info("Criando acordo para oficina: {}", oficinaId);

        Oficina oficina = oficinaRepository.findById(oficinaId)
            .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada: " + oficinaId));

        // Check if already has active agreement
        if (acordoRepository.hasActiveAgreement(oficinaId)) {
            throw new BusinessException("Oficina já possui um acordo ativo");
        }

        // Validate invoices belong to the workshop
        List<Fatura> faturas = request.getFaturaIds().stream()
            .map(id -> faturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + id)))
            .peek(f -> {
                if (!f.getOficina().getId().equals(oficinaId)) {
                    throw new BusinessException("Fatura não pertence à oficina");
                }
                if (f.isPaga()) {
                    throw new BusinessException("Fatura já está paga: " + f.getNumero());
                }
            })
            .toList();

        // Calculate original value
        BigDecimal valorOriginal = faturas.stream()
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate discount
        BigDecimal percentualDesconto = request.getPercentualDesconto() != null
            ? request.getPercentualDesconto() : BigDecimal.ZERO;
        BigDecimal valorDesconto = valorOriginal.multiply(percentualDesconto)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Validate agreed value
        if (request.getValorTotalAcordado().compareTo(valorOriginal.subtract(valorDesconto)) > 0) {
            throw new BusinessException("Valor acordado não pode ser maior que o valor original menos desconto");
        }

        // Generate number
        Long seq = acordoRepository.getNextNumeroSequence();
        String numero = Acordo.gerarNumero(seq.intValue());

        // Create agreement
        Acordo acordo = Acordo.builder()
            .numero(numero)
            .oficina(oficina)
            .valorOriginal(valorOriginal)
            .percentualDesconto(percentualDesconto)
            .valorDesconto(valorDesconto)
            .valorAcordado(request.getValorTotalAcordado())
            .numeroParcelas(request.getNumeroParcelas())
            .primeiroVencimento(request.getPrimeiroVencimento())
            .observacoes(request.getObservacoes())
            .criadoPor(SecurityUtils.getCurrentUserId())
            .build();

        // Add invoices
        faturas.forEach(acordo::addFatura);

        // Generate installments
        acordo.gerarParcelas();

        acordo = acordoRepository.save(acordo);

        auditService.log("ACORDO", "CRIAR", acordo.getId(),
            String.format("Acordo criado: %s - Valor: %s em %d parcelas",
                numero, request.getValorTotalAcordado(), request.getNumeroParcelas()));

        log.info("Acordo criado: {} para oficina {}", numero, oficina.getNomeFantasia());

        return mapToAcordoDTO(acordo);
    }

    /**
     * Get agreement by ID.
     */
    @Transactional(readOnly = true)
    public AcordoDTO getAcordo(UUID id) {
        Acordo acordo = acordoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Acordo não encontrado: " + id));
        return mapToAcordoDTO(acordo);
    }

    /**
     * List agreements with filters.
     */
    @Transactional(readOnly = true)
    public Page<AcordoDTO> listarAcordos(UUID oficinaId, StatusAcordo status, Pageable pageable) {
        return acordoRepository.findWithFilters(oficinaId, status, pageable)
            .map(this::mapToAcordoDTO);
    }

    /**
     * Cancel an agreement.
     */
    @Transactional
    public AcordoDTO cancelarAcordo(UUID id, String motivo) {
        Acordo acordo = acordoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Acordo não encontrado: " + id));

        acordo.cancelar(motivo);
        acordo = acordoRepository.save(acordo);

        auditService.log("ACORDO", "CANCELAR", id,
            "Acordo cancelado: " + motivo);

        return mapToAcordoDTO(acordo);
    }

    /**
     * Register installment payment.
     */
    @Transactional
    public AcordoDTO registrarPagamentoParcela(UUID acordoId, UUID parcelaId,
                                                String metodoPagamento, String transacaoId) {
        Acordo acordo = acordoRepository.findById(acordoId)
            .orElseThrow(() -> new ResourceNotFoundException("Acordo não encontrado: " + acordoId));

        ParcelaAcordo parcela = acordo.getParcelas().stream()
            .filter(p -> p.getId().equals(parcelaId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Parcela não encontrada: " + parcelaId));

        if (!parcela.getStatus().isPagavel()) {
            throw new BusinessException("Parcela não pode ser paga no status atual: " + parcela.getStatus());
        }

        parcela.marcarComoPago(metodoPagamento, transacaoId);
        acordo.registrarPagamentoParcela(parcela);

        acordo = acordoRepository.save(acordo);

        auditService.log("ACORDO", "PAGAR_PARCELA", acordoId,
            String.format("Parcela %d/%d paga - Valor: %s",
                parcela.getNumeroParcela(), acordo.getNumeroParcelas(), parcela.getValor()));

        return mapToAcordoDTO(acordo);
    }

    private AcordoDTO mapToAcordoDTO(Acordo acordo) {
        List<AcordoDTO.FaturaAcordoDTO> faturas = acordo.getFaturas().stream()
            .map(f -> new AcordoDTO.FaturaAcordoDTO(
                f.getId(),
                f.getNumero(),
                f.getValorTotal(),
                f.getDataVencimento(),
                f.getMesReferenciaFormatado()
            ))
            .toList();

        List<AcordoDTO.ParcelaAcordoDTO> parcelas = acordo.getParcelas().stream()
            .map(p -> new AcordoDTO.ParcelaAcordoDTO(
                p.getId(),
                p.getNumeroParcela(),
                p.getValor(),
                p.getDataVencimento(),
                p.getDataPagamento(),
                AcordoDTO.ParcelaAcordoDTO.StatusParcela.valueOf(p.getStatus().name()),
                p.getStatus().getLabel()
            ))
            .toList();

        return new AcordoDTO(
            acordo.getId(),
            acordo.getNumero(),
            acordo.getOficina().getId(),
            acordo.getOficina().getNomeFantasia(),
            AcordoDTO.StatusAcordo.valueOf(acordo.getStatus().name()),
            acordo.getStatus().getLabel(),
            acordo.getValorOriginal(),
            acordo.getValorDesconto(),
            acordo.getValorAcordado(),
            acordo.getPercentualDesconto(),
            acordo.getNumeroParcelas(),
            acordo.getParcelasPagas(),
            acordo.getParcelasPendentes(),
            acordo.getValorPago(),
            acordo.getValorRestante(),
            acordo.getDataAcordo(),
            acordo.getPrimeiroVencimento(),
            acordo.getProximoVencimento(),
            acordo.getObservacoes(),
            acordo.getCriadoPor(),
            null, // TODO: Get creator name
            acordo.getCreatedAt(),
            acordo.getUpdatedAt(),
            faturas,
            parcelas
        );
    }
}
