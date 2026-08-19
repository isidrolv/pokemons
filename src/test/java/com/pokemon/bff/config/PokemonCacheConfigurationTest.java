package com.pokemon.bff.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Iterator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PokemonCacheConfigurationTest {
    private final PokemonCacheConfiguration configuration = new PokemonCacheConfiguration();

    @Test
    void shouldCreateNoOpCacheManagerWhenProviderIsNone() {
        PokemonCacheProperties properties = new PokemonCacheProperties();
        properties.setProvider(PokemonCacheProperties.Provider.NONE);

        CacheManager cacheManager = configuration.cacheManager(properties, objectProvider(null));

        assertInstanceOf(NoOpCacheManager.class, cacheManager);
    }

    @Test
    void shouldCreateCaffeineCacheManagerWhenProviderIsCaffeine() {
        PokemonCacheProperties properties = new PokemonCacheProperties();
        properties.setProvider(PokemonCacheProperties.Provider.CAFFEINE);

        CacheManager cacheManager = configuration.cacheManager(properties, objectProvider(null));

        assertInstanceOf(SimpleCacheManager.class, cacheManager);
    }

    @Test
    void shouldCreateRedisCacheManagerWhenProviderIsRedis() {
        PokemonCacheProperties properties = new PokemonCacheProperties();
        properties.setProvider(PokemonCacheProperties.Provider.REDIS);

        CacheManager cacheManager = configuration.cacheManager(properties, objectProvider(mock(RedisConnectionFactory.class)));

        assertInstanceOf(RedisCacheManager.class, cacheManager);
    }

    @Test
    void shouldFailWhenRedisProviderHasNoConnectionFactory() {
        PokemonCacheProperties properties = new PokemonCacheProperties();
        properties.setProvider(PokemonCacheProperties.Provider.REDIS);

        assertThrows(IllegalStateException.class,
                () -> configuration.cacheManager(properties, objectProvider(null)));
    }

    private static <T> ObjectProvider<T> objectProvider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public Iterator<T> iterator() {
                return stream().iterator();
            }

            @Override
            public Stream<T> stream() {
                return value == null ? Stream.empty() : Stream.of(value);
            }

            @Override
            public Stream<T> orderedStream() {
                return stream();
            }
        };
    }
}
