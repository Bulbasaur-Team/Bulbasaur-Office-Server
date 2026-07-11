package ru.bulbasaur.office.infra.persistence.repository;

/** Число владельцев одной ачивки (по коду). */
public interface AchievementOwnersProjection {

    String getCode();

    long getOwners();
}
