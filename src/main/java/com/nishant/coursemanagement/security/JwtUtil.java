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

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;
import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;


@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Loggable(
            action = "JWT_GENERATE_TOKEN",
            extras = {"#email", "#role.name()"},
            extraKeys = {"email", "role"},
            level = DEBUG
    )
    public String generateToken(String email, Role role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(jwtProperties.getExpirationSeconds())))
                .signWith(signingKey)
                .compact();
    }

    @Loggable(
            action = "JWT_EXTRACT_EMAIL",
            level = DEBUG
    )
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Loggable(
            action = "JWT_EXTRACT_ROLE",
            level = DEBUG
    )
    public Role extractRole(String token) {
        return Role.valueOf(extractAllClaims(token).get("role", String.class));
    }

    @Loggable(
            action = "JWT_VALIDATE_TOKEN",
            level = DEBUG
    )
    public boolean isTokenValid(String token) {
        try {
            return !extractAllClaims(token).getExpiration().before(Date.from(Instant.now()));
        } catch (Exception ex) {
            LogUtil.log(log, WARN, "JWT_VALIDATE_TOKEN_FAILED", "Token validation failed");
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
