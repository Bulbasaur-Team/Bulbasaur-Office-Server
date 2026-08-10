package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.QuestStatus;

public record QuestStatusView(String code, QuestStatus status) {
}
