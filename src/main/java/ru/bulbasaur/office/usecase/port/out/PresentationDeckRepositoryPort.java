package ru.bulbasaur.office.usecase.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PresentationDeckRepositoryPort {

    void save(UUID playerId, List<String> slideIds);

    Optional<List<String>> find(UUID playerId);

    void delete(UUID playerId);
}
