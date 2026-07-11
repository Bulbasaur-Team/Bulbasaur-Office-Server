package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.Player;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.usecase.dto.CommunityPlayerView;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepositoryPort {

    boolean existsByLogin(String login);

    Player create(String login, String passwordHash);

    Optional<StoredPlayer> findByLogin(String login);

    Optional<StoredPlayer> findById(UUID id);

    void deleteById(UUID id);

    /** Идентификаторы всех игроков — для периодической перепроверки ачивок. */
    List<UUID> findAllIds();

    /** Общее число игроков — знаменатель процента редкости ачивок. */
    long countPlayers();

    /** Выбранная роль игрока; empty — роль ещё не выбрана. */
    Optional<Role> roleOf(UUID id);

    void updateRole(UUID id, Role role);

    void updatePassword(UUID id, String passwordHash);

    /** Игроки для сообщества в порядке регистрации, с числом полученных ачивок. */
    List<CommunityPlayerView> community();
}
