package ru.bulbasaur.office.usecase.poker;

/** Лимиты planning poker. */
public final class PokerConstants {

    public static final int MAX_ACTIVE_ROOMS = 20;
    public static final long TTL_MS = 2 * 60 * 60 * 1000L;

    private PokerConstants() {
    }
}
