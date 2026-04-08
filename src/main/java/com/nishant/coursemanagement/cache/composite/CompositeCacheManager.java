package com.nishant.coursemanagement.cache.composite;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link CacheManager} that creates {@link CompositeCache} instances
 * backed by an L1 (Caffeine) and L2 (Redis) cache manager.
 *
 * <p>When {@code getCache(name)} is called, it retrieves (or creates)
 * the corresponding cache from both underlying managers and wraps them
 * in a {@link CompositeCache}.  Composite instances are cached locally
 * so the wrapping cost is incurred only once per cache name.
 */
@RequiredArgsConstructor
public class CompositeCacheManager implements CacheManager {

    private final CacheManager l1CacheManager;
    private final CacheManager l2CacheManager;

    private final ConcurrentMap<String, Cache> compositeMap = new ConcurrentHashMap<>();

    @Override
    public Cache getCache(@NonNull String name) {
        return compositeMap.computeIfAbsent(name, this::createCompositeCache);
    }

    @Override
    public @NonNull Collection<String> getCacheNames() {
        var names = new LinkedHashSet<>(l1CacheManager.getCacheNames());
        names.addAll(l2CacheManager.getCacheNames());
        return names;
    }

    private Cache createCompositeCache(String name) {
        Cache l1 = l1CacheManager.getCache(name);
        Cache l2 = l2CacheManager.getCache(name);

        if (l1 == null || l2 == null) {
            throw new IllegalStateException(
                    "Unable to create composite cache [%s]: L1=%s, L2=%s".formatted(name, l1, l2));
        }

        return new CompositeCache(l1, l2);
    }
}
