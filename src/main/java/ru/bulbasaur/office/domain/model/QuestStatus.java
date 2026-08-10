package ru.bulbasaur.office.domain.model;

/**
 * Статус квеста для игрока.
 * {@code LOCKED} — ещё не открыт (мало ачивок); {@code AVAILABLE} — можно начать;
 * в таблице хранятся только {@code IN_PROGRESS} и {@code COMPLETED}.
 */
public enum QuestStatus {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED
}
