package com.pokemon.bff.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(PokemonCacheProperties.class)
public class PokemonCacheConfiguration {

    @Bean
    public CacheManager cacheManager(PokemonCacheProperties properties,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        return switch (properties.getProvider()) {
            case NONE -> new NoOpCacheManager();
            case CAFFEINE -> caffeineCacheManager(properties);
            case REDIS -> redisCacheManager(properties, redisConnectionFactoryProvider.getIfAvailable());
        };
    }

    CacheManager caffeineCacheManager(PokemonCacheProperties properties) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCaffeineCache(PokemonCacheNames.POKEMON_PAGE, properties.getPage()),
                buildCaffeineCache(PokemonCacheNames.POKEMON_DETAIL, properties.getDetail())));
        return cacheManager;
    }

    CacheManager redisCacheManager(PokemonCacheProperties properties, RedisConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalStateException("Redis cache provider requires a RedisConnectionFactory");
        }

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .prefixCacheNameWith("pokemon-api-bff::");

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                PokemonCacheNames.POKEMON_PAGE, defaults.entryTtl(properties.getPage().getTtl()),
                PokemonCacheNames.POKEMON_DETAIL, defaults.entryTtl(properties.getDetail().getTtl()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private CaffeineCache buildCaffeineCache(String name, PokemonCacheProperties.CacheSpec spec) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(spec.getTtl())
                .maximumSize(spec.getMaximumSize())
                .build());
    }
}
