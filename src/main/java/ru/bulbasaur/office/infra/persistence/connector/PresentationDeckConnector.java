package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.PlayerPresentationDeckEntity;
import ru.bulbasaur.office.infra.persistence.repository.PlayerPresentationDeckJpaRepository;
import ru.bulbasaur.office.usecase.port.out.PresentationDeckRepositoryPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PresentationDeckConnector implements PresentationDeckRepositoryPort {

    private final PlayerPresentationDeckJpaRepository repository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public void save(UUID playerId, List<String> slideIds) {
        PlayerPresentationDeckEntity row = repository.findById(playerId).orElseGet(() -> {
            PlayerPresentationDeckEntity created = new PlayerPresentationDeckEntity();
            created.setPlayerId(playerId);
            return created;
        });
        row.setSlideIds(jsonMapper.writeValueAsString(slideIds));
        row.setUpdatedAt(Instant.now());
        repository.save(row);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<String>> find(UUID playerId) {
        return repository.findById(playerId).map(row -> {
            JsonNode node = jsonMapper.readTree(row.getSlideIds());
            if (node == null || !node.isArray()) {
                return List.<String>of();
            }
            List<String> ids = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                if (item != null && item.isString()) {
                    ids.add(item.asString());
                }
            }
            return List.copyOf(ids);
        });
    }

    @Override
    @Transactional
    public void delete(UUID playerId) {
        repository.deleteById(playerId);
    }
}
