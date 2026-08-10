package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.QuestStatus;

public record CompleteQuestResponse(String code, QuestStatus status, long bulbaCoinBalance, boolean rewarded) {
}
