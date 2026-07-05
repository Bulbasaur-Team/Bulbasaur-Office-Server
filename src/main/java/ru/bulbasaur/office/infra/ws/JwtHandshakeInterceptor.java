package ru.bulbasaur.office.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.bulbasaur.office.infra.security.AuthPrincipal;
import ru.bulbasaur.office.infra.security.JwtService;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Аутентификация WebSocket-хендшейка. Браузерный WebSocket не умеет слать заголовок
 * Authorization, поэтому JWT передаётся в query (?token=...). При валидном токене
 * кладём id и логин игрока в атрибуты сессии; иначе отказываем в хендшейке (403).
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PLAYER_ID = "playerId";
    public static final String LOGIN = "login";

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = queryParam(request.getURI(), "token");
        if (token == null) {
            return false;
        }
        try {
            AuthPrincipal principal = jwtService.parse(token);
            attributes.put(PLAYER_ID, principal.id());
            attributes.put(LOGIN, principal.login());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Нечего делать после хендшейка.
    }

    private String queryParam(URI uri, String name) {
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
