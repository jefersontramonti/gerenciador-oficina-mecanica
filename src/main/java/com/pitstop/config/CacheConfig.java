package com.pitstop.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
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
 * Cache strategy:
 * - Detail caches (entity by ID): Longer TTL (1 hour)
 * - List caches (paginated results): Shorter TTL (5 minutes) - evicted on mutations
 * - Static data (categorias): 24 hours
 * - Reports: 15 minutes
 *
 * Performance notes:
 * - List caches use shorter TTL because they need to reflect new items quickly
 * - Detail caches can have longer TTL since specific item changes are less frequent
 * - Using @CacheEvict with key instead of allEntries where possible
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Detail caches (single entity)
    public static final String CATEGORIAS_CACHE = "categorias";
    public static final String PECAS_CACHE = "pecas";
    public static final String CLIENTES_CACHE = "clientes";
    public static final String VEICULOS_CACHE = "veiculos";
    public static final String USUARIOS_CACHE = "usuarios";
    public static final String ORDEM_SERVICO_CACHE = "ordemServico";

    // List caches (paginated results) - shorter TTL
    public static final String PECAS_LIST_CACHE = "pecas-list";
    public static final String CLIENTES_LIST_CACHE = "clientes-list";
    public static final String VEICULOS_LIST_CACHE = "veiculos-list";
    public static final String OS_LIST_CACHE = "os-list";

    // Aggregation caches
    public static final String CONSULTAS_CACHE = "consultas";
    public static final String RELATORIOS_CACHE = "relatorios";
    public static final String DASHBOARD_CACHE = "dashboard";
    public static final String OS_COUNT_CACHE = "osCountByStatus";
    public static final String ESTOQUE_BAIXO_CACHE = "estoqueBaixo";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create ObjectMapper with Java 8 Date/Time support (JSR-310)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Configure type validation for polymorphic deserialization
        // This allows Redis to deserialize objects back to their original types
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.pitstop")
            .allowIfSubType("java.util")
            .allowIfSubType("java.time")
            .allowIfSubType("java.lang")
            .allowIfSubType("java.math")
            .allowIfBaseType(Object.class)
            .build();

        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

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

        // Static data caches (24 hours)
        cacheConfigurations.put(CATEGORIAS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(24)));

        // Detail caches (1 hour) - single entity by ID
        cacheConfigurations.put(PECAS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CLIENTES_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(VEICULOS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(USUARIOS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(ORDEM_SERVICO_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));

        // List caches (5 minutes) - shorter TTL for quick updates
        RedisCacheConfiguration listConfig = defaultConfig.entryTtl(Duration.ofMinutes(5));
        cacheConfigurations.put(PECAS_LIST_CACHE, listConfig);
        cacheConfigurations.put(CLIENTES_LIST_CACHE, listConfig);
        cacheConfigurations.put(VEICULOS_LIST_CACHE, listConfig);
        cacheConfigurations.put(OS_LIST_CACHE, listConfig);

        // Aggregation caches
        cacheConfigurations.put(CONSULTAS_CACHE, defaultConfig
            .entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(DASHBOARD_CACHE, defaultConfig
            .entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(OS_COUNT_CACHE, defaultConfig
            .entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(ESTOQUE_BAIXO_CACHE, defaultConfig
            .entryTtl(Duration.ofMinutes(15)));

        // Reports (15 minutes)
        cacheConfigurations.put(RELATORIOS_CACHE, defaultConfig
            .entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
