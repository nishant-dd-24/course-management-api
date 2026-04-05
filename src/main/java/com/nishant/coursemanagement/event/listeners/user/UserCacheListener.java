package com.nishant.coursemanagement.event.listeners.user;

import com.nishant.coursemanagement.event.events.user.UserUpdatedEvent;
import com.nishant.coursemanagement.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCacheListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserUpdate(UserUpdatedEvent event) {
        Long userId = event.userId();
        try {
            LogUtil.put("action", "CACHE_EVICT_USER");
            LogUtil.put("userId", userId);
            log.debug("Evicting user from cache");
        } finally {
            LogUtil.clear();
        }
        evict("userById", userId);
        clear("users");
    }


    private void evict(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
        else logEmptyCache(cacheName);
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
        else logEmptyCache(cacheName);
    }

    private void logEmptyCache(String cacheName) {
        try {
            LogUtil.put("action", "CACHE_NOT_FOUND");
            LogUtil.put("cacheName", cacheName);
            log.warn("Cache not found");
        } finally {
            LogUtil.clear();
        }
    }
}
