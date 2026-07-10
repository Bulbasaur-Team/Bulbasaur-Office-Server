package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Выданная игроку ачивка. Запись идёт через native upsert (не вставляем повторно),
 * сущность нужна для чтения. Ачивка хранится по строковому коду {@code Achievement.code}.
 */
@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
public class AchievementEntity {

    @Id
    private UUID id;

    private UUID playerId;

    private String code;

    private Instant grantedAt;
}
