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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class AuthResourceTest {

    @Inject
    JwtUtil jwtUtil;

    @BeforeEach
    @Transactional
    void setup() {
        User user = new User();
        user.setUsername("existinguser");
        user.setEmail("existing@example.com");
        user.setPassword(BcryptUtil.bcryptHash("correctpassword"));
        user.setRoles("user");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user.persist();
    }

    @AfterEach
    @Transactional
    void cleanup() {
        User.delete("username", "existinguser");
        User.delete("username", "testuser3");
        User.delete("username", "testuser");
    }

    @Test
    void registerWithValidData_shouldReturn201() {
        given().contentType(ContentType.JSON).body("""
                    {
                        "username": "testuser3",
                        "email": "test3@example.com",
                        "password": "password123"
                    }
                    """)
                .when().post("/api/auth/register").then()
                .log().all()
                .statusCode(201)
                .body("username", equalTo("testuser3"))
                .body("email", equalTo("test3@example.com"));
    }

    @Test
    void registerDuplicateUsername_shouldReturn409() {
        given().contentType(ContentType.JSON).body("""
                    {
                        "username": "existinguser",
                        "email": "test@example.com",
                        "password": "password123"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(409)
                .body("code", equalTo(1001))
                .body("message", equalTo("User already exists"));
    }

    @Test
    void registerDuplicateEmail_shouldReturn409() {
        given().contentType(ContentType.JSON).body("""
                {
                    "username": "testuser",
                    "email": "existing@example.com",
                    "password": "password123"
                }
                """)
            .when().post("/api/auth/register").then()
                .statusCode(409)
                .body("code", equalTo(1001))
                .body("message", equalTo("User already exists"));
    }

    @Test
    void registerBlankUsername_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                    {
                        "username": "",
                        "email": "test@example.com",
                        "password": "password123"
                    }
                    """)
                .when().post("/api/auth/register").then()
                .statusCode(400);
    }

    @Test
    void registerBlankEmail_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                {
                    "username": "testuser",
                    "email": "",
                    "password": "password123"
                }
                """)
            .when().post("/api/auth/register").then()
            .statusCode(400);
    }

    @Test
    void registerBlankPassword_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": ""
                }
                """)
                .when().post("/api/auth/register").then()
                .statusCode(400);
    }

    @Test
    void registerInvalidEmailFormat_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                {
                    "username": "testuser",
                    "email": "existingexample.com",
                    "password": "password123"
                }
                """)
                .when().post("/api/auth/register").then()
                .statusCode(400);
    }

    @Test
    void loginWithValidData_shouldReturn200() {
        given().contentType(ContentType.JSON).body("""
                {
                    "email": "existing@example.com",
                    "password": "correctpassword"
                }
                """)
                .when().post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user", equalTo("existinguser"));
    }

    @Test
    void loginWrongPassword_shouldReturn401() {
        given().contentType(ContentType.JSON).body("""
                {
                    "email": "existing@example.com",
                    "password": "wrongpassword"
                }
                """)
                .when().post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("code", equalTo(1003))
                .body("message", equalTo("Invalid Credentials"));
    }

    @Test
    void loginNonExistentUsername_shouldReturn401() {
        given().contentType(ContentType.JSON).body("""
                {
                    "email": "nonexistent@example.com",
                    "password": "password123"
                }
                """)
                .when().post("/api/auth/login").then()
                .statusCode(401)
                .body("code", equalTo(1003))
                .body("message", equalTo("Invalid Credentials"));
    }

    @Test
    void loginBlankPassword_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                    {
                        "email": "test@example.com",
                        "password": ""
                    }
                    """)
                .when().post("/api/auth/login").then()
                .statusCode(400);
    }

    @Test
    void loginBlankEmail_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                    {
                        "email": "",
                        "password": "password123"
                    }
                    """)
                .when().post("/api/auth/login").then()
                .statusCode(400);
    }

    // ── /me ───────────────────────────────────────────────────────────────────

    @Test
    void getMe_shouldReturn200() {
        String token = jwtUtil.generateAccessToken("existinguser", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get("/api/auth/me")
            .then()
            .statusCode(200)
            .body("username", equalTo("existinguser"))
            .body("email", equalTo("existing@example.com"))
            .body("id", notNullValue());
    }

    @Test
    void getMe_unauthenticated_shouldReturn401() {
        given()
            .when().get("/api/auth/me")
            .then()
            .statusCode(401);
    }

    // ── /refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_withRefreshToken_shouldReturn200() {
        String refreshToken = jwtUtil.generateRefreshToken("existinguser");

        given()
            .header("Authorization", "Bearer " + refreshToken)
            .contentType(ContentType.JSON)
            .when().post("/api/auth/refresh")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("refreshToken", notNullValue())
            .body("user", equalTo("existinguser"));
    }

    @Test
    void refresh_withAccessToken_shouldReturn400() {
        String accessToken = jwtUtil.generateAccessToken("existinguser", "user");

        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .when().post("/api/auth/refresh")
            .then()
            .statusCode(400)
            .body("code", equalTo(3001));
    }

    @Test
    void refresh_unauthenticated_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/auth/refresh")
            .then()
            .statusCode(401);
    }

    // ── /profile ──────────────────────────────────────────────────────────────

    @Test
    void updateProfile_email_shouldReturn200() {
        String token = jwtUtil.generateAccessToken("existinguser", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"updated@example.com\" }")
            .when().put("/api/auth/profile")
            .then()
            .statusCode(200)
            .body("email", equalTo("updated@example.com"));
    }

    @Test
    void updateProfile_password_shouldReturn200() {
        String token = jwtUtil.generateAccessToken("existinguser", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"password\": \"newpassword123\" }")
            .when().put("/api/auth/profile")
            .then()
            .statusCode(200)
            .body("username", equalTo("existinguser"));
    }

    @Test
    void updateProfile_emptyBody_shouldReturn400() {
        String token = jwtUtil.generateAccessToken("existinguser", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{}")
            .when().put("/api/auth/profile")
            .then()
            .statusCode(400)
            .body("code", equalTo(3001));
    }

    @Test
    void updateProfile_duplicateEmail_shouldReturn409() {
        String token = jwtUtil.generateAccessToken("existinguser", "user");

        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"existing@example.com\" }")
            .when().put("/api/auth/profile")
            .then()
            .statusCode(409)
            .body("code", equalTo(1001));
    }

    @Test
    void updateProfile_unauthenticated_shouldReturn401() {
        given()
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"new@example.com\" }")
            .when().put("/api/auth/profile")
            .then()
            .statusCode(401);
    }
}