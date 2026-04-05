package com.nishant.coursemanagement.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nishant.coursemanagement.exception.ErrorCode;
import com.nishant.coursemanagement.exception.response.ErrorResponse;
import com.nishant.coursemanagement.exception.response.ErrorResponseFactory;
import com.nishant.coursemanagement.exception.response.ErrorResponseWriter;
import com.nishant.coursemanagement.security.JwtUtil;
import com.nishant.coursemanagement.util.LogUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

record RateLimitContext(String key, int limit) {
}

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int ADMIN_LIMIT = 100;
    private static final int INSTRUCTOR_LIMIT = 50;
    private static final int STUDENT_LIMIT = 20;
    private static final int ANONYMOUS_LIMIT = 10;

    private static final int LOGIN_LIMIT = 5;
    private static final int ENROLL_LIMIT = 10;
    private static final int READ_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 20;

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private final ErrorResponseFactory errorResponseFactory;
    private final ErrorResponseWriter errorResponseWriter;
    private final JwtUtil jwtUtil;

    private Bucket createBucket(int limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit)
                .refillGreedy(limit, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private Bucket resolveBucket(String key, int limit) {
        return cache.get(key, k -> createBucket(limit));
    }

    private int resolveEndpointLimit(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.equals("/users/login") && method.equals("POST")) {
            return LOGIN_LIMIT;
        }

        if (path.startsWith("/enrollments") && method.equals("POST")) {
            return ENROLL_LIMIT;
        }

        if (path.startsWith("/courses") && method.equals("GET")) {
            return READ_LIMIT;
        }

        return DEFAULT_LIMIT;
    }

    private RateLimitContext resolveContext(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        String path = request.getRequestURI().replaceAll("/\\d+(?=/|$)", "/{id}");
        String method = request.getMethod();
        int endpointLimit = resolveEndpointLimit(request);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                var role = jwtUtil.extractRole(token);
                String email = jwtUtil.extractEmail(token);
                int roleLimit = switch (role) {
                    case ADMIN -> ADMIN_LIMIT;
                    case INSTRUCTOR -> INSTRUCTOR_LIMIT;
                    case STUDENT -> STUDENT_LIMIT;
                };
                int finalLimit = Math.min(roleLimit, endpointLimit);
                return new RateLimitContext(email + ":" + role.name() + ":" + path + ":" + method, finalLimit);
            } catch (Exception e) {
                try {
                    LogUtil.put("action", "RATE_LIMIT_AUTH_ERROR");
                    LogUtil.put("message", e.getMessage());
                    LogUtil.put("ip", request.getRemoteAddr());
                    log.warn("Rate limit auth error");
                } finally {
                    LogUtil.clear();
                }
                int finalLimit = Math.min(ANONYMOUS_LIMIT, endpointLimit);
                return new RateLimitContext(request.getRemoteAddr() + ":" + path + ":" + method, finalLimit);
            }
        }
        int finalLimit = Math.min(ANONYMOUS_LIMIT, endpointLimit);
        return new RateLimitContext(request.getRemoteAddr() + ":" + path + ":" + method, finalLimit);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            LogUtil.put("action", "RATE_LIMIT_SYSTEM_ACTIVE");
            log.debug("Rate limit system active");
        } finally {
            LogUtil.clear();
        }
        try {
            LogUtil.put("action", "RATE_LIMIT_CHECK");
            LogUtil.put("path", request.getRequestURI());
            LogUtil.put("method", request.getMethod());
            LogUtil.put("ip", request.getRemoteAddr());
            log.debug("Checking rate limit");
        } finally {
            LogUtil.clear();
        }
        RateLimitContext rateLimitContext = resolveContext(request);
        try {
            LogUtil.put("action", "RATE_LIMIT_CONTEXT");
            LogUtil.put("key", rateLimitContext.key());
            LogUtil.put("limit", rateLimitContext.limit());
            log.debug("Resolved rate limit context");
        } finally {
            LogUtil.clear();
        }
        String key = rateLimitContext.key();
        int limit = rateLimitContext.limit();
        Bucket bucket = resolveBucket(key, limit);
        response.setHeader(HEADER_LIMIT, String.valueOf(limit));
        if (bucket.tryConsume(1)) {
            try {
                LogUtil.put("action", "RATE_LIMIT_ALLOWED");
                LogUtil.put("key", key);
                LogUtil.put("remaining", bucket.getAvailableTokens());
                log.debug("Rate limit allowed");
            } finally {
                LogUtil.clear();
            }
            response.setHeader(HEADER_REMAINING, String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            try {
                LogUtil.put("action", "RATE_LIMIT_EXCEEDED");
                LogUtil.put("key", key);
                LogUtil.put("limit", limit);
                log.warn("Rate limit exceeded");
            } finally {
                LogUtil.clear();
            }
            response.setHeader(HEADER_REMAINING, "0");

            long waitTime = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            long waitSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitTime));
            response.setHeader("Retry-After", String.valueOf(waitSeconds));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            ErrorResponse error = errorResponseFactory.build(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.", ErrorCode.RATE_LIMIT_EXCEEDED, request);
            errorResponseWriter.write(response, error);
        }
    }
}
