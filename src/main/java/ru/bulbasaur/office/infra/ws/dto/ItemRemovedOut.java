package ru.bulbasaur.office.infra.ws.dto;

/** Предмет убрали со стола: забрали в лапы или истёк срок жизни. */
public record ItemRemovedOut(String type, String itemId) {

    public static ItemRemovedOut of(String itemId) {
        return new ItemRemovedOut("itemRemoved", itemId);
    }
}
