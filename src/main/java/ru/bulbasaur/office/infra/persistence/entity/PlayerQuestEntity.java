package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.bulbasaur.office.domain.model.QuestStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_quests")
@Getter
@Setter
@NoArgsConstructor
public class PlayerQuestEntity {

    @Id
    private UUID id;

    private UUID playerId;

    private String questCode;

    @Enumerated(EnumType.STRING)
    private QuestStatus status;

    private Instant startedAt;

    private Instant completedAt;
}
