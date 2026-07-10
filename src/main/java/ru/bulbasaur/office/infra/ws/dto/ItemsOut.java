package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/** Снапшот предметов комнаты — шлётся вошедшему при join/смене комнаты. */
public record ItemsOut(String type, List<ItemStateDto> items) {

    public static ItemsOut of(List<ItemStateDto> items) {
        return new ItemsOut("items", items);
    }
}
