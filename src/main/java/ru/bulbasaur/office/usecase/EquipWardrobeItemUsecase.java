package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.domain.model.WardrobeCategory;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.UUID;

/** Надеть или снять предмет в слоте категории. */
@Service
@RequiredArgsConstructor
public class EquipWardrobeItemUsecase {

    private final WardrobeRepositoryPort wardrobe;
    private final AppearanceBroadcaster appearanceBroadcaster;

    @Transactional
    public PlayerAppearance execute(UUID playerId, WardrobeCategory category, String itemCode) {
        PlayerAppearance current = wardrobe.appearanceOf(playerId);
        if (itemCode != null) {
            WardrobeItemView item = wardrobe.findItem(itemCode, playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
            if (item.category() != category) {
                throw new IllegalArgumentException("Предмет не подходит к этой категории");
            }
            if (!item.owned()) {
                throw new IllegalArgumentException("Сначала купите предмет");
            }
        }
        wardrobe.equip(playerId, category, itemCode);
        PlayerAppearance updated = current.withSlot(category, itemCode);
        appearanceBroadcaster.broadcast(playerId, updated);
        return updated;
    }
}
