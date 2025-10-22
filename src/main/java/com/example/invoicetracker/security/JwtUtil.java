package com.example.invoicetracker.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private static final long JWT_EXPIRATION = 24 * 60 * 60 * 1000;
    private static final String SECRET_KEY = "thisIsASuperLongSecureJwtSecretKeyThatIsDefinitely64CharsOrMore!";
 

   private final Key secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());


    public String generateToken(String username, Set<String> roles) {
        Set<String> prefixedRoles = roles.stream()
            .map(r -> "ROLE_" + r)
            .collect(Collectors.toSet());
            
        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of("roles", prefixedRoles))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        List<String> rolesList = getClaims(token).get("roles", List.class);
        return new HashSet<>(rolesList);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'extractUsername'");
    }
}