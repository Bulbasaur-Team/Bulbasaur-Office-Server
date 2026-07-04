package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.Player;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;

import java.util.Optional;

public interface PlayerRepositoryPort {

    boolean existsByLogin(String login);

    Player create(String login, String passwordHash);

    Optional<StoredPlayer> findByLogin(String login);
}
