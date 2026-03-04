package org.fractalschema.config;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.security.Principal;

@ApplicationScoped
public class SecurityConfig {
    @Inject SecurityIdentity identity;

    public String getUserIdentity() {
        return identity.getPrincipal().getName();
    }
}
