package ru.bulbasaur.office.usecase.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Неверный логин или пароль");
    }
}
