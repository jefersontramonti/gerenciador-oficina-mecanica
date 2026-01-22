package com.pitstop.financeiro.service;

import com.pitstop.estoque.repository.MovimentacaoEstoqueRepository;
import com.pitstop.financeiro.domain.CategoriaDespesa;
import com.pitstop.financeiro.domain.Despesa;
import com.pitstop.financeiro.dto.AlertaDREDTO;
import com.pitstop.financeiro.dto.AlertaFluxoCaixaDTO;
import com.pitstop.financeiro.dto.DRESimplificadoDTO;
import com.pitstop.financeiro.dto.FluxoCaixaDTO;
import com.pitstop.financeiro.dto.ProjecaoFinanceiraDTO;
import com.pitstop.financeiro.repository.DespesaRepository;
import com.pitstop.financeiro.repository.PagamentoRepository;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Serviço para cálculos de fluxo de caixa, DRE e projeções financeiras.
 * Usa dados REAIS do sistema:
 * - Receitas: Pagamentos confirmados (status=PAGO)
 * - Despesas: Tabela de despesas + Compras de peças (ENTRADA no estoque)
 * - CMV: Peças baixadas para OS (BAIXA_OS no estoque)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FluxoCaixaService {

    private final PagamentoRepository pagamentoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final DespesaRepository despesaRepository;

    // ========== Fluxo de Caixa ==========

    /**
     * Calcula o fluxo de caixa para um período usando dados REAIS.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "fluxoCaixa", key = "#root.target.getTenantId() + '-' + #inicio + '-' + #fim")
    public FluxoCaixaDTO getFluxoCaixa(LocalDate inicio, LocalDate fim) {
        UUID oficinaId = TenantContext.getTenantId();
        log.debug("Calculando fluxo de caixa REAL para oficina {} de {} a {}", oficinaId, inicio, fim);

        // ========== RECEITAS (Pagamentos confirmados) ==========
        List<Object[]> pagamentosDiarios = pagamentoRepository.findPagamentosDiariosByOficinaAndPeriodo(
            oficinaId, inicio, fim
        );
        Map<LocalDate, BigDecimal> receitasPorDia = new HashMap<>();
        for (Object[] row : pagamentosDiarios) {
            LocalDate data = ((java.sql.Date) row[0]).toLocalDate();
            BigDecimal valor = (BigDecimal) row[1];
            receitasPorDia.put(data, valor);
        }

        // ========== DESPESAS ==========
        // 1. Despesas operacionais (tabela despesas)
        List<Object[]> despesasDiarias = despesaRepository.findDespesasDiariasByPeriodo(
            oficinaId, inicio, fim
        );
        Map<LocalDate, BigDecimal> despesasOpPorDia = new HashMap<>();
        for (Object[] row : despesasDiarias) {
            if (row[0] != null) {
                LocalDate data = ((java.sql.Date) row[0]).toLocalDate();
                BigDecimal valor = (BigDecimal) row[1];
                despesasOpPorDia.put(data, valor);
            }
        }

        // 2. Compras de peças (ENTRADA no estoque)
        List<Object[]> comprasDiarias = movimentacaoRepository.findComprasDiariasByPeriodo(
            oficinaId, inicio, fim
        );
        Map<LocalDate, BigDecimal> comprasPorDia = new HashMap<>();
        for (Object[] row : comprasDiarias) {
            if (row[0] != null) {
                LocalDate data = ((java.sql.Date) row[0]).toLocalDate();
                BigDecimal valor = (BigDecimal) row[1];
                comprasPorDia.put(data, valor);
            }
        }

        // ========== CALCULAR TOTAIS E MOVIMENTOS DIÁRIOS ==========
        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;
        BigDecimal totalComprasPecas = BigDecimal.ZERO;

        List<FluxoCaixaDTO.MovimentoDiarioDTO> movimentosDiarios = new ArrayList<>();
        BigDecimal saldoAcumulado = BigDecimal.ZERO;

        LocalDate dataAtual = inicio;
        while (!dataAtual.isAfter(fim)) {
            BigDecimal receitaDia = receitasPorDia.getOrDefault(dataAtual, BigDecimal.ZERO);
            BigDecimal despesaOpDia = despesasOpPorDia.getOrDefault(dataAtual, BigDecimal.ZERO);
            BigDecimal compraDia = comprasPorDia.getOrDefault(dataAtual, BigDecimal.ZERO);

            // Despesa total = Despesas operacionais + Compras de peças
            BigDecimal despesaTotalDia = despesaOpDia.add(compraDia);

            BigDecimal saldoDia = receitaDia.subtract(despesaTotalDia);
            saldoAcumulado = saldoAcumulado.add(saldoDia);

            totalReceitas = totalReceitas.add(receitaDia);
            totalDespesas = totalDespesas.add(despesaTotalDia);
            totalComprasPecas = totalComprasPecas.add(compraDia);

            movimentosDiarios.add(FluxoCaixaDTO.MovimentoDiarioDTO.builder()
                .data(dataAtual)
                .receitas(receitaDia)
                .despesas(despesaTotalDia)
                .saldo(saldoDia)
                .saldoAcumulado(saldoAcumulado)
                .build());

            dataAtual = dataAtual.plusDays(1);
        }

        // ========== RECEITAS POR CATEGORIA (tipo de pagamento) ==========
        List<Object[]> receitasPorTipo = pagamentoRepository.findReceitasPorTipoPagamento(
            oficinaId, inicio, fim
        );

        List<FluxoCaixaDTO.MovimentoCategoriaDTO> receitasPorCategoria = new ArrayList<>();
        for (Object[] row : receitasPorTipo) {
            String tipo = (String) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            Long qtd = (Long) row[2];

            BigDecimal percentual = totalReceitas.compareTo(BigDecimal.ZERO) > 0
                ? valor.multiply(BigDecimal.valueOf(100)).divide(totalReceitas, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            receitasPorCategoria.add(FluxoCaixaDTO.MovimentoCategoriaDTO.builder()
                .categoria(getLabelTipoPagamento(tipo))
                .cor(getCorPorTipoPagamento(tipo))
                .valor(valor)
                .percentual(percentual)
                .quantidade(qtd.intValue())
                .build());
        }

        // ========== DESPESAS POR CATEGORIA ==========
        List<FluxoCaixaDTO.MovimentoCategoriaDTO> despesasPorCategoria = new ArrayList<>();

        // 1. Adicionar compras de peças como categoria
        if (totalComprasPecas.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentCompras = totalDespesas.compareTo(BigDecimal.ZERO) > 0
                ? totalComprasPecas.multiply(BigDecimal.valueOf(100)).divide(totalDespesas, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            despesasPorCategoria.add(FluxoCaixaDTO.MovimentoCategoriaDTO.builder()
                .categoria("Compra de Peças")
                .cor(CategoriaDespesa.COMPRA_PECAS.getCor())
                .valor(totalComprasPecas)
                .percentual(percentCompras)
                .quantidade(comprasPorDia.size())
                .build());
        }

        // 2. Adicionar despesas operacionais por categoria
        List<Object[]> despesasPorCat = despesaRepository.findDespesasPorCategoria(
            oficinaId, inicio, fim
        );
        for (Object[] row : despesasPorCat) {
            String categoriaStr = (String) row[0];
            BigDecimal valor = (BigDecimal) row[1];
            Long qtd = (Long) row[2];

            try {
                CategoriaDespesa categoria = CategoriaDespesa.valueOf(categoriaStr);
                BigDecimal percentual = totalDespesas.compareTo(BigDecimal.ZERO) > 0
                    ? valor.multiply(BigDecimal.valueOf(100)).divide(totalDespesas, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

                despesasPorCategoria.add(FluxoCaixaDTO.MovimentoCategoriaDTO.builder()
                    .categoria(categoria.getDescricao())
                    .cor(categoria.getCor())
                    .valor(valor)
                    .percentual(percentual)
                    .quantidade(qtd.intValue())
                    .build());
            } catch (IllegalArgumentException e) {
                log.warn("Categoria de despesa desconhecida: {}", categoriaStr);
            }
        }

        // Ordenar despesas por valor (decrescente)
        despesasPorCategoria.sort((a, b) -> b.getValor().compareTo(a.getValor()));

        // ========== VARIAÇÃO EM RELAÇÃO AO PERÍODO ANTERIOR ==========
        long diasPeriodo = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        LocalDate inicioAnterior = inicio.minusDays(diasPeriodo);
        LocalDate fimAnterior = inicio.minusDays(1);

        BigDecimal receitasAnterior = pagamentoRepository.sumPagamentosByOficinaAndPeriodo(
            oficinaId, inicioAnterior, fimAnterior
        );
        if (receitasAnterior == null) receitasAnterior = BigDecimal.ZERO;

        BigDecimal despesasAnterior = despesaRepository.sumDespesasPagasByPeriodo(
            oficinaId, inicioAnterior, fimAnterior
        );
        if (despesasAnterior == null) despesasAnterior = BigDecimal.ZERO;

        BigDecimal comprasAnterior = movimentacaoRepository.sumComprasByPeriodo(
            oficinaId, inicioAnterior, fimAnterior
        );
        if (comprasAnterior == null) comprasAnterior = BigDecimal.ZERO;

        BigDecimal totalDespesasAnterior = despesasAnterior.add(comprasAnterior);

        BigDecimal variacaoReceitas = calcularVariacao(totalReceitas, receitasAnterior);
        BigDecimal variacaoDespesas = calcularVariacao(totalDespesas, totalDespesasAnterior);

        BigDecimal saldoAnterior = receitasAnterior.subtract(totalDespesasAnterior);
        BigDecimal saldoAtual = totalReceitas.subtract(totalDespesas);
        BigDecimal variacaoSaldo = calcularVariacao(saldoAtual, saldoAnterior);

        // Contar dias com saldo negativo
        int diasComSaldoNegativo = (int) movimentosDiarios.stream()
            .filter(m -> m.getSaldoAcumulado().compareTo(BigDecimal.ZERO) < 0)
            .count();

        // Encontrar categoria de despesa com maior concentração
        String categoriaConcentrada = null;
        BigDecimal percentualConcentracao = BigDecimal.ZERO;
        if (!despesasPorCategoria.isEmpty()) {
            FluxoCaixaDTO.MovimentoCategoriaDTO maiorDespesa = despesasPorCategoria.get(0);
            categoriaConcentrada = maiorDespesa.getCategoria();
            percentualConcentracao = maiorDespesa.getPercentual();
        }

        return FluxoCaixaDTO.builder()
            .dataInicio(inicio)
            .dataFim(fim)
            .saldoInicial(BigDecimal.ZERO) // TODO: Implementar saldo inicial da conta
            .totalReceitas(totalReceitas)
            .totalDespesas(totalDespesas)
            .saldoFinal(saldoAtual)
            .variacaoReceitas(variacaoReceitas)
            .variacaoDespesas(variacaoDespesas)
            .variacaoSaldo(variacaoSaldo)
            .movimentosDiarios(movimentosDiarios)
            .receitasPorCategoria(receitasPorCategoria)
            .despesasPorCategoria(despesasPorCategoria)
            .alertas(gerarAlertasFluxoCaixa(
                saldoAtual, totalReceitas, totalDespesas,
                variacaoReceitas, variacaoDespesas,
                diasComSaldoNegativo, movimentosDiarios.size(),
                categoriaConcentrada, percentualConcentracao
            ))
            .build();
    }

    // ========== DRE Simplificado ==========

    /**
     * Calcula o DRE simplificado para um mês usando dados REAIS.
     *
     * Estrutura do DRE:
     * (+) Receita Bruta de Serviços (mão de obra das OS entregues)
     * (+) Receita Bruta de Peças (peças das OS entregues)
     * (+) Outras Receitas (lançamentos manuais)
     * (=) RECEITA BRUTA TOTAL
     * (-) Descontos Concedidos (diferença entre valorTotal e valorFinal das OS)
     * (-) Cancelamentos (valor das OS canceladas)
     * (=) RECEITA LÍQUIDA
     * (-) CMV - Custo das Mercadorias Vendidas (custo das peças baixadas para OS)
     * (-) Custo de Mão de Obra (proporcional às despesas com pessoal)
     * (=) LUCRO BRUTO
     * (-) Despesas Operacionais (administrativas, pessoal, marketing, outras)
     * (=) RESULTADO OPERACIONAL (EBIT)
     * (+) Receitas Financeiras (juros recebidos, rendimentos)
     * (-) Despesas Financeiras (juros, tarifas)
     * (=) RESULTADO ANTES DOS IMPOSTOS
     * (-) Impostos Estimados (10% sobre lucro positivo)
     * (=) LUCRO LÍQUIDO
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dre", key = "#root.target.getTenantId() + '-' + #mes + '-' + #ano")
    public DRESimplificadoDTO getDREMensal(int mes, int ano) {
        UUID oficinaId = TenantContext.getTenantId();
        log.debug("Calculando DRE REAL para oficina {} - {}/{}", oficinaId, mes, ano);

        YearMonth yearMonth = YearMonth.of(ano, mes);
        LocalDate inicio = yearMonth.atDay(1);
        LocalDate fim = yearMonth.atEndOfMonth();

        // ========== RECEITA BRUTA ==========
        // Receita de Serviços (mão de obra das OS entregues)
        BigDecimal receitaServicos = ordemServicoRepository.sumValorMaoObraByOficinaAndPeriodo(
            oficinaId, inicio, fim
        );
        if (receitaServicos == null) receitaServicos = BigDecimal.ZERO;

        // Receita de Peças (peças das OS entregues)
        BigDecimal receitaPecas = ordemServicoRepository.sumValorPecasPagasByOficinaAndPeriodo(
            oficinaId, inicio, fim
        );
        if (receitaPecas == null) receitaPecas = BigDecimal.ZERO;

        // Outras Receitas (lançamentos manuais na categoria OUTRAS_RECEITAS)
        BigDecimal outrasReceitas = despesaRepository.sumOutrasReceitas(oficinaId, inicio, fim);
        if (outrasReceitas == null) outrasReceitas = BigDecimal.ZERO;

        // Receita Bruta Total
        BigDecimal receitaBrutaTotal = receitaServicos.add(receitaPecas).add(outrasReceitas);

        // ========== DEDUÇÕES ==========
        // Descontos Concedidos (diferença entre valorTotal e valorFinal das OS entregues)
        BigDecimal descontosConcedidos = ordemServicoRepository.sumDescontosConcedidosByOficinaAndPeriodo(
            oficinaId, inicio, fim
        );
        if (descontosConcedidos == null) descontosConcedidos = BigDecimal.ZERO;

        // Cancelamentos (valor das OS canceladas no período)
        BigDecimal cancelamentos = ordemServicoRepository.sumValorCanceladosByOficinaAndPeriodo(
            oficinaId, inicio, fim
        );
        if (cancelamentos == null) cancelamentos = BigDecimal.ZERO;

        // Total de Deduções
        BigDecimal deducoesTotal = descontosConcedidos.add(cancelamentos);

        // Receita Líquida
        BigDecimal receitaLiquida = receitaBrutaTotal.subtract(deducoesTotal);

        // ========== CUSTOS ==========
        // CMV - Custo das Mercadorias Vendidas (custo real das peças baixadas para OS)
        BigDecimal cmvPecas = movimentacaoRepository.sumCMVByPeriodo(
            oficinaId, inicio, fim
        );
        if (cmvPecas == null) cmvPecas = BigDecimal.ZERO;

        // Custo de Mão de Obra (estimado como proporcional às despesas com pessoal)
        // Usamos as despesas com pessoal como proxy para o custo de mão de obra
        BigDecimal despesasPessoal = despesaRepository.sumDespesasPessoal(oficinaId, inicio, fim);
        if (despesasPessoal == null) despesasPessoal = BigDecimal.ZERO;

        // Calculamos o custo de mão de obra como uma proporção das despesas com pessoal
        // baseado na relação entre receita de serviços e receita total
        BigDecimal custoMaoObra = BigDecimal.ZERO;
        if (receitaBrutaTotal.compareTo(BigDecimal.ZERO) > 0 && receitaServicos.compareTo(BigDecimal.ZERO) > 0) {
            // Proporção da receita de serviços no total
            BigDecimal proporcaoServicos = receitaServicos.divide(receitaBrutaTotal, 4, RoundingMode.HALF_UP);
            // Custo de mão de obra = despesas com pessoal * proporção de serviços
            custoMaoObra = despesasPessoal.multiply(proporcaoServicos).setScale(2, RoundingMode.HALF_UP);
        }

        // Total de Custos
        BigDecimal custosTotal = cmvPecas.add(custoMaoObra);

        // ========== LUCRO BRUTO ==========
        BigDecimal lucroBruto = receitaLiquida.subtract(custosTotal);
        BigDecimal margemBruta = receitaLiquida.compareTo(BigDecimal.ZERO) > 0
            ? lucroBruto.multiply(BigDecimal.valueOf(100)).divide(receitaLiquida, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // ========== DESPESAS OPERACIONAIS ==========
        BigDecimal despesasAdmin = despesaRepository.sumDespesasAdministrativas(oficinaId, inicio, fim);
        if (despesasAdmin == null) despesasAdmin = BigDecimal.ZERO;

        BigDecimal despesasMarketing = despesaRepository.sumDespesasMarketing(oficinaId, inicio, fim);
        if (despesasMarketing == null) despesasMarketing = BigDecimal.ZERO;

        // Total de despesas pagas (excluindo receitas financeiras e outras receitas)
        BigDecimal totalDespesasPagas = despesaRepository.sumDespesasPagasByPeriodo(oficinaId, inicio, fim);
        if (totalDespesasPagas == null) totalDespesasPagas = BigDecimal.ZERO;

        // Despesas financeiras (para subtrair do total operacional)
        BigDecimal despesasFinanceiras = despesaRepository.sumDespesasFinanceiras(oficinaId, inicio, fim);
        if (despesasFinanceiras == null) despesasFinanceiras = BigDecimal.ZERO;

        // Receitas financeiras (para subtrair do total)
        BigDecimal receitasFinanceiras = despesaRepository.sumReceitasFinanceiras(oficinaId, inicio, fim);
        if (receitasFinanceiras == null) receitasFinanceiras = BigDecimal.ZERO;

        // Total de despesas operacionais (excluindo financeiras e receitas)
        BigDecimal totalDespesasOp = totalDespesasPagas
            .subtract(despesasFinanceiras)
            .subtract(receitasFinanceiras)
            .subtract(outrasReceitas);
        if (totalDespesasOp.compareTo(BigDecimal.ZERO) < 0) totalDespesasOp = BigDecimal.ZERO;

        // Outras despesas (total operacional - categorizadas)
        // Não incluímos despesas com pessoal aqui pois já foram consideradas no custo de mão de obra
        BigDecimal despesasPessoalNaoDRE = despesasPessoal.subtract(custoMaoObra);
        if (despesasPessoalNaoDRE.compareTo(BigDecimal.ZERO) < 0) despesasPessoalNaoDRE = BigDecimal.ZERO;

        BigDecimal outrasDespesas = totalDespesasOp
            .subtract(despesasPessoalNaoDRE)
            .subtract(despesasAdmin)
            .subtract(despesasMarketing);
        if (outrasDespesas.compareTo(BigDecimal.ZERO) < 0) outrasDespesas = BigDecimal.ZERO;

        // ========== RESULTADO OPERACIONAL ==========
        BigDecimal resultadoOperacional = lucroBruto.subtract(totalDespesasOp);
        BigDecimal margemOperacional = receitaLiquida.compareTo(BigDecimal.ZERO) > 0
            ? resultadoOperacional.multiply(BigDecimal.valueOf(100)).divide(receitaLiquida, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // ========== RESULTADO FINANCEIRO ==========
        BigDecimal resultadoFinanceiro = receitasFinanceiras.subtract(despesasFinanceiras);

        // ========== RESULTADO ANTES DOS IMPOSTOS ==========
        BigDecimal resultadoAntesImpostos = resultadoOperacional.add(resultadoFinanceiro);

        // Estimativa de impostos (Simples Nacional aproximado - 6% a 15%)
        BigDecimal aliquotaImposto = BigDecimal.valueOf(0.10); // 10% estimado
        BigDecimal impostos = BigDecimal.ZERO;
        if (resultadoAntesImpostos.compareTo(BigDecimal.ZERO) > 0) {
            impostos = resultadoAntesImpostos.multiply(aliquotaImposto).setScale(2, RoundingMode.HALF_UP);
        }

        // ========== LUCRO LÍQUIDO ==========
        BigDecimal lucroLiquido = resultadoAntesImpostos.subtract(impostos);
        BigDecimal margemLiquida = receitaLiquida.compareTo(BigDecimal.ZERO) > 0
            ? lucroLiquido.multiply(BigDecimal.valueOf(100)).divide(receitaLiquida, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // ========== COMPARATIVO MÊS ANTERIOR ==========
        YearMonth mesAnterior = yearMonth.minusMonths(1);
        LocalDate inicioAnterior = mesAnterior.atDay(1);
        LocalDate fimAnterior = mesAnterior.atEndOfMonth();

        // Receita do mês anterior (serviços + peças)
        BigDecimal receitaServicosAnterior = ordemServicoRepository.sumValorMaoObraByOficinaAndPeriodo(
            oficinaId, inicioAnterior, fimAnterior
        );
        if (receitaServicosAnterior == null) receitaServicosAnterior = BigDecimal.ZERO;

        BigDecimal receitaPecasAnterior = ordemServicoRepository.sumValorPecasPagasByOficinaAndPeriodo(
            oficinaId, inicioAnterior, fimAnterior
        );
        if (receitaPecasAnterior == null) receitaPecasAnterior = BigDecimal.ZERO;

        BigDecimal receitaMesAnterior = receitaServicosAnterior.add(receitaPecasAnterior);

        BigDecimal variacaoReceita = calcularVariacao(receitaBrutaTotal, receitaMesAnterior);

        String nomeMes = yearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        String periodo = nomeMes.substring(0, 1).toUpperCase() + nomeMes.substring(1) + "/" + ano;

        return DRESimplificadoDTO.builder()
            .mes(mes)
            .ano(ano)
            .periodo(periodo)
            // Receitas
            .receitaBrutaServicos(receitaServicos)
            .receitaBrutaPecas(receitaPecas)
            .outrasReceitas(outrasReceitas)
            .receitaBrutaTotal(receitaBrutaTotal)
            // Deduções
            .descontosConcedidos(descontosConcedidos)
            .cancelamentos(cancelamentos)
            .deducoesTotal(deducoesTotal)
            .receitaLiquida(receitaLiquida)
            // Custos
            .custoPecasVendidas(cmvPecas)
            .custoMaoObra(custoMaoObra)
            .custosTotal(custosTotal)
            // Lucro Bruto
            .lucroBruto(lucroBruto)
            .margemBruta(margemBruta)
            // Despesas Operacionais
            .despesasAdministrativas(despesasAdmin)
            .despesasPessoal(despesasPessoalNaoDRE)
            .despesasMarketing(despesasMarketing)
            .outrasDespesas(outrasDespesas)
            .despesasOperacionaisTotal(totalDespesasOp)
            // Resultado Operacional
            .resultadoOperacional(resultadoOperacional)
            .margemOperacional(margemOperacional)
            // Financeiro
            .receitasFinanceiras(receitasFinanceiras)
            .despesasFinanceiras(despesasFinanceiras)
            .resultadoFinanceiro(resultadoFinanceiro)
            // Resultado Final
            .resultadoAntesImpostos(resultadoAntesImpostos)
            .impostos(impostos)
            .lucroLiquido(lucroLiquido)
            .margemLiquida(margemLiquida)
            // Comparativo
            .comparativoMesAnterior(DRESimplificadoDTO.ComparativoDTO.builder()
                .receitaAnterior(receitaMesAnterior)
                .variacaoReceita(variacaoReceita)
                .build())
            // Alertas inteligentes
            .alertas(gerarAlertasDRE(
                receitaBrutaTotal, receitaLiquida, receitaServicos, receitaPecas,
                deducoesTotal, cmvPecas, custoMaoObra, lucroBruto, margemBruta,
                totalDespesasOp, despesasPessoalNaoDRE, resultadoOperacional, margemOperacional,
                lucroLiquido, margemLiquida
            ))
            .build();
    }

    /**
     * Gera alertas inteligentes baseados nos indicadores do DRE.
     */
    private List<AlertaDREDTO> gerarAlertasDRE(
            BigDecimal receitaBrutaTotal, BigDecimal receitaLiquida,
            BigDecimal receitaServicos, BigDecimal receitaPecas,
            BigDecimal deducoesTotal, BigDecimal cmvPecas, BigDecimal custoMaoObra,
            BigDecimal lucroBruto, BigDecimal margemBruta,
            BigDecimal despesasOperacionais, BigDecimal despesasPessoal,
            BigDecimal resultadoOperacional, BigDecimal margemOperacional,
            BigDecimal lucroLiquido, BigDecimal margemLiquida
    ) {
        List<AlertaDREDTO> alertas = new ArrayList<>();

        // 1. Sem receita no período
        if (receitaBrutaTotal.compareTo(BigDecimal.ZERO) == 0) {
            alertas.add(AlertaDREDTO.semReceita());
            return alertas; // Retorna apenas este alerta se não há receita
        }

        // 2. Margem Bruta baixa (< 20%)
        BigDecimal limiteMargemBruta = new BigDecimal("20");
        if (margemBruta.compareTo(limiteMargemBruta) < 0) {
            alertas.add(AlertaDREDTO.margemBrutaBaixa(margemBruta));
        }

        // 3. Margem Operacional baixa (< 10%)
        BigDecimal limiteMargemOperacional = new BigDecimal("10");
        if (margemOperacional.compareTo(limiteMargemOperacional) < 0 &&
            margemOperacional.compareTo(BigDecimal.ZERO) >= 0) {
            alertas.add(AlertaDREDTO.margemOperacionalBaixa(margemOperacional));
        }

        // 4. Resultado operacional negativo
        if (resultadoOperacional.compareTo(BigDecimal.ZERO) < 0) {
            alertas.add(AlertaDREDTO.resultadoOperacionalNegativo(resultadoOperacional));
        }

        // 5. Margem Líquida negativa
        if (margemLiquida.compareTo(BigDecimal.ZERO) < 0) {
            alertas.add(AlertaDREDTO.margemLiquidaNegativa(margemLiquida));
        }

        // 6. Lucro Líquido negativo (se ainda não adicionou alerta de margem negativa)
        if (lucroLiquido.compareTo(BigDecimal.ZERO) < 0 && margemLiquida.compareTo(BigDecimal.ZERO) >= 0) {
            alertas.add(AlertaDREDTO.lucroLiquidoNegativo(lucroLiquido));
        }

        // 7. CMV alto em relação à receita de peças (> 70%)
        if (receitaPecas.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentualCMV = cmvPecas.multiply(BigDecimal.valueOf(100))
                .divide(receitaPecas, 2, RoundingMode.HALF_UP);
            BigDecimal limiteCMV = new BigDecimal("70");
            if (percentualCMV.compareTo(limiteCMV) > 0) {
                alertas.add(AlertaDREDTO.cmvAlto(percentualCMV));
            }
        }

        // 8. Despesas operacionais altas (> 40% da receita líquida)
        if (receitaLiquida.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentualDespOp = despesasOperacionais.multiply(BigDecimal.valueOf(100))
                .divide(receitaLiquida, 2, RoundingMode.HALF_UP);
            BigDecimal limiteDespOp = new BigDecimal("40");
            if (percentualDespOp.compareTo(limiteDespOp) > 0) {
                alertas.add(AlertaDREDTO.despesasOperacionaisAltas(percentualDespOp));
            }
        }

        // 9. Despesas com pessoal altas (> 30% da receita líquida)
        if (receitaLiquida.compareTo(BigDecimal.ZERO) > 0 && despesasPessoal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentualPessoal = despesasPessoal.multiply(BigDecimal.valueOf(100))
                .divide(receitaLiquida, 2, RoundingMode.HALF_UP);
            BigDecimal limitePessoal = new BigDecimal("30");
            if (percentualPessoal.compareTo(limitePessoal) > 0) {
                alertas.add(AlertaDREDTO.despesasPessoalAltas(percentualPessoal));
            }
        }

        // 10. Custo de mão de obra alto (> 50% da receita de serviços)
        if (receitaServicos.compareTo(BigDecimal.ZERO) > 0 && custoMaoObra.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentualMaoObra = custoMaoObra.multiply(BigDecimal.valueOf(100))
                .divide(receitaServicos, 2, RoundingMode.HALF_UP);
            BigDecimal limiteMaoObra = new BigDecimal("50");
            if (percentualMaoObra.compareTo(limiteMaoObra) > 0) {
                alertas.add(AlertaDREDTO.custoMaoObraAlto(percentualMaoObra));
            }
        }

        // 11. Deduções altas (> 5% da receita bruta)
        if (receitaBrutaTotal.compareTo(BigDecimal.ZERO) > 0 && deducoesTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentualDeducoes = deducoesTotal.multiply(BigDecimal.valueOf(100))
                .divide(receitaBrutaTotal, 2, RoundingMode.HALF_UP);
            BigDecimal limiteDeducoes = new BigDecimal("5");
            if (percentualDeducoes.compareTo(limiteDeducoes) > 0) {
                alertas.add(AlertaDREDTO.deducoesAltas(percentualDeducoes));
            }
        }

        // Ordenar alertas por severidade (CRITICAL primeiro, depois WARNING, depois INFO)
        alertas.sort((a, b) -> {
            int prioridadeA = a.getNivel() == AlertaDREDTO.NivelAlerta.CRITICAL ? 0 :
                             a.getNivel() == AlertaDREDTO.NivelAlerta.WARNING ? 1 : 2;
            int prioridadeB = b.getNivel() == AlertaDREDTO.NivelAlerta.CRITICAL ? 0 :
                             b.getNivel() == AlertaDREDTO.NivelAlerta.WARNING ? 1 : 2;
            return prioridadeA - prioridadeB;
        });

        return alertas;
    }

    /**
     * Gera alertas inteligentes baseados nos indicadores do Fluxo de Caixa.
     */
    private List<AlertaFluxoCaixaDTO> gerarAlertasFluxoCaixa(
            BigDecimal saldoFinal, BigDecimal totalReceitas, BigDecimal totalDespesas,
            BigDecimal variacaoReceitas, BigDecimal variacaoDespesas,
            int diasComSaldoNegativo, int totalDias,
            String categoriaConcentrada, BigDecimal percentualConcentracao
    ) {
        List<AlertaFluxoCaixaDTO> alertas = new ArrayList<>();

        // 1. Saldo negativo no período
        if (saldoFinal.compareTo(BigDecimal.ZERO) < 0) {
            alertas.add(AlertaFluxoCaixaDTO.saldoNegativo(saldoFinal));
        }

        // 2. Sem receitas no período
        if (totalReceitas.compareTo(BigDecimal.ZERO) == 0) {
            alertas.add(AlertaFluxoCaixaDTO.semReceitas(totalDias));
            return alertas; // Retorna apenas este alerta se não há receitas
        }

        // 3. Despesas superam receitas (queimando caixa)
        if (totalDespesas.compareTo(totalReceitas) > 0) {
            alertas.add(AlertaFluxoCaixaDTO.queimandoCaixa(totalDespesas, totalReceitas));
        }

        // 4. Receita em queda significativa (mais de 10%)
        if (variacaoReceitas != null && variacaoReceitas.compareTo(BigDecimal.valueOf(-10)) < 0) {
            alertas.add(AlertaFluxoCaixaDTO.receitaEmQueda(variacaoReceitas));
        }

        // 5. Despesas crescentes (mais de 20%)
        if (variacaoDespesas != null && variacaoDespesas.compareTo(BigDecimal.valueOf(20)) > 0) {
            alertas.add(AlertaFluxoCaixaDTO.despesasCrescentes(variacaoDespesas));
        }

        // 6. Muitos dias com saldo negativo (mais de 20%)
        if (totalDias > 0 && diasComSaldoNegativo > 0) {
            BigDecimal percentualDiasNegativos = BigDecimal.valueOf(diasComSaldoNegativo * 100.0 / totalDias);
            if (percentualDiasNegativos.compareTo(BigDecimal.valueOf(20)) > 0) {
                alertas.add(AlertaFluxoCaixaDTO.diasNegativos(diasComSaldoNegativo, totalDias));
            }
        }

        // 7. Concentração alta de despesas em uma categoria (mais de 50%)
        if (categoriaConcentrada != null && percentualConcentracao.compareTo(BigDecimal.valueOf(50)) > 0) {
            alertas.add(AlertaFluxoCaixaDTO.concentracaoDespesa(categoriaConcentrada, percentualConcentracao));
        }

        // Ordenar alertas por severidade (CRITICAL primeiro, depois WARNING, depois INFO)
        alertas.sort((a, b) -> {
            int prioridadeA = a.getNivel() == AlertaFluxoCaixaDTO.NivelAlerta.CRITICAL ? 0 :
                             a.getNivel() == AlertaFluxoCaixaDTO.NivelAlerta.WARNING ? 1 : 2;
            int prioridadeB = b.getNivel() == AlertaFluxoCaixaDTO.NivelAlerta.CRITICAL ? 0 :
                             b.getNivel() == AlertaFluxoCaixaDTO.NivelAlerta.WARNING ? 1 : 2;
            return prioridadeA - prioridadeB;
        });

        return alertas;
    }

    // ========== Projeção Financeira ==========

    /**
     * Calcula a projeção financeira para os próximos dias.
     */
    @Transactional(readOnly = true)
    public ProjecaoFinanceiraDTO getProjecao(int dias) {
        UUID oficinaId = TenantContext.getTenantId();
        log.debug("Calculando projeção {} dias para oficina {}", dias, oficinaId);

        LocalDate hoje = LocalDate.now();
        LocalDate dataFim = hoje.plusDays(dias);

        // ========== RECEITAS ESPERADAS (OS pendentes) ==========
        List<Object[]> osPendentes = ordemServicoRepository.findOSPendentesByOficinaWithValor(oficinaId);

        BigDecimal receitasEsperadas = BigDecimal.ZERO;
        List<ProjecaoFinanceiraDTO.ReceitaEsperadaDTO> detalhamentoReceitas = new ArrayList<>();

        for (Object[] row : osPendentes) {
            String numero = (String) row[0];
            String status = (String) row[1];
            BigDecimal valor = (BigDecimal) row[2];

            if (valor == null) continue;
            receitasEsperadas = receitasEsperadas.add(valor);

            String probabilidade = "MEDIA";
            if ("APROVADO".equals(status) || "EM_ANDAMENTO".equals(status)) {
                probabilidade = "ALTA";
            } else if ("ORCAMENTO".equals(status)) {
                probabilidade = "BAIXA";
            }

            detalhamentoReceitas.add(ProjecaoFinanceiraDTO.ReceitaEsperadaDTO.builder()
                .origem("OS_" + status)
                .descricao("OS #" + numero)
                .valor(valor)
                .dataEsperada(hoje.plusDays(7))
                .probabilidade(probabilidade)
                .build());
        }

        // ========== DESPESAS PREVISTAS (a vencer) ==========
        List<Despesa> despesasAVencer = despesaRepository.findDespesasAVencer(
            oficinaId, hoje, dataFim
        );

        BigDecimal despesasPrevistas = BigDecimal.ZERO;
        List<ProjecaoFinanceiraDTO.DespesaPrevistaDTO> detalhamentoDespesas = new ArrayList<>();

        for (var despesa : despesasAVencer) {
            despesasPrevistas = despesasPrevistas.add(despesa.getValor());

            detalhamentoDespesas.add(ProjecaoFinanceiraDTO.DespesaPrevistaDTO.builder()
                .categoria(despesa.getCategoria().getDescricao())
                .descricao(despesa.getDescricao())
                .valor(despesa.getValor())
                .dataVencimento(despesa.getDataVencimento())
                .recorrente(despesa.getRecorrente())
                .build());
        }

        // ========== MÉDIAS DIÁRIAS (últimos 30 dias) ==========
        LocalDate inicio30Dias = hoje.minusDays(30);

        BigDecimal receitas30Dias = pagamentoRepository.sumPagamentosByOficinaAndPeriodo(
            oficinaId, inicio30Dias, hoje
        );
        if (receitas30Dias == null) receitas30Dias = BigDecimal.ZERO;

        BigDecimal despesas30Dias = despesaRepository.sumDespesasPagasByPeriodo(
            oficinaId, inicio30Dias, hoje
        );
        if (despesas30Dias == null) despesas30Dias = BigDecimal.ZERO;

        BigDecimal compras30Dias = movimentacaoRepository.sumComprasByPeriodo(
            oficinaId, inicio30Dias, hoje
        );
        if (compras30Dias == null) compras30Dias = BigDecimal.ZERO;

        BigDecimal totalDespesas30Dias = despesas30Dias.add(compras30Dias);

        BigDecimal mediaReceitaDiaria = receitas30Dias.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        BigDecimal mediaDespesaDiaria = totalDespesas30Dias.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

        // ========== PROJEÇÃO DIÁRIA ==========
        List<ProjecaoFinanceiraDTO.ProjecaoDiariaDTO> projecaoDiaria = new ArrayList<>();
        BigDecimal saldoAcumulado = BigDecimal.ZERO;

        LocalDate dataAtual = hoje;
        while (!dataAtual.isAfter(dataFim)) {
            BigDecimal receitaPrevista = mediaReceitaDiaria;

            // Verificar despesas específicas do dia
            final LocalDate dataVerificar = dataAtual;
            BigDecimal despesaDia = detalhamentoDespesas.stream()
                .filter(d -> d.getDataVencimento().equals(dataVerificar))
                .map(ProjecaoFinanceiraDTO.DespesaPrevistaDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Se não há despesa específica, usar média
            if (despesaDia.compareTo(BigDecimal.ZERO) == 0) {
                despesaDia = mediaDespesaDiaria;
            }

            BigDecimal saldoDia = receitaPrevista.subtract(despesaDia);
            saldoAcumulado = saldoAcumulado.add(saldoDia);

            projecaoDiaria.add(ProjecaoFinanceiraDTO.ProjecaoDiariaDTO.builder()
                .data(dataAtual)
                .receitasPrevistas(receitaPrevista)
                .despesasPrevistas(despesaDia)
                .saldoDia(saldoDia)
                .saldoAcumulado(saldoAcumulado)
                .alertaSaldoNegativo(saldoAcumulado.compareTo(BigDecimal.ZERO) < 0)
                .build());

            dataAtual = dataAtual.plusDays(1);
        }

        // ========== ALERTAS ==========
        List<ProjecaoFinanceiraDTO.AlertaFluxoDTO> alertas = new ArrayList<>();

        // Alerta de saldo negativo
        if (saldoAcumulado.compareTo(BigDecimal.ZERO) < 0) {
            alertas.add(ProjecaoFinanceiraDTO.AlertaFluxoDTO.builder()
                .tipo("SALDO_NEGATIVO")
                .nivel("DANGER")
                .mensagem("Projeção indica saldo negativo no período")
                .build());
        }

        // Alerta de despesas vencidas
        List<Despesa> despesasVencidas = despesaRepository.findDespesasVencidas(oficinaId, hoje);
        if (!despesasVencidas.isEmpty()) {
            BigDecimal totalVencido = despesasVencidas.stream()
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            alertas.add(ProjecaoFinanceiraDTO.AlertaFluxoDTO.builder()
                .tipo("DESPESAS_VENCIDAS")
                .nivel("WARNING")
                .mensagem(String.format("%d despesa(s) vencida(s) totalizando R$ %.2f",
                    despesasVencidas.size(), totalVencido))
                .build());
        }

        // Alerta de orçamentos pendentes
        if (detalhamentoReceitas.stream().anyMatch(r -> "BAIXA".equals(r.getProbabilidade()))) {
            alertas.add(ProjecaoFinanceiraDTO.AlertaFluxoDTO.builder()
                .tipo("ORCAMENTOS_PENDENTES")
                .nivel("INFO")
                .mensagem("Há orçamentos pendentes de aprovação")
                .build());
        }

        // ========== INDICADORES ==========
        String tendencia = mediaReceitaDiaria.compareTo(mediaDespesaDiaria) > 0 ? "POSITIVA" : "NEGATIVA";
        if (mediaReceitaDiaria.subtract(mediaDespesaDiaria).abs()
            .compareTo(mediaReceitaDiaria.multiply(BigDecimal.valueOf(0.05))) < 0) {
            tendencia = "ESTAVEL";
        }

        ProjecaoFinanceiraDTO.IndicadoresProjecaoDTO indicadores =
            ProjecaoFinanceiraDTO.IndicadoresProjecaoDTO.builder()
                .mediaReceitaDiaria(mediaReceitaDiaria)
                .mediaDespesaDiaria(mediaDespesaDiaria)
                .ticketMedioMes(receitas30Dias.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP))
                .tendencia(tendencia)
                .build();

        return ProjecaoFinanceiraDTO.builder()
            .dataBase(hoje)
            .diasProjecao(dias)
            .dataFimProjecao(dataFim)
            .saldoAtual(BigDecimal.ZERO) // TODO: Implementar saldo atual da conta
            .saldoProjetado(saldoAcumulado)
            .variacaoProjetada(BigDecimal.ZERO)
            .receitasEsperadas(receitasEsperadas)
            .detalhamentoReceitas(detalhamentoReceitas)
            .despesasPrevistas(despesasPrevistas)
            .detalhamentoDespesas(detalhamentoDespesas)
            .alertas(alertas)
            .projecaoDiaria(projecaoDiaria)
            .indicadores(indicadores)
            .build();
    }

    // ========== Métodos Auxiliares ==========

    public UUID getTenantId() {
        return TenantContext.getTenantId();
    }

    private BigDecimal calcularVariacao(BigDecimal valorAtual, BigDecimal valorAnterior) {
        if (valorAnterior == null || valorAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valorAtual.subtract(valorAnterior)
            .multiply(BigDecimal.valueOf(100))
            .divide(valorAnterior, 2, RoundingMode.HALF_UP);
    }

    private String getLabelTipoPagamento(String tipo) {
        return switch (tipo) {
            case "PIX" -> "PIX";
            case "CARTAO_CREDITO" -> "Cartão Crédito";
            case "CARTAO_DEBITO" -> "Cartão Débito";
            case "DINHEIRO" -> "Dinheiro";
            case "TRANSFERENCIA" -> "Transferência";
            case "BOLETO" -> "Boleto";
            default -> tipo;
        };
    }

    private String getCorPorTipoPagamento(String tipo) {
        return switch (tipo) {
            case "PIX" -> "#22C55E";
            case "CARTAO_CREDITO" -> "#3B82F6";
            case "CARTAO_DEBITO" -> "#8B5CF6";
            case "DINHEIRO" -> "#F59E0B";
            case "TRANSFERENCIA" -> "#14B8A6";
            case "BOLETO" -> "#EC4899";
            default -> "#6B7280";
        };
    }
}
