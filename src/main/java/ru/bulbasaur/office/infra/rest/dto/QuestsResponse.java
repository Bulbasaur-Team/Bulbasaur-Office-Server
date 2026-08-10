package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

public record QuestsResponse(List<QuestStatusResponse> quests) {
}
