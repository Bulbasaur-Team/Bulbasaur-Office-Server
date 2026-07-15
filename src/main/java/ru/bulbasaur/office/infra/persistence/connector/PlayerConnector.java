package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.Player;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.infra.persistence.mapper.PlayerPersistenceMapper;
import ru.bulbasaur.office.infra.persistence.repository.PlayerJpaRepository;
import ru.bulbasaur.office.usecase.dto.StoredCommunityPlayer;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlayerConnector implements PlayerRepositoryPort {

    private final PlayerJpaRepository repository;
    private final PlayerPersistenceMapper mapper;

    @Override
    public boolean existsByLogin(String login) {
        return repository.existsByLogin(login);
    }

    @Override
    public Player create(String login, String passwordHash) {
        PlayerEntity entity = new PlayerEntity();
        entity.setId(UUID.randomUUID());
        entity.setLogin(login);
        entity.setPasswordHash(passwordHash);
        entity.setCreatedAt(Instant.now());
        return mapper.toPlayer(repository.save(entity));
    }

    @Override
    public Optional<StoredPlayer> findByLogin(String login) {
        return repository.findByLogin(login).map(mapper::toStoredPlayer);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteAccount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAllIds() {
        return repository.findAllIds();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredPlayer> findById(UUID id) {
        return repository.findById(id).map(mapper::toStoredPlayer);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPlayers() {
        return repository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> roleOf(UUID id) {
        return repository.findById(id).map(PlayerEntity::getRole);
    }

    @Override
    @Transactional
    public void updateRole(UUID id, Role role) {
        repository.findById(id).ifPresent(entity -> {
            entity.setRole(role);
            repository.save(entity);
        });
    }

    @Override
    @Transactional
    public void updatePassword(UUID id, String passwordHash) {
        repository.findById(id).ifPresent(entity -> {
            entity.setPasswordHash(passwordHash);
            repository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredCommunityPlayer> community() {
        return repository.findCommunityRows(Achievement.secretCodes()).stream()
                .map(row -> new StoredCommunityPlayer(row.getLogin(), row.getRole(), row.getOwned()))
                .toList();
    }
}
