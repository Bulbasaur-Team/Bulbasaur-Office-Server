package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;
import ru.bulbasaur.office.infra.ws.dto.ItemStateDto;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Текущее состояние физичных предметов по локациям. Каталога на сервере нет:
 * расстановка живёт в картах Tiled на клиенте (слой items), а здесь состояние
 * предмета заводится при первом ударе по нему. Эфемерный, как
 * {@link PresenceRegistry}: при рестарте предметы возвращаются на точки из карт.
 * Если владелец предмета отключился в полёте, предмет замирает на последнем
 * репорте — до следующего удара любым игроком.
 */
@Component
public class ItemRegistry {

    /** Защита от мусорных сообщений: длина id и число предметов в локации ограничены. */
    private static final int MAX_ITEM_ID_LENGTH = 64;
    private static final int MAX_ITEMS_PER_LOCATION = 100;

    private final Map<String, Map<String, ItemState>> byLocation = new ConcurrentHashMap<>();

    /** Состояние предметов локации, по которым уже били (пустой список — никто не бил). */
    public List<ItemStateDto> snapshot(String locationId) {
        return itemsOf(locationId).values().stream()
                .map(ItemState::toDto)
                .toList();
    }

    /** Арбитраж удара: true — удар принят и его нужно разослать комнате. */
    public boolean tryKick(String locationId, String itemId, String sessionId,
                           double x, double y, double vx, double vy) {
        if (itemId == null || itemId.length() > MAX_ITEM_ID_LENGTH) {
            return false;
        }
        Map<String, ItemState> items = itemsOf(locationId);
        ItemState item = items.get(itemId);
        if (item == null) {
            if (items.size() >= MAX_ITEMS_PER_LOCATION) {
                return false;
            }
            item = items.computeIfAbsent(itemId, ItemState::new);
        }
        return item.tryKick(sessionId, System.currentTimeMillis(), x, y, vx, vy);
    }

    /** Репорт позиции от владельца: true — принят и его нужно разослать комнате. */
    public boolean move(String locationId, String itemId, String sessionId,
                        double x, double y, double vx, double vy) {
        if (itemId == null) {
            return false;
        }
        ItemState item = itemsOf(locationId).get(itemId);
        return item != null && item.moveByOwner(sessionId, x, y, vx, vy);
    }

    private Map<String, ItemState> itemsOf(String locationId) {
        return byLocation.computeIfAbsent(locationId, l -> new ConcurrentHashMap<>());
    }
}
