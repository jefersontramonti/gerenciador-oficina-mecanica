package com.pitstop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for PitStop application.
 *
 * Defines cache names and TTLs for different types of data:
 * - categorias: 24 hours (static data)
 * - pecas: 24 hours (inventory data)
 * - consultas: 1 hour (frequent queries)
 * - relatorios: 15 minutes (reports with calculations)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CATEGORIAS_CACHE = "categorias";
    public static final String PECAS_CACHE = "pecas";
    public static final String CLIENTES_CACHE = "clientes";
    public static final String CONSULTAS_CACHE = "consultas";
    public static final String RELATORIOS_CACHE = "relatorios";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create ObjectMapper with Java 8 Date/Time support (JSR-310)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Create serializer with custom ObjectMapper
        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(serializer))
            .disableCachingNullValues();

        // Specific cache configurations with custom TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put(CATEGORIAS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(24)));

        cacheConfigurations.put(PECAS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(24)));

        cacheConfigurations.put(CLIENTES_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(24)));

        cacheConfigurations.put(CONSULTAS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));

        cacheConfigurations.put(RELATORIOS_CACHE, defaultConfig
            .entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
