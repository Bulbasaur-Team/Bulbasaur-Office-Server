package ru.bulbasaur.office.infra.rest.dto;

/** Код/пин квеста. Для сюжетных квестов без секрета может быть пустым. */
public record CompleteQuestRequest(String pin) {
}
