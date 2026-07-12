package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.UUID;

/** Сохранение выбранной роли (скина Бульбазавра), чтобы не выбирать её при каждом входе. */
@Service
@RequiredArgsConstructor
public class SaveRoleUsecase {

    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;

    public void execute(UUID playerId, Role role) {
        players.updateRole(playerId, role);
        players.findById(playerId)
                .map(StoredPlayer::login)
                .ifPresent(login -> eventLog.roleChanged(login, role));
    }
}
