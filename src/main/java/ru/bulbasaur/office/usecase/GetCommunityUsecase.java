package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.dto.CommunityPlayerView;
import ru.bulbasaur.office.usecase.dto.StoredCommunityPlayer;
import ru.bulbasaur.office.usecase.port.out.OnlinePlayersPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Список игроков для экрана сообщества (в порядке регистрации) с признаком «сейчас в игре». */
@Service
@RequiredArgsConstructor
public class GetCommunityUsecase {

    private final PlayerRepositoryPort players;
    private final OnlinePlayersPort onlinePlayers;
    private final WardrobeRepositoryPort wardrobe;
    private final GrantAchievementUsecase grantAchievement;

    public List<CommunityPlayerView> execute(UUID playerId) {
        grantAchievement.execute(playerId, Achievement.SOCIAL);
        Set<String> online = onlinePlayers.onlineLogins();
        List<StoredCommunityPlayer> community = players.community();
        Map<UUID, PlayerAppearance> appearances = wardrobe.appearancesOf(
                community.stream().map(StoredCommunityPlayer::id).toList());
        return community.stream()
                .map(p -> new CommunityPlayerView(
                        p.login(),
                        appearances.getOrDefault(p.id(), PlayerAppearance.defaults()),
                        p.ownedAchievements(),
                        online.contains(p.login())))
                .toList();
    }

    public int totalAchievements() {
        return Achievement.publicCount();
    }
}
