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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class GoalResourceTest {

    @Inject
    JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    @Transactional
    void setup() {
        User user = new User();
        user.setUsername("goaluser");
        user.setEmail("goaluser@example.com");
        user.setPassword(BcryptUtil.bcryptHash("password123"));
        user.setRoles("user");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.persist();

        token = jwtUtil.generateAccessToken("goaluser", "user");
    }

    @AfterEach
    @Transactional
    void cleanup() {
        // Deleting the user cascades to goals via ON DELETE CASCADE
        User.delete("username", "goaluser");
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    void createGoal_shouldReturn201() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "SLEEP",
                    "target": 8.0,
                    "goalFrequency": "DAILY",
                    "startDate": "2026-03-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("goalLabel", equalTo("sleep"))
            .body("goalMetrics", equalTo("hours"))
            .body("target", equalTo(8.0f))
            .body("goalFrequency", equalTo("daily"))
            .body("frequencyDays", equalTo(1))
            .body("active", equalTo(true))
            .body("startDate", equalTo("2026-03-10"))
            .body("createdAt", notNullValue());
    }

    @Test
    void createGoal_withEndDate_shouldReturn201() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "WATER",
                    "target": 2000,
                    "goalFrequency": "DAILY",
                    "startDate": "2026-03-10",
                    "endDate": "2026-06-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(201)
            .body("goalLabel", equalTo("water"))
            .body("goalMetrics", equalTo("ml"))
            .body("endDate", equalTo("2026-06-10"));
    }

    @Test
    void createGoal_weeklyFrequency_shouldReturn201() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "WORKOUT",
                    "target": 150,
                    "goalFrequency": "WEEKLY",
                    "startDate": "2026-03-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(201)
            .body("goalLabel", equalTo("workout"))
            .body("goalMetrics", equalTo("minutes"))
            .body("goalFrequency", equalTo("weekly"))
            .body("frequencyDays", equalTo(7));
    }

    @Test
    void createGoal_unauthenticated_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "SLEEP",
                    "target": 8.0,
                    "goalFrequency": "DAILY",
                    "startDate": "2026-03-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(401);
    }

    @Test
    void createGoal_missingGoalType_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "target": 8.0,
                    "goalFrequency": "DAILY",
                    "startDate": "2026-03-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(400);
    }

    @Test
    void createGoal_missingStartDate_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "SLEEP",
                    "target": 8.0,
                    "goalFrequency": "DAILY"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(400);
    }

    @Test
    void createGoal_zeroTarget_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "SLEEP",
                    "target": 0,
                    "goalFrequency": "DAILY",
                    "startDate": "2026-03-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(400);
    }

    @Test
    void createGoal_negativeTarget_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "goalType": "WATER",
                    "target": -500,
                    "goalFrequency": "DAILY",
                    "startDate": "2026-03-10"
                }
                """)
            .when().post("/api/goals")
            .then()
            .statusCode(400);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @Test
    void getAllGoals_shouldReturn200() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "READING", "target": 20, "goalFrequency": "DAILY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "HOBBY", "target": 60, "goalFrequency": "WEEKLY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/goals")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("goalLabel", hasItems("reading", "hobby"));
    }

    @Test
    void getAllGoals_noGoals_shouldReturnEmptyList() {
        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/goals")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    void getAllGoals_unauthenticated_shouldReturn401() {
        given()
            .when().get("/api/goals")
            .then()
            .statusCode(401);
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @Test
    void getGoal_shouldReturn200() {
        int id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 8.0, "goalFrequency": "DAILY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/goals/" + id)
            .then()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("goalLabel", equalTo("sleep"));
    }

    @Test
    void getGoal_notFound_shouldReturn404() {
        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/goals/999999")
            .then()
            .statusCode(404)
            .body("code", equalTo(4003))
            .body("message", equalTo("Goal not found"));
    }

    @Test
    void getGoal_unauthenticated_shouldReturn401() {
        given()
            .when().get("/api/goals/1")
            .then()
            .statusCode(401);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void updateGoal_shouldReturn200() {
        int id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 7.0, "goalFrequency": "DAILY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"targetValue\": 8.5 }")
            .when().patch("/api/goals/" + id)
            .then()
            .statusCode(200)
            .body("target", equalTo(8.5f));
    }

    @Test
    void updateGoal_deactivate_shouldReturn200() {
        int id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WATER", "target": 2000, "goalFrequency": "DAILY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"active\": false }")
            .when().patch("/api/goals/" + id)
            .then()
            .statusCode(200)
            .body("active", equalTo(false));
    }

    @Test
    void updateGoal_changeFrequency_shouldReturn200() {
        int id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WORKOUT", "target": 30, "goalFrequency": "DAILY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"frequency\": \"MONTHLY\" }")
            .when().patch("/api/goals/" + id)
            .then()
            .statusCode(200)
            .body("goalFrequency", equalTo("monthly"))
            .body("frequencyDays", equalTo(30));
    }

    @Test
    void updateGoal_notFound_shouldReturn404() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"targetValue\": 10 }")
            .when().patch("/api/goals/999999")
            .then()
            .statusCode(404)
            .body("code", equalTo(4003))
            .body("message", equalTo("Goal not found"));
    }

    @Test
    void updateGoal_unauthenticated_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .body("{ \"targetValue\": 10 }")
            .when().patch("/api/goals/1")
            .then()
            .statusCode(401);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteGoal_shouldReturn204() {
        int id = given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "READING", "target": 10, "goalFrequency": "DAILY", "startDate": "2026-03-10" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/goals/" + id)
            .then()
            .statusCode(204);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/goals/" + id)
            .then()
            .statusCode(404);
    }

    @Test
    void deleteGoal_notFound_shouldReturn404() {
        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/goals/999999")
            .then()
            .statusCode(404)
            .body("code", equalTo(4003))
            .body("message", equalTo("Goal not found"));
    }

    @Test
    void deleteGoal_unauthenticated_shouldReturn401() {
        given()
            .when().delete("/api/goals/1")
            .then()
            .statusCode(401);
    }

    // ── PROGRESS ──────────────────────────────────────────────────────────────

    @Test
    void getGoalProgress_shouldReturn501NotImplemented() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("period", "WEEK")
            .when().get("/api/goals/progress")
            .then()
            .statusCode(501);
    }

    @Test
    void getGoalProgress_unauthenticated_shouldReturn401() {
        given()
            .queryParam("period", "WEEK")
            .when().get("/api/goals/progress")
            .then()
            .statusCode(401);
    }
}