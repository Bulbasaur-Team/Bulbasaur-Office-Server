package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroMoodEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroMoodJpaRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpsertRetroMoodUsecase {

    private final RetroMoodJpaRepository moods;

    @Transactional
    public void execute(UUID roomId, UUID playerId, double value) {
        double clamped = Math.max(0, Math.min(1, value));
        RetroMoodEntity mood = moods.findById(new RetroMoodEntity.Pk(roomId, playerId))
                .orElseGet(() -> {
                    RetroMoodEntity m = new RetroMoodEntity();
                    m.setRoomId(roomId);
                    m.setPlayerId(playerId);
                    return m;
                });
        mood.setValue(clamped);
        mood.setUpdatedAt(Instant.now());
        moods.save(mood);
    }
}
