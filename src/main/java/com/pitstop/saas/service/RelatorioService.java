package com.pitstop.saas.service;

import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.saas.domain.Fatura;
import com.pitstop.saas.domain.StatusFatura;
import com.pitstop.saas.dto.*;
import com.pitstop.saas.repository.FaturaRepository;
import com.pitstop.saas.repository.PlanoRepository;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para geração de relatórios do SaaS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final OficinaRepository oficinaRepository;
    private final FaturaRepository faturaRepository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter MES_ANO_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    /**
     * Gera relatório financeiro completo.
     */
    @Transactional(readOnly = true)
    public RelatorioFinanceiroDTO gerarRelatorioFinanceiro(LocalDate dataInicio, LocalDate dataFim) {
        log.info("Gerando relatório financeiro de {} a {}", dataInicio, dataFim);

        // Buscar faturas do período
        List<Fatura> faturasPeriodo = faturaRepository.findByDataEmissaoBetween(dataInicio, dataFim);

        // Calcular receita total
        BigDecimal receitaTotal = faturasPeriodo.stream()
            .filter(f -> f.getStatus() == StatusFatura.PAGO)
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular MRR e ARR atuais
        BigDecimal mrrAtual = calcularMRRAtual();
        BigDecimal arrAtual = mrrAtual.multiply(BigDecimal.valueOf(12));

        // Período anterior para comparativo
        long diasPeriodo = ChronoUnit.DAYS.between(dataInicio, dataFim);
        LocalDate dataInicioAnterior = dataInicio.minusDays(diasPeriodo);
        LocalDate dataFimAnterior = dataInicio.minusDays(1);

        List<Fatura> faturasPeriodoAnterior = faturaRepository.findByDataEmissaoBetween(dataInicioAnterior, dataFimAnterior);
        BigDecimal receitaPeriodoAnterior = faturasPeriodoAnterior.stream()
            .filter(f -> f.getStatus() == StatusFatura.PAGO)
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variacaoPercentual = BigDecimal.ZERO;
        if (receitaPeriodoAnterior.compareTo(BigDecimal.ZERO) > 0) {
            variacaoPercentual = receitaTotal.subtract(receitaPeriodoAnterior)
                .divide(receitaPeriodoAnterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // Estatísticas de faturas
        long faturasPagas = faturasPeriodo.stream().filter(f -> f.getStatus() == StatusFatura.PAGO).count();
        long faturasPendentes = faturasPeriodo.stream().filter(f -> f.getStatus() == StatusFatura.PENDENTE).count();
        long faturasVencidas = faturasPeriodo.stream().filter(f -> f.getStatus() == StatusFatura.VENCIDO).count();
        long faturasCanceladas = faturasPeriodo.stream().filter(f -> f.getStatus() == StatusFatura.CANCELADO).count();

        BigDecimal valorFaturasPagas = faturasPeriodo.stream()
            .filter(f -> f.getStatus() == StatusFatura.PAGO)
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorFaturasPendentes = faturasPeriodo.stream()
            .filter(f -> f.getStatus() == StatusFatura.PENDENTE)
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorFaturasVencidas = faturasPeriodo.stream()
            .filter(f -> f.getStatus() == StatusFatura.VENCIDO)
            .map(Fatura::getValorTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Inadimplência
        int oficinasInadimplentes = (int) oficinaRepository.findAll().stream()
            .filter(o -> o.getStatus() == StatusOficina.ATIVA || o.getStatus() == StatusOficina.SUSPENSA)
            .filter(o -> temFaturasVencidas(o.getId()))
            .count();

        BigDecimal valorInadimplente = valorFaturasVencidas;
        BigDecimal taxaInadimplencia = BigDecimal.ZERO;
        if (receitaTotal.compareTo(BigDecimal.ZERO) > 0) {
            taxaInadimplencia = valorInadimplente
                .divide(receitaTotal.add(valorInadimplente), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // Ticket médio
        BigDecimal ticketMedio = BigDecimal.ZERO;
        if (faturasPagas > 0) {
            ticketMedio = valorFaturasPagas.divide(BigDecimal.valueOf(faturasPagas), 2, RoundingMode.HALF_UP);
        }

        // Receita por plano
        List<RelatorioFinanceiroDTO.ReceitaPorPlano> receitaPorPlano = calcularReceitaPorPlano(faturasPeriodo, receitaTotal);

        // Evolução mensal
        List<RelatorioFinanceiroDTO.EvolucaoMensal> evolucaoMensal = calcularEvolucaoMensalFinanceiro(dataInicio, dataFim);

        // Top oficinas por receita
        List<RelatorioFinanceiroDTO.OficinaReceita> topOficinas = calcularTopOficinasPorReceita(faturasPeriodo);

        // Calcular receita mensal média
        long meses = ChronoUnit.MONTHS.between(YearMonth.from(dataInicio), YearMonth.from(dataFim)) + 1;
        BigDecimal receitaMensal = meses > 0 ? receitaTotal.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new RelatorioFinanceiroDTO(
            dataInicio,
            dataFim,
            receitaTotal,
            receitaMensal,
            mrrAtual,
            arrAtual,
            ticketMedio,
            receitaPeriodoAnterior,
            variacaoPercentual,
            faturasPeriodo.size(),
            (int) faturasPagas,
            (int) faturasPendentes,
            (int) faturasVencidas,
            (int) faturasCanceladas,
            valorFaturasPagas,
            valorFaturasPendentes,
            valorFaturasVencidas,
            oficinasInadimplentes,
            valorInadimplente,
            taxaInadimplencia,
            receitaPorPlano,
            evolucaoMensal,
            topOficinas
        );
    }

    /**
     * Gera relatório operacional completo.
     */
    @Transactional(readOnly = true)
    public RelatorioOperacionalDTO gerarRelatorioOperacional(LocalDate dataInicio, LocalDate dataFim) {
        log.info("Gerando relatório operacional de {} a {}", dataInicio, dataFim);

        List<Oficina> todasOficinas = oficinaRepository.findAll();

        int totalOficinas = todasOficinas.size();
        int oficinasAtivas = (int) todasOficinas.stream().filter(o -> o.getStatus() == StatusOficina.ATIVA).count();
        int oficinasEmTrial = (int) todasOficinas.stream().filter(o -> o.getStatus() == StatusOficina.TRIAL).count();
        int oficinasSuspensas = (int) todasOficinas.stream().filter(o -> o.getStatus() == StatusOficina.SUSPENSA).count();
        int oficinasCanceladas = (int) todasOficinas.stream().filter(o -> o.getStatus() == StatusOficina.CANCELADA).count();

        // Contagem de usuários
        long totalUsuarios = usuarioRepository.count();
        long usuariosAtivos = usuarioRepository.countByAtivoTrue();

        // Métricas de uso (simuladas - em produção viriam de tabelas reais)
        Long totalOrdensServico = 0L;
        Long ordensServicoPeriodo = 0L;
        Long totalClientes = 0L;
        Long clientesPeriodo = 0L;
        Long totalVeiculos = 0L;
        Long veiculosPeriodo = 0L;

        // Médias
        double mediaOSPorOficina = oficinasAtivas > 0 ? (double) totalOrdensServico / oficinasAtivas : 0;
        double mediaClientesPorOficina = oficinasAtivas > 0 ? (double) totalClientes / oficinasAtivas : 0;
        double mediaUsuariosPorOficina = oficinasAtivas > 0 ? (double) totalUsuarios / oficinasAtivas : 0;

        // Distribuição por plano
        List<RelatorioOperacionalDTO.DistribuicaoPlano> distribuicaoPlanos = calcularDistribuicaoPlanos(todasOficinas);

        // Distribuição por status
        List<RelatorioOperacionalDTO.DistribuicaoStatus> distribuicaoStatus = Arrays.asList(
            new RelatorioOperacionalDTO.DistribuicaoStatus("ATIVA", oficinasAtivas, totalOficinas > 0 ? (double) oficinasAtivas / totalOficinas * 100 : 0),
            new RelatorioOperacionalDTO.DistribuicaoStatus("TRIAL", oficinasEmTrial, totalOficinas > 0 ? (double) oficinasEmTrial / totalOficinas * 100 : 0),
            new RelatorioOperacionalDTO.DistribuicaoStatus("SUSPENSA", oficinasSuspensas, totalOficinas > 0 ? (double) oficinasSuspensas / totalOficinas * 100 : 0),
            new RelatorioOperacionalDTO.DistribuicaoStatus("CANCELADA", oficinasCanceladas, totalOficinas > 0 ? (double) oficinasCanceladas / totalOficinas * 100 : 0)
        );

        // Oficinas mais ativas (placeholder - em produção viriam de métricas reais)
        List<RelatorioOperacionalDTO.OficinaAtividade> oficinaMaisAtivas = todasOficinas.stream()
            .filter(o -> o.getStatus() == StatusOficina.ATIVA)
            .limit(10)
            .map(o -> new RelatorioOperacionalDTO.OficinaAtividade(
                o.getId().toString(),
                o.getNomeFantasia(),
                o.getPlano() != null ? o.getPlano().getNome() : "Sem plano",
                0, 0, 0, 0
            ))
            .toList();

        List<RelatorioOperacionalDTO.OficinaAtividade> oficinaMenosAtivas = new ArrayList<>();

        // Evolução mensal
        List<RelatorioOperacionalDTO.EvolucaoOperacional> evolucaoMensal = calcularEvolucaoOperacional(dataInicio, dataFim);

        return new RelatorioOperacionalDTO(
            dataInicio,
            dataFim,
            totalOficinas,
            oficinasAtivas,
            oficinasEmTrial,
            oficinasSuspensas,
            oficinasCanceladas,
            (int) totalUsuarios,
            (int) usuariosAtivos,
            0, // loginsPeriodo - seria necessário tracking de logins
            0.0, // mediaLoginsPorOficina
            totalOrdensServico,
            ordensServicoPeriodo,
            totalClientes,
            clientesPeriodo,
            totalVeiculos,
            veiculosPeriodo,
            mediaOSPorOficina,
            mediaClientesPorOficina,
            mediaUsuariosPorOficina,
            distribuicaoPlanos,
            distribuicaoStatus,
            oficinaMaisAtivas,
            oficinaMenosAtivas,
            evolucaoMensal
        );
    }

    /**
     * Gera relatório de crescimento completo.
     */
    @Transactional(readOnly = true)
    public RelatorioCrescimentoDTO gerarRelatorioCrescimento(LocalDate dataInicio, LocalDate dataFim) {
        log.info("Gerando relatório de crescimento de {} a {}", dataInicio, dataFim);

        List<Oficina> todasOficinas = oficinaRepository.findAll();

        // Novas oficinas no período
        int novasOficinas = (int) todasOficinas.stream()
            .filter(o -> o.getCreatedAt() != null)
            .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(dataInicio))
            .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(dataFim))
            .count();

        // Cancelamentos no período
        int cancelamentos = (int) todasOficinas.stream()
            .filter(o -> o.getStatus() == StatusOficina.CANCELADA)
            .filter(o -> o.getUpdatedAt() != null)
            .filter(o -> !o.getUpdatedAt().toLocalDate().isBefore(dataInicio))
            .filter(o -> !o.getUpdatedAt().toLocalDate().isAfter(dataFim))
            .count();

        int crescimentoLiquido = novasOficinas - cancelamentos;

        // Taxa de crescimento
        int oficinasInicioPeriodo = (int) todasOficinas.stream()
            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isBefore(dataInicio))
            .count();

        BigDecimal taxaCrescimento = BigDecimal.ZERO;
        if (oficinasInicioPeriodo > 0) {
            taxaCrescimento = BigDecimal.valueOf(crescimentoLiquido)
                .divide(BigDecimal.valueOf(oficinasInicioPeriodo), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // Churn
        int oficinasAtivas = (int) todasOficinas.stream()
            .filter(o -> o.getStatus() == StatusOficina.ATIVA || o.getStatus() == StatusOficina.TRIAL)
            .count();

        BigDecimal churnRate = BigDecimal.ZERO;
        if (oficinasAtivas + cancelamentos > 0) {
            churnRate = BigDecimal.valueOf(cancelamentos)
                .divide(BigDecimal.valueOf(oficinasAtivas + cancelamentos), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // MRR
        BigDecimal mrrAtual = calcularMRRAtual();
        BigDecimal churnMRR = BigDecimal.ZERO; // Seria calculado com base nas faturas canceladas

        // Trial metrics
        int trialsIniciados = (int) todasOficinas.stream()
            .filter(o -> o.getCreatedAt() != null)
            .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(dataInicio))
            .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(dataFim))
            .count();

        int trialsConvertidos = (int) todasOficinas.stream()
            .filter(o -> o.getStatus() == StatusOficina.ATIVA)
            .filter(o -> o.getCreatedAt() != null)
            .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(dataInicio))
            .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(dataFim))
            .count();

        BigDecimal taxaConversaoTrial = BigDecimal.ZERO;
        if (trialsIniciados > 0) {
            taxaConversaoTrial = BigDecimal.valueOf(trialsConvertidos)
                .divide(BigDecimal.valueOf(trialsIniciados), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        // LTV e CAC (valores placeholder - em produção viriam de métricas reais)
        BigDecimal ltv = mrrAtual.multiply(BigDecimal.valueOf(24)); // Assume 24 meses de vida média
        BigDecimal cac = BigDecimal.valueOf(500); // Placeholder
        BigDecimal ltvCacRatio = cac.compareTo(BigDecimal.ZERO) > 0 ?
            ltv.divide(cac, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Evolução mensal
        List<RelatorioCrescimentoDTO.EvolucaoCrescimento> evolucaoMensal = calcularEvolucaoCrescimento(dataInicio, dataFim, todasOficinas);

        // Motivos de cancelamento (placeholder)
        List<RelatorioCrescimentoDTO.MotivoCancelamento> motivosCancelamento = Arrays.asList(
            new RelatorioCrescimentoDTO.MotivoCancelamento("Preço", cancelamentos / 3, 33.3),
            new RelatorioCrescimentoDTO.MotivoCancelamento("Funcionalidades", cancelamentos / 3, 33.3),
            new RelatorioCrescimentoDTO.MotivoCancelamento("Outro", cancelamentos / 3, 33.4)
        );

        // Fontes de aquisição (placeholder)
        List<RelatorioCrescimentoDTO.FonteAquisicao> fontesAquisicao = Arrays.asList(
            new RelatorioCrescimentoDTO.FonteAquisicao("Orgânico", novasOficinas / 2, 50.0, mrrAtual.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)),
            new RelatorioCrescimentoDTO.FonteAquisicao("Indicação", novasOficinas / 3, 33.3, mrrAtual.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP)),
            new RelatorioCrescimentoDTO.FonteAquisicao("Marketing", novasOficinas / 6, 16.7, mrrAtual.divide(BigDecimal.valueOf(6), 2, RoundingMode.HALF_UP))
        );

        return new RelatorioCrescimentoDTO(
            dataInicio,
            dataFim,
            novasOficinas,
            cancelamentos,
            crescimentoLiquido,
            taxaCrescimento,
            churnRate,
            churnMRR,
            cancelamentos,
            cac,
            ltv,
            ltvCacRatio,
            14, // diasMediaConversao placeholder
            trialsIniciados,
            trialsConvertidos,
            taxaConversaoTrial,
            14.0, // mediaDiasTrial placeholder
            BigDecimal.valueOf(85), // taxaRetencao30d placeholder
            BigDecimal.valueOf(75), // taxaRetencao90d placeholder
            BigDecimal.valueOf(60), // taxaRetencao12m placeholder
            mrrAtual.subtract(mrrAtual.multiply(BigDecimal.valueOf(0.05))), // mrrInicio aproximado
            mrrAtual,
            mrrAtual.multiply(BigDecimal.valueOf(0.03)), // mrrNovo
            mrrAtual.multiply(BigDecimal.valueOf(0.02)), // mrrExpansao
            mrrAtual.multiply(BigDecimal.valueOf(0.01)), // mrrContracao
            churnMRR,
            BigDecimal.ZERO, // mrrReativacao
            evolucaoMensal,
            new ArrayList<>(), // cohortAnalysis
            motivosCancelamento,
            fontesAquisicao
        );
    }

    /**
     * Retorna resumo dos relatórios disponíveis.
     */
    public RelatorioSummaryDTO getRelatoriosSummary() {
        List<RelatorioSummaryDTO.RelatorioDisponivel> relatoriosDisponiveis = Arrays.asList(
            new RelatorioSummaryDTO.RelatorioDisponivel(
                "FINANCEIRO",
                "Relatório Financeiro",
                "Análise completa de receitas, faturas, inadimplência e evolução financeira",
                "DollarSign",
                Arrays.asList("PDF", "EXCEL", "CSV")
            ),
            new RelatorioSummaryDTO.RelatorioDisponivel(
                "OPERACIONAL",
                "Relatório Operacional",
                "Métricas de uso, distribuição de oficinas e atividade do sistema",
                "Activity",
                Arrays.asList("PDF", "EXCEL", "CSV")
            ),
            new RelatorioSummaryDTO.RelatorioDisponivel(
                "CRESCIMENTO",
                "Relatório de Crescimento",
                "Análise de aquisição, churn, retenção e evolução do MRR",
                "TrendingUp",
                Arrays.asList("PDF", "EXCEL", "CSV")
            )
        );

        // Em produção, buscar relatórios recentes do banco
        List<RelatorioSummaryDTO.RelatorioRecente> relatoriosRecentes = new ArrayList<>();

        // Período disponível baseado na oficina mais antiga
        LocalDate dataMinima = oficinaRepository.findAll().stream()
            .filter(o -> o.getCreatedAt() != null)
            .map(o -> o.getCreatedAt().toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now().minusYears(1));

        RelatorioSummaryDTO.PeriodoDisponivel periodoDisponivel = new RelatorioSummaryDTO.PeriodoDisponivel(
            dataMinima.toString(),
            LocalDate.now().toString()
        );

        return new RelatorioSummaryDTO(relatoriosDisponiveis, relatoriosRecentes, periodoDisponivel);
    }

    // ========== Métodos auxiliares ==========

    private BigDecimal calcularMRRAtual() {
        return oficinaRepository.findAll().stream()
            .filter(o -> o.getStatus() == StatusOficina.ATIVA)
            .filter(o -> o.getPlano() != null)
            .map(o -> o.getPlano().getValorMensal())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean temFaturasVencidas(UUID oficinaId) {
        return faturaRepository.findByOficinaIdAndStatus(oficinaId, StatusFatura.VENCIDO).size() > 0;
    }

    private List<RelatorioFinanceiroDTO.ReceitaPorPlano> calcularReceitaPorPlano(List<Fatura> faturas, BigDecimal receitaTotal) {
        Map<String, BigDecimal> receitaPorPlano = new HashMap<>();
        Map<String, Integer> oficinaPorPlano = new HashMap<>();

        for (Fatura fatura : faturas) {
            if (fatura.getStatus() == StatusFatura.PAGO && fatura.getOficina().getPlano() != null) {
                String planoNome = fatura.getOficina().getPlano().getNome();
                receitaPorPlano.merge(planoNome, fatura.getValorTotal(), BigDecimal::add);
                oficinaPorPlano.merge(planoNome, 1, Integer::sum);
            }
        }

        return receitaPorPlano.entrySet().stream()
            .map(entry -> {
                BigDecimal percentual = receitaTotal.compareTo(BigDecimal.ZERO) > 0 ?
                    entry.getValue().divide(receitaTotal, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO;
                return new RelatorioFinanceiroDTO.ReceitaPorPlano(
                    entry.getKey(),
                    entry.getKey().toUpperCase().replace(" ", "_"),
                    oficinaPorPlano.getOrDefault(entry.getKey(), 0),
                    entry.getValue(),
                    percentual
                );
            })
            .sorted((a, b) -> b.receitaTotal().compareTo(a.receitaTotal()))
            .toList();
    }

    private List<RelatorioFinanceiroDTO.EvolucaoMensal> calcularEvolucaoMensalFinanceiro(LocalDate dataInicio, LocalDate dataFim) {
        List<RelatorioFinanceiroDTO.EvolucaoMensal> evolucao = new ArrayList<>();
        YearMonth mesAtual = YearMonth.from(dataInicio);
        YearMonth mesFim = YearMonth.from(dataFim);

        while (!mesAtual.isAfter(mesFim)) {
            LocalDate inicioMes = mesAtual.atDay(1);
            LocalDate fimMes = mesAtual.atEndOfMonth();

            List<Fatura> faturasMes = faturaRepository.findByDataEmissaoBetween(inicioMes, fimMes);

            BigDecimal receita = faturasMes.stream()
                .filter(f -> f.getStatus() == StatusFatura.PAGO)
                .map(Fatura::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal mrr = calcularMRRAtual(); // Simplificado

            evolucao.add(new RelatorioFinanceiroDTO.EvolucaoMensal(
                mesAtual.format(MES_ANO_FORMATTER),
                receita,
                mrr,
                0, // novasOficinas
                0  // cancelamentos
            ));

            mesAtual = mesAtual.plusMonths(1);
        }

        return evolucao;
    }

    private List<RelatorioFinanceiroDTO.OficinaReceita> calcularTopOficinasPorReceita(List<Fatura> faturas) {
        Map<UUID, BigDecimal> receitaPorOficina = new HashMap<>();
        Map<UUID, Oficina> oficinas = new HashMap<>();

        for (Fatura fatura : faturas) {
            if (fatura.getStatus() == StatusFatura.PAGO) {
                UUID oficinaId = fatura.getOficina().getId();
                receitaPorOficina.merge(oficinaId, fatura.getValorTotal(), BigDecimal::add);
                oficinas.putIfAbsent(oficinaId, fatura.getOficina());
            }
        }

        return receitaPorOficina.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(entry -> {
                Oficina oficina = oficinas.get(entry.getKey());
                int mesesAtivo = oficina.getCreatedAt() != null ?
                    (int) ChronoUnit.MONTHS.between(oficina.getCreatedAt().toLocalDate(), LocalDate.now()) + 1 : 0;
                return new RelatorioFinanceiroDTO.OficinaReceita(
                    entry.getKey().toString(),
                    oficina.getNomeFantasia(),
                    oficina.getCnpjCpf(),
                    oficina.getPlano() != null ? oficina.getPlano().getNome() : "Sem plano",
                    entry.getValue(),
                    mesesAtivo
                );
            })
            .toList();
    }

    private List<RelatorioOperacionalDTO.DistribuicaoPlano> calcularDistribuicaoPlanos(List<Oficina> oficinas) {
        Map<String, Long> countPorPlano = oficinas.stream()
            .filter(o -> o.getPlano() != null)
            .collect(Collectors.groupingBy(o -> o.getPlano().getNome(), Collectors.counting()));

        long total = oficinas.size();

        return countPorPlano.entrySet().stream()
            .map(entry -> new RelatorioOperacionalDTO.DistribuicaoPlano(
                entry.getKey(),
                entry.getKey().toUpperCase().replace(" ", "_"),
                entry.getValue().intValue(),
                total > 0 ? (double) entry.getValue() / total * 100 : 0
            ))
            .sorted((a, b) -> Integer.compare(b.quantidade(), a.quantidade()))
            .toList();
    }

    private List<RelatorioOperacionalDTO.EvolucaoOperacional> calcularEvolucaoOperacional(LocalDate dataInicio, LocalDate dataFim) {
        List<RelatorioOperacionalDTO.EvolucaoOperacional> evolucao = new ArrayList<>();
        YearMonth mesAtual = YearMonth.from(dataInicio);
        YearMonth mesFim = YearMonth.from(dataFim);

        while (!mesAtual.isAfter(mesFim)) {
            evolucao.add(new RelatorioOperacionalDTO.EvolucaoOperacional(
                mesAtual.format(MES_ANO_FORMATTER),
                0, // oficinasAtivas - seria calculado por mês
                0L, // ordensServico
                0L, // clientes
                0   // usuarios
            ));
            mesAtual = mesAtual.plusMonths(1);
        }

        return evolucao;
    }

    private List<RelatorioCrescimentoDTO.EvolucaoCrescimento> calcularEvolucaoCrescimento(LocalDate dataInicio, LocalDate dataFim, List<Oficina> oficinas) {
        List<RelatorioCrescimentoDTO.EvolucaoCrescimento> evolucao = new ArrayList<>();
        YearMonth mesAtual = YearMonth.from(dataInicio);
        YearMonth mesFim = YearMonth.from(dataFim);

        while (!mesAtual.isAfter(mesFim)) {
            LocalDate inicioMes = mesAtual.atDay(1);
            LocalDate fimMes = mesAtual.atEndOfMonth();

            int novasOficinas = (int) oficinas.stream()
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().toLocalDate().isBefore(inicioMes))
                .filter(o -> !o.getCreatedAt().toLocalDate().isAfter(fimMes))
                .count();

            int cancelamentos = (int) oficinas.stream()
                .filter(o -> o.getStatus() == StatusOficina.CANCELADA)
                .filter(o -> o.getUpdatedAt() != null)
                .filter(o -> !o.getUpdatedAt().toLocalDate().isBefore(inicioMes))
                .filter(o -> !o.getUpdatedAt().toLocalDate().isAfter(fimMes))
                .count();

            evolucao.add(new RelatorioCrescimentoDTO.EvolucaoCrescimento(
                mesAtual.format(MES_ANO_FORMATTER),
                novasOficinas,
                cancelamentos,
                novasOficinas - cancelamentos,
                calcularMRRAtual(),
                BigDecimal.ZERO // churnRate mensal
            ));

            mesAtual = mesAtual.plusMonths(1);
        }

        return evolucao;
    }
}
