package ru.bulbasaur.office.infra.security;

import java.util.UUID;

/** Аутентифицированный игрок в SecurityContext (принципал из JWT). */
public record AuthPrincipal(UUID id, String login) {
}
