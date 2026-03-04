package org.fractalschema.auth;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthRepository {
    public boolean existsByUsername(String username ) {
        return User.existsByUsername(username);
    }

    public boolean existsByEmail(String email ) {
        return User.existsByEmail(email);
    }

    public User findByUsername(String username ) {
        return User.find("LOWER(username)", username.toLowerCase()).firstResult();
    }

    public void save(User user) {
        user.persist();
    }
}
