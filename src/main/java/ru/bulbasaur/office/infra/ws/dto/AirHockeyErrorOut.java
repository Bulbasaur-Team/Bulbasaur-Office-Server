package ru.bulbasaur.office.infra.ws.dto;

/** Отказ операции аэрохоккея — показывается игроку. */
public record AirHockeyErrorOut(String type, String message) {

    public static AirHockeyErrorOut of(String message) {
        return new AirHockeyErrorOut("airhockeyError", message);
    }
}
