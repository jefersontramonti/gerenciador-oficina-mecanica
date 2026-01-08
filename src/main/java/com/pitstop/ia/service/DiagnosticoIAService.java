package com.pitstop.ia.service;

import com.pitstop.ia.domain.ConfiguracaoIA;
import com.pitstop.ia.dto.*;
import com.pitstop.ia.dto.DiagnosticoIAResponse.*;
import com.pitstop.ia.repository.ConfiguracaoIARepository;
import com.pitstop.shared.security.tenant.TenantContext;
import com.pitstop.veiculo.domain.Veiculo;
import com.pitstop.veiculo.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.UUID;

/**
 * Service principal para geração de diagnósticos assistidos por IA.
 * Orquestra todo o fluxo: pré-validação, cache, roteamento de modelos e compressão de contexto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosticoIAService {

    private final ConfiguracaoIARepository configuracaoRepository;
    private final ConfiguracaoIAService configuracaoService;
    private final PreValidacaoService preValidacaoService;
    private final ContextCompressionService compressionService;
    private final AnthropicClientService anthropicClient;
    private final CriptografiaService criptografiaService;
    private final VeiculoRepository veiculoRepository;

    /**
     * Gera diagnóstico para o problema relatado.
     * Aplica otimizações: pré-validação, cache, roteamento inteligente.
     *
     * @param request Requisição com problema e veículo
     * @return Diagnóstico gerado
     */
    @Transactional
    public DiagnosticoIAResponse gerarDiagnostico(DiagnosticoIARequest request) {
        UUID oficinaId = TenantContext.getTenantId();
        log.info("Gerando diagnóstico para oficina {} - veículo {}", oficinaId, request.veiculoId());

        // 1. Busca configuração da oficina
        ConfiguracaoIA config = configuracaoRepository.findByOficinaId(oficinaId)
                .orElseThrow(() -> new IllegalStateException("IA não configurada para esta oficina"));

        if (!config.isConfigurada()) {
            throw new IllegalStateException("IA não está habilitada ou API key não configurada");
        }

        if (!config.podeRequisitar()) {
            throw new IllegalStateException("Limite diário de requisições atingido");
        }

        String problema = request.problemasRelatados().trim();

        // 2. Tenta pré-validação com templates
        if (config.getUsarPreValidacao()) {
            Optional<DiagnosticoIAResponse> template = preValidacaoService.tentarResolverSemIA(problema);
            if (template.isPresent()) {
                log.info("Diagnóstico resolvido por template - economia de API");
                config.incrementarTemplateHit();
                configuracaoRepository.save(config);
                return template.get();
            }
        }

        // 3. Tenta buscar do cache
        if (config.getUsarCache()) {
            Optional<DiagnosticoIAResponse> cached = buscarCache(problema, request.veiculoId());
            if (cached.isPresent()) {
                log.info("Diagnóstico encontrado no cache - economia de API");
                config.incrementarCacheHit();
                configuracaoRepository.save(config);
                return cached.get();
            }
        }

        // 4. Busca dados do veículo
        Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, request.veiculoId())
                .orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado"));

        String dadosVeiculo = formatarDadosVeiculo(veiculo);

        // 5. Descriptografa API key
        String apiKey = criptografiaService.decrypt(config.getApiKeyEncrypted());

        // 6. Roteamento inteligente ou diagnóstico direto
        DiagnosticoIAResponse diagnostico;
        int tokensUsados;
        BigDecimal custo;

        if (config.getUsarRoteamentoInteligente()) {
            diagnostico = diagnosticarComRoteamento(config, apiKey, problema, dadosVeiculo, request.veiculoId());
        } else {
            // Usa modelo padrão (Haiku) direto
            AnthropicClientService.ResultadoIA resultado = anthropicClient.gerarDiagnosticoSimples(
                    apiKey,
                    config.getModeloPadrao(),
                    problema,
                    dadosVeiculo,
                    config.getMaxTokensResposta()
            );
            diagnostico = resultado.diagnostico();
            tokensUsados = resultado.tokensConsumidos();
            custo = resultado.custoEstimado();

            // Atualiza estatísticas
            config.incrementarRequisicoes(tokensUsados, custo);
            configuracaoRepository.save(config);
        }

        log.info("Diagnóstico gerado com sucesso para oficina {} via {}",
                oficinaId, diagnostico.metadados().origem());

        return diagnostico;
    }

    /**
     * Diagnóstico com roteamento inteligente.
     * Haiku classifica, depois roteia para modelo adequado.
     */
    private DiagnosticoIAResponse diagnosticarComRoteamento(ConfiguracaoIA config, String apiKey,
                                                            String problema, String dadosVeiculo,
                                                            UUID veiculoId) {
        // 1. Classifica com Haiku (barato, rápido)
        ClassificacaoProblema classificacao = anthropicClient.classificarProblema(
                apiKey,
                config.getModeloPadrao(),
                problema
        );

        log.debug("Problema classificado como: {} ({})", classificacao.complexidade(), classificacao.categoria());

        AnthropicClientService.ResultadoIA resultado;

        if (classificacao.complexidade() == ClassificacaoProblema.Complexidade.SIMPLES) {
            // 2a. Problema simples -> Haiku resolve
            resultado = anthropicClient.gerarDiagnosticoSimples(
                    apiKey,
                    config.getModeloPadrao(),
                    problema,
                    dadosVeiculo,
                    config.getMaxTokensResposta()
            );
        } else {
            // 2b. Problema complexo -> Sonnet + histórico comprimido
            HistoricoComprimido historico = compressionService.comprimirHistorico(veiculoId);

            resultado = anthropicClient.gerarDiagnosticoComplexo(
                    apiKey,
                    config.getModeloAvancado(),
                    problema,
                    historico,
                    config.getMaxTokensResposta()
            );
        }

        // 3. Atualiza estatísticas
        config.incrementarRequisicoes(resultado.tokensConsumidos(), resultado.custoEstimado());
        configuracaoRepository.save(config);

        return resultado.diagnostico();
    }

    /**
     * Busca diagnóstico no cache baseado em problema normalizado.
     */
    @Cacheable(value = "diagnostico-cache", key = "#root.target.gerarCacheKey(#problema, #veiculoId)")
    public Optional<DiagnosticoIAResponse> buscarCache(String problema, UUID veiculoId) {
        // O cache é gerenciado pelo Spring Cache
        // Se chegou aqui, é cache miss
        return Optional.empty();
    }

    /**
     * Gera chave de cache baseada em problema normalizado e características do veículo.
     */
    public String gerarCacheKey(String problema, UUID veiculoId) {
        UUID oficinaId = TenantContext.getTenantId();

        // Normaliza problema (remove números, datas, horários)
        String problemaNormalizado = problema.toLowerCase()
                .replaceAll("\\d+", "N")           // Números -> N
                .replaceAll("\\d{2}/\\d{2}", "DATA") // Datas
                .replaceAll("\\d{2}:\\d{2}", "HORA") // Horários
                .replaceAll("\\s+", " ")           // Múltiplos espaços
                .trim();

        // Busca dados do veículo para adicionar à chave
        Veiculo veiculo = veiculoRepository.findByOficinaIdAndId(oficinaId, veiculoId).orElse(null);

        String contextoVeiculo = "";
        if (veiculo != null) {
            // Agrupa por marca/modelo e faixa de quilometragem
            int faixaKm = (veiculo.getQuilometragem() / 50000) * 50000; // Faixas de 50k
            contextoVeiculo = String.format("%s|%s|%d", veiculo.getMarca(), veiculo.getModelo(), faixaKm);
        }

        // Gera hash
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = problemaNormalizado + "|" + contextoVeiculo + "|" + oficinaId;
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // Usa apenas primeiros 16 chars
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return problema.hashCode() + "-" + veiculoId.toString().substring(0, 8);
        }
    }

    /**
     * Formata dados do veículo para o prompt.
     */
    private String formatarDadosVeiculo(Veiculo veiculo) {
        return String.format("%s %s %d - %d km",
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getAno(),
                veiculo.getQuilometragem() != null ? veiculo.getQuilometragem() : 0
        );
    }

    /**
     * Verifica se a IA está disponível para a oficina atual.
     */
    @Transactional(readOnly = true)
    public boolean isIADisponivel() {
        UUID oficinaId = TenantContext.getTenantId();
        return configuracaoRepository.findByOficinaId(oficinaId)
                .map(ConfiguracaoIA::isConfigurada)
                .orElse(false);
    }

    /**
     * Retorna estatísticas de uso da IA.
     */
    @Transactional(readOnly = true)
    public EstatisticasUsoIA getEstatisticas() {
        UUID oficinaId = TenantContext.getTenantId();
        ConfiguracaoIA config = configuracaoRepository.findByOficinaId(oficinaId)
                .orElseThrow(() -> new IllegalStateException("Configuração não encontrada"));

        double taxaEconomia = 0;
        if (config.getTotalRequisicoes() > 0) {
            long economizadas = config.getTotalCacheHits() + config.getTotalTemplateHits();
            taxaEconomia = (double) economizadas / config.getTotalRequisicoes() * 100;
        }

        return new EstatisticasUsoIA(
                config.getTotalRequisicoes(),
                config.getTotalCacheHits(),
                config.getTotalTemplateHits(),
                config.getTotalTokensConsumidos(),
                config.getCustoEstimadoTotal(),
                taxaEconomia,
                config.getRequisicoesHoje(),
                config.getMaxRequisicoesDia() - config.getRequisicoesHoje()
        );
    }

    /**
     * DTO para estatísticas de uso.
     */
    public record EstatisticasUsoIA(
            Long totalRequisicoes,
            Long cacheHits,
            Long templateHits,
            Long tokensConsumidos,
            BigDecimal custoTotal,
            Double taxaEconomia,
            Integer requisicoesHoje,
            Integer requisicoesRestantes
    ) {}
}
