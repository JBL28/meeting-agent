package com.ssafy.meeting.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-seconds:3600}") long accessTokenValiditySeconds,
        @Value("${jwt.refresh-token-seconds:604800}") long refreshTokenValiditySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public String createAccessToken(Long memberId, String email) {
        return createToken(memberId, email, accessTokenValiditySeconds, "access");
    }

    public String createRefreshToken(Long memberId, String email) {
        return createToken(memberId, email, refreshTokenValiditySeconds, "refresh");
    }

    public Long getMemberId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public boolean validate(String token) {
        parseClaims(token);
        return true;
    }

    public LocalDateTime refreshTokenExpiresAt() {
        return LocalDateTime.ofInstant(Instant.now().plusSeconds(refreshTokenValiditySeconds), ZoneId.systemDefault());
    }

    private String createToken(Long memberId, String email, long validitySeconds, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .claim("email", email)
            .claim("typ", type)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(validitySeconds)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
