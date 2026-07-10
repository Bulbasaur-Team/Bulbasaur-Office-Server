package ru.bulbasaur.office.infra.ws.dto;

/**
 * Игрок ударил по предмету. kickId — клиентский идентификатор удара: по нему
 * ударивший отличает эхо своего удара от чужого, победившего в арбитраже.
 */
public record ItemKickMessage(String itemId, String kickId, double x, double y, double vx, double vy) {
}
