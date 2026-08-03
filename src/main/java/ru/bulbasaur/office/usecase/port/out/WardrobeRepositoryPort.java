package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.domain.model.WardrobeCategory;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WardrobeRepositoryPort {

    List<WardrobeItemView> catalog(UUID playerId);

    Optional<WardrobeItemView> findItem(String code, UUID playerId);

    boolean owns(UUID playerId, String itemCode);

    void grantItem(UUID playerId, String itemCode);

    void removeItem(UUID playerId, String itemCode);

    PlayerAppearance appearanceOf(UUID playerId);

    /** Внешний вид нескольких игроков одним запросом. */
    Map<UUID, PlayerAppearance> appearancesOf(Collection<UUID> playerIds);

    void equip(UUID playerId, WardrobeCategory category, String itemCode);

    /** Выдать дефолтную одежду новому игроку. */
    void grantDefaults(UUID playerId);

    Set<String> ownedCodes(UUID playerId);
}
