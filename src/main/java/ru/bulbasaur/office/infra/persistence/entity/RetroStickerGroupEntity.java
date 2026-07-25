package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "retro_sticker_groups")
@Getter
@Setter
@NoArgsConstructor
public class RetroStickerGroupEntity {

    @Id
    private UUID id;

    private UUID roomId;

    private String board;
}
