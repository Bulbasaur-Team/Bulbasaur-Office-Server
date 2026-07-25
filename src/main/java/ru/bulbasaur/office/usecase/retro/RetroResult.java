package ru.bulbasaur.office.usecase.retro;

/** Результат операции ретро: либо значение, либо русская ошибка. */
public record RetroResult<T>(T value, String error) {

    public static <T> RetroResult<T> ok(T value) {
        return new RetroResult<>(value, null);
    }

    public static <T> RetroResult<T> error(String error) {
        return new RetroResult<>(null, error);
    }

    public static RetroResult<Void> okEmpty() {
        return new RetroResult<>(null, null);
    }

    public boolean ok() {
        return error == null;
    }
}
