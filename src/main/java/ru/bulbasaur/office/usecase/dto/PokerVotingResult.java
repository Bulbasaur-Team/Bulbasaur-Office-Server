package ru.bulbasaur.office.usecase.dto;

/**
 * Итог голосования: средняя по учитываемым голосам и ближайшая карта выбранной
 * колоды. У футболок average = null, recommended — размер («M»). Оба null —
 * учитываемых голосов не было (все «?» и «coffee»).
 */
public record PokerVotingResult(Double average, String recommended) {
}
