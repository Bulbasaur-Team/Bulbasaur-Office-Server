package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.QuestStatus;

public record QuestStatusResponse(String code, QuestStatus status) {
}
