package ru.bulbasaur.office.usecase.exception;

public class PokerRoomNotFoundException extends RuntimeException {

    public PokerRoomNotFoundException() {
        super("Комната не найдена");
    }
}
