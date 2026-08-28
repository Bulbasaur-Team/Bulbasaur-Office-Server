package ru.bulbasaur.office;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class QuestPlotIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("сюжет идёт по порядку: сначала пролог, холодильник закрыт")
    void plotStartsWithPrologueAndLocksFridge() {
        String token = register("plotter", "secret123");

        given().header("Authorization", "Bearer " + token)
                .when().get("/api/quests")
                .then().statusCode(200)
                .body("quests.find { it.code == 'prologue' }.status", equalTo("AVAILABLE"))
                .body("quests.find { it.code == 'adaptation' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'fridge_pin' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'green_alert' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'lost_package' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'strategy' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'reconciliation' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'presentation' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'quantum' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'day_x' }.status", equalTo("LOCKED"));

        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/api/quests/fridge_pin/start")
                .then().statusCode(400);

        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("{\"pin\":\"\"}")
                .when().post("/api/quests/prologue/complete")
                .then().statusCode(200)
                .body("code", equalTo("prologue"))
                .body("status", equalTo("COMPLETED"))
                .body("rewarded", equalTo(false));

        given().header("Authorization", "Bearer " + token)
                .when().get("/api/quests")
                .then().statusCode(200)
                .body("quests.find { it.code == 'prologue' }.status", equalTo("COMPLETED"))
                .body("quests.find { it.code == 'adaptation' }.status", equalTo("LOCKED"))
                .body("quests.find { it.code == 'fridge_pin' }.status", equalTo("LOCKED"));
    }

    private String register(String login, String password) {
        String body = jsonMapper.writeValueAsString(new AuthPayload(login, password));
        return given().contentType(ContentType.JSON).body(body)
                .when().post("/api/auth/register")
                .then().statusCode(200)
                .extract().path("token");
    }

    private record AuthPayload(String login, String password) {
    }
}
