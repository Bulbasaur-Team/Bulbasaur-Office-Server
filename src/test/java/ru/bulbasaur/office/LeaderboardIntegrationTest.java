package ru.bulbasaur.office;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.bulbasaur.office.infra.persistence.repository.LeaderboardJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PlayerJpaRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class LeaderboardIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LeaderboardJpaRepository leaderboardRepository;

    @Autowired
    private PlayerJpaRepository playerRepository;

    @BeforeEach
    void cleanUp() {
        leaderboardRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void bulbaWordle_keepsBestScore_andOrdersDescending() {
        String ash = register("ash", "secret123");
        String misty = register("misty", "secret123");

        submit(ash, "bulbawordle", 5);
        submit(ash, "bulbawordle", 3);  // хуже (меньше слов) — игнорируется
        submit(ash, "bulbawordle", 9);  // лучше — заменяет
        submit(misty, "bulbawordle", 8);

        // От имени misty: топ по убыванию, своя строка помечена you=true.
        given().header("Authorization", bearer(misty))
                .when().get("/api/leaderboard/bulbawordle")
                .then().statusCode(200)
                .body("entries.size()", equalTo(2))
                .body("entries[0].login", equalTo("ash"))
                .body("entries[0].value", equalTo(9))
                .body("entries[0].rank", equalTo(1))
                .body("entries[0].you", equalTo(false))
                .body("entries[1].login", equalTo("misty"))
                .body("entries[1].value", equalTo(8))
                .body("entries[1].you", equalTo(true))
                .body("you.rank", equalTo(2))
                .body("you.value", equalTo(8))
                .body("you.you", equalTo(true));
    }

    @Test
    void bulbaColors_keepsBestScore_andOrdersDescending() {
        String ash = register("ash", "secret123");
        String misty = register("misty", "secret123");

        submit(ash, "bulbacolors", 5);
        submit(ash, "bulbacolors", 3);  // хуже — игнорируется
        submit(ash, "bulbacolors", 9);  // лучше — заменяет
        submit(misty, "bulbacolors", 8);

        given().header("Authorization", bearer(misty))
                .when().get("/api/leaderboard/bulbacolors")
                .then().statusCode(200)
                .body("entries.size()", equalTo(2))
                .body("entries[0].login", equalTo("ash"))
                .body("entries[0].value", equalTo(9))
                .body("entries[0].rank", equalTo(1))
                .body("entries[0].you", equalTo(false))
                .body("entries[1].login", equalTo("misty"))
                .body("entries[1].value", equalTo(8))
                .body("entries[1].you", equalTo(true))
                .body("you.rank", equalTo(2))
                .body("you.value", equalTo(8))
                .body("you.you", equalTo(true));
    }

    @Test
    void bulbaTanks_keepsBestScore_andOrdersDescending() {
        String ash = register("ash", "secret123");
        String misty = register("misty", "secret123");

        submit(ash, "bulbatanks", 5);
        submit(ash, "bulbatanks", 3);  // хуже — игнорируется
        submit(ash, "bulbatanks", 9);  // лучше — заменяет
        submit(misty, "bulbatanks", 8);

        given().header("Authorization", bearer(misty))
                .when().get("/api/leaderboard/bulbatanks")
                .then().statusCode(200)
                .body("entries.size()", equalTo(2))
                .body("entries[0].login", equalTo("ash"))
                .body("entries[0].value", equalTo(9))
                .body("entries[0].rank", equalTo(1))
                .body("entries[0].you", equalTo(false))
                .body("entries[1].login", equalTo("misty"))
                .body("entries[1].value", equalTo(8))
                .body("entries[1].you", equalTo(true))
                .body("you.rank", equalTo(2))
                .body("you.value", equalTo(8))
                .body("you.you", equalTo(true));
    }

    @Test
    void bulbaParking_lowerTimeIsBetter() {
        String racer = register("racer", "secret123");

        submit(racer, "bulbaparking", 100);
        submit(racer, "bulbaparking", 80);   // лучше (меньше время) — заменяет
        submit(racer, "bulbaparking", 120);  // хуже — игнорируется

        given().header("Authorization", bearer(racer))
                .when().get("/api/leaderboard/bulbaparking")
                .then().statusCode(200)
                .body("entries.size()", equalTo(1))
                .body("entries[0].login", equalTo("racer"))
                .body("entries[0].value", equalTo(80))
                .body("entries[0].rank", equalTo(1))
                .body("entries[0].you", equalTo(true))
                .body("you.value", equalTo(80));
    }

    @Test
    void unknownGame_returns404() {
        String player = register("bob", "secret123");

        given().header("Authorization", bearer(player))
                .contentType(ContentType.JSON)
                .body("{\"value\":1}")
                .when().post("/api/leaderboard/notagame")
                .then().statusCode(404);
    }

    @Test
    void leaderboardWithoutToken_isRejected() {
        given()
                .when().get("/api/leaderboard/bulbawordle")
                .then().statusCode(403);
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
