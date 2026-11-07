package ru.bulbasaur.office;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.JumpSeasonCalendar;
import ru.bulbasaur.office.infra.persistence.entity.BulbaCoinLedgerEntity;
import ru.bulbasaur.office.infra.persistence.entity.EventLogEntity;
import ru.bulbasaur.office.infra.persistence.entity.JumpSeasonEntity;
import ru.bulbasaur.office.infra.persistence.entity.LeaderboardEntryEntity;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.infra.persistence.repository.BulbaCoinLedgerJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.EventLogJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.JumpSeasonJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.JumpSeasonScoreJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.LeaderboardJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PlayerJpaRepository;
import ru.bulbasaur.office.usecase.JumpSeasonUsecase;
import ru.bulbasaur.office.usecase.dto.JumpSeasonView;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class JumpSeasonIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JumpSeasonUsecase seasons;

    @Autowired
    private JumpSeasonJpaRepository seasonRepository;

    @Autowired
    private JumpSeasonScoreJpaRepository scoreRepository;

    @Autowired
    private LeaderboardJpaRepository leaderboardRepository;

    @Autowired
    private PlayerJpaRepository playerRepository;

    @Autowired
    private EventLogJpaRepository eventLogRepository;

    @Autowired
    private BulbaCoinLedgerJpaRepository ledgerRepository;

    @BeforeEach
    void cleanUp() {
        scoreRepository.deleteAll();
        seasonRepository.deleteBySeasonNumberGreaterThan(1);
        seasonRepository.findBySeasonNumber(1).ifPresent(season -> {
            season.setAwardedAt(null);
            season.setStartsAt(JumpSeasonCalendar.startOf(1));
            season.setEndsAt(JumpSeasonCalendar.endOf(1));
            seasonRepository.save(season);
        });
        leaderboardRepository.deleteAll();
        playerRepository.deleteAll();
        eventLogRepository.deleteAll();
    }

    @Test
    void firstSeason_isSeededForFourWeeksFrom30Oct2026() {
        JumpSeasonEntity first = seasonRepository.findBySeasonNumber(1).orElseThrow();
        assertEquals(JumpSeasonCalendar.startOf(1), first.getStartsAt());
        assertEquals(JumpSeasonCalendar.endOf(1), first.getEndsAt());
    }

    @Test
    void seasonBoard_countsOnlyScoresMadeInsideTheWindow() {
        Instant now = Instant.now();
        int number = JumpSeasonCalendar.numberAt(now);
        assumeTrue(number >= 1, "сезоны ещё не начались");

        String ash = register("ash", "secret123");
        String misty = register("misty", "secret123");
        PlayerEntity ashPlayer = playerRepository.findByLogin("ash").orElseThrow();

        LeaderboardEntryEntity ancient = new LeaderboardEntryEntity();
        ancient.setId(UUID.randomUUID());
        ancient.setPlayer(ashPlayer);
        ancient.setGame(GameId.BULBA_JUMP);
        ancient.setValue(999_999L);
        ancient.setUpdatedAt(JumpSeasonCalendar.startOf(1).minus(Duration.ofDays(1)));
        leaderboardRepository.save(ancient);

        submit(ash, "bulbajump", 1500);
        submit(ash, "bulbajump", 400); // хуже сезонного рекорда — не затирает
        submit(misty, "bulbapacker", 9_000);

        seasons.catchUp(now);
        assertTrue(seasonRepository.findBySeasonNumber(number + 1).isEmpty());
        assertNull(seasonRepository.findBySeasonNumber(number).orElseThrow().getAwardedAt());

        given().header("Authorization", bearer(ash))
                .when().get("/api/jump-season")
                .then().statusCode(200)
                .body("number", equalTo(number))
                .body("prizes", equalTo(List.of(10_000, 5_000, 3_000)))
                .body("previousWinner.season", equalTo(0))
                .body("previousWinner.login", equalTo("Sodnom"))
                .body("previousWinner.value", equalTo(729_467))
                .body("entries.size()", equalTo(1))
                .body("entries[0].login", equalTo("ash"))
                .body("entries[0].value", equalTo(1500))
                .body("entries[0].rank", equalTo(1))
                .body("entries[0].you", equalTo(true))
                .body("you.value", equalTo(1500));

        String startsAt = given().header("Authorization", bearer(ash))
                .when().get("/api/jump-season")
                .then().statusCode(200)
                .extract().path("startsAt");
        String endsAt = given().header("Authorization", bearer(ash))
                .when().get("/api/jump-season")
                .then().statusCode(200)
                .extract().path("endsAt");
        assertEquals(JumpSeasonCalendar.startOf(number), Instant.parse(startsAt));
        assertEquals(JumpSeasonCalendar.endOf(number), Instant.parse(endsAt));

        given().header("Authorization", bearer(ash))
                .when().get("/api/leaderboard/bulbajump")
                .then().statusCode(200)
                .body("entries[0].value", equalTo(999_999));

        given().header("Authorization", bearer(misty))
                .when().get("/api/jump-season")
                .then().statusCode(200)
                .body("you", nullValue());
    }

    @Test
    void finishedSeason_paysTopThree_logsOnce_andOpensTheNext() {
        Instant now = Instant.now();
        int number = JumpSeasonCalendar.numberAt(now);
        assumeTrue(number >= 1, "сезоны ещё не начались");

        String ash = register("ash", "secret123");
        String misty = register("misty", "secret123");
        String brock = register("brock", "secret123");
        submit(ash, "bulbajump", 300);
        submit(misty, "bulbajump", 200);
        submit(brock, "bulbajump", 100);

        Instant end = JumpSeasonCalendar.endOf(number);
        seasons.catchUp(end);

        assertSeasonPrize("ash", number, 1, 10_000L);
        assertSeasonPrize("misty", number, 2, 5_000L);
        assertSeasonPrize("brock", number, 3, 3_000L);
        long ashBalance = balance("ash");
        assertTrue(seasonRepository.findBySeasonNumber(number).orElseThrow().getAwardedAt() != null);
        assertTrue(seasonRepository.findBySeasonNumber(number + 1).isPresent());
        assertNull(seasonRepository.findBySeasonNumber(number + 1).orElseThrow().getAwardedAt());

        List<String> payouts = eventLogRepository.findAll().stream()
                .filter(row -> "jump-season-service".equals(row.getLogger()))
                .map(EventLogEntity::getMessage)
                .filter(message -> message.contains("Начислены Bulba Coins"))
                .toList();
        assertEquals(1, payouts.size());
        assertTrue(payouts.getFirst().contains("ash"));
        assertTrue(payouts.getFirst().contains("10000 Bulba Coins"));
        assertTrue(payouts.getFirst().contains("misty"));
        assertTrue(payouts.getFirst().contains("5000 Bulba Coins"));
        assertTrue(payouts.getFirst().contains("brock"));
        assertTrue(payouts.getFirst().contains("3000 Bulba Coins"));

        seasons.catchUp(end);
        assertEquals(ashBalance, balance("ash"));
        assertSeasonPrize("ash", number, 1, 10_000L);
        assertEquals(1, eventLogRepository.findAll().stream()
                .filter(row -> row.getMessage().contains("Начислены Bulba Coins"))
                .count());

        UUID mistyId = playerRepository.findByLogin("misty").orElseThrow().getId();
        seasons.record(mistyId, 50, end);
        JumpSeasonView next = seasons.current(mistyId, "misty", 20, end);
        assertEquals(number + 1, next.number());
        assertEquals(number, next.previousWinner().season());
        assertEquals("ash", next.previousWinner().login());
        assertEquals(300L, next.previousWinner().value());
        assertEquals(1, next.top().size());
        assertEquals("misty", next.top().getFirst().login());
        assertEquals(50L, next.top().getFirst().value());

        UUID closedId = seasonRepository.findBySeasonNumber(number).orElseThrow().getId();
        UUID nextId = seasonRepository.findBySeasonNumber(number + 1).orElseThrow().getId();
        assertEquals(200L, scoreRepository.findValue(closedId, mistyId).orElseThrow());
        assertEquals(50L, scoreRepository.findValue(nextId, mistyId).orElseThrow());
    }

    @Test
    void seasonEndedBeforeTheCalendar_paysAndOpensTheNextAtThatMoment() {
        Instant now = Instant.now();
        int number = JumpSeasonCalendar.numberAt(now);
        assumeTrue(number >= 1, "сезоны ещё не начались");
        JumpSeasonEntity current = seasonRepository.findBySeasonNumber(number).orElseThrow();
        Instant earlyEnd = now.minus(Duration.ofMinutes(1));
        assumeTrue(earlyEnd.isAfter(current.getStartsAt()), "окно сезона ещё не началось");

        String ash = register("ash", "secret123");
        submit(ash, "bulbajump", 42);
        long before = balance("ash");
        current.setEndsAt(earlyEnd);
        seasonRepository.saveAndFlush(current);

        UUID ashId = playerRepository.findByLogin("ash").orElseThrow().getId();
        JumpSeasonView view = seasons.current(ashId, "ash", 20, now);

        assertEquals(number + 1, view.number());
        assertEquals(earlyEnd, view.startsAt());
        assertEquals(earlyEnd.plus(JumpSeasonCalendar.LENGTH), view.endsAt());
        assertEquals(number, view.previousWinner().season());
        assertEquals("ash", view.previousWinner().login());
        assertEquals(42L, view.previousWinner().value());
        assertSeasonPrize("ash", number, 1, 10_000L);
        assertEquals(before + 10_000L, balance("ash"));
        assertNull(view.me());

        seasons.record(ashId, 7, now);
        JumpSeasonView played = seasons.current(ashId, "ash", 20, now);
        assertEquals(7L, played.me().value());
        assertEquals(before + 10_000L, balance("ash"));
    }

    @Test
    void jumpSeasonWithoutToken_isRejected() {
        given()
                .contentType(ContentType.JSON)
                .when().get("/api/jump-season")
                .then().statusCode(403);
    }

    private long balance(String login) {
        UUID id = playerRepository.findByLogin(login).orElseThrow().getId();
        return playerRepository.findBalance(id).orElseThrow();
    }

    private void assertSeasonPrize(String login, int seasonNumber, int place, long coins) {
        UUID id = playerRepository.findByLogin(login).orElseThrow().getId();
        BulbaCoinLedgerEntity row = ledgerRepository
                .findByPlayerIdAndKindAndRef(id, "JUMP_SEASON", "jump-season:" + seasonNumber + ":" + place)
                .orElseThrow();
        assertEquals(coins, row.getAmount());
    }

    private String register(String login, String password) {
        String body = jsonMapper.writeValueAsString(new AuthPayload(login, password));
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/register")
                .then().statusCode(200)
                .extract().path("token");
    }

    private void submit(String token, String game, long value) {
        given().header("Authorization", bearer(token))
                .contentType(ContentType.JSON)
                .body("{\"value\":" + value + "}")
                .when().post("/api/leaderboard/" + game)
                .then().statusCode(200);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record AuthPayload(String login, String password) {
    }
}
