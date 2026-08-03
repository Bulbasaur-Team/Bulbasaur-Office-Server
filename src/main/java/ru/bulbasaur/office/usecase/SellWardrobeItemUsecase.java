package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;
import ru.bulbasaur.office.usecase.dto.WardrobeSellView;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.UUID;

/** Продажа предмета гардероба (возврат половины цены). */
@Service
@RequiredArgsConstructor
public class SellWardrobeItemUsecase {

    private final WardrobeRepositoryPort wardrobe;
    private final CreditBulbaCoinsUsecase credit;
    private final GetBulbaCoinBalanceUsecase balance;
    private final AppearanceBroadcaster appearanceBroadcaster;
    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;

    @Transactional
    public WardrobeSellView execute(UUID playerId, String itemCode) {
        WardrobeItemView item = wardrobe.findItem(itemCode, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Предмет не найден"));
        if (!item.sellable()) {
            throw new IllegalArgumentException("Этот предмет нельзя продать");
        }
        if (!item.owned()) {
            throw new IllegalArgumentException("Предмет не куплен");
        }
        long refund = item.price() / 2;
        if (item.equipped()) {
            wardrobe.equip(playerId, item.category(), null);
        }
        wardrobe.removeItem(playerId, itemCode);
        credit.execute(playerId, refund, BulbaCoinKind.WARDROBE_SELL,
                "sell:" + itemCode + ":" + UUID.randomUUID(),
                "Продажа: " + item.name());
        players.findById(playerId)
                .ifPresent(player -> eventLog.wardrobeItemSold(player.login(), item.name(), refund));
        PlayerAppearance appearance = wardrobe.appearanceOf(playerId);
        appearanceBroadcaster.broadcast(playerId, appearance);
        return new WardrobeSellView(balance.execute(playerId), refund, appearance);
    }
}
