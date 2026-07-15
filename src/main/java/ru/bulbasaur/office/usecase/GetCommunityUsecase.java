package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.usecase.dto.CommunityPlayerView;
import ru.bulbasaur.office.usecase.port.out.OnlinePlayersPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Список игроков для экрана сообщества (в порядке регистрации) с признаком «сейчас в игре». */
@Service
@RequiredArgsConstructor
public class GetCommunityUsecase {

    private final PlayerRepositoryPort players;
    private final OnlinePlayersPort onlinePlayers;
    private final AchievementService achievements;

    public List<CommunityPlayerView> execute(UUID playerId) {
        achievements.grant(playerId, Achievement.SOCIAL);
        Set<String> online = onlinePlayers.onlineLogins();
        return players.community().stream()
                .map(p -> new CommunityPlayerView(
                        p.login(),
                        p.role(),
                        p.ownedAchievements(),
                        online.contains(p.login())))
                .toList();
    }

    public int totalAchievements() {
        return Achievement.publicCount();
    }
}
