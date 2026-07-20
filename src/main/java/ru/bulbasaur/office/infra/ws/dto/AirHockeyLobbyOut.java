package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/**
 * Кто ждёт у столов аэрохоккея в локации. Рассылается всей комнате, чтобы
 * над персонажами показывать облачко «Сыграем в аэрохоккей?».
 * Несколько пар могут ждать одновременно — в {@code waiting} все ожидающие.
 */
public record AirHockeyLobbyOut(
        String type,
        List<WaitingSeat> waiting
) {

    public record WaitingSeat(String side, String sessionId, String login) {
    }

    public static AirHockeyLobbyOut of(List<WaitingSeat> waiting) {
        return new AirHockeyLobbyOut("airhockeyLobby", List.copyOf(waiting));
    }
}
