package ru.bulbasaur.office.usecase.exception;

public class PokerRoomActiveException extends RuntimeException {

    public PokerRoomActiveException() {
        super("Комната ещё идёт — зайдите из лобби");
    }
}
