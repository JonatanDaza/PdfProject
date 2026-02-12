package com.example.PruebaPDFBox.config;

import com.example.PruebaPDFBox.model.User;
import com.example.PruebaPDFBox.model.UserType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET;

    @Value("${jwt.expiration}")
    private long tokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("usertype", user.getUserTypes().stream().map(UserType::getName).toList())
                .claim("username", user.getName())
                .claim("id", user.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }
}
