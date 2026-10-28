package ru.bulbasaur.office.usecase.exception;

/** Комната закрыта паролем, а игрок ещё не в списке допущенных. */
public class RoomLockedException extends RuntimeException {

    public static final String REQUIRED = "Комната закрыта паролем.";
    public static final String WRONG = "Неверный пароль.";
    public static final String TOO_LONG = "Пароль слишком длинный.";

    public RoomLockedException(String message) {
        super(message);
    }
}
