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
@Table(name = "retro_stickers")
@Getter
@Setter
@NoArgsConstructor
public class RetroStickerEntity {

    public static final String BOARD_GOOD = "good";
    public static final String BOARD_IMPROVE = "improve";
    public static final String BOARD_ACTIONS = "actions";

    @Id
    private UUID id;

    private UUID roomId;

    private String board;

    private UUID authorId;

    private String text;

    private UUID groupId;

    private int sortOrder;

    private Instant createdAt;
}
