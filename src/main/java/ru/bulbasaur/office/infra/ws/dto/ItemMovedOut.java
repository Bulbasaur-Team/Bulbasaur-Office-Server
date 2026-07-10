package ru.bulbasaur.office.infra.ws.dto;

/** Предмет сдвинулся (репорт владельца) — рассылается остальным в комнате. */
public record ItemMovedOut(String type, String itemId, double x, double y, double vx, double vy) {

    public static ItemMovedOut of(String itemId, double x, double y, double vx, double vy) {
        return new ItemMovedOut("itemMoved", itemId, x, y, vx, vy);
    }
}
