package com.pitstop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração do WebSocket com STOMP para notificações em tempo real.
 *
 * <p>Endpoints configurados:
 * <ul>
 *   <li><b>/ws</b>: Endpoint de conexão WebSocket (com SockJS fallback)</li>
 *   <li><b>/app</b>: Prefixo para mensagens do cliente para o servidor</li>
 *   <li><b>/topic</b>: Prefixo para mensagens broadcast (um-para-muitos)</li>
 *   <li><b>/queue</b>: Prefixo para mensagens ponto-a-ponto (um-para-um)</li>
 * </ul>
 *
 * <p><b>Exemplos de uso:</b>
 * <ul>
 *   <li>Cliente envia para: <code>/app/notify</code></li>
 *   <li>Servidor envia broadcast para: <code>/topic/os-updates</code></li>
 *   <li>Servidor envia para usuário específico: <code>/queue/notifications</code></li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${application.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    /**
     * Configura o message broker (broker de mensagens).
     *
     * <p>Usa um broker simples em memória para desenvolvimento.
     * Em produção, considere usar um broker externo como RabbitMQ ou ActiveMQ.
     *
     * @param config configuração do message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita um broker simples em memória para /topic e /queue
        config.enableSimpleBroker("/topic", "/queue");

        // Define o prefixo para mensagens destinadas ao servidor
        config.setApplicationDestinationPrefixes("/app");

        // Define o prefixo para mensagens destinadas a usuários específicos
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registra o endpoint STOMP que os clientes usarão para conectar ao WebSocket.
     *
     * <p>SockJS é habilitado para fallback em navegadores que não suportam WebSocket nativo.
     *
     * @param registry registro de endpoints STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS(); // Habilita fallback SockJS para navegadores antigos
    }
}
