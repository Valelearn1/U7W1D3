package com.example.demo.auth;

import com.example.demo.entities.Utente;
import com.example.demo.exceptions.UnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTTools {

    @Value("${app.jwt.secret}")
    private String secret;

    private static final long DURATA_MS = 1000L * 60 * 60 * 24; // 24 ore

    public String generateToken(Utente utente) {
        return Jwts.builder()
                // Nel subject va l'ID, non l'email e mai dati sensibili: il payload di un JWT
                // e' solo codificato in base64, chiunque lo intercetti puo' leggerlo in chiaro.
                .subject(String.valueOf(utente.getId()))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + DURATA_MS))
                // La firma HMAC-SHA garantisce INTEGRITA': se qualcuno cambia un carattere
                // del payload la firma non torna piu' e il token viene rifiutato.
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public void verifyToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token);
        } catch (Exception ex) {
            throw new UnauthorizedException("Token non valido o scaduto, rifare il login");
        }
    }

    public long extractIdFromToken(String token) {
        try {
            return Long.parseLong(Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject());
        } catch (Exception ex) {
            throw new UnauthorizedException("Token non valido o scaduto, rifare il login");
        }
    }
}
