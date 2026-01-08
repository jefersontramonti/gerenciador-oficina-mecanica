package com.pitstop.ia.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pitstop.ia.dto.ClassificacaoProblema;
import com.pitstop.ia.dto.DiagnosticoIAResponse;
import com.pitstop.ia.dto.DiagnosticoIAResponse.*;
import com.pitstop.ia.dto.HistoricoComprimido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service cliente para comunicação com a API da Anthropic (Claude).
 * Implementa chamadas HTTP diretas para máximo controle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnthropicClientService {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    // ===== PROMPTS =====

    // ===== PROMPTS OTIMIZADOS (Pipe-Separated Format - 60-70% economia de tokens) =====

    private static final String PROMPT_CLASSIFICACAO = """
        Classifique: SIMPLES (rotina: óleo,freio,bateria,pneus) ou COMPLEXO (investigação: múltiplos sistemas)
        Problema: %s
        Formato: CLASSIFICACAO:SIMPLES/COMPLEXO|CATEGORIA:motor/freio/suspensao/eletrico/transmissao/arrefecimento/direcao/outro
        """;

    private static final String PROMPT_DIAGNOSTICO_SIMPLES = """
        Veículo: %s
        Problema: %s

        FORMATO OBRIGATÓRIO (pipe-separated PURO, será parseado por regex):

        RESUMO: [texto em UMA linha]
        CAUSAS: [causa]|[0-100]|[ALTA/MEDIA/BAIXA]; [causa2]|[prob]|[grav]
        ACOES: [acao1]; [acao2]; [acao3]
        PECAS: [peca]|[precoMin]-[precoMax]|[ALTA/MEDIA/BAIXA]; [peca2]|[preco]|[urg]
        TEMPO: [horasMin]-[horasMax]
        CUSTO: [reaisMin]-[reaisMax]

        REGRAS: Use | entre campos, ; entre itens. Números SEM símbolos (75 não 75%%, 1200 não R$1200).
        NUNCA use JSON, aspas ou texto extra. TODAS as linhas são OBRIGATÓRIAS.

        COMECE COM "RESUMO:" AGORA:
        """;

    private static final String PROMPT_DIAGNOSTICO_COMPLEXO = """
        %s
        Problema atual: %s

        FORMATO OBRIGATÓRIO (pipe-separated PURO, será parseado por regex):

        RESUMO: [diagnóstico técnico em UMA linha]
        CAUSAS: [causa]|[0-100]|[ALTA/MEDIA/BAIXA]; [causa2]|[prob]|[grav]; [causa3]|[prob]|[grav]
        ACOES: [acao1]; [acao2]; [acao3]; [acao4]
        PECAS: [peca]|[precoMin]-[precoMax]|[ALTA/MEDIA/BAIXA]; [peca2]|[preco]|[urg]
        TEMPO: [horasMin]-[horasMax]
        CUSTO: [reaisMin]-[reaisMax]
        OBS: [observações do histórico em UMA linha]

        REGRAS: Use | entre campos, ; entre itens. Números SEM símbolos (75 não 75%%, 1200 não R$1200).
        NUNCA use JSON, aspas ou texto extra. TODAS as linhas são OBRIGATÓRIAS.

        COMECE COM "RESUMO:" AGORA:
        """;

    /**
     * Classifica a complexidade do problema usando Haiku.
     */
    public ClassificacaoProblema classificarProblema(String apiKey, String modeloHaiku, String problema) {
        log.debug("Classificando problema com Haiku: {}", truncar(problema, 50));

        String prompt = String.format(PROMPT_CLASSIFICACAO, problema);
        String resposta = chamarAPI(apiKey, modeloHaiku, prompt, 100);

        return parseClassificacao(resposta);
    }

    /**
     * Gera diagnóstico simples usando Haiku.
     */
    public ResultadoIA gerarDiagnosticoSimples(String apiKey, String modeloHaiku,
                                                String problema, String dadosVeiculo, int maxTokens) {
        log.debug("Gerando diagnóstico simples com Haiku");

        // Escapa % para evitar erros no String.format
        String prompt = String.format(PROMPT_DIAGNOSTICO_SIMPLES,
                escaparParaFormat(dadosVeiculo), escaparParaFormat(problema));
        long inicio = System.currentTimeMillis();

        String resposta = chamarAPI(apiKey, modeloHaiku, prompt, maxTokens);
        long tempoMs = System.currentTimeMillis() - inicio;

        DiagnosticoIAResponse diagnostico = parseDiagnostico(resposta, Origem.IA_HAIKU, modeloHaiku, tempoMs);

        // Estima tokens (aproximado)
        int tokensEstimados = (prompt.length() + resposta.length()) / 4;
        BigDecimal custoEstimado = calcularCusto(modeloHaiku, tokensEstimados);

        return new ResultadoIA(diagnostico, tokensEstimados, custoEstimado);
    }

    /**
     * Gera diagnóstico complexo usando Sonnet.
     */
    public ResultadoIA gerarDiagnosticoComplexo(String apiKey, String modeloSonnet,
                                                 String problema, HistoricoComprimido historico, int maxTokens) {
        log.debug("Gerando diagnóstico complexo com Sonnet");

        String contexto = historico != null ? historico.toFormatoCompacto() : "Histórico não disponível";
        // Escapa % para evitar erros no String.format
        String prompt = String.format(PROMPT_DIAGNOSTICO_COMPLEXO,
                escaparParaFormat(contexto), escaparParaFormat(problema));
        long inicio = System.currentTimeMillis();

        String resposta = chamarAPI(apiKey, modeloSonnet, prompt, maxTokens);
        long tempoMs = System.currentTimeMillis() - inicio;

        DiagnosticoIAResponse diagnostico = parseDiagnostico(resposta, Origem.IA_SONNET, modeloSonnet, tempoMs);

        int tokensEstimados = (prompt.length() + resposta.length()) / 4;
        BigDecimal custoEstimado = calcularCusto(modeloSonnet, tokensEstimados);

        return new ResultadoIA(diagnostico, tokensEstimados, custoEstimado);
    }

    /**
     * Chama a API da Anthropic.
     */
    private String chamarAPI(String apiKey, String modelo, String prompt, int maxTokens) {
        try {
            AnthropicRequest request = new AnthropicRequest(
                    modelo,
                    maxTokens,
                    List.of(new Message("user", prompt))
            );

            String jsonBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ANTHROPIC_API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                // Sanitiza resposta para não vazar informações sensíveis nos logs
                String errorMsg = sanitizeErrorResponse(response.body());
                log.error("Erro na API Anthropic: {} - {}", response.statusCode(), errorMsg);
                throw new RuntimeException("Erro na API Anthropic: " + response.statusCode());
            }

            AnthropicResponse anthropicResponse = objectMapper.readValue(response.body(), AnthropicResponse.class);

            if (anthropicResponse.content != null && !anthropicResponse.content.isEmpty()) {
                return anthropicResponse.content.get(0).text;
            }

            throw new RuntimeException("Resposta vazia da API Anthropic");

        } catch (Exception e) {
            // Não loga a exception completa para evitar vazamento de API key em stack traces
            log.error("Erro ao chamar API Anthropic: {}", e.getMessage());
            throw new RuntimeException("Erro ao chamar API Anthropic: " + e.getMessage(), e);
        }
    }

    /**
     * Sanitiza resposta de erro para não vazar informações sensíveis nos logs.
     */
    private String sanitizeErrorResponse(String body) {
        if (body == null) return "null";
        // Remove possíveis tokens/keys da resposta de erro
        String sanitized = body.replaceAll("(api[_-]?key|token|authorization)[\"']?\\s*[:=]\\s*[\"']?[^\"',}\\s]+", "$1=***REDACTED***");
        // Limita tamanho para evitar logs enormes
        return sanitized.length() > 500 ? sanitized.substring(0, 500) + "..." : sanitized;
    }

    /**
     * Parse da resposta de classificação (formato compacto).
     */
    private ClassificacaoProblema parseClassificacao(String resposta) {
        ClassificacaoProblema.Complexidade complexidade = ClassificacaoProblema.Complexidade.SIMPLES;
        String categoria = "outro";

        // Parse formato: CLASSIFICACAO:COMPLEXO|CATEGORIA:motor
        String[] partes = resposta.replace("\n", "|").split("\\|");
        for (String parte : partes) {
            parte = parte.trim().toUpperCase();
            if (parte.startsWith("CLASSIFICACAO:") && parte.contains("COMPLEXO")) {
                complexidade = ClassificacaoProblema.Complexidade.COMPLEXO;
            } else if (parte.startsWith("CATEGORIA:")) {
                categoria = parte.substring(10).trim().toLowerCase();
            }
        }

        return new ClassificacaoProblema(complexidade, categoria, "");
    }

    /**
     * Parse da resposta de diagnóstico (formato pipe-separated otimizado).
     */
    private DiagnosticoIAResponse parseDiagnostico(String resposta, Origem origem, String modelo, long tempoMs) {
        String resumo = "";
        List<CausaPossivel> causas = new ArrayList<>();
        List<String> acoes = new ArrayList<>();
        List<PecaProvavel> pecas = new ArrayList<>();
        String tempo = "A definir";
        FaixaCusto custo = new FaixaCusto(BigDecimal.ZERO, BigDecimal.ZERO, "BRL");

        for (String linha : resposta.split("\n")) {
            linha = linha.trim();

            // Suporta formato antigo (RESUMO:) e novo (R:)
            if (linha.startsWith("R:") || linha.startsWith("RESUMO:")) {
                resumo = linha.substring(linha.indexOf(':') + 1).trim();
            } else if (linha.startsWith("C:") || linha.startsWith("CAUSAS:")) {
                causas = parseCausas(linha.substring(linha.indexOf(':') + 1).trim());
            } else if (linha.startsWith("A:") || linha.startsWith("ACOES:")) {
                acoes = parseAcoes(linha.substring(linha.indexOf(':') + 1).trim());
            } else if (linha.startsWith("P:") || linha.startsWith("PECAS:")) {
                pecas = parsePecas(linha.substring(linha.indexOf(':') + 1).trim());
            } else if (linha.startsWith("T:") || linha.startsWith("TEMPO:")) {
                tempo = linha.substring(linha.indexOf(':') + 1).trim();
            } else if (linha.startsWith("$:") || linha.startsWith("CUSTO:")) {
                custo = parseCusto(linha.substring(linha.indexOf(':') + 1).trim());
            }
        }

        MetadadosDiagnostico metadados = new MetadadosDiagnostico(
                origem, modelo, 0, BigDecimal.ZERO, tempoMs
        );

        return new DiagnosticoIAResponse(resumo, causas, acoes, pecas, tempo, custo, metadados);
    }

    private List<CausaPossivel> parseCausas(String texto) {
        List<CausaPossivel> causas = new ArrayList<>();
        for (String causa : texto.split(";")) {
            String[] partes = causa.trim().split("\\|");
            if (partes.length >= 2) {
                String descricao = partes[0].trim();
                int probabilidade = extrairNumero(partes[1], 50);
                Gravidade gravidade = partes.length >= 3
                        ? parseGravidade(partes[2].trim())
                        : Gravidade.MEDIA;
                causas.add(new CausaPossivel(descricao, probabilidade, gravidade));
            }
        }
        return causas;
    }

    private List<String> parseAcoes(String texto) {
        return Arrays.stream(texto.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<PecaProvavel> parsePecas(String texto) {
        List<PecaProvavel> pecas = new ArrayList<>();
        for (String peca : texto.split(";")) {
            String[] partes = peca.trim().split("\\|");
            if (partes.length >= 2) {
                String nome = partes[0].trim();
                // Novo formato: [peca]|[precoMin]-[precoMax]|[urgencia]
                // Antigo formato: [peca]|[cod]|[urg]|[custo]
                BigDecimal custoEst = BigDecimal.ZERO;
                Urgencia urgencia = Urgencia.MEDIA;
                String codigo = "";

                if (partes.length == 3) {
                    // Novo formato: nome|preco|urgencia
                    custoEst = new BigDecimal(extrairNumero(partes[1], 0));
                    urgencia = parseUrgencia(partes[2].trim());
                } else if (partes.length >= 4) {
                    // Antigo formato: nome|codigo|urgencia|custo
                    codigo = partes[1].trim();
                    urgencia = parseUrgencia(partes[2].trim());
                    custoEst = new BigDecimal(extrairNumero(partes[3], 0));
                } else {
                    // Formato simples: nome|urgencia
                    urgencia = parseUrgencia(partes[1].trim());
                }
                pecas.add(new PecaProvavel(nome, codigo, urgencia, custoEst));
            }
        }
        return pecas;
    }

    private FaixaCusto parseCusto(String texto) {
        Pattern pattern = Pattern.compile("(\\d+)[^\\d]*(\\d+)?");
        Matcher matcher = pattern.matcher(texto.replaceAll("[^\\d-]", " "));

        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;

        if (matcher.find()) {
            min = new BigDecimal(matcher.group(1));
            max = matcher.group(2) != null ? new BigDecimal(matcher.group(2)) : min;
        }

        return new FaixaCusto(min, max, "BRL");
    }

    private Gravidade parseGravidade(String texto) {
        texto = texto.toUpperCase().trim();
        // Suporta formato curto (C/A/M/B) e longo (CRITICA/ALTA/MEDIA/BAIXA)
        if (texto.equals("C") || texto.contains("CRIT")) return Gravidade.CRITICA;
        if (texto.equals("A") || texto.contains("ALTA")) return Gravidade.ALTA;
        if (texto.equals("B") || texto.contains("BAIX")) return Gravidade.BAIXA;
        return Gravidade.MEDIA;
    }

    private Urgencia parseUrgencia(String texto) {
        texto = texto.toUpperCase().trim();
        // Suporta formato curto (I/A/M/B) e longo (IMEDIATA/ALTA/MEDIA/BAIXA)
        if (texto.equals("I") || texto.contains("IMED")) return Urgencia.IMEDIATA;
        if (texto.equals("A") || texto.contains("ALTA")) return Urgencia.ALTA;
        if (texto.equals("B") || texto.contains("BAIX")) return Urgencia.BAIXA;
        return Urgencia.MEDIA;
    }

    private int extrairNumero(String texto, int padrao) {
        try {
            // Se for um range "850-1200", pega só o primeiro número
            String limpo = texto.replaceAll("[^\\d\\-]", "");
            if (limpo.contains("-") && limpo.indexOf("-") > 0) {
                limpo = limpo.substring(0, limpo.indexOf("-"));
            }
            return Integer.parseInt(limpo.replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            return padrao;
        }
    }

    private BigDecimal calcularCusto(String modelo, int tokens) {
        // Preços aproximados por 1M tokens (2024)
        double precoPorMilhao = modelo.contains("haiku") ? 0.25 : 3.0;
        return BigDecimal.valueOf(tokens * precoPorMilhao / 1_000_000);
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max) + "..." : texto;
    }

    /**
     * Escapa caracteres % para uso seguro em String.format()
     */
    private String escaparParaFormat(String texto) {
        if (texto == null) return "";
        return texto.replace("%", "%%");
    }

    // ===== CLASSES INTERNAS PARA JSON =====

    public record ResultadoIA(
            DiagnosticoIAResponse diagnostico,
            int tokensConsumidos,
            BigDecimal custoEstimado
    ) {}

    private record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            List<Message> messages
    ) {}

    private record Message(String role, String content) {}

    private static class AnthropicResponse {
        public List<ContentBlock> content;
        public Usage usage;
    }

    private static class ContentBlock {
        public String type;
        public String text;
    }

    private static class Usage {
        @JsonProperty("input_tokens")
        public int inputTokens;
        @JsonProperty("output_tokens")
        public int outputTokens;
    }
}
