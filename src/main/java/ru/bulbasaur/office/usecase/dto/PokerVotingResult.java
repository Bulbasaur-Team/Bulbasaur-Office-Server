package ru.bulbasaur.office.usecase.dto;

/**
 * Итог голосования: средняя по числовым голосам и ближайшее число Фибоначчи как
 * рекомендация. null — числовых голосов не было (все "?" и "coffee").
 */
public record PokerVotingResult(Double average, Integer recommended) {
}
