package ru.bulbasaur.office.usecase.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException() {
        super("Комната не найдена");
    }
}
