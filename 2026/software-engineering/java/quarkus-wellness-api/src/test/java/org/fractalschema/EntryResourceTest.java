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
public class EntryResourceTest {

    @Inject
    JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    @Transactional
    void setup() {
        User user = new User();
        user.setUsername("entryuser");
        user.setEmail("entryuser@example.com");
        user.setPassword(BcryptUtil.bcryptHash("password123"));
        user.setRoles("user");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.persist();

        token = jwtUtil.generateAccessToken("entryuser", "user");
    }

    @AfterEach
    @Transactional
    void cleanup() {
        // Deleting the user cascades to daily_entries, which cascades to meals (via @OnDelete)
        User.delete("username", "entryuser");
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    void createEntry_shouldReturn201() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "entryDate": "2025-01-15",
                    "sleepHours": 7.5,
                    "sleepQuality": 4,
                    "waterMl": 2000,
                    "workoutDone": true,
                    "workoutType": "Running",
                    "workoutDurationMin": 30,
                    "moodRating": 4,
                    "notes": "Good day"
                }
                """)
            .when().post("/api/entries")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("entryDate", equalTo("2025-01-15"))
            .body("sleepQuality", equalTo(4))
            .body("moodRating", equalTo(4))
            .body("workoutType", equalTo("Running"))
            .body("meals", empty());
    }

    @Test
    void createEntryWithMeals_shouldReturn201() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "entryDate": "2025-01-16",
                    "meals": [
                        { "mealType": "BREAKFAST", "description": "Oatmeal", "calories": 350 },
                        { "mealType": "LUNCH",     "description": "Chicken salad", "calories": 500 }
                    ]
                }
                """)
            .when().post("/api/entries")
            .then()
            .statusCode(201)
            .body("meals.size()", equalTo(2))
            .body("meals[0].mealType", equalTo("BREAKFAST"))
            .body("meals[0].description", equalTo("Oatmeal"))
            .body("meals[0].calories", equalTo(350))
            .body("meals[0].id", notNullValue())
            .body("meals[0].loggedAt", notNullValue());
    }

    @Test
    void createDuplicateEntry_shouldReturn409() {
        String body = """
            { "entryDate": "2025-01-17" }
            """;

        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON).body(body)
            .when().post("/api/entries").then().statusCode(201);

        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON).body(body)
            .when().post("/api/entries")
            .then()
            .statusCode(409)
            .body("code", equalTo(4001))
            .body("message", equalTo("Duplicate entry"));
    }

    @Test
    void createEntry_unauthenticated_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-01-18\" }")
            .when().post("/api/entries")
            .then()
            .statusCode(401);
    }

    @Test
    void createEntry_sleepQualityOutOfRange_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "entryDate": "2025-01-19", "sleepQuality": 10 }
                """)
            .when().post("/api/entries")
            .then()
            .statusCode(400);
    }

    @Test
    void createEntry_moodRatingOutOfRange_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                { "entryDate": "2025-01-19", "moodRating": 0 }
                """)
            .when().post("/api/entries")
            .then()
            .statusCode(400);
    }

    @Test
    void createEntry_mealMissingDescription_shouldReturn400() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "entryDate": "2025-01-19",
                    "meals": [ { "mealType": "BREAKFAST" } ]
                }
                """)
            .when().post("/api/entries")
            .then()
            .statusCode(400);
    }

    // ── GET BY DATE ───────────────────────────────────────────────────────────

    @Test
    void getEntryByDate_shouldReturn200() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-01-20\", \"moodRating\": 3 }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/entries/2025-01-20")
            .then()
            .statusCode(200)
            .body("entryDate", equalTo("2025-01-20"))
            .body("moodRating", equalTo(3));
    }

    @Test
    void getEntryByDate_notFound_shouldReturn404() {
        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/entries/2020-01-01")
            .then()
            .statusCode(404)
            .body("code", equalTo(4002))
            .body("message", equalTo("Entry not found"));
    }

    @Test
    void getEntryByDate_unauthenticated_shouldReturn401() {
        given()
            .when().get("/api/entries/2025-01-20")
            .then()
            .statusCode(401);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    void updateEntry_shouldReturn200() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-01-21\", \"moodRating\": 2, \"waterMl\": 1000 }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"moodRating\": 5, \"waterMl\": 2500 }")
            .when().put("/api/entries/2025-01-21")
            .then()
            .statusCode(200)
            .body("moodRating", equalTo(5))
            .body("waterMl", equalTo(2500));
    }

    @Test
    void updateEntry_replacesMeals() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "entryDate": "2025-01-23",
                    "meals": [ { "mealType": "BREAKFAST", "description": "Toast", "calories": 200 } ]
                }
                """)
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "meals": [
                        { "mealType": "DINNER", "description": "Pasta", "calories": 700 },
                        { "mealType": "SNACK",  "description": "Apple", "calories": 80 }
                    ]
                }
                """)
            .when().put("/api/entries/2025-01-23")
            .then()
            .statusCode(200)
            .body("meals.size()", equalTo(2))
            .body("meals.mealType", hasItems("DINNER", "SNACK"));
    }

    @Test
    void updateEntry_notFound_shouldReturn404() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"moodRating\": 5 }")
            .when().put("/api/entries/2020-01-01")
            .then()
            .statusCode(404)
            .body("code", equalTo(4002));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteEntry_shouldReturn204() {
        given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-01-22\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/entries/2025-01-22")
            .then()
            .statusCode(204);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/entries/2025-01-22")
            .then()
            .statusCode(404);
    }

    @Test
    void deleteEntry_notFound_shouldReturn404() {
        given()
            .header("Authorization", "Bearer " + token)
            .when().delete("/api/entries/2020-01-01")
            .then()
            .statusCode(404)
            .body("code", equalTo(4002));
    }

    // ── RANGE ─────────────────────────────────────────────────────────────────

    @Test
    void getEntryRange_shouldReturn200() {
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-02-01\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-02-03\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-02-05\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("from", "2025-02-01")
            .queryParam("to", "2025-02-03")
            .when().get("/api/entries/range")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("entryDate", hasItems("2025-02-01", "2025-02-03"));
    }

    @Test
    void getEntryRange_noMatches_shouldReturnEmptyList() {
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("from", "2000-01-01")
            .queryParam("to", "2000-01-31")
            .when().get("/api/entries/range")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    // ── LATEST ────────────────────────────────────────────────────────────────

    @Test
    void getLatestEntries_shouldReturnInDescendingOrder() {
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-03-01\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-03-02\" }")
            .when().post("/api/entries").then().statusCode(201);
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{ \"entryDate\": \"2025-03-03\" }")
            .when().post("/api/entries").then().statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("limit", 2)
            .when().get("/api/entries/latest")
            .then()
            .statusCode(200)
            .body("size()", equalTo(2))
            .body("[0].entryDate", equalTo("2025-03-03"))
            .body("[1].entryDate", equalTo("2025-03-02"));
    }

    @Test
    void getLatestEntries_defaultLimit_shouldReturn7() {
        for (int i = 1; i <= 10; i++) {
            given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
                .body(String.format("{ \"entryDate\": \"2025-04-%02d\" }", i))
                .when().post("/api/entries").then().statusCode(201);
        }

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/entries/latest")
            .then()
            .statusCode(200)
            .body("size()", equalTo(7));
    }
}