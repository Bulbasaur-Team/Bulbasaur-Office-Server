package ru.bulbasaur.office.usecase.port.out;

import java.time.LocalDate;

/**
 * Непрозрачный сид слова дня для (namespace, дата). Само слово по сети не передаётся -
 * клиент выводит его из сида и своего словаря, поэтому по запросам слово не отследить
 */
public interface WotdSeedPort {

    String seed(String namespace, LocalDate date);
}
