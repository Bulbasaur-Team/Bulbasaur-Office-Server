package ru.bulbasaur.office.infra.persistence.repository;

import java.util.UUID;

/** Победитель закрытого сезона: номер сезона, логин и результат. */
public interface JumpSeasonWinnerProjection {

    String getLogin();

    UUID getPlayerId();

    long getValue();

    int getSeasonNumber();
}
