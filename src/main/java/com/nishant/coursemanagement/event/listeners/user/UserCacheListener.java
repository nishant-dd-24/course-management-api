package com.nishant.coursemanagement.event.listeners.user;

import com.nishant.coursemanagement.cache.event.CacheEvictEvent;
import com.nishant.coursemanagement.cache.redis.RedisCachePublisher;
import com.nishant.coursemanagement.event.events.user.UserUpdatedEvent;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.nishant.coursemanagement.log.annotation.LogLevel.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCacheListener {

    private final CacheManager cacheManager;
    private final RedisCachePublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Loggable(
            action = "CACHE_EVICT_USER",
            message = "Evicting user from cache",
            extras = {"#event.userId()"},
            extraKeys = {"userId"},
            level = DEBUG
    )
    public void handleUserUpdate(UserUpdatedEvent event) {
        Long userId = event.userId();
        evict("userById", userId);
        clear("users");
    }


    private void evict(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            publishRedisCacheEvictEvent(cacheName, key, false);
        }
        else logEmptyCache(cacheName);
    }

    private void clear(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            publishRedisCacheEvictEvent(cacheName, null, true);
        }
        else logEmptyCache(cacheName);
    }

    private void publishRedisCacheEvictEvent(String cacheName, Object key, boolean evictAll){
        publisher.publish(new CacheEvictEvent(
                cacheName,
                key,
                evictAll
        ));
        LogUtil.log(log, INFO, "PUBLISHED_REDIS_CACHE_EVICT_EVENT", "", "cacheName: ", cacheName, "key: ", key, "evictAll: ", evictAll);
    }

    private void logEmptyCache(String cacheName) {
        LogUtil.log(log, WARN, "CACHE_NOT_FOUND", "Cache not found", "cacheName", cacheName);
    }
}
