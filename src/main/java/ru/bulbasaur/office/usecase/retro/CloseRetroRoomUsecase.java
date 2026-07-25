package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroRoomJpaRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloseRetroRoomUsecase {

    private final RetroRoomJpaRepository rooms;

    @Transactional
    public void execute(UUID roomId) {
        rooms.findById(roomId).ifPresent(room -> {
            room.setStatus(RetroRoomEntity.STATUS_CLOSED);
            room.setClosedAt(Instant.now());
            rooms.save(room);
        });
    }
}
