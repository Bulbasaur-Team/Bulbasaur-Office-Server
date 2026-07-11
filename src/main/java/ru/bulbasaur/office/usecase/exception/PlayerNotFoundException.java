package ru.bulbasaur.office.usecase.exception;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String login) {
        super("Игрок не найден: " + login);
    }
}
