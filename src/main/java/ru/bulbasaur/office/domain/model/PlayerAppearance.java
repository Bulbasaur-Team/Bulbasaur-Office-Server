package ru.bulbasaur.office.domain.model;

/**
 * Надетая одежда игрока. Значения — коды предметов каталога; null в слоте —
 * ничего не надето.
 */
public record PlayerAppearance(
        String hat,
        String glasses,
        String top,
        String bottom,
        String shoes
) {
    public static PlayerAppearance defaults() {
        return new PlayerAppearance(null, null, "top_hoodie_black", "bottom_black_shorts", null);
    }

    public String slot(WardrobeCategory category) {
        return switch (category) {
            case HAT -> hat;
            case GLASSES -> glasses;
            case TOP -> top;
            case BOTTOM -> bottom;
            case SHOES -> shoes;
        };
    }

    public PlayerAppearance withSlot(WardrobeCategory category, String itemCode) {
        return switch (category) {
            case HAT -> new PlayerAppearance(itemCode, glasses, top, bottom, shoes);
            case GLASSES -> new PlayerAppearance(hat, itemCode, top, bottom, shoes);
            case TOP -> new PlayerAppearance(hat, glasses, itemCode, bottom, shoes);
            case BOTTOM -> new PlayerAppearance(hat, glasses, top, itemCode, shoes);
            case SHOES -> new PlayerAppearance(hat, glasses, top, bottom, itemCode);
        };
    }
}
