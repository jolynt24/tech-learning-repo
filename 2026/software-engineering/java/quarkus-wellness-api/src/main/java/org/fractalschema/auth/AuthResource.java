package org.fractalschema.auth;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.fractalschema.dto.request.LoginRequest;
import org.fractalschema.dto.request.RegisterRequest;
import org.fractalschema.dto.request.UpdateProfileRequest;
import org.fractalschema.dto.response.AuthResponse;
import org.fractalschema.dto.response.UserResponse;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject AuthService authService;

    @POST @Path("/register")
    public Response registerUser(@Valid RegisterRequest registerRequest) {
        UserResponse userResponse = authService.register(registerRequest);
        return Response.status(Response.Status.CREATED).entity(userResponse).build();
    }

    @POST @Path("/login")
    public Response loginUser(@Valid LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return Response.ok(authResponse).build();
    }

    @POST @Path("/refresh") @RolesAllowed("user")
    public Response refreshUser() {
        AuthResponse authResponse = authService.refresh();
        return Response.ok(authResponse).build();
    }

    @GET @Path("/me") @RolesAllowed("user")
    public Response getUserInfo() {
        UserResponse userResponse = authService.me();
        return Response.ok(userResponse).build();
    }

    @PUT @Path("/profile") @RolesAllowed("user")
    public Response updateProfile(@Valid UpdateProfileRequest profileRequest) {
        UserResponse userResponse = authService.profile(profileRequest);
        return Response.ok(userResponse).build();
    }
}
