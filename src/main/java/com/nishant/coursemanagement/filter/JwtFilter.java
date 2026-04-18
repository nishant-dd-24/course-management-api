package com.nishant.coursemanagement.filter;


import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.exception.custom.JwtAuthenticationException;
import com.nishant.coursemanagement.exception.security.CustomAuthenticationEntryPoint;
import com.nishant.coursemanagement.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final JwtUtil jwtUtil;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new JwtAuthenticationException("Missing JWT Token")
            );
            return;
        }
        String token = authHeader.substring(7);
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null && Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("Invalid JWT Token"));
            return;
        }
        try {
            if (jwtUtil.isTokenInvalid(token)) {
                authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("Invalid JWT Token"));
                return;
            }
            Long id = jwtUtil.extractSubject(token);
            Role role = jwtUtil.extractAuthorities(token);
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    id,
                    null,
                    authorities
            );
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new JwtAuthenticationException("Invalid or expired token"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/users/login")
                || path.startsWith("/users/register")
                || path.startsWith("/users/refresh");
    }
}
