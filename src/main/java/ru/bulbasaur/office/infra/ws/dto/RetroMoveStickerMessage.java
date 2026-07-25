package ru.bulbasaur.office.infra.ws.dto;

/** Перемещение стикера: порядок / в группу / из группы. */
public record RetroMoveStickerMessage(
        String stickerId,
        String board,
        String ontoStickerId,
        String ontoGroupId,
        Boolean toBoard,
        String beforeStickerId
) {
}
