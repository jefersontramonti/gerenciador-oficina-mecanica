package com.pitstop.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate bean.
 *
 * <p>Used by external API clients such as:
 * <ul>
 *   <li>EvolutionApiClient - WhatsApp Business integration</li>
 * </ul>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean with sensible defaults.
     *
     * <p>Configured with:
     * <ul>
     *   <li>Connection timeout: 10 seconds</li>
     *   <li>Read timeout: 30 seconds</li>
     * </ul>
     *
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
