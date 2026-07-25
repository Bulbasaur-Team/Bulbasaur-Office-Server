package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retro_moods")
@IdClass(RetroMoodEntity.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class RetroMoodEntity {

    @Id
    private UUID roomId;

    @Id
    private UUID playerId;

    private double value;

    private Instant updatedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private UUID roomId;
        private UUID playerId;
    }
}
