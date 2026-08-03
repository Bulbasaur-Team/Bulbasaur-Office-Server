package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bulba_coin_ledger")
@Getter
@Setter
@NoArgsConstructor
public class BulbaCoinLedgerEntity {

    @Id
    private UUID id;

    private UUID playerId;

    private long amount;

    private String kind;

    private String ref;

    private String title;

    private Instant createdAt;
}
