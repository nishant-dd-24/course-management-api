package com.nishant.coursemanagement.cache.event;

public record CacheEvictEvent(
        String cacheName,
        Object key,
        boolean evictAll
) {
}
