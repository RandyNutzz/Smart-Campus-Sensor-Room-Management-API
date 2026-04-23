package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * Why 422 over 404:
 * HTTP 404 (Not Found) indicates the *requested resource* (the URL itself) was
 * not found. In this scenario, the URL /api/v1/sensors is perfectly valid and
 * was found. The problem is that a *reference inside the request body*
 * (the roomId field) points to a non-existent entity. The request is
 * syntactically correct JSON but semantically invalid because a dependency
 * it declares does not exist. HTTP 422 (Unprocessable Entity) precisely
 * describes this: the server understands the content type and syntax, but
 * cannot process the instructions due to semantic errors in the payload.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 422);
        error.put("error", "Unprocessable Entity");
        error.put("message", exception.getMessage());
        error.put("hint", "Ensure the referenced roomId exists before registering a sensor.");

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
