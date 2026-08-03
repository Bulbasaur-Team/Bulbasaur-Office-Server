package ru.bulbasaur.office.infra.ws.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

/** Смена одежды игрока — рассылается всем в его локации. */
public record AppearanceOut(String type, String id, PlayerAppearance appearance) {
    public static AppearanceOut of(String sessionId, PlayerAppearance appearance) {
        return new AppearanceOut("appearance", sessionId, appearance);
    }
}
