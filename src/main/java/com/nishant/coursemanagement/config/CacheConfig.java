package com.nishant.coursemanagement.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.nishant.coursemanagement.cache.composite.CompositeCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
@Profile("!test")
public class CacheConfig {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    // ── L1: Caffeine (in-memory) ────────────────────────────────────────

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL)
                .maximumSize(10_000);
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    // ── L2: Redis (distributed) ─────────────────────────────────────────

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        RedisCacheConfiguration cacheDefaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(
                        SerializationPair.fromSerializer(
                                GenericJacksonJsonRedisSerializer.builder()
                                        .enableDefaultTyping(typeValidator)
                                        .build()
                        )
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheDefaults)
                .build();
    }

    // ── Composite: L1 + L2 ──────────────────────────────────────────────

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager) {
        return new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
    }
}
