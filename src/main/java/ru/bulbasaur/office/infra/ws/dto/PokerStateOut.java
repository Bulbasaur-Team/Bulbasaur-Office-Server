package ru.bulbasaur.office.infra.ws.dto;

import java.util.List;

/**
 * Полное состояние покер-комнаты, персонализированное под получателя (isAdmin,
 * myVote). Рассылается всем участникам при каждом изменении — состояние маленькое,
 * и так клиент никогда не рассинхронизируется.
 */
public record PokerStateOut(String type, String id, String name, boolean isAdmin,
                            long remainingMs, String myVote,
                            List<Participant> participants, Current current,
                            List<DoneTask> tasks) {

    /** Участник комнаты; voted — отдал ли голос в текущем голосовании. */
    public record Participant(String login, String role, boolean admin, boolean voted) {
    }

    /** Текущая задача; votes заполняются только после вскрытия. */
    public record Current(String title, boolean revealed, Double average, Integer recommended,
                          List<Vote> votes) {
    }

    /** Вскрытый голос. */
    public record Vote(String login, String role, String value) {
    }

    /** Завершённая задача из списка вверху экрана. */
    public record DoneTask(String title, Double average, Integer recommended) {
    }

    public static PokerStateOut of(String id, String name, boolean isAdmin,
                                   long remainingMs, String myVote,
                                   List<Participant> participants, Current current,
                                   List<DoneTask> tasks) {
        return new PokerStateOut("pokerState", id, name, isAdmin, remainingMs, myVote,
                participants, current, tasks);
    }
}
