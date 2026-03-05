package org.fractalschema.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {

    @Email
    private String email;

    @Size(min = 8)
    private String password;
}
