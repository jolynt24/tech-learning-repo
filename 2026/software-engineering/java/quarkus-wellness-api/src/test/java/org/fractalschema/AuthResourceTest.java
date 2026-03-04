package org.fractalschema;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.fractalschema.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestTransaction
public class AuthResourceTest {

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
                    "username": "loginuser",
                    "password": "correctpassword"
                }
                """)
                .when().post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user", equalTo("loginuser"));
    }

    @Test
    void loginWrongPassword_shouldReturn401() {
        given().contentType(ContentType.JSON).body("""
                {
                    "username": "loginuser",
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
                    "username": "diffuser",
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
                        "username": "testuser",
                        "password": ""
                    }
                    """)
                .when().post("/api/auth/login").then()
                .statusCode(400);
    }

    @Test
    void loginBlankUsername_shouldReturn400() {
        given().contentType(ContentType.JSON).body("""
                    {
                        "username": "",
                        "password": "password123"
                    }
                    """)
                .when().post("/api/auth/login").then()
                .statusCode(400);
    }
}