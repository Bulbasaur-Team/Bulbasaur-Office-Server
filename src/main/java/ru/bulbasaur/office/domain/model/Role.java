package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/**
 * Роль игрока в мультиплеере. Скин на клиенте выбирается по имени роли —
 * в домене привязки к спрайтам нет.
 */
public enum Role {
    DEV,
    DEV_FE,
    QA,
    LEAD,
    ANALYSIS,
    DESIGN,
    PRODUCT_OWNER;

    public static Optional<Role> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
