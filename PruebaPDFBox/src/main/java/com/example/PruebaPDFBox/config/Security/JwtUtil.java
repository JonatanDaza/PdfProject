package com.example.PruebaPDFBox.config.Security;

import com.example.PruebaPDFBox.model.Role;
import com.example.PruebaPDFBox.model.Users;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    public String generateToken(Users users) {
        List<String> roleNames = users.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        return Jwts.builder()
                .subject(users.getEmail())
                .claim("roles", roleNames)
                .claim("name", users.getName())
                .claim("id", users.getId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }
}
