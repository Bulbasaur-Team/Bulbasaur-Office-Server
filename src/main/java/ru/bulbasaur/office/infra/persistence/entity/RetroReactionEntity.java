package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "retro_reactions")
@Getter
@Setter
@NoArgsConstructor
public class RetroReactionEntity {

    public static final String TARGET_STICKER = "sticker";
    public static final String TARGET_MEME = "meme";

    @Id
    private UUID id;

    private String targetType;

    private UUID targetId;

    private UUID playerId;

    private String emoji;
}
