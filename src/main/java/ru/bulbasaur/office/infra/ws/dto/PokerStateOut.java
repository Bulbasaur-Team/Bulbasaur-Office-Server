package ru.bulbasaur.office.infra.ws.dto;

import lombok.Builder;
import ru.bulbasaur.office.domain.model.PlayerAppearance;

import java.util.List;

/**
 * Полное состояние покер-комнаты, персонализированное под получателя (isAdmin,
 * myVote). Рассылается всем участникам при каждом изменении — состояние маленькое,
 * и так клиент никогда не рассинхронизируется.
 */
@Builder
public record PokerStateOut(String type, String id, String name, boolean isAdmin, boolean readOnly,
                            long remainingMs, String myVote,
                            List<Participant> participants, Current current,
                            List<DoneTask> tasks) {

    /** Участник комнаты; voted — отдал ли голос в текущем голосовании. */
    @Builder
    public record Participant(String login, PlayerAppearance appearance, boolean admin, boolean voted) {
    }

    /** Текущая задача; votes заполняются только после вскрытия. */
    @Builder
    public record Current(String title, boolean revealed, Double average, Integer recommended,
                          List<Vote> votes) {
    }

    /** Вскрытый голос. */
    @Builder
    public record Vote(String login, PlayerAppearance appearance, String value) {
    }

    /** Завершённая задача из списка вверху экрана; votes — для истории. */
    @Builder
    public record DoneTask(String title, Double average, Integer recommended, List<Vote> votes) {
    }
}
