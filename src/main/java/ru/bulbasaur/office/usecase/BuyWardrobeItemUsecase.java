package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.UUID;

/** Покупка предмета гардероба за Bulba Coins. */
@Service
@RequiredArgsConstructor
public class BuyWardrobeItemUsecase {

    private final WardrobeRepositoryPort wardrobe;
    private final DebitBulbaCoinsUsecase debit;
    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;

    @Transactional
    public WardrobeItemView execute(UUID playerId, String itemCode) {
        WardrobeItemView item = wardrobe.findItem(itemCode, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
        if (!item.sellable() || item.price() <= 0) {
            throw new IllegalArgumentException("Этот предмет нельзя купить");
        }
        if (item.owned()) {
            throw new IllegalArgumentException("Предмет уже куплен");
        }
        debit.execute(playerId, item.price(), BulbaCoinKind.WARDROBE_BUY,
                "buy:" + itemCode + ":" + UUID.randomUUID(),
                "Покупка: " + item.name());
        wardrobe.grantItem(playerId, itemCode);
        players.findById(playerId)
                .ifPresent(player -> eventLog.wardrobeItemBought(player.login(), item.name(), item.price()));
        return wardrobe.findItem(itemCode, playerId).orElseThrow();
    }
}
