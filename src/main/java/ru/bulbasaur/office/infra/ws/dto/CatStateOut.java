package ru.bulbasaur.office.infra.ws.dto;

/** Состояние Бульба Кота в main-office — снапшот при входе и периодические апдейты. */
public record CatStateOut(String type, double x, double y, boolean facing, boolean moving) {

    public static CatStateOut of(double x, double y, boolean facing, boolean moving) {
        return new CatStateOut("catState", x, y, facing, moving);
    }
}
