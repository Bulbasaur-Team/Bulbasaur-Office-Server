package ru.bulbasaur.office;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.bulbasaur.office.usecase.GetOfficeMetricsUsecase;
import ru.bulbasaur.office.usecase.RecordOfficeMetricsTickUsecase;
import ru.bulbasaur.office.usecase.dto.OfficeMetricsPoint;
import ru.bulbasaur.office.usecase.port.out.LiveMetricsPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

class MetricsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LiveMetricsPort liveMetrics;

    @Autowired
    private RecordOfficeMetricsTickUsecase recordTick;

    @Autowired
    private GetOfficeMetricsUsecase getMetrics;

    @Autowired
    private PlayerRepositoryPort players;

    @Test
    void metricsWithoutToken_isRejected() {
        given()
                .when().get("/api/metrics")
                .then().statusCode(403);
    }

    @Test
    void metrics_return_dense_series_and_live_bucket() {
        String token = register("metrics-user", "secret123");
        UUID playerId = players.findByLogin("metrics-user").orElseThrow().id();

        liveMetrics.recordTennisKick();
        liveMetrics.recordTennisKick();
        liveMetrics.recordVolleyballKick();
        liveMetrics.recordCoffeeCup();

        List<OfficeMetricsPoint> points = getMetrics.execute(playerId);
        assertThat(points).isNotEmpty();
        OfficeMetricsPoint current = points.get(points.size() - 1);
        assertThat(current.tennisKicks()).isEqualTo(2);
        assertThat(current.volleyballKicks()).isEqualTo(1);
        assertThat(current.coffeeCups()).isEqualTo(1);

        recordTick.execute();
        // После тика live обнулился — текущий бакет снова пуст, а прошлый записан в БД.
        List<OfficeMetricsPoint> after = getMetrics.execute(playerId);
        OfficeMetricsPoint liveNow = after.get(after.size() - 1);
        assertThat(liveNow.tennisKicks()).isZero();
        assertThat(after.stream().mapToInt(OfficeMetricsPoint::tennisKicks).sum()).isEqualTo(2);

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/metrics")
                .then()
                .statusCode(200)
                .body("bucketMinutes", equalTo(5))
                .body("points.size()", greaterThan(100));
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
