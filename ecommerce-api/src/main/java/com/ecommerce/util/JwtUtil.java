package com.ecommerce.util;

import com.ecommerce.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    private static final String SECRET = AppConfig.getProperty("jwt.secret", "your_super_secret_key_that_is_at_least_32_characters_long_for_HS256");
    private static final long EXPIRATION = Long.parseLong(AppConfig.getProperty("jwt.expiration", "86400000"));

    public static String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return ((Number) claims.get("userId")).longValue();
        }
        return null;
    }

    public static String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return claims.getSubject();
        }
        return null;
    }

    public static String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims != null) {
            return (String) claims.get("role");
        }
        return null;
    }

    public static boolean isTokenValid(String token) {
        return validateToken(token) != null;
    }
}