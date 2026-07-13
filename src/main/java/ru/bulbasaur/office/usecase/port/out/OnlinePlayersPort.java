package ru.bulbasaur.office.usecase.port.out;

import java.util.Set;

/** Кто сейчас в игре (есть открытое реалтайм-соединение). */
public interface OnlinePlayersPort {

    Set<String> onlineLogins();
}
