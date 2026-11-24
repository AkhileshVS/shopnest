package com.akhilesh.shopnest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    public static final String PRODUCT = "product";
    public static final String PRODUCT_PAGES = "productPages";
    public static final String CATEGORIES = "categories";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory,
                                          @Value("${app.cache.product-ttl-minutes}") long ttl) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("shopnest:")
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(ttl));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(Map.of(
                        PRODUCT_PAGES, base.entryTtl(Duration.ofMinutes(2)),
                        CATEGORIES, base.entryTtl(Duration.ofHours(1))))
                .build();
    }
}
