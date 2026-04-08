package com.nishant.coursemanagement.cache.composite;


import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;

/**
 * Two-level cache that delegates to an L1 (Caffeine, in-memory) and
 * L2 (Redis, distributed) cache of the same logical name.
 *
 * <ul>
 *   <li><b>Read</b>: L1 → L2 → loader.  L1 is populated on L2 hits.</li>
 *   <li><b>Write</b>: written to both L1 and L2.</li>
 *   <li><b>Evict / Clear</b>: propagated to both layers.</li>
 * </ul>
 *
 * The {@code get(key, Callable)} path preserves {@code sync = true}
 * stampede protection via the underlying Caffeine cache.
 */
@RequiredArgsConstructor
@Slf4j
public class CompositeCache implements Cache {

    private final Cache l1;
    private final Cache l2;

    @Override
    public @NonNull String getName() {
        return l1.getName();
    }

    @Override
    public @NonNull Object getNativeCache() {
        return l1.getNativeCache();
    }

    // --- Read path ---

    @Override
    public ValueWrapper get(@NonNull Object key) {
        ValueWrapper wrapper = l1.get(key);
        if (wrapper != null) {
            LogUtil.log(log, DEBUG, "CACHE_L1_HIT", "L1 cache hit", "cacheName", getName(), "key", key);
            return wrapper;
        }

        wrapper = l2.get(key);
        if (wrapper != null) {
            l1.put(key, wrapper.get());
            LogUtil.log(log, DEBUG, "CACHE_L2_HIT", "L2 cache hit, populated L1", "cacheName", getName(), "key", key);
        }
        return wrapper;
    }

    @Override
    public <T> T get(@NonNull Object key, Class<T> type) {
        T value = l1.get(key, type);
        if (value != null) {
            LogUtil.log(log, DEBUG, "CACHE_L1_HIT", "L1 cache hit", "cacheName", getName(), "key", key);
            return value;
        }

        value = l2.get(key, type);
        if (value != null) {
            l1.put(key, value);
            LogUtil.log(log, DEBUG, "CACHE_L2_HIT", "L2 cache hit, populated L1", "cacheName", getName(), "key", key);
        }
        return value;
    }

    /**
     * This overload is invoked when {@code @Cacheable(sync = true)}.
     * Caffeine's native implementation serializes concurrent calls for the same key,
     * preventing cache stampede.  The callable we pass in checks L2 first before
     * falling back to the actual loader.
     */
    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        return l1.get(key, () -> {
            // L1 miss — check L2 before invoking the expensive loader
            LogUtil.log(log, DEBUG, "CACHE_L1_MISS", "L1 cache miss, checking L2", "cacheName", getName(), "key", key);
            @SuppressWarnings("unchecked")
            T l2Value = l2.get(key, (Class<T>) Object.class);
            if (l2Value != null) {
                LogUtil.log(log, DEBUG, "CACHE_L2_HIT_SYNC", "L2 cache hit inside sync loader, populated L1", "cacheName", getName(), "key", key);
                return l2Value;
            }
            // Full miss — invoke the actual loader and populate L2
            LogUtil.log(log, DEBUG, "CACHE_L2_MISS", "L2 cache miss, loading from DB", "cacheName", getName(), "key", key);
            LogUtil.log(log, DEBUG, "CACHE_DB_HIT", "Cache miss, loading from DB", "cacheName", getName(), "key", key);
            T loaded = valueLoader.call();
            if (loaded != null) {
                l2.put(key, loaded);
            }
            return loaded;
        });
    }

    // --- Write path ---

    @Override
    public void put(@NonNull Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(@NonNull Object key, Object value) {
        ValueWrapper existing = l1.putIfAbsent(key, value);
        if (existing == null) {
            l2.putIfAbsent(key, value);
        }
        return existing;
    }

    // --- Eviction path ---

    @Override
    public void evict(@NonNull Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        boolean l1Evicted = l1.evictIfPresent(key);
        boolean l2Evicted = l2.evictIfPresent(key);
        return l1Evicted || l2Evicted;
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }

    @Override
    public boolean invalidate() {
        boolean l1Invalidated = l1.invalidate();
        boolean l2Invalidated = l2.invalidate();
        return l1Invalidated || l2Invalidated;
    }
}
