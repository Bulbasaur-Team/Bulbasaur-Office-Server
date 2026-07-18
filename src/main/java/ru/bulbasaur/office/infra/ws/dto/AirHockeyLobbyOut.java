package ru.bulbasaur.office.infra.ws.dto;

/**
 * Кто ждёт у стола аэрохоккея в локации. Рассылается всей комнате, чтобы
 * над персонажами показывать облачко «Сыграем в аэрохоккей?».
 */
public record AirHockeyLobbyOut(
        String type,
        String redSessionId,
        String redLogin,
        String blueSessionId,
        String blueLogin,
        String phase
) {

    public static AirHockeyLobbyOut of(
            String redSessionId, String redLogin,
            String blueSessionId, String blueLogin,
            String phase
    ) {
        return new AirHockeyLobbyOut(
                "airhockeyLobby",
                redSessionId, redLogin,
                blueSessionId, blueLogin,
                phase
        );
    }
}
