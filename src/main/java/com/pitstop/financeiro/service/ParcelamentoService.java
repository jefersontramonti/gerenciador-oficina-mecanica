package com.pitstop.financeiro.service;

import com.pitstop.financeiro.domain.ConfiguracaoParcelamento;
import com.pitstop.financeiro.domain.TabelaJurosParcelamento;
import com.pitstop.financeiro.domain.TipoJuros;
import com.pitstop.financeiro.dto.*;
import com.pitstop.financeiro.repository.ConfiguracaoParcelamentoRepository;
import com.pitstop.financeiro.repository.TabelaJurosParcelamentoRepository;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.shared.security.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

/**
 * Serviço para cálculo e gestão de parcelamento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelamentoService {

    private final TabelaJurosParcelamentoRepository tabelaJurosRepository;
    private final ConfiguracaoParcelamentoRepository configuracaoRepository;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // ========== Simulação de Parcelamento ==========

    /**
     * Simula as opções de parcelamento para um valor.
     */
    @Transactional(readOnly = true)
    public SimulacaoParcelamentoDTO simularParcelamento(BigDecimal valor) {
        UUID oficinaId = TenantContext.getTenantId();

        // Buscar configuração da oficina ou usar padrão
        ConfiguracaoParcelamento config = configuracaoRepository.findByOficinaId(oficinaId)
            .orElse(criarConfiguracaoPadrao());

        // Buscar faixas de juros
        List<TabelaJurosParcelamento> faixas = tabelaJurosRepository
            .findByOficinaIdAndAtivoTrueOrderByParcelasMinimoAsc(oficinaId);

        // Calcular parcelas máximas considerando valor mínimo da parcela
        int parcelasMaximas = config.calcularParcelasMaximas(valor);
        parcelasMaximas = Math.min(parcelasMaximas, config.getParcelasMaximas());

        // Gerar opções de parcelamento
        List<SimulacaoParcelamentoDTO.OpcaoParcelamentoDTO> opcoes = new ArrayList<>();

        for (int parcelas = 1; parcelas <= config.getParcelasMaximas(); parcelas++) {
            SimulacaoParcelamentoDTO.OpcaoParcelamentoDTO opcao = calcularOpcao(
                valor, parcelas, faixas, config
            );
            opcoes.add(opcao);
        }

        return SimulacaoParcelamentoDTO.builder()
            .valorOriginal(valor)
            .parcelasMaximas(parcelasMaximas)
            .opcoes(opcoes)
            .build();
    }

    /**
     * Calcula uma opção de parcelamento específica.
     */
    private SimulacaoParcelamentoDTO.OpcaoParcelamentoDTO calcularOpcao(
            BigDecimal valor,
            int parcelas,
            List<TabelaJurosParcelamento> faixas,
            ConfiguracaoParcelamento config) {

        // Encontrar faixa de juros aplicável
        TabelaJurosParcelamento faixa = faixas.stream()
            .filter(f -> f.isParcelasDentroFaixa(parcelas))
            .findFirst()
            .orElse(null);

        BigDecimal percentualJuros = BigDecimal.ZERO;
        TipoJuros tipoJuros = TipoJuros.SEM_JUROS;
        boolean repassarCliente = true;

        if (faixa != null) {
            percentualJuros = faixa.getPercentualJuros();
            tipoJuros = faixa.getTipoJuros();
            repassarCliente = Boolean.TRUE.equals(faixa.getRepassarCliente());
        }

        // Calcular valor total e parcela
        BigDecimal valorTotal;
        BigDecimal valorParcela;
        BigDecimal valorJuros = BigDecimal.ZERO;
        boolean semJuros = tipoJuros == TipoJuros.SEM_JUROS ||
                           percentualJuros.compareTo(BigDecimal.ZERO) == 0 ||
                           !repassarCliente;

        if (semJuros || parcelas == 1) {
            // Sem juros ou à vista
            valorTotal = valor;
            valorParcela = valor.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.CEILING);
        } else if (tipoJuros == TipoJuros.JUROS_SIMPLES) {
            // Juros simples: J = P * i * n
            BigDecimal taxaMensal = percentualJuros.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            valorJuros = valor.multiply(taxaMensal).multiply(BigDecimal.valueOf(parcelas));
            valorTotal = valor.add(valorJuros);
            valorParcela = valorTotal.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.CEILING);
        } else {
            // Juros composto: Usando fórmula Price
            // PMT = PV * [i * (1+i)^n] / [(1+i)^n - 1]
            BigDecimal taxaMensal = percentualJuros.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

            double i = taxaMensal.doubleValue();
            double n = parcelas;
            double pv = valor.doubleValue();

            double fator = Math.pow(1 + i, n);
            double pmt = pv * (i * fator) / (fator - 1);

            valorParcela = BigDecimal.valueOf(pmt).setScale(2, RoundingMode.CEILING);
            valorTotal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            valorJuros = valorTotal.subtract(valor);
        }

        // Calcular CET anual aproximado
        BigDecimal cetAnual = BigDecimal.ZERO;
        if (!semJuros && parcelas > 1 && valorJuros.compareTo(BigDecimal.ZERO) > 0) {
            // CET aproximado: ((valorTotal/valorOriginal)^(12/n) - 1) * 100
            double taxaTotal = valorTotal.divide(valor, 10, RoundingMode.HALF_UP).doubleValue();
            double cetAnualDouble = (Math.pow(taxaTotal, 12.0 / parcelas) - 1) * 100;
            cetAnual = BigDecimal.valueOf(cetAnualDouble).setScale(2, RoundingMode.HALF_UP);
        }

        // Verificar se está disponível
        boolean disponivel = valorParcela.compareTo(config.getValorMinimoParcela()) >= 0;
        String mensagemIndisponivel = null;
        if (!disponivel) {
            mensagemIndisponivel = "Valor mínimo da parcela é " +
                CURRENCY_FORMAT.format(config.getValorMinimoParcela());
        }

        // Gerar texto de exibição
        String textoExibicao = gerarTextoExibicao(parcelas, valorParcela, semJuros, percentualJuros);

        return SimulacaoParcelamentoDTO.OpcaoParcelamentoDTO.builder()
            .parcelas(parcelas)
            .valorParcela(valorParcela)
            .valorTotal(valorTotal)
            .valorJuros(valorJuros)
            .percentualJurosMensal(semJuros ? BigDecimal.ZERO : percentualJuros)
            .cetAnual(cetAnual)
            .semJuros(semJuros)
            .textoExibicao(textoExibicao)
            .disponivel(disponivel)
            .mensagemIndisponivel(mensagemIndisponivel)
            .build();
    }

    private String gerarTextoExibicao(int parcelas, BigDecimal valorParcela,
                                      boolean semJuros, BigDecimal percentualJuros) {
        String valorFormatado = CURRENCY_FORMAT.format(valorParcela);

        if (parcelas == 1) {
            return "À vista " + valorFormatado;
        } else if (semJuros) {
            return parcelas + "x de " + valorFormatado + " sem juros";
        } else {
            return parcelas + "x de " + valorFormatado + " (" + percentualJuros + "% a.m.)";
        }
    }

    // ========== CRUD de Tabela de Juros ==========

    /**
     * Lista todas as faixas de juros da oficina.
     */
    @Transactional(readOnly = true)
    public List<TabelaJurosDTO> listarFaixasJuros() {
        UUID oficinaId = TenantContext.getTenantId();
        List<TabelaJurosParcelamento> faixas = tabelaJurosRepository
            .findByOficinaIdOrderByParcelasMinimoAsc(oficinaId);

        return faixas.stream()
            .map(this::toTabelaJurosDTO)
            .toList();
    }

    /**
     * Cria uma nova faixa de juros.
     */
    @Transactional
    public TabelaJurosDTO criarFaixaJuros(TabelaJurosRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        // Validar faixa
        if (request.getParcelasMinimo() > request.getParcelasMaximo()) {
            throw new IllegalArgumentException("Parcelas mínimas não pode ser maior que máximas");
        }

        // Verificar sobreposição
        boolean temSobreposicao = tabelaJurosRepository.existeSobreposicao(
            oficinaId,
            request.getParcelasMinimo(),
            request.getParcelasMaximo(),
            UUID.randomUUID() // ID inexistente para nova faixa
        );

        if (temSobreposicao) {
            throw new IllegalArgumentException("Já existe uma faixa de parcelas que sobrepõe este intervalo");
        }

        TabelaJurosParcelamento faixa = TabelaJurosParcelamento.builder()
            .parcelasMinimo(request.getParcelasMinimo())
            .parcelasMaximo(request.getParcelasMaximo())
            .percentualJuros(request.getPercentualJuros())
            .tipoJuros(request.getTipoJuros())
            .repassarCliente(request.getRepassarCliente())
            .ativo(request.getAtivo())
            .build();

        faixa = tabelaJurosRepository.save(faixa);
        log.info("Faixa de juros criada: {}x a {}x com {}%",
            request.getParcelasMinimo(), request.getParcelasMaximo(), request.getPercentualJuros());

        return toTabelaJurosDTO(faixa);
    }

    /**
     * Atualiza uma faixa de juros.
     */
    @Transactional
    public TabelaJurosDTO atualizarFaixaJuros(UUID id, TabelaJurosRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        TabelaJurosParcelamento faixa = tabelaJurosRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new IllegalArgumentException("Faixa de juros não encontrada"));

        // Validar faixa
        if (request.getParcelasMinimo() > request.getParcelasMaximo()) {
            throw new IllegalArgumentException("Parcelas mínimas não pode ser maior que máximas");
        }

        // Verificar sobreposição (excluindo a própria faixa)
        boolean temSobreposicao = tabelaJurosRepository.existeSobreposicao(
            oficinaId,
            request.getParcelasMinimo(),
            request.getParcelasMaximo(),
            id
        );

        if (temSobreposicao) {
            throw new IllegalArgumentException("Já existe uma faixa de parcelas que sobrepõe este intervalo");
        }

        faixa.setParcelasMinimo(request.getParcelasMinimo());
        faixa.setParcelasMaximo(request.getParcelasMaximo());
        faixa.setPercentualJuros(request.getPercentualJuros());
        faixa.setTipoJuros(request.getTipoJuros());
        faixa.setRepassarCliente(request.getRepassarCliente());
        faixa.setAtivo(request.getAtivo());

        faixa = tabelaJurosRepository.save(faixa);
        log.info("Faixa de juros atualizada: {}", id);

        return toTabelaJurosDTO(faixa);
    }

    /**
     * Remove uma faixa de juros.
     */
    @Transactional
    public void removerFaixaJuros(UUID id) {
        UUID oficinaId = TenantContext.getTenantId();

        TabelaJurosParcelamento faixa = tabelaJurosRepository.findByIdAndOficinaId(id, oficinaId)
            .orElseThrow(() -> new IllegalArgumentException("Faixa de juros não encontrada"));

        tabelaJurosRepository.delete(faixa);
        log.info("Faixa de juros removida: {}", id);
    }

    // ========== CRUD de Configuração ==========

    /**
     * Busca configuração de parcelamento da oficina.
     */
    @Transactional(readOnly = true)
    public ConfiguracaoParcelamentoDTO buscarConfiguracao() {
        UUID oficinaId = TenantContext.getTenantId();

        ConfiguracaoParcelamento config = configuracaoRepository.findByOficinaId(oficinaId)
            .orElse(null);

        if (config == null) {
            // Retornar configuração padrão (não salva)
            config = criarConfiguracaoPadrao();
        }

        List<TabelaJurosParcelamento> faixas = tabelaJurosRepository
            .findByOficinaIdAndAtivoTrueOrderByParcelasMinimoAsc(oficinaId);

        return toConfiguracaoDTO(config, faixas);
    }

    /**
     * Salva ou atualiza configuração de parcelamento.
     */
    @Transactional
    public ConfiguracaoParcelamentoDTO salvarConfiguracao(ConfiguracaoParcelamentoRequestDTO request) {
        UUID oficinaId = TenantContext.getTenantId();

        ConfiguracaoParcelamento config = configuracaoRepository.findByOficinaId(oficinaId)
            .orElse(new ConfiguracaoParcelamento());

        // Setar oficina se novo
        if (config.getId() == null) {
            Oficina oficina = new Oficina();
            oficina.setId(oficinaId);
            config.setOficina(oficina);
        }

        config.setParcelasMaximas(request.getParcelasMaximas());
        config.setValorMinimoParcela(request.getValorMinimoParcela());
        config.setValorMinimoParcelamento(request.getValorMinimoParcelamento());
        config.setAceitaVisa(request.getAceitaVisa());
        config.setAceitaMastercard(request.getAceitaMastercard());
        config.setAceitaElo(request.getAceitaElo());
        config.setAceitaAmex(request.getAceitaAmex());
        config.setAceitaHipercard(request.getAceitaHipercard());
        config.setExibirValorTotal(request.getExibirValorTotal());
        config.setExibirJuros(request.getExibirJuros());
        config.setAtivo(request.getAtivo());

        config = configuracaoRepository.save(config);
        log.info("Configuração de parcelamento salva para oficina {}", oficinaId);

        List<TabelaJurosParcelamento> faixas = tabelaJurosRepository
            .findByOficinaIdAndAtivoTrueOrderByParcelasMinimoAsc(oficinaId);

        return toConfiguracaoDTO(config, faixas);
    }

    // ========== Métodos Auxiliares ==========

    private ConfiguracaoParcelamento criarConfiguracaoPadrao() {
        return ConfiguracaoParcelamento.builder()
            .parcelasMaximas(12)
            .valorMinimoParcela(new BigDecimal("50.00"))
            .valorMinimoParcelamento(new BigDecimal("100.00"))
            .aceitaVisa(true)
            .aceitaMastercard(true)
            .aceitaElo(true)
            .aceitaAmex(true)
            .aceitaHipercard(true)
            .exibirValorTotal(true)
            .exibirJuros(true)
            .ativo(true)
            .build();
    }

    private TabelaJurosDTO toTabelaJurosDTO(TabelaJurosParcelamento entity) {
        String descricaoFaixa;
        if (entity.isSemJuros()) {
            descricaoFaixa = entity.getParcelasMinimo() + "x a " + entity.getParcelasMaximo() + "x sem juros";
        } else {
            descricaoFaixa = entity.getParcelasMinimo() + "x a " + entity.getParcelasMaximo() +
                "x com " + entity.getPercentualJuros() + "% a.m.";
        }

        return TabelaJurosDTO.builder()
            .id(entity.getId())
            .parcelasMinimo(entity.getParcelasMinimo())
            .parcelasMaximo(entity.getParcelasMaximo())
            .percentualJuros(entity.getPercentualJuros())
            .tipoJuros(entity.getTipoJuros())
            .tipoJurosDescricao(entity.getTipoJuros().getNome())
            .repassarCliente(entity.getRepassarCliente())
            .ativo(entity.getAtivo())
            .descricaoFaixa(descricaoFaixa)
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private ConfiguracaoParcelamentoDTO toConfiguracaoDTO(
            ConfiguracaoParcelamento config,
            List<TabelaJurosParcelamento> faixas) {

        List<String> bandeirasAceitas = new ArrayList<>();
        if (Boolean.TRUE.equals(config.getAceitaVisa())) bandeirasAceitas.add("Visa");
        if (Boolean.TRUE.equals(config.getAceitaMastercard())) bandeirasAceitas.add("Mastercard");
        if (Boolean.TRUE.equals(config.getAceitaElo())) bandeirasAceitas.add("Elo");
        if (Boolean.TRUE.equals(config.getAceitaAmex())) bandeirasAceitas.add("American Express");
        if (Boolean.TRUE.equals(config.getAceitaHipercard())) bandeirasAceitas.add("Hipercard");

        return ConfiguracaoParcelamentoDTO.builder()
            .id(config.getId())
            .parcelasMaximas(config.getParcelasMaximas())
            .valorMinimoParcela(config.getValorMinimoParcela())
            .valorMinimoParcelamento(config.getValorMinimoParcelamento())
            .aceitaVisa(config.getAceitaVisa())
            .aceitaMastercard(config.getAceitaMastercard())
            .aceitaElo(config.getAceitaElo())
            .aceitaAmex(config.getAceitaAmex())
            .aceitaHipercard(config.getAceitaHipercard())
            .exibirValorTotal(config.getExibirValorTotal())
            .exibirJuros(config.getExibirJuros())
            .ativo(config.getAtivo())
            .createdAt(config.getCreatedAt())
            .updatedAt(config.getUpdatedAt())
            .faixasJuros(faixas.stream().map(this::toTabelaJurosDTO).toList())
            .bandeirasAceitas(bandeirasAceitas)
            .build();
    }
}
