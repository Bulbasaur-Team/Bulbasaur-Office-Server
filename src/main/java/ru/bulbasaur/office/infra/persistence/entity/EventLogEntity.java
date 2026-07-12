package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Строка журнала событий Бульба Офиса. */
@Entity
@Table(name = "event_log")
@Getter
@Setter
@NoArgsConstructor
public class EventLogEntity {

    @Id
    private UUID id;

    private Instant createdAt;

    private String level;

    private String logger;

    @Column(columnDefinition = "text")
    private String message;
}
