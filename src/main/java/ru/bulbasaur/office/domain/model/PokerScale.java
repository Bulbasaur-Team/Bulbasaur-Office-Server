package ru.bulbasaur.office.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Колода planning poker. Код уходит клиенту в {@code pokerCreate.scale} и
 * {@code pokerState.scale} — те же строки, что в селекте создания комнаты.
 * «?» и «coffee» есть в каждой колоде и в среднее не входят.
 */
public enum PokerScale {

    SEQUENTIAL("sequential", nums(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
    FIBONACCI("fibonacci", nums(0, 1, 2, 3, 5, 8, 13, 21)),
    /** 0 и степени двойки до 64: 0, 1, 2, 4, 8, 16, 32, 64. */
    POWERS("powers", nums(0, 1, 2, 4, 8, 16, 32, 64)),
    TSHIRTS("tshirts", List.of("XS", "S", "M", "L", "XL"));

    public static final String UNKNOWN = "?";
    public static final String COFFEE = "coffee";

    private final String code;
    /** Карты, которые участвуют в среднем и в рекомендации. */
    private final List<String> values;
    /** Полная колода: оценки, затем «?» и кофе. */
    private final List<String> cards;

    PokerScale(String code, List<String> values) {
        this.code = code;
        this.values = List.copyOf(values);
        List<String> deck = new ArrayList<>(values);
        deck.add(UNKNOWN);
        deck.add(COFFEE);
        this.cards = List.copyOf(deck);
    }

    public String code() {
        return code;
    }

    public List<String> cards() {
        return cards;
    }

    public boolean accepts(String vote) {
        return vote != null && cards.contains(vote);
    }

    /**
     * Вес голоса для среднего. У числовых колод это само число, у футболок —
     * порядок XS=1 … XL=5. null — «?», кофе или чужая карта.
     */
    public Double weight(String vote) {
        int index = values.indexOf(vote);
        if (index < 0) {
            return null;
        }
        if (this == TSHIRTS) {
            return (double) (index + 1);
        }
        return Double.valueOf(vote);
    }

    /** true — среднее можно показать числом. У футболок среднее не числовое. */
    public boolean numericAverage() {
        return this != TSHIRTS;
    }

    /** Ближайшая карта колоды. При равном расстоянии — большая (дальше по колоде). */
    public String nearest(double average) {
        String best = values.get(0);
        double bestDistance = Math.abs(weight(best) - average);
        for (int i = 1; i < values.size(); i++) {
            String value = values.get(i);
            double distance = Math.abs(weight(value) - average);
            if (distance <= bestDistance) {
                best = value;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static PokerScale fromCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        for (PokerScale scale : values()) {
            if (scale.code.equals(normalized)) {
                return scale;
            }
        }
        return null;
    }

    private static List<String> nums(int... values) {
        List<String> cards = new ArrayList<>(values.length);
        for (int value : values) {
            cards.add(Integer.toString(value));
        }
        return cards;
    }
}
