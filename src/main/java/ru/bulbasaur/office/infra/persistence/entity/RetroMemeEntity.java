package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retro_memes")
@Getter
@Setter
@NoArgsConstructor
public class RetroMemeEntity {

    @Id
    private UUID id;

    private UUID roomId;

    private UUID authorId;

    private String mimeType;

    /** PostgreSQL bytea (не oid/@Lob). */
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] imageData;

    private Instant createdAt;
}
