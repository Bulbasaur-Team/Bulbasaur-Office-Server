package ru.bulbasaur.office;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

class AuthRefreshIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("refresh выдаёт новый токен по живому jwt")
    void refreshIssuesNewTokenForValidJwt() {
        String token = register("refresher", "secret123");

        String refreshed = given()
                .header("Authorization", "Bearer " + token)
                .when().post("/api/account/refresh")
                .then().statusCode(200)
                .body("token", notNullValue())
                .body("token", not(equalTo(token)))
                .body("login", equalTo("refresher"))
                .extract().path("token");

        given().header("Authorization", "Bearer " + refreshed)
                .when().get("/api/account/profile")
                .then().statusCode(200)
                .body("login", equalTo("refresher"));
    }

    @Test
    @DisplayName("refresh без токена отклоняется")
    void refreshWithoutTokenIsRejected() {
        given().when().post("/api/account/refresh")
                .then().statusCode(403);
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
