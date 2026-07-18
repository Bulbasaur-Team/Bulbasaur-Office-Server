package ru.bulbasaur.office.infra.ws.dto;

/** Ответ на клиентский ping — эхо метки времени клиента для замера RTT. */
public record PongOut(String type, long t) {

    public static PongOut of(long t) {
        return new PongOut("pong", t);
    }
}
