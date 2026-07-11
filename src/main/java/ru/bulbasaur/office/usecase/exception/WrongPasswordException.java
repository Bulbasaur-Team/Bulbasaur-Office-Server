package ru.bulbasaur.office.usecase.exception;

public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("Неверный старый пароль");
    }
}
