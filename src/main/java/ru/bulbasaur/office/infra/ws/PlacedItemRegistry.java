package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;
import ru.bulbasaur.office.infra.ws.dto.PlacedItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Предметы, поставленные на столы (чашки кофе): в отличие от мячей, они не летают,
 * а стоят на месте из слоя tables и видны всем в локации. Эфемерный, как остальное
 * присутствие: при рестарте сервера столы пустеют.
 *
 * <p>Срок жизни чашки считается от момента, когда её взяли (выдали на кухне), и не
 * продлевается перекладыванием: {@link #expiryOf} запоминает срок за id предмета.
 */
@Component
public class PlacedItemRegistry {

    /** Чашка кофе живёт полчаса с момента выдачи. */
    public static final long COFFEE_TTL_MS = 30 * 60 * 1000L;

    private static final int MAX_ID_LENGTH = 64;
    private static final int MAX_TYPE_LENGTH = 32;
    private static final int MAX_PLACED_PER_LOCATION = 100;
    private static final int MAX_TRACKED_EXPIRY = 10_000;

    private final Map<String, Map<String, PlacedItemDto>> byLocation = new ConcurrentHashMap<>();
    private final Map<String, Long> expiryById = new ConcurrentHashMap<>();

    /** Живые (не истёкшие) предметы на столах локации. */
    public List<PlacedItemDto> snapshot(String locationId) {
        long now = System.currentTimeMillis();
        return itemsOf(locationId).values().stream()
                .filter(item -> now < item.expiresAt())
                .toList();
    }

    /**
     * Срок жизни предмета: назначается при первом взятии и дальше только читается,
     * чтобы перекладывание чашки со стола на стол его не продлевало.
     */
    public long expiryOf(String itemId) {
        if (itemId == null || itemId.length() > MAX_ID_LENGTH) {
            return 0;
        }
        if (expiryById.size() >= MAX_TRACKED_EXPIRY) {
            purgeExpiredIds();
        }
        return expiryById.computeIfAbsent(itemId, id -> System.currentTimeMillis() + COFFEE_TTL_MS);
    }

    /** Поставить предмет на стол. false — данные не приняты (мусор или место занято). */
    public boolean place(String locationId, String id, String type, int tableIndex,
                         double x, double y, long expiresAt) {
        if (id == null || id.length() > MAX_ID_LENGTH
                || type == null || type.length() > MAX_TYPE_LENGTH
                || tableIndex < 0 || expiresAt <= System.currentTimeMillis()) {
            return false;
        }
        Map<String, PlacedItemDto> items = itemsOf(locationId);
        if (items.size() >= MAX_PLACED_PER_LOCATION && !items.containsKey(id)) {
            return false;
        }
        // На одно место — только один предмет.
        long now = System.currentTimeMillis();
        boolean taken = items.values().stream()
                .anyMatch(item -> item.tableIndex() == tableIndex && !item.id().equals(id) && now < item.expiresAt());
        if (taken) {
            return false;
        }
        items.put(id, new PlacedItemDto(id, type, tableIndex, x, y, expiresAt));
        return true;
    }

    /** Убрать предмет со стола (его забрали в лапы). true — он там был. */
    public boolean remove(String locationId, String id) {
        return id != null && itemsOf(locationId).remove(id) != null;
    }

    /** Убрать предмет со столов всех локаций (не знаем, где он лежит). */
    public void removeEverywhere(String id) {
        if (id == null) {
            return;
        }
        for (Map<String, PlacedItemDto> items : byLocation.values()) {
            items.remove(id);
        }
    }

    /** Истёкшие предметы: снимаем со столов и отдаём по локациям — комнатам нужно разослать удаление. */
    public Map<String, List<String>> sweepExpired() {
        long now = System.currentTimeMillis();
        Map<String, List<String>> removed = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, PlacedItemDto>> entry : byLocation.entrySet()) {
            List<String> ids = new ArrayList<>();
            entry.getValue().values().removeIf(item -> {
                if (now < item.expiresAt()) {
                    return false;
                }
                ids.add(item.id());
                return true;
            });
            if (!ids.isEmpty()) {
                removed.put(entry.getKey(), ids);
            }
        }
        purgeExpiredIds();
        return removed;
    }

    private void purgeExpiredIds() {
        long now = System.currentTimeMillis();
        expiryById.values().removeIf(expiresAt -> now >= expiresAt);
    }

    private Map<String, PlacedItemDto> itemsOf(String locationId) {
        return byLocation.computeIfAbsent(locationId, l -> new ConcurrentHashMap<>());
    }
}
