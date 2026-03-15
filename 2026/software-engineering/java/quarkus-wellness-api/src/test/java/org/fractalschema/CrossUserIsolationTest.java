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

/**
 * Verifies that users cannot read or modify each other's data.
 */
@QuarkusTest
public class CrossUserIsolationTest {

    @Inject
    JwtUtil jwtUtil;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    @Transactional
    void setup() {
        User userA = new User();
        userA.setUsername("isolation_a");
        userA.setEmail("isolation_a@example.com");
        userA.setPassword(BcryptUtil.bcryptHash("password123"));
        userA.setRoles("user");
        userA.setCreatedAt(Instant.now());
        userA.setUpdatedAt(Instant.now());
        userA.persist();

        User userB = new User();
        userB.setUsername("isolation_b");
        userB.setEmail("isolation_b@example.com");
        userB.setPassword(BcryptUtil.bcryptHash("password123"));
        userB.setRoles("user");
        userB.setCreatedAt(Instant.now());
        userB.setUpdatedAt(Instant.now());
        userB.persist();

        tokenA = jwtUtil.generateAccessToken("isolation_a", "user");
        tokenB = jwtUtil.generateAccessToken("isolation_b", "user");
    }

    @AfterEach
    @Transactional
    void cleanup() {
        User.delete("username", "isolation_a");
        User.delete("username", "isolation_b");
    }

    // ── ENTRY ISOLATION ───────────────────────────────────────────────────────

    @Test
    void getEntry_cannotReadAnotherUsersEntry_shouldReturn404() {
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-06-01\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenB)
            .when().get("/api/entries/2025-06-01")
            .then()
            .statusCode(404);
    }

    @Test
    void updateEntry_cannotModifyAnotherUsersEntry_shouldReturn404() {
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-06-02\", \"moodRating\": 3 }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("{ \"moodRating\": 5 }")
            .when().put("/api/entries/2025-06-02")
            .then()
            .statusCode(404);

        // Original value unchanged
        given()
            .header("Authorization", "Bearer " + tokenA)
            .when().get("/api/entries/2025-06-02")
            .then()
            .statusCode(200)
            .body("moodRating", equalTo(3));
    }

    @Test
    void deleteEntry_cannotDeleteAnotherUsersEntry_shouldReturn404() {
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-06-03\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenB)
            .when().delete("/api/entries/2025-06-03")
            .then()
            .statusCode(404);

        // Entry still exists for user A
        given()
            .header("Authorization", "Bearer " + tokenA)
            .when().get("/api/entries/2025-06-03")
            .then()
            .statusCode(200);
    }

    @Test
    void getEntryRange_onlyReturnsOwnEntries() {
        // Both users create an entry on the same date
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-07-01\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-07-02\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-07-01\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenB)
            .queryParam("from", "2025-07-01")
            .queryParam("to", "2025-07-02")
            .when().get("/api/entries/range")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].entryDate", equalTo("2025-07-01"));
    }

    @Test
    void getLatestEntries_onlyReturnsOwnEntries() {
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-08-01\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-08-02\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-08-03\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenA)
            .queryParam("limit", 10)
            .when().get("/api/entries/latest")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2));
    }

    // ── GOAL ISOLATION ────────────────────────────────────────────────────────

    @Test
    void getGoal_cannotReadAnotherUsersGoal_shouldReturn404() {
        int id = given()
            .header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 8.0, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + tokenB)
            .when().get("/api/goals/" + id)
            .then()
            .statusCode(404)
            .body("code", equalTo(4003));
    }

    @Test
    void updateGoal_cannotModifyAnotherUsersGoal_shouldReturn404() {
        int id = given()
            .header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WATER", "target": 2000, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("{ \"targetValue\": 9999 }")
            .when().patch("/api/goals/" + id)
            .then()
            .statusCode(404)
            .body("code", equalTo(4003));

        // Original value unchanged for user A
        given()
            .header("Authorization", "Bearer " + tokenA)
            .when().get("/api/goals/" + id)
            .then()
            .statusCode(200)
            .body("target", equalTo(2000.0f));
    }

    @Test
    void deleteGoal_cannotDeleteAnotherUsersGoal_shouldReturn404() {
        int id = given()
            .header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "HOBBY", "target": 30, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals")
            .then().statusCode(201)
            .extract().path("id");

        given()
            .header("Authorization", "Bearer " + tokenB)
            .when().delete("/api/goals/" + id)
            .then()
            .statusCode(404)
            .body("code", equalTo(4003));

        // Goal still exists for user A
        given()
            .header("Authorization", "Bearer " + tokenA)
            .when().get("/api/goals/" + id)
            .then()
            .statusCode(200);
    }

    @Test
    void getAllGoals_onlyReturnsOwnGoals() {
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "SLEEP", "target": 8.0, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);
        given().header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "WATER", "target": 2000, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);
        given().header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON)
            .body("""
                { "goalType": "READING", "target": 20, "goalFrequency": "DAILY", "startDate": "2026-01-01" }
                """)
            .when().post("/api/goals").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + tokenA)
            .when().get("/api/goals")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("goalLabel", not(hasItem("reading")));

        given()
            .header("Authorization", "Bearer " + tokenB)
            .when().get("/api/goals")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].goalLabel", equalTo("reading"));
    }
}