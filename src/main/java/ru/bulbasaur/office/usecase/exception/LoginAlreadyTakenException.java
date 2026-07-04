package ru.bulbasaur.office.usecase.exception;

public class LoginAlreadyTakenException extends RuntimeException {

    public LoginAlreadyTakenException(String login) {
        super("Логин уже занят: " + login);
    }
}
