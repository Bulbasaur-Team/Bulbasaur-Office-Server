package ru.bulbasaur.office.domain.model;

import java.util.List;
import java.util.Set;

/** Правила приёмки колоды Бульбулем (зеркало клиентского reviewDeck). */
public final class PresentationDeckRules {

    public static final Set<String> ALLOWED_SLIDES = Set.of(
            "history", "pain", "miracle", "cabin", "beach", "ai", "team", "metrics",
            "tired", "limit", "roadmap", "market", "moat", "green", "culture", "ask"
    );

    private static final Set<String> SOLUTION_SLIDES = Set.of("miracle", "cabin", "beach");

    private PresentationDeckRules() {
    }

    /** null — ок; иначе текст отказа для игрока. */
    public static String rejectReason(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "Пустой набор я принимать не буду. Включи нужные слайды и выстрой порядок.";
        }
        for (String id : ids) {
            if (id == null || !ALLOWED_SLIDES.contains(id)) {
                return "Неизвестный слайд в наборе.";
            }
        }
        Set<String> set = Set.copyOf(ids);
        if (!set.contains("pain")) {
            return "Вернул. А какую проблему мы решаем? Без слайда про долгую перевозку между городами инвесторы не поймут, зачем нам телепорт.";
        }
        if (!set.contains("miracle")) {
            return "Вернул. Почему убрал мгновенную доставку? Там же очень красивая картинка с земным шаром получилась. Без этого чуда презентация пустая.";
        }
        if (!set.contains("beach")) {
            return "Вернул. Почему убрал «Пилот — точка прибытия»? Там же очень красивая картинка с пляжем получилась. Верни.";
        }
        if (!set.contains("ai")) {
            return "Вернул. Во всех современных презентациях обязательно что-то должно быть про AI. Добавь слайд про нейросети.";
        }
        if (!set.contains("tired")) {
            return "Вернул. Почему не подсветил, зачем нам деньги? Добавь, что модели устали: нет денег — нет токенов.";
        }
        if (!set.contains("market")) {
            return "Вернул. Надо добавить «Революцию в ecom». Заголовок громкий — инвесторы такое любят.";
        }
        if (set.contains("moat")) {
            return "Вернул. «Мы не понимаем, как это работает» — инвесторам такое нельзя говорить. Услышишь — денег не дадут. Убери.";
        }
        if (set.contains("green")) {
            return "Вернул. Экология — лишнее. В современном мире столько проблем, что экология уже никому не интересна. Убери.";
        }
        if (!set.contains("ask")) {
            return "Красиво. А где мы просим денег? Это же самый важный слайд — «Инвестиции: миллиарды BC». Верни.";
        }
        if (!"ask".equals(ids.get(ids.size() - 1))) {
            return "Вернул. Порядок важен. Слайд с инвестициями должен быть последним. Переставь.";
        }
        int painAt = ids.indexOf("pain");
        int firstSolution = -1;
        for (int i = 0; i < ids.size(); i++) {
            if (SOLUTION_SLIDES.contains(ids.get(i))) {
                firstSolution = i;
                break;
            }
        }
        if (firstSolution >= 0 && painAt > firstSolution) {
            return "Вернул. Сначала проблема, потом решение. Слайд про межгород должен быть раньше, чем слайды с чудом и пилотом. Переставь.";
        }
        return null;
    }
}
