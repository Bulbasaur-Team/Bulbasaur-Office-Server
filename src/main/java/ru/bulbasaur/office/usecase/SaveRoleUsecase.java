package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.Optional;
import java.util.UUID;

/** Сохранение выбранной роли (скина Бульбазавра), чтобы не выбирать её при каждом входе. */
@Service
@RequiredArgsConstructor
public class SaveRoleUsecase {

    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;
    private final AchievementService achievements;

    public void execute(UUID playerId, Role role) {
        Optional<Role> previous = players.roleOf(playerId);
        players.updateRole(playerId, role);
        if (previous.isPresent() && previous.get() != role) {
            achievements.grant(playerId, Achievement.CHAMELEON);
        }
        players.findById(playerId)
                .map(StoredPlayer::login)
                .ifPresent(login -> eventLog.roleChanged(login, role));
    }
}
