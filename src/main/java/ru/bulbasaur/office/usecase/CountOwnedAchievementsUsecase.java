package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;

import java.util.Set;
import java.util.UUID;

/** Число полученных публичных ачивок для счётчика «Получено X/Y». */
@Service
@RequiredArgsConstructor
public class CountOwnedAchievementsUsecase {

    private final AchievementRepositoryPort achievements;

    public int execute(UUID playerId) {
        Set<Achievement> owned = achievements.findOwned(playerId);
        return (int) owned.stream().filter(a -> !a.secret()).count();
    }
}
