package org.fractalschema.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {

    @Email
    public String email;

    @Size(min = 8)
    public String password;
}
