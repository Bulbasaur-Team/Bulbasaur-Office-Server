package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;
import ru.bulbasaur.office.infra.ws.dto.ProjectorStateOut;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Состояние проектора по локации: кто крутит слайды и какой кадр сейчас на экране.
 * Эфемерно — при рестарте сервера проекторы выключаются.
 */
@Component
public class ProjectorRegistry {

    private static final int MAX_OWNER_ID_LENGTH = 64;
    private static final int MAX_INDEX = 10_000;

    private final Map<String, State> byLocation = new ConcurrentHashMap<>();

    public ProjectorStateOut snapshot(String locationId) {
        State state = byLocation.get(locationId);
        if (state == null || !state.on) {
            return ProjectorStateOut.off();
        }
        return ProjectorStateOut.on(state.ownerId, state.index);
    }

    /** Включить проектор (или сменить колоду). false — данные не приняты. */
    public boolean turnOn(String locationId, String ownerId) {
        if (locationId == null || locationId.isBlank()
                || ownerId == null || ownerId.isBlank() || ownerId.length() > MAX_OWNER_ID_LENGTH) {
            return false;
        }
        byLocation.put(locationId, new State(true, ownerId.strip(), 0));
        return true;
    }

    /** Выключить. true — раньше был включён. */
    public boolean turnOff(String locationId) {
        State prev = byLocation.put(locationId, State.OFF);
        return prev != null && prev.on;
    }

    /** Перелистнуть слайд. false — проектор выключен или индекс мусор. */
    public boolean setIndex(String locationId, int index) {
        if (index < 0 || index > MAX_INDEX) {
            return false;
        }
        State prev = byLocation.get(locationId);
        if (prev == null || !prev.on) {
            return false;
        }
        byLocation.put(locationId, new State(true, prev.ownerId, index));
        return true;
    }

    private record State(boolean on, String ownerId, int index) {
        static final State OFF = new State(false, null, 0);
    }
}
