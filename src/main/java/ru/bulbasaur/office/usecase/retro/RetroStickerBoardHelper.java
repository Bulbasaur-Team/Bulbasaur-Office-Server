package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerGroupJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Общий порядок стикеров на доске: renumber / coalesce / cleanup групп. */
@Component
@RequiredArgsConstructor
public class RetroStickerBoardHelper {

    private final RetroStickerJpaRepository stickers;
    private final RetroStickerGroupJpaRepository groups;

    public List<RetroStickerEntity> boardStickers(UUID roomId, String board) {
        return stickers.findByRoomIdAndBoardOrderBySortOrderAscCreatedAtAsc(roomId, board);
    }

    public static int indexOf(List<RetroStickerEntity> list, UUID id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Transactional
    public void renumber(UUID roomId, String board, UUID movingId, UUID beforeId) {
        List<RetroStickerEntity> ordered = new ArrayList<>(boardStickers(roomId, board));
        RetroStickerEntity moving = null;
        for (RetroStickerEntity s : ordered) {
            if (s.getId().equals(movingId)) {
                moving = s;
                break;
            }
        }
        if (moving == null) {
            return;
        }
        ordered.removeIf(s -> s.getId().equals(movingId));
        int idx = ordered.size();
        if (beforeId != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).getId().equals(beforeId)) {
                    idx = i;
                    break;
                }
            }
        }
        ordered.add(idx, moving);
        applyOrder(ordered);
    }

    /**
     * Все стикеры одной группы идут подряд в sort_order
     * (позиция группы — место первого встретившегося члена).
     */
    @Transactional
    public void coalesceGroups(UUID roomId, String board) {
        List<RetroStickerEntity> ordered = boardStickers(roomId, board);
        Set<UUID> emitted = new HashSet<>();
        List<RetroStickerEntity> rebuilt = new ArrayList<>();
        for (RetroStickerEntity s : ordered) {
            UUID gid = s.getGroupId();
            if (gid == null) {
                rebuilt.add(s);
                continue;
            }
            if (emitted.contains(gid)) {
                continue;
            }
            emitted.add(gid);
            for (RetroStickerEntity m : ordered) {
                if (gid.equals(m.getGroupId())) {
                    rebuilt.add(m);
                }
            }
        }
        applyOrder(rebuilt);
    }

    @Transactional
    public void cleanupGroup(UUID groupId) {
        if (groupId == null) {
            return;
        }
        List<RetroStickerEntity> members = stickers.findByGroupId(groupId);
        if (members.size() <= 1) {
            for (RetroStickerEntity s : members) {
                s.setGroupId(null);
                stickers.save(s);
            }
            groups.deleteById(groupId);
        }
    }

    @Transactional
    public void applyOrder(List<RetroStickerEntity> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            RetroStickerEntity s = ordered.get(i);
            if (s.getSortOrder() != i) {
                s.setSortOrder(i);
                stickers.save(s);
            }
        }
    }
}
