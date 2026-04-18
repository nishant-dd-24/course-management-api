package com.nishant.coursemanagement.security;


import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.log.util.LogUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;
import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Loggable(
            action = "JWT_GENERATE_TOKEN",
            extras = {"#id", "#role.name()"},
            extraKeys = {"userId", "role"},
            level = DEBUG
    )
    public String generateToken(Long id, Role role) {
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("role", role.name())
                .claim("type", ACCESS_TOKEN_TYPE)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(jwtProperties.getExpirationSeconds())))
                .signWith(signingKey)
                .compact();
    }

    @Loggable(
            action = "JWT_GENERATE_REFRESH_TOKEN",
            extras = {"#userId"},
            extraKeys = {"userId"},
            level = DEBUG
    )
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", REFRESH_TOKEN_TYPE)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(jwtProperties.getRefreshExpirationSeconds())))
                .signWith(signingKey)
                .compact();
    }

    @Loggable(
            action = "JWT_EXTRACT_ID",
            level = DEBUG
    )
    public long extractSubject(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    @Loggable(
            action = "JWT_EXTRACT_ROLE",
            level = DEBUG
    )
    public Role extractAuthorities(String token) {
        return Role.valueOf(extractAllClaims(token).get("role", String.class));
    }

    @Loggable(
            action = "JWT_VALIDATE_TOKEN",
            level = DEBUG
    )
    public boolean isTokenInvalid(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(Date.from(Instant.now()));
        } catch (Exception ex) {
            LogUtil.log(log, WARN, "JWT_VALIDATE_TOKEN_FAILED", "Token validation failed", "error", ex.getMessage());
            return true;
        }
    }

    @Loggable(
            action = "JWT_EXTRACT_EXPIRATION",
            level = DEBUG
    )
    public Instant extractExpiration(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }

    @Loggable(
            action = "JWT_EXTRACT_TYPE",
            level = DEBUG
    )
    public String extractType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
