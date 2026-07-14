package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Снимок метрик офиса за 5‑минутный бакет. */
@Entity
@Table(name = "office_metrics")
@Getter
@Setter
@NoArgsConstructor
public class OfficeMetricsEntity {

    @Id
    private Instant bucketStart;

    private int onlineCount;

    private int tennisKicks;

    private int volleyballKicks;

    private int coffeeCups;
}
