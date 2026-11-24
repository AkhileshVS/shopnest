package com.akhilesh.shopnest.services;

import com.akhilesh.shopnest.config.RedisCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class ProductCacheEvictor {

    private final CacheManager cacheManager;

    public void evict(Collection<Long> productIds) {
        Cache products = cacheManager.getCache(RedisCacheConfig.PRODUCT);
        if (products != null) {
            productIds.forEach(products::evict);
        }
        Cache pages = cacheManager.getCache(RedisCacheConfig.PRODUCT_PAGES);
        if (pages != null) {
            pages.clear();
        }
    }
}
