package org.fractalschema.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.fractalschema.dto.response.ErrorResponse;

@Provider
public class CustomExceptionMapper implements ExceptionMapper<CustomExceptions> {
    @Override
    public Response toResponse(CustomExceptions e) {
        return Response.status(e.getErrorCode().getStatusCode())
                .entity(new ErrorResponse(e.getErrorCode()))
                .build();
    }
}
