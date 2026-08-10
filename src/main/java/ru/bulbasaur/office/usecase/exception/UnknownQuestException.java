package ru.bulbasaur.office.usecase.exception;

public class UnknownQuestException extends RuntimeException {

    public UnknownQuestException(String code) {
        super("Неизвестный квест: " + code);
    }
}
