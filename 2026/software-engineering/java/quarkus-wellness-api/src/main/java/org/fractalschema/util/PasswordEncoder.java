package org.fractalschema.util;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordEncoder {

    public String hash(String password) {
        return BcryptUtil.bcryptHash(password);
    }

    public boolean verify(String plainText, String hashed) {
        return BcryptUtil.matches(plainText, hashed);
    }
}