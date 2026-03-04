package org.fractalschema.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;

@Getter
@Entity
@Table(name = "users")
@UserDefinition
public class User extends PanacheEntity {

    @Column(name = "username", nullable = false, length = 50)
    @NotBlank(message = "Username cannot be empty")
    @Username
    private String username;

    @Setter
    @Column(name = "email", nullable = false)
    @Email @NotBlank(message = "Email address cannot be empty")
    private String email;

    @Column(name = "password_hash", nullable = false)
    @Setter
    @Password(PasswordType.MCF)
    private String password;

    @Setter
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Setter
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Roles @Setter
    @Column(name = "roles")
    private String roles;

    public void setUsername(String username) {
        this.username = username.toLowerCase();
    }

    public static boolean existsByUsername(String username) {
        return count("LOWER(username) = ?1", username.toLowerCase()) > 0;
    }

    public static boolean existsByEmail(String email) {
        return count("LOWER(email) = ?1", email.toLowerCase()) > 0;
    }

}