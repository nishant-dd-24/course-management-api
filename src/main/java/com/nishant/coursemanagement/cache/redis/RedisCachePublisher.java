package com.nishant.coursemanagement.cache.redis;

import com.nishant.coursemanagement.cache.event.CacheEvictEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCachePublisher {

    private static final String CHANNEL = "cache-evict";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCachePublisher(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(CacheEvictEvent event) {
        redisTemplate.convertAndSend(CHANNEL, event);
    }
}