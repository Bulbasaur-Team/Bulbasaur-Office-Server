package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.Achievement;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AchievementRepositoryPort {

    /** Ачивки, которые уже есть у игрока. */
    Set<Achievement> findOwned(UUID playerId);

    /** Выдать ачивку. true — если она была выдана только что (раньше её не было). */
    boolean grant(UUID playerId, Achievement achievement);

    /** Сколько игроков владеет каждой ачивкой — для процента редкости. */
    Map<Achievement, Long> countOwners();
}
