package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;

/**
 * Единственный стол аэрохоккея — в чилл-зоне. Состояние в памяти.
 */
@Component
public class AirHockeyRegistry {

    private final AirHockeyTable table = new AirHockeyTable();

    public AirHockeyTable table() {
        return table;
    }
}
