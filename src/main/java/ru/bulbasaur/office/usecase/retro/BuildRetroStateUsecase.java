package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroMoodEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroReactionEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroMemeJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroMoodJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroReactionJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;
import ru.bulbasaur.office.infra.ws.RetroRoom;
import ru.bulbasaur.office.infra.ws.dto.RetroStateOut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildRetroStateUsecase {

    private final RetroMoodJpaRepository moods;
    private final RetroStickerJpaRepository stickers;
    private final RetroMemeJpaRepository memes;
    private final RetroReactionJpaRepository reactions;
    private final GetRetroRoomsUsecase rooms;

    @Transactional(readOnly = true)
    public RetroStateOut execute(
            RetroRoomEntity room,
            UUID viewerId,
            List<RetroRoom.Participant> onlineParticipants,
            long remainingMs,
            boolean readOnly
    ) {
        UUID roomId = room.getId();
        Set<UUID> playerIds = new HashSet<>();
        playerIds.add(room.getAdminPlayerId());

        List<RetroMoodEntity> moodRows = moods.findByRoomId(roomId);
        for (RetroMoodEntity m : moodRows) {
            playerIds.add(m.getPlayerId());
        }

        List<RetroStickerEntity> stickerRows = stickers.findByRoomIdOrderBySortOrderAscCreatedAtAsc(roomId);
        for (RetroStickerEntity s : stickerRows) {
            playerIds.add(s.getAuthorId());
        }

        List<RetroMemeJpaRepository.MemeMeta> memeRows = memes.findMetaByRoomId(roomId);
        for (RetroMemeJpaRepository.MemeMeta m : memeRows) {
            playerIds.add(m.getAuthorId());
        }

        List<UUID> stickerIds = stickerRows.stream().map(RetroStickerEntity::getId).toList();
        List<UUID> memeIds = memeRows.stream().map(RetroMemeJpaRepository.MemeMeta::getId).toList();

        List<RetroReactionEntity> stickerReactions = stickerIds.isEmpty()
                ? List.of()
                : reactions.findByTargetTypeAndTargetIdIn(RetroReactionEntity.TARGET_STICKER, stickerIds);
        List<RetroReactionEntity> memeReactions = memeIds.isEmpty()
                ? List.of()
                : reactions.findByTargetTypeAndTargetIdIn(RetroReactionEntity.TARGET_MEME, memeIds);

        for (RetroReactionEntity r : stickerReactions) {
            playerIds.add(r.getPlayerId());
        }
        for (RetroReactionEntity r : memeReactions) {
            playerIds.add(r.getPlayerId());
        }
        for (RetroRoom.Participant p : onlineParticipants) {
            playerIds.add(p.playerId());
        }

        Map<UUID, String> logins = rooms.loginMap(playerIds);
        Map<UUID, String> roles = rooms.roleMap(playerIds);

        List<RetroStateOut.Participant> participants = new ArrayList<>();
        if (onlineParticipants.isEmpty() && readOnly) {
            participants.add(new RetroStateOut.Participant(
                    logins.getOrDefault(room.getAdminPlayerId(), "?"),
                    roles.get(room.getAdminPlayerId()),
                    true));
        } else {
            for (RetroRoom.Participant p : onlineParticipants) {
                participants.add(new RetroStateOut.Participant(
                        p.login(), p.role(), room.getAdminPlayerId().equals(p.playerId())));
            }
        }

        List<RetroStateOut.Mood> moodViews = new ArrayList<>();
        for (RetroMoodEntity m : moodRows) {
            moodViews.add(new RetroStateOut.Mood(
                    logins.getOrDefault(m.getPlayerId(), "?"),
                    roles.get(m.getPlayerId()),
                    m.getValue()));
        }

        Map<UUID, List<RetroReactionEntity>> reactionsBySticker = stickerReactions.stream()
                .collect(Collectors.groupingBy(RetroReactionEntity::getTargetId));
        Map<UUID, List<RetroReactionEntity>> reactionsByMeme = memeReactions.stream()
                .collect(Collectors.groupingBy(RetroReactionEntity::getTargetId));

        Map<String, List<RetroStateOut.Sticker>> boards = new LinkedHashMap<>();
        for (String board : RetroConstants.BOARDS) {
            boards.put(board, new ArrayList<>());
        }
        for (RetroStickerEntity s : stickerRows) {
            boards.computeIfAbsent(s.getBoard(), k -> new ArrayList<>()).add(new RetroStateOut.Sticker(
                    s.getId().toString(),
                    s.getBoard(),
                    s.getText(),
                    logins.getOrDefault(s.getAuthorId(), "?"),
                    s.getAuthorId().equals(viewerId),
                    s.getGroupId() == null ? null : s.getGroupId().toString(),
                    aggregateReactions(reactionsBySticker.getOrDefault(s.getId(), List.of()), logins)));
        }

        List<RetroStateOut.Meme> memeViews = new ArrayList<>();
        for (RetroMemeJpaRepository.MemeMeta m : memeRows) {
            memeViews.add(new RetroStateOut.Meme(
                    m.getId().toString(),
                    logins.getOrDefault(m.getAuthorId(), "?"),
                    m.getAuthorId().equals(viewerId),
                    "/api/retro/memes/" + m.getId(),
                    aggregateReactions(reactionsByMeme.getOrDefault(m.getId(), List.of()), logins)));
        }

        return RetroStateOut.of(
                roomId.toString(),
                room.getName(),
                room.getAdminPlayerId().equals(viewerId),
                readOnly,
                remainingMs,
                participants,
                moodViews,
                boards,
                memeViews);
    }

    private List<RetroStateOut.Reaction> aggregateReactions(
            List<RetroReactionEntity> rows, Map<UUID, String> logins) {
        Map<String, List<String>> byEmoji = new LinkedHashMap<>();
        for (RetroReactionEntity r : rows) {
            byEmoji.computeIfAbsent(r.getEmoji(), k -> new ArrayList<>())
                    .add(logins.getOrDefault(r.getPlayerId(), "?"));
        }
        List<RetroStateOut.Reaction> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : byEmoji.entrySet()) {
            out.add(new RetroStateOut.Reaction(e.getKey(), e.getValue().size(), e.getValue()));
        }
        return out;
    }
}
