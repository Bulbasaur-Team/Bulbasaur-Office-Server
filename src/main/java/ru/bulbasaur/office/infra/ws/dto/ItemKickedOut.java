package ru.bulbasaur.office.infra.ws.dto;

/** Принятый арбитражем удар — рассылается всей комнате, включая ударившего. */
public record ItemKickedOut(String type, String itemId, String kickId,
                            double x, double y, double vx, double vy) {

    public static ItemKickedOut of(String itemId, String kickId, double x, double y, double vx, double vy) {
        return new ItemKickedOut("itemKicked", itemId, kickId, x, y, vx, vy);
    }
}
