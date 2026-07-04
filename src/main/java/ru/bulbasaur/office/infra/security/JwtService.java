package ru.bulbasaur.office.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.infra.config.AppProperties;
import ru.bulbasaur.office.usecase.port.out.TokenPort;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService implements TokenPort {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = properties.jwt().ttl();
    }

    @Override
    public String issue(UUID playerId, String login) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(playerId.toString())
                .claim("login", login)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Проверяет подпись и срок, возвращает принципала. Бросает исключение на невалидном токене. */
    public AuthPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthPrincipal(UUID.fromString(claims.getSubject()), claims.get("login", String.class));
    }
}
