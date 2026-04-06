package com.nishant.coursemanagement.event.listeners.course;

import com.nishant.coursemanagement.event.events.course.CourseUpdatedEvent;
import com.nishant.coursemanagement.event.events.enrollment.EnrollmentChangedEvent;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;
import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseCacheListener {
    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Loggable(
            action = "CACHE_EVICT_COURSE",
            message = "Evicting course from cache",
            extras = {"#event.courseId()"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public void handleCourseUpdate(CourseUpdatedEvent event) {
        Long courseId = event.courseId();
        evictCourseCaches(courseId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Loggable(
            action = "CACHE_EVICT_COURSE_ENROLLMENT",
            message = "Evicting course from cache due to change in enrollment",
            extras = {"#event.courseId()"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public void handleEnrollmentChange(EnrollmentChangedEvent event) {
        Long courseId = event.courseId();
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
        LogUtil.log(log, WARN, "CACHE_NOT_FOUND", "Cache not found", "cacheName", cacheName);
    }
}
