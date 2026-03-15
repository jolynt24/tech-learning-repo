package org.fractalschema;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.fractalschema.auth.User;
import org.fractalschema.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class AnalyticsResourceTest {

    @Inject
    JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    @Transactional
    void setup() {
        User user = new User();
        user.setUsername("analyticsuser");
        user.setEmail("analyticsuser@example.com");
        user.setPassword(BcryptUtil.bcryptHash("password123"));
        user.setRoles("user");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.persist();

        token = jwtUtil.generateAccessToken("analyticsuser", "user");
    }

    @AfterEach
    @Transactional
    void cleanup() {
        User.delete("username", "analyticsuser");
    }

    // ── STREAKS ────────────────────────────────────────────────────────────────

    @Test
    void getStreaks_noActiveGoals_shouldReturnEmptyList() {
        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    void getStreaks_withGoalButNoEntries_shouldReturnStreakOfZero() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 7.0, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].goalType", equalTo("SLEEP"))
            .body("[0].currentStreak", equalTo(0))
            .body("[0].activeToday", equalTo(false))
            .body("[0].calculatedAt", notNullValue());
    }

    @Test
    void getStreaks_goalMetYesterday_shouldReturnStreakOfOne() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 7.0, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": 8.0 }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("[0].currentStreak", equalTo(1))
            .body("[0].longestStreak", equalTo(1));
    }

    @Test
    void getStreaks_goalNotMetYesterday_shouldReturnStreakOfZero() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 8.0, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        // Sleep hours below target
        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": 5.0 }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("[0].currentStreak", equalTo(0));
    }

    @Test
    void getStreaks_goalMetConsecutiveDays_shouldReturnCorrectCount() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WATER", "target": 2000, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        for (int i = 1; i <= 3; i++) {
            String date = LocalDate.now().minusDays(i).toString();
            given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"entryDate\": \"%s\", \"waterMl\": 2500 }", date))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("[0].currentStreak", equalTo(3));
    }

    @Test
    void getStreaks_goalMetTodayButNotYesterday_activeTodayTrueStreakZero() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WATER", "target": 2000, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        // Only today's entry, not yesterday's
        String today = LocalDate.now().toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"waterMl\": 2500 }", today))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("[0].activeToday", equalTo(true))
            .body("[0].currentStreak", equalTo(0));
    }

    @Test
    void getStreaks_workoutGoalMet_shouldRecogniseBooleanGoal() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WORKOUT", "target": 1, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"workoutDone\": true }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("[0].goalType", equalTo("WORKOUT"))
            .body("[0].currentStreak", equalTo(1));
    }

    @Test
    void getStreaks_inactiveGoal_shouldNotAppearInResults() {
        int id = given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "READING", "target": 20, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"active\": false }")
            .when().patch("/api/goals/" + id).then().statusCode(200);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    void getStreaks_unauthenticated_shouldReturn401() {
        given()
            .when().get("/api/analytics/streaks")
            .then()
            .statusCode(401);
    }

    // ── TRENDS ─────────────────────────────────────────────────────────────────

    @Test
    void getTrends_noData_shouldReturnZeroStats() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "SLEEP")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("metric", equalTo("SLEEP"))
            .body("period", equalTo(7))
            .body("average", equalTo(0.0f))
            .body("min", equalTo(0.0f))
            .body("max", equalTo(0.0f))
            .body("dataPoints", empty());
    }

    @Test
    void getTrends_withSleepData_shouldReturnCorrectStats() {
        // 3 entries: sleepHours 7, 8, 9 → avg 8.0, min 7.0, max 9.0
        for (int i = 1; i <= 3; i++) {
            String date = LocalDate.now().minusDays(i).toString();
            given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": %d.0 }", date, 6 + i))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "SLEEP")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("dataPoints.size()", equalTo(3))
            .body("average", equalTo(8.0f))
            .body("min", equalTo(7.0f))
            .body("max", equalTo(9.0f))
            .body("trendDirection", notNullValue());
    }

    @Test
    void getTrends_dataPoints_shouldBeInAscendingDateOrder() {
        for (int i = 3; i >= 1; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": %d.0 }", date, 6 + i))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "SLEEP")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("dataPoints[0].time", equalTo(LocalDate.now().minusDays(3).toString()))
            .body("dataPoints[1].time", equalTo(LocalDate.now().minusDays(2).toString()))
            .body("dataPoints[2].time", equalTo(LocalDate.now().minusDays(1).toString()));
    }

    @Test
    void getTrends_withWaterData_shouldReturn200() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"waterMl\": 2500 }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "WATER")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("metric", equalTo("WATER"))
            .body("dataPoints.size()", equalTo(1))
            .body("average", equalTo(2500.0f));
    }

    @Test
    void getTrends_withWorkoutData_shouldReturn200() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"workoutDone\": true, \"workoutDurationMin\": 45 }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "WORKOUT")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("metric", equalTo("WORKOUT"))
            .body("dataPoints.size()", equalTo(1))
            .body("average", equalTo(45.0f));
    }

    @Test
    void getTrends_withReadingData_shouldReturn200() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"readingMinutes\": 30 }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "READING")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("metric", equalTo("READING"))
            .body("average", equalTo(30.0f));
    }

    @Test
    void getTrends_withHobbyData_shouldReturn200() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"hobbyDurationMin\": 60 }", yesterday))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "HOBBY")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("metric", equalTo("HOBBY"))
            .body("average", equalTo(60.0f));
    }

    @Test
    void getTrends_increasingValues_shouldReturnIncreasingDirection() {
        // 4 entries: values clearly increasing (1, 2, 5, 10)
        int[] hours = {1, 2, 5, 10};
        for (int i = 0; i < hours.length; i++) {
            String date = LocalDate.now().minusDays(hours.length - i).toString();
            given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": %d.0 }", date, hours[i]))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "SLEEP")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("trendDirection", equalTo("INCREASING"));
    }

    @Test
    void getTrends_decreasingValues_shouldReturnDecreasingDirection() {
        int[] hours = {10, 5, 2, 1};
        for (int i = 0; i < hours.length; i++) {
            String date = LocalDate.now().minusDays(hours.length - i).toString();
            given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": %d.0 }", date, hours[i]))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "SLEEP")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(200)
            .body("trendDirection", equalTo("DECREASING"));
    }

    @Test
    void getTrends_invalidMetric_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("metric", "INVALID_METRIC")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(400);
    }

    @Test
    void getTrends_unauthenticated_shouldReturn401() {
        given()
            .queryParam("metric", "SLEEP")
            .queryParam("period", 7)
            .when().get("/api/analytics/trends")
            .then()
            .statusCode(401);
    }

    // ── SUMMARY ────────────────────────────────────────────────────────────────

    @Test
    void getSummary_week_noData_shouldReturnZeroStats() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("period", equalTo("WEEK"))
            .body("totalEntries", equalTo(0))
            .body("avgSleepHours", equalTo(0.0f))
            .body("avgWaterMl", equalTo(0))
            .body("workoutDays", equalTo(0))
            .body("totalWorkoutDuration", equalTo(0))
            .body("totalReadingMinutes", equalTo(0))
            .body("totalHobbyMinutes", equalTo(0))
            .body("avgMoodRating", equalTo(0.0f))
            .body("streaks", empty());
    }

    @Test
    void getSummary_month_noData_shouldReturn200() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "MONTH")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("period", equalTo("MONTH"))
            .body("totalEntries", equalTo(0));
    }

    @Test
    void getSummary_week_withData_shouldReturnCorrectAggregates() {
        for (int i = 1; i <= 3; i++) {
            String date = LocalDate.now().minusDays(i).toString();
            given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "entryDate": "%s",
                        "sleepHours": 7.0,
                        "waterMl": 2000,
                        "workoutDone": true,
                        "workoutDurationMin": 30,
                        "readingMinutes": 20,
                        "hobbyDurationMin": 45,
                        "moodRating": 4
                    }
                    """, date))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("totalEntries", equalTo(3))
            .body("avgSleepHours", equalTo(7.0f))
            .body("avgWaterMl", equalTo(2000))
            .body("workoutDays", equalTo(3))
            .body("totalWorkoutDuration", equalTo(90))
            .body("totalReadingMinutes", equalTo(60))
            .body("totalHobbyMinutes", equalTo(135))
            .body("avgMoodRating", equalTo(4.0f));
    }

    @Test
    void getSummary_week_shouldIncludeCorrectDateRange() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("startDate", equalTo(LocalDate.now().minusDays(7).toString()))
            .body("endDate", equalTo(LocalDate.now().toString()));
    }

    @Test
    void getSummary_month_shouldIncludeCorrectDateRange() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "MONTH")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("startDate", equalTo(LocalDate.now().minusDays(30).toString()))
            .body("endDate", equalTo(LocalDate.now().toString()));
    }

    @Test
    void getSummary_entryOutsidePeriod_shouldNotBeIncluded() {
        // Entry 30 days ago — outside the 7-day WEEK window
        String oldDate = LocalDate.now().minusDays(30).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": 9.0 }", oldDate))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("totalEntries", equalTo(0))
            .body("avgSleepHours", equalTo(0.0f));
    }

    @Test
    void getSummary_monthIncludesEntriesThatWeekMisses() {
        // Entry 20 days ago — inside MONTH but outside WEEK
        String date = LocalDate.now().minusDays(20).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"sleepHours\": 7.0 }", date))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("totalEntries", equalTo(0));

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "MONTH")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("totalEntries", equalTo(1));
    }

    @Test
    void getSummary_onlyWorkoutDaysCountedNotAllEntries() {
        // 2 entries: one with workout, one without
        String day1 = LocalDate.now().minusDays(1).toString();
        String day2 = LocalDate.now().minusDays(2).toString();
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"workoutDone\": true }", day1))
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(String.format("{ \"entryDate\": \"%s\", \"workoutDone\": false }", day2))
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("totalEntries", equalTo(2))
            .body("workoutDays", equalTo(1));
    }

    @Test
    void getSummary_includesStreaksSubObject() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 7.0, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(200)
            .body("streaks", notNullValue())
            .body("streaks.size()", equalTo(1))
            .body("streaks[0].goalType", equalTo("SLEEP"));
    }

    @Test
    void getSummary_invalidPeriod_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "DECADE")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(400);
    }

    @Test
    void getSummary_unauthenticated_shouldReturn401() {
        given()
            .queryParam("period", "WEEK")
            .when().get("/api/analytics/summary")
            .then()
            .statusCode(401);
    }
}