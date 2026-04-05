package com.nishant.coursemanagement.event.listeners.course;

import com.nishant.coursemanagement.event.events.course.CourseUpdatedEvent;
import com.nishant.coursemanagement.event.events.enrollment.EnrollmentChangedEvent;
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
public class CourseCacheListener {
    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseUpdate(CourseUpdatedEvent event) {
        Long courseId = event.courseId();
        try {
            LogUtil.put("action", "CACHE_EVICT_COURSE");
            LogUtil.put("courseId", courseId);
            log.debug("Evicting course from cache");
        } finally {
            LogUtil.clear();
        }
        evictCourseCaches(courseId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnrollmentChange(EnrollmentChangedEvent event) {
        Long courseId = event.courseId();
        try {
            LogUtil.put("action", "CACHE_EVICT_COURSE_ENROLLMENT");
            LogUtil.put("courseId", courseId);
            log.debug("Evicting course from cache due to change in enrollment");
        } finally {
            LogUtil.clear();
        }
        evictCourseCaches(courseId);
    }

    private void evictCourseCaches(Long courseId) {
        evict("courseById", courseId);
        evict("activeCourseById", courseId);
        clear("courses");
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
            log.warn("Cache not found");
        } finally {
            LogUtil.clear();
        }
    }
}
