package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;
import java.util.Map;

/**
 * Полное состояние ретро-комнаты (без байтов картинок).
 * Рассылается всем участникам при каждом изменении.
 */
public record RetroStateOut(
        String type,
        String id,
        String name,
        boolean isAdmin,
        boolean readOnly,
        long remainingMs,
        List<Participant> participants,
        List<Mood> moods,
        Map<String, List<Sticker>> stickers,
        List<Meme> memes
) {

    public record Participant(String login, String role, boolean admin) {
    }

    public record Mood(String login, String role, double value) {
    }

    public record Sticker(
            String id,
            String board,
            String text,
            String authorLogin,
            boolean mine,
            String groupId,
            List<Reaction> reactions
    ) {
    }

    public record Meme(
            String id,
            String authorLogin,
            boolean mine,
            String imageUrl,
            List<Reaction> reactions
    ) {
    }

    public record Reaction(String emoji, int count, List<String> logins) {
    }

    public static RetroStateOut of(
            String id,
            String name,
            boolean isAdmin,
            boolean readOnly,
            long remainingMs,
            List<Participant> participants,
            List<Mood> moods,
            Map<String, List<Sticker>> stickers,
            List<Meme> memes
    ) {
        return new RetroStateOut(
                "retroState", id, name, isAdmin, readOnly, remainingMs,
                participants, moods, stickers, memes);
    }
}
