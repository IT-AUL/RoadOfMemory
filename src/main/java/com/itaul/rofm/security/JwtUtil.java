package com.itaul.rofm.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${app.jwt.access.secret}")
    private String accessSecret;

    @Value("${app.jwt.access.expirationIn}")
    private int accessTokenExpirationIn;

    @Value("${app.jwt.refresh.secret}")
    private String refreshSecret;

    @Value("${app.jwt.refresh.expirationIn}")
    private long refreshTokenExpirationIn;


    public JwtUtil() {
    }

    public String generateAccessToken(Long userId) {
        return generateToken(userId, accessSecret, accessTokenExpirationIn);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, refreshSecret, refreshTokenExpirationIn);
    }

    private String generateToken(Long userId, String secret, long expirationInMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationInMs))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    public Long getUserIdFromAccessToken(String token) {
        return getUserIdFromToken(token, accessSecret);
    }

    public Long getUserIdFromRefreshToken(String token) {
        return getUserIdFromToken(token, refreshSecret);
    }

    private Long getUserIdFromToken(String token, String secret) {
        return Long.parseLong(Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }
}
