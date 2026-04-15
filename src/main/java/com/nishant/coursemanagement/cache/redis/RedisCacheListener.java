package com.nishant.coursemanagement.cache.redis;

import com.nishant.coursemanagement.cache.event.CacheEvictEvent;
import com.nishant.coursemanagement.log.annotation.LogLevel;
import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class RedisCacheListener implements MessageListener {

    private final CaffeineCacheManager caffeineCacheManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern){
        try{
            CacheEvictEvent event = objectMapper.readValue(message.getBody(), CacheEvictEvent.class);

            Cache cache = caffeineCacheManager.getCache(event.cacheName());

            if(cache==null) return;

            if(event.evictAll()){
                cache.clear();
            } else {
                cache.evict(event.key());
            }
        } catch (Exception ex){
            LogUtil.log(log, LogLevel.WARN, "CACHE_EVICTION_EVEN_FAILED", "Failed to process cache eviction event: " + ex.getMessage());
        }
    }
}
