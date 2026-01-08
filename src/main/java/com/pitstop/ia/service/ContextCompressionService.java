package com.pitstop.ia.service;

import com.pitstop.ia.dto.HistoricoComprimido;
import com.pitstop.ia.dto.HistoricoComprimido.*;
import com.pitstop.ordemservico.domain.OrdemServico;
import com.pitstop.ordemservico.repository.OrdemServicoRepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service para comprimir histórico de veículos para envio à IA.
 * Reduz tokens mantendo informações relevantes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextCompressionService {

    private final VeiculoRepository veiculoRepository;
    private final OrdemServicoRepository ordemServicoRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final int MAX_OS_RECENTES = 2;

    // Palavras-chave para categorização
    private static final List<Pattern> PATTERNS_MOTOR = List.of(
            Pattern.compile("motor|óleo|vela|correia|ignição|injeção|bomba", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> PATTERNS_FREIO = List.of(
            Pattern.compile("freio|pastilha|disco|abs|pinça", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> PATTERNS_SUSPENSAO = List.of(
            Pattern.compile("suspensão|amortecedor|mola|bucha|pivô|terminal", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> PATTERNS_ELETRICO = List.of(
            Pattern.compile("elétric|bateria|alternador|farol|luz|sensor|módulo", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Comprime histórico do veículo para formato otimizado.
     * Cache de 1 hora para evitar reprocessamento.
     *
     * @param veiculoId ID do veículo
     * @return Histórico comprimido
     */
    // Cache temporariamente desabilitado devido a problemas de serialização
    // @Cacheable(value = "historico-comprimido-v2", key = "#veiculoId", unless = "#result == null")
    @Transactional(readOnly = true)
    public HistoricoComprimido comprimirHistorico(UUID veiculoId) {
        log.debug("Comprimindo histórico do veículo: {}", veiculoId);

        UUID oficinaId = TenantContext.getTenantId();

        // Busca dados do veículo
        Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, veiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado"));

        // Busca OS do veículo (usando paginação para pegar as mais recentes)
        List<OrdemServico> todasOS = ordemServicoRepository.findHistoricoVeiculoByOficinaId(
                oficinaId, veiculoId, org.springframework.data.domain.Pageable.unpaged()).getContent();

        // Extrai OS recentes (últimas 2)
        List<OSRecente> osRecentes = todasOS.stream()
                .limit(MAX_OS_RECENTES)
                .map(this::converterParaOSRecente)
                .toList();

        // Gera resumo estatístico
        ResumoEstatistico resumo = gerarResumoEstatistico(todasOS);

        // Monta dados do veículo
        DadosVeiculo dadosVeiculo = new DadosVeiculo(
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getQuilometragem(),
                "N/I" // Veículo não tem campo combustível no sistema atual
        );

        HistoricoComprimido historico = new HistoricoComprimido(dadosVeiculo, osRecentes, resumo);
        log.info("Histórico comprimido para veículo {}: {} OS recentes, {} total",
                veiculoId, osRecentes.size(), resumo.totalOS());

        return historico;
    }

    /**
     * Converte uma OS para formato resumido.
     */
    private OSRecente converterParaOSRecente(OrdemServico os) {
        String data = os.getDataAbertura() != null
                ? os.getDataAbertura().format(DATE_FORMATTER)
                : "N/I";

        // Trunca problema e diagnóstico para economizar tokens
        String problema = truncar(os.getProblemasRelatados(), 100);
        String diagnostico = truncar(os.getDiagnostico(), 80);

        // Extrai solução dos itens da OS (simplificado)
        String solucao = extrairSolucaoResumida(os);

        return new OSRecente(data, problema, diagnostico, solucao);
    }

    /**
     * Extrai solução resumida dos itens da OS.
     */
    private String extrairSolucaoResumida(OrdemServico os) {
        if (os.getItens() == null || os.getItens().isEmpty()) {
            return os.getDiagnostico() != null ? truncar(os.getDiagnostico(), 50) : "Sem registro";
        }

        // Pega os primeiros 3 itens como resumo
        return os.getItens().stream()
                .limit(3)
                .map(item -> item.getDescricao())
                .reduce((a, b) -> a + "; " + b)
                .map(s -> truncar(s, 100))
                .orElse("Serviços diversos");
    }

    /**
     * Gera resumo estatístico do histórico completo.
     */
    private ResumoEstatistico gerarResumoEstatistico(List<OrdemServico> ordens) {
        int motor = 0, freio = 0, suspensao = 0, eletrico = 0, outros = 0;
        String ultimaTrocaOleo = null;
        String ultimaRevisao = null;

        for (OrdemServico os : ordens) {
            String texto = (os.getProblemasRelatados() + " " +
                    (os.getDiagnostico() != null ? os.getDiagnostico() : "")).toLowerCase();

            // Categoriza
            if (matchAny(PATTERNS_MOTOR, texto)) motor++;
            else if (matchAny(PATTERNS_FREIO, texto)) freio++;
            else if (matchAny(PATTERNS_SUSPENSAO, texto)) suspensao++;
            else if (matchAny(PATTERNS_ELETRICO, texto)) eletrico++;
            else outros++;

            // Detecta última troca de óleo
            if (texto.contains("troca") && texto.contains("óleo") && ultimaTrocaOleo == null) {
                ultimaTrocaOleo = os.getDataAbertura() != null
                        ? os.getDataAbertura().format(DATE_FORMATTER) : null;
            }

            // Detecta última revisão
            if (texto.contains("revisão") && ultimaRevisao == null) {
                ultimaRevisao = os.getDataAbertura() != null
                        ? os.getDataAbertura().format(DATE_FORMATTER) : null;
            }
        }

        return new ResumoEstatistico(
                ordens.size(),
                motor, freio, suspensao, eletrico, outros,
                ultimaTrocaOleo, ultimaRevisao
        );
    }

    /**
     * Verifica se o texto faz match com algum padrão.
     */
    private boolean matchAny(List<Pattern> patterns, String texto) {
        return patterns.stream().anyMatch(p -> p.matcher(texto).find());
    }

    /**
     * Trunca texto para um tamanho máximo.
     */
    private String truncar(String texto, int maxLength) {
        if (texto == null) return "";
        texto = texto.trim().replaceAll("\\s+", " ");
        if (texto.length() <= maxLength) return texto;
        return texto.substring(0, maxLength - 3) + "...";
    }
}
