package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.usecase.dto.CommunityPlayerView;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.List;

/** Список игроков для экрана сообщества (в порядке регистрации). */
@Service
@RequiredArgsConstructor
public class GetCommunityUsecase {

    private final PlayerRepositoryPort players;

    public List<CommunityPlayerView> execute() {
        return players.community();
    }

    public int totalAchievements() {
        return Achievement.values().length;
    }
}
