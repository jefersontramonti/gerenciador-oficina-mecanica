package com.pitstop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web MVC configuration for PitStop application.
 *
 * <p>Configures:
 * <ul>
 *   <li><b>UTF-8 encoding</b>: Force UTF-8 for all HTTP message converters</li>
 *   <li><b>JSON serialization</b>: Jackson with UTF-8 support</li>
 * </ul>
 *
 * <p><b>Encoding strategy:</b>
 * <ol>
 *   <li>Force UTF-8 at server level (Tomcat via application.yml)</li>
 *   <li>Force UTF-8 at Spring MVC level (this configuration)</li>
 *   <li>Force UTF-8 at Jackson level (ObjectMapper)</li>
 *   <li>Force UTF-8 at database level (HikariCP + PostgreSQL)</li>
 * </ol>
 *
 * <p>This ensures that all data flows (HTTP, JSON, Database) use UTF-8 encoding,
 * preventing issues with special characters (ã, õ, ç, é, etc.).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures HTTP message converters to use UTF-8 encoding.
     *
     * <p>This method overrides the default message converters to ensure
     * all text-based converters (String, JSON) use UTF-8.
     *
     * @param converters list of HTTP message converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add String converter with UTF-8 encoding
        converters.add(stringHttpMessageConverter());

        // Add Jackson JSON converter with UTF-8 encoding
        converters.add(mappingJackson2HttpMessageConverter());
    }

    /**
     * Creates a String HTTP message converter with UTF-8 encoding.
     *
     * <p>This converter handles plain text requests/responses.
     *
     * @return configured StringHttpMessageConverter
     */
    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    /**
     * Creates a Jackson JSON message converter with UTF-8 encoding.
     *
     * <p>This converter handles JSON requests/responses.
     * Jackson configuration (dates, timezones, etc.) is done in application.yml.
     *
     * @return configured MappingJackson2HttpMessageConverter
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        return converter;
    }
}
