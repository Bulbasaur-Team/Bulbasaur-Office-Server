package ru.bulbasaur.office.usecase.exception;

public class UnknownGameException extends RuntimeException {

    public UnknownGameException(String code) {
        super("Неизвестная игра: " + code);
    }
}
