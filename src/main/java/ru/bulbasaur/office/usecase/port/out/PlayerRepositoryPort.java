package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.Player;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepositoryPort {

    boolean existsByLogin(String login);

    Player create(String login, String passwordHash);

    Optional<StoredPlayer> findByLogin(String login);

    void deleteById(UUID id);

    /** Идентификаторы всех игроков — для периодической перепроверки ачивок. */
    List<UUID> findAllIds();
}
