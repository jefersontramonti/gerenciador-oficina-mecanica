package com.pitstop.notificacao.integration.evolution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cliente para a Evolution API (WhatsApp).
 *
 * Documentacao: https://doc.evolution-api.com/
 *
 * @author PitStop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EvolutionApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Envia mensagem de texto simples.
     *
     * @param config Configuracao da Evolution API
     * @param numero Numero do destinatario (formato: 5511999999999)
     * @param mensagem Texto da mensagem
     * @return Resultado do envio
     */
    public EvolutionSendResult enviarTexto(
        EvolutionConfig config,
        String numero,
        String mensagem
    ) {
        String url = buildUrl(config, "/message/sendText/" + config.instanceName());

        Map<String, Object> body = Map.of(
            "number", formatarNumero(numero),
            "text", mensagem
        );

        return enviar(url, config.apiToken(), body);
    }

    /**
     * Envia mensagem com botoes.
     *
     * @param config Configuracao da Evolution API
     * @param numero Numero do destinatario
     * @param titulo Titulo da mensagem
     * @param descricao Descricao
     * @param rodape Rodape
     * @param botoes Lista de botoes
     * @return Resultado do envio
     */
    public EvolutionSendResult enviarComBotoes(
        EvolutionConfig config,
        String numero,
        String titulo,
        String descricao,
        String rodape,
        java.util.List<EvolutionButton> botoes
    ) {
        String url = buildUrl(config, "/message/sendButtons/" + config.instanceName());

        Map<String, Object> body = Map.of(
            "number", formatarNumero(numero),
            "title", titulo,
            "description", descricao,
            "footer", rodape,
            "buttons", botoes.stream()
                .map(b -> Map.of(
                    "type", "reply",
                    "displayText", b.texto(),
                    "id", b.id()
                ))
                .toList()
        );

        return enviar(url, config.apiToken(), body);
    }

    /**
     * Envia mensagem com lista de opcoes.
     *
     * @param config Configuracao da Evolution API
     * @param numero Numero do destinatario
     * @param titulo Titulo
     * @param descricao Descricao
     * @param textoBotao Texto do botao de abrir lista
     * @param secoes Secoes da lista
     * @return Resultado do envio
     */
    public EvolutionSendResult enviarLista(
        EvolutionConfig config,
        String numero,
        String titulo,
        String descricao,
        String textoBotao,
        java.util.List<EvolutionListSection> secoes
    ) {
        String url = buildUrl(config, "/message/sendList/" + config.instanceName());

        Map<String, Object> body = Map.of(
            "number", formatarNumero(numero),
            "title", titulo,
            "description", descricao,
            "buttonText", textoBotao,
            "sections", secoes.stream()
                .map(s -> Map.of(
                    "title", s.titulo(),
                    "rows", s.opcoes().stream()
                        .map(o -> Map.of(
                            "title", o.titulo(),
                            "description", o.descricao(),
                            "rowId", o.id()
                        ))
                        .toList()
                ))
                .toList()
        );

        return enviar(url, config.apiToken(), body);
    }

    /**
     * Envia documento/arquivo.
     *
     * @param config Configuracao da Evolution API
     * @param numero Numero do destinatario
     * @param mediaUrl URL do arquivo
     * @param fileName Nome do arquivo
     * @param caption Legenda opcional
     * @return Resultado do envio
     */
    public EvolutionSendResult enviarDocumento(
        EvolutionConfig config,
        String numero,
        String mediaUrl,
        String fileName,
        String caption
    ) {
        String url = buildUrl(config, "/message/sendMedia/" + config.instanceName());

        Map<String, Object> body = Map.of(
            "number", formatarNumero(numero),
            "mediatype", "document",
            "media", mediaUrl,
            "fileName", fileName,
            "caption", caption != null ? caption : ""
        );

        return enviar(url, config.apiToken(), body);
    }

    /**
     * Verifica status da instancia.
     *
     * @param config Configuracao da Evolution API
     * @return Status da instancia
     */
    public EvolutionInstanceStatus verificarStatus(EvolutionConfig config) {
        String url = buildUrl(config, "/instance/connectionState/" + config.instanceName());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", config.apiToken());

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String state = json.path("instance").path("state").asText("unknown");

                return new EvolutionInstanceStatus(
                    true,
                    state,
                    "open".equalsIgnoreCase(state),
                    null
                );
            }

            return new EvolutionInstanceStatus(false, "unknown", false, "Resposta invalida");

        } catch (HttpClientErrorException e) {
            log.error("Erro ao verificar status da instancia: {}", e.getMessage());
            return new EvolutionInstanceStatus(false, "error", false, e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao verificar status da instancia", e);
            return new EvolutionInstanceStatus(false, "error", false, e.getMessage());
        }
    }

    /**
     * Gera QR Code para conexao.
     *
     * @param config Configuracao da Evolution API
     * @return URL do QR Code ou null se ja conectado
     */
    public String gerarQrCode(EvolutionConfig config) {
        String url = buildUrl(config, "/instance/connect/" + config.instanceName());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", config.apiToken());

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                return json.path("base64").asText(null);
            }

            return null;

        } catch (Exception e) {
            log.error("Erro ao gerar QR Code", e);
            return null;
        }
    }

    // ===== METODOS AUXILIARES =====

    private EvolutionSendResult enviar(String url, String apiToken, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiToken);

            String jsonBody = objectMapper.writeValueAsString(body);
            log.debug("Enviando para Evolution API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(jsonBody, headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                // Extrai o message ID
                String messageId = json.path("key").path("id").asText(null);
                if (messageId == null) {
                    messageId = json.path("messageId").asText(null);
                }

                return EvolutionSendResult.sucesso(messageId, response.getBody());
            }

            return EvolutionSendResult.falha(
                "RESPONSE_ERROR",
                "Resposta inesperada: " + response.getStatusCode(),
                response.getBody()
            );

        } catch (HttpClientErrorException e) {
            log.error("Erro HTTP ao enviar via Evolution API: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());

            String codigo = "HTTP_" + e.getStatusCode().value();
            String mensagem = extrairMensagemErro(e.getResponseBodyAsString());

            return EvolutionSendResult.falha(codigo, mensagem, e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Erro ao enviar via Evolution API", e);
            return EvolutionSendResult.falha("EXCEPTION", e.getMessage(), null);
        }
    }

    private String buildUrl(EvolutionConfig config, String path) {
        String baseUrl = config.apiUrl().endsWith("/")
            ? config.apiUrl().substring(0, config.apiUrl().length() - 1)
            : config.apiUrl();
        return baseUrl + path;
    }

    private String formatarNumero(String numero) {
        // Remove caracteres nao numericos
        String limpo = numero.replaceAll("[^0-9]", "");

        // Adiciona codigo do pais se nao tiver
        if (!limpo.startsWith("55") && limpo.length() <= 11) {
            limpo = "55" + limpo;
        }

        return limpo;
    }

    private String extrairMensagemErro(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            return json.path("message").asText(
                json.path("error").asText("Erro desconhecido")
            );
        } catch (Exception e) {
            return responseBody;
        }
    }
}
