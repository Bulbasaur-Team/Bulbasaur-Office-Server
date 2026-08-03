package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.usecase.dto.AchievementView;
import ru.bulbasaur.office.usecase.exception.PlayerNotFoundException;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Список ачивок для окна ачивок: признак «получена» и редкость.
 * Секретные видны только владельцу; для чужого профиля по логину — скрыты.
 */
@Service
@RequiredArgsConstructor
public class ListAchievementsUsecase {

    private final AchievementRepositoryPort achievements;
    private final PlayerRepositoryPort players;

    public List<AchievementView> execute(UUID playerId) {
        return list(playerId, false);
    }

    public List<AchievementView> executeByLogin(String login) {
        UUID playerId = players.findByLogin(login)
                .orElseThrow(() -> new PlayerNotFoundException(login))
                .id();
        return list(playerId, true);
    }

    private List<AchievementView> list(UUID playerId, boolean hideSecret) {
        Set<Achievement> owned = achievements.findOwned(playerId);
        Map<Achievement, Long> owners = achievements.countOwners();
        long totalPlayers = Math.max(1, players.countPlayers());
        return Arrays.stream(Achievement.values())
                .filter(a -> !hideSecret || !a.secret())
                .filter(a -> !a.secret() || owned.contains(a))
                .map(a -> new AchievementView(
                        a.code(), a.title(), a.description(), a.image(), owned.contains(a),
                        owners.getOrDefault(a, 0L) * 100.0 / totalPlayers))
                .sorted(Comparator.comparingDouble(AchievementView::percent).reversed())
                .toList();
    }
}
