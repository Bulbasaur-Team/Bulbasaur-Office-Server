package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

public record RetroGroupStickersMessage(String board, List<String> stickerIds) {
}
