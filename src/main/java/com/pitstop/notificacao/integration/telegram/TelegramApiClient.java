package com.pitstop.notificacao.integration.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente para a Telegram Bot API.
 *
 * Documentacao: https://core.telegram.org/bots/api
 *
 * @author PitStop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Envia mensagem de texto simples.
     *
     * @param config Configuracao do Telegram
     * @param mensagem Texto da mensagem (suporta Markdown)
     * @return Resultado do envio
     */
    public TelegramSendResult enviarTexto(TelegramConfig config, String mensagem) {
        return enviarTexto(config, config.chatId(), mensagem);
    }

    /**
     * Envia mensagem de texto para um chat especifico.
     *
     * @param config Configuracao do Telegram
     * @param chatId ID do chat destino
     * @param mensagem Texto da mensagem (suporta Markdown)
     * @return Resultado do envio
     */
    public TelegramSendResult enviarTexto(TelegramConfig config, String chatId, String mensagem) {
        String url = config.getApiUrl("sendMessage");

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", mensagem);
        body.put("parse_mode", "Markdown");
        body.put("disable_web_page_preview", true);

        return enviar(url, body);
    }

    /**
     * Envia mensagem com botoes inline para o chat configurado.
     *
     * @param config Configuracao do Telegram
     * @param mensagem Texto da mensagem
     * @param botoes Lista de botoes (cada lista interna e uma linha)
     * @return Resultado do envio
     */
    public TelegramSendResult enviarComBotoes(
        TelegramConfig config,
        String mensagem,
        List<List<TelegramButton>> botoes
    ) {
        return enviarComBotoes(config, config.chatId(), mensagem, botoes);
    }

    /**
     * Envia mensagem com botoes inline para um chat especifico.
     *
     * @param config Configuracao do Telegram
     * @param chatId ID do chat destino
     * @param mensagem Texto da mensagem
     * @param botoes Lista de botoes (cada lista interna e uma linha)
     * @return Resultado do envio
     */
    public TelegramSendResult enviarComBotoes(
        TelegramConfig config,
        String chatId,
        String mensagem,
        List<List<TelegramButton>> botoes
    ) {
        String url = config.getApiUrl("sendMessage");

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", mensagem);
        body.put("parse_mode", "Markdown");

        // Inline keyboard
        List<List<Map<String, String>>> keyboard = botoes.stream()
            .map(linha -> linha.stream()
                .map(btn -> {
                    Map<String, String> btnMap = new HashMap<>();
                    btnMap.put("text", btn.texto());
                    if (btn.url() != null) {
                        btnMap.put("url", btn.url());
                    } else if (btn.callbackData() != null) {
                        btnMap.put("callback_data", btn.callbackData());
                    }
                    return btnMap;
                })
                .toList()
            )
            .toList();

        body.put("reply_markup", Map.of("inline_keyboard", keyboard));

        return enviar(url, body);
    }

    /**
     * Envia documento/arquivo via URL.
     *
     * @param config Configuracao do Telegram
     * @param documentUrl URL do documento
     * @param caption Legenda opcional
     * @return Resultado do envio
     */
    public TelegramSendResult enviarDocumento(
        TelegramConfig config,
        String documentUrl,
        String caption
    ) {
        String url = config.getApiUrl("sendDocument");

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", config.chatId());
        body.put("document", documentUrl);
        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
            body.put("parse_mode", "Markdown");
        }

        return enviar(url, body);
    }

    /**
     * Envia documento/arquivo via bytes (upload direto).
     * Usa multipart/form-data para enviar o arquivo binario.
     *
     * @param config Configuracao do Telegram
     * @param documentBytes Conteudo do documento em bytes
     * @param fileName Nome do arquivo
     * @param caption Legenda opcional
     * @return Resultado do envio
     */
    public TelegramSendResult enviarDocumentoBytes(
        TelegramConfig config,
        byte[] documentBytes,
        String fileName,
        String caption
    ) {
        String url = config.getApiUrl("sendDocument");

        try {
            // Usa multipart para enviar o arquivo
            org.springframework.util.LinkedMultiValueMap<String, Object> body =
                new org.springframework.util.LinkedMultiValueMap<>();

            body.add("chat_id", config.chatId());

            // Cria resource do arquivo
            org.springframework.core.io.ByteArrayResource fileResource =
                new org.springframework.core.io.ByteArrayResource(documentBytes) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                };

            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.APPLICATION_PDF);
            HttpEntity<org.springframework.core.io.ByteArrayResource> fileEntity =
                new HttpEntity<>(fileResource, fileHeaders);

            body.add("document", fileEntity);

            if (caption != null && !caption.isBlank()) {
                body.add("caption", caption);
                body.add("parse_mode", "Markdown");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            log.debug("Enviando documento para Telegram API: {} ({} bytes)", fileName, documentBytes.length);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                if (json.path("ok").asBoolean(false)) {
                    String messageId = String.valueOf(json.path("result").path("message_id").asLong());
                    log.info("Documento enviado com sucesso via Telegram: {} (message_id: {})", fileName, messageId);
                    return TelegramSendResult.sucesso(messageId, response.getBody());
                }

                String errorDescription = json.path("description").asText("Erro desconhecido");
                String errorCode = String.valueOf(json.path("error_code").asInt(0));
                return TelegramSendResult.falha(errorCode, errorDescription, response.getBody());
            }

            return TelegramSendResult.falha(
                "RESPONSE_ERROR",
                "Resposta inesperada: " + response.getStatusCode(),
                response.getBody()
            );

        } catch (HttpClientErrorException e) {
            log.error("Erro HTTP ao enviar documento via Telegram API: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());

            String codigo = "HTTP_" + e.getStatusCode().value();
            String mensagem = extrairMensagemErro(e.getResponseBodyAsString());

            return TelegramSendResult.falha(codigo, mensagem, e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Erro ao enviar documento via Telegram API: {}", e.getMessage(), e);
            return TelegramSendResult.falha("EXCEPTION", e.getMessage(), null);
        }
    }

    /**
     * Envia foto.
     *
     * @param config Configuracao do Telegram
     * @param photoUrl URL da foto
     * @param caption Legenda opcional
     * @return Resultado do envio
     */
    public TelegramSendResult enviarFoto(
        TelegramConfig config,
        String photoUrl,
        String caption
    ) {
        String url = config.getApiUrl("sendPhoto");

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", config.chatId());
        body.put("photo", photoUrl);
        if (caption != null && !caption.isBlank()) {
            body.put("caption", caption);
            body.put("parse_mode", "Markdown");
        }

        return enviar(url, body);
    }

    /**
     * Obtem informacoes do bot.
     *
     * @param config Configuracao do Telegram
     * @return Informacoes do bot ou null em caso de erro
     */
    public TelegramBotInfo getMe(TelegramConfig config) {
        String url = config.getApiUrl("getMe");

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                if (json.path("ok").asBoolean(false)) {
                    JsonNode result = json.path("result");
                    return new TelegramBotInfo(
                        result.path("id").asLong(),
                        result.path("first_name").asText(),
                        result.path("username").asText(),
                        result.path("can_join_groups").asBoolean(false),
                        result.path("can_read_all_group_messages").asBoolean(false)
                    );
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Erro ao obter informacoes do bot", e);
            return null;
        }
    }

    /**
     * Verifica se o bot esta funcionando.
     *
     * @param config Configuracao do Telegram
     * @return true se o bot responde corretamente
     */
    public boolean verificarConexao(TelegramConfig config) {
        return getMe(config) != null;
    }

    // ===== METODOS AUXILIARES =====

    private TelegramSendResult enviar(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonBody = objectMapper.writeValueAsString(body);
            log.debug("Enviando para Telegram API: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(jsonBody, headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                if (json.path("ok").asBoolean(false)) {
                    String messageId = String.valueOf(json.path("result").path("message_id").asLong());
                    return TelegramSendResult.sucesso(messageId, response.getBody());
                }

                String errorDescription = json.path("description").asText("Erro desconhecido");
                String errorCode = String.valueOf(json.path("error_code").asInt(0));
                return TelegramSendResult.falha(errorCode, errorDescription, response.getBody());
            }

            return TelegramSendResult.falha(
                "RESPONSE_ERROR",
                "Resposta inesperada: " + response.getStatusCode(),
                response.getBody()
            );

        } catch (HttpClientErrorException e) {
            log.error("Erro HTTP ao enviar via Telegram API: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());

            String codigo = "HTTP_" + e.getStatusCode().value();
            String mensagem = extrairMensagemErro(e.getResponseBodyAsString());

            return TelegramSendResult.falha(codigo, mensagem, e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Erro ao enviar via Telegram API", e);
            return TelegramSendResult.falha("EXCEPTION", e.getMessage(), null);
        }
    }

    private String extrairMensagemErro(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            return json.path("description").asText(
                json.path("error").asText("Erro desconhecido")
            );
        } catch (Exception e) {
            return responseBody;
        }
    }

    // ===== CLASSES INTERNAS =====

    /**
     * Botao inline do Telegram.
     */
    public record TelegramButton(
        String texto,
        String url,
        String callbackData
    ) {
        /**
         * Cria botao que abre URL.
         */
        public static TelegramButton url(String texto, String url) {
            return new TelegramButton(texto, url, null);
        }

        /**
         * Cria botao que dispara callback.
         */
        public static TelegramButton callback(String texto, String callbackData) {
            return new TelegramButton(texto, null, callbackData);
        }
    }

    /**
     * Informacoes do bot.
     */
    public record TelegramBotInfo(
        long id,
        String firstName,
        String username,
        boolean canJoinGroups,
        boolean canReadAllGroupMessages
    ) {}
}
