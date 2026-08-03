package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.domain.model.WardrobeCategory;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEquippedEntity;
import ru.bulbasaur.office.infra.persistence.entity.WardrobeItemEntity;
import ru.bulbasaur.office.infra.persistence.repository.PlayerEquippedJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PlayerWardrobeJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.WardrobeItemJpaRepository;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WardrobeConnector implements WardrobeRepositoryPort {

    private final WardrobeItemJpaRepository items;
    private final PlayerWardrobeJpaRepository owned;
    private final PlayerEquippedJpaRepository equipped;

    @Override
    @Transactional(readOnly = true)
    public List<WardrobeItemView> catalog(UUID playerId) {
        Map<String, Instant> purchasedAt = purchasedAtByCode(playerId);
        PlayerAppearance appearance = appearanceOf(playerId);
        return items.findAll().stream()
                .sorted(Comparator
                        .comparingLong(WardrobeItemEntity::getPrice)
                        .thenComparing(Comparator.comparingInt(WardrobeItemEntity::getSortOrder).reversed())
                        .thenComparing(WardrobeItemEntity::getCode))
                .map(item -> toView(item, purchasedAt, appearance))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WardrobeItemView> findItem(String code, UUID playerId) {
        Map<String, Instant> purchasedAt = purchasedAtByCode(playerId);
        PlayerAppearance appearance = appearanceOf(playerId);
        return items.findById(code).map(item -> toView(item, purchasedAt, appearance));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean owns(UUID playerId, String itemCode) {
        return owned.existsByPlayerIdAndItemCode(playerId, itemCode);
    }

    @Override
    @Transactional
    public void grantItem(UUID playerId, String itemCode) {
        owned.insertIfAbsent(playerId, itemCode);
    }

    @Override
    @Transactional
    public void removeItem(UUID playerId, String itemCode) {
        owned.deleteByPlayerIdAndItemCode(playerId, itemCode);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerAppearance appearanceOf(UUID playerId) {
        Map<WardrobeCategory, String> slots = new EnumMap<>(WardrobeCategory.class);
        for (PlayerEquippedEntity row : equipped.findByPlayerId(playerId)) {
            WardrobeCategory.fromName(row.getCategory())
                    .ifPresent(cat -> slots.put(cat, row.getItemCode()));
        }
        return fromSlots(slots);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, PlayerAppearance> appearancesOf(Collection<UUID> playerIds) {
        Map<UUID, PlayerAppearance> result = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return result;
        }
        Map<UUID, Map<WardrobeCategory, String>> byPlayer = new HashMap<>();
        for (PlayerEquippedEntity row : equipped.findByPlayerIdIn(playerIds)) {
            WardrobeCategory.fromName(row.getCategory()).ifPresent(cat ->
                    byPlayer.computeIfAbsent(row.getPlayerId(), ignored -> new EnumMap<>(WardrobeCategory.class))
                            .put(cat, row.getItemCode()));
        }
        for (UUID playerId : playerIds) {
            result.put(playerId, fromSlots(byPlayer.getOrDefault(playerId, Map.of())));
        }
        return result;
    }

    @Override
    @Transactional
    public void equip(UUID playerId, WardrobeCategory category, String itemCode) {
        saveEquipped(playerId, category, itemCode);
    }

    @Override
    @Transactional
    public void grantDefaults(UUID playerId) {
        owned.insertIfAbsent(playerId, "top_hoodie_black");
        owned.insertIfAbsent(playerId, "bottom_black_shorts");
        equipped.insertIfAbsent(playerId, WardrobeCategory.TOP.name(), "top_hoodie_black");
        equipped.insertIfAbsent(playerId, WardrobeCategory.BOTTOM.name(), "bottom_black_shorts");
        equipped.insertIfAbsent(playerId, WardrobeCategory.HAT.name(), null);
        equipped.insertIfAbsent(playerId, WardrobeCategory.GLASSES.name(), null);
        equipped.insertIfAbsent(playerId, WardrobeCategory.SHOES.name(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> ownedCodes(UUID playerId) {
        return purchasedAtByCode(playerId).keySet();
    }

    private Map<String, Instant> purchasedAtByCode(UUID playerId) {
        Map<String, Instant> result = new HashMap<>();
        for (var row : owned.findByPlayerId(playerId)) {
            result.put(row.getItemCode(), row.getPurchasedAt());
        }
        return result;
    }

    private WardrobeItemView toView(
            WardrobeItemEntity item,
            Map<String, Instant> purchasedAt,
            PlayerAppearance appearance
    ) {
        WardrobeCategory category = WardrobeCategory.fromName(item.getCategory())
                .orElseThrow(() -> new IllegalStateException("Неизвестная категория: " + item.getCategory()));
        boolean isOwned = purchasedAt.containsKey(item.getCode());
        boolean isEquipped = item.getCode().equals(appearance.slot(category));
        return new WardrobeItemView(
                item.getCode(), category, item.getName(), item.getPrice(),
                item.isSellable(), isOwned, isEquipped,
                isOwned ? purchasedAt.get(item.getCode()) : null);
    }

    private void saveEquipped(UUID playerId, WardrobeCategory category, String itemCode) {
        equipped.upsert(playerId, category.name(), itemCode);
    }

    private static PlayerAppearance fromSlots(Map<WardrobeCategory, String> slots) {
        if (slots.isEmpty()) {
            return PlayerAppearance.defaults();
        }
        return new PlayerAppearance(
                slots.get(WardrobeCategory.HAT),
                slots.get(WardrobeCategory.GLASSES),
                slots.get(WardrobeCategory.TOP),
                slots.get(WardrobeCategory.BOTTOM),
                slots.get(WardrobeCategory.SHOES));
    }
}
