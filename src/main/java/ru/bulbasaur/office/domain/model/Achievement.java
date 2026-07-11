package ru.bulbasaur.office.domain.model;

import java.util.Optional;

/**
 * Ачивка: достижение игрока за определённую активность. Порядок констант задаёт
 * порядок отображения в списке. code — стабильный идентификатор (в БД и REST),
 * image — имя png-файла в папке achievements на клиенте.
 */
public enum Achievement {
    BULBAZAVR("bulbazavr", "Я уже Бульбазавр!", "Зарегистрироваться в игре"),
    VOLLEYBALL("volleyball", "Волейболист", "Пнуть волейбольный мяч на пляже в режиме мультиплеера"),
    JUMPER("jumper", "Попрыгун", "Хотя бы раз сыграть в Bulba Jump"),
    JUMPER_10K("jumper_10k", "Прыгун", "Набрать в Bulba Jump 10000 очков"),
    JUMPER_100K("jumper_100k", "Великий прыгун", "Набрать в Bulba Jump 100000 очков"),
    JUMPER_200K("jumper_200k", "Легенда прыжков", "Набрать в Bulba Jump 200000 очков"),
    SHOPAHOLIC("shopaholic", "Шопоголик", "Хотя бы раз сыграть в Bulba Packer"),
    TRUCKER("trucker", "Дальнобойщик", "Припарковать дальнобойный грузовик"),
    TRUCKER_PRO("trucker_pro", "Матёрый дальнобойщик", "Припарковать дальнобойный грузовик за 4.5 секунды или меньше"),
    PSYCHIC("psychic", "Экстрасенс", "Отгадать 1 слово в Bulba Guess"),
    DECODER("decoder", "Дешифратор", "Отгадать 1 слово в Bulba Wordle"),
    GUARD("guard", "Охранник", "Отгадать 10 слов в Bulba Wordle"),
    LIGHTNING("lightning", "Молния Маккуин", "Пройти слово дня быстрее всех"),
    DISCIPLINE("discipline", "Дисциплина", "Пройти слово дня в Bulba Wordle 5 дней подряд"),
    CHAMPION("champion", "Чемпион", "Попасть на первую строчку лидерборда"),
    DAY_CHAMPION("day_champion", "Чемпион дня", "Занять первое место в режиме «Слово дня»"),
    LOVER("lover", "Любвеобильный", "Отправить эмодзи сердечка в режиме мультиплеера"),
    TENNIS("tennis", "Теннисист", "Пнуть теннисный мячик в режиме мультиплеера");

    private final String code;
    private final String title;
    private final String description;

    Achievement(String code, String title, String description) {
        this.code = code;
        this.title = title;
        this.description = description;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** Имя картинки в папке achievements на клиенте. */
    public String image() {
        return code + ".png";
    }

    public static Optional<Achievement> fromCode(String code) {
        for (Achievement achievement : values()) {
            if (achievement.code.equals(code)) {
                return Optional.of(achievement);
            }
        }
        return Optional.empty();
    }
}
