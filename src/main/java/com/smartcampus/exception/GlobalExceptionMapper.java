package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global safety-net exception mapper.
 * Catches ALL unhandled Throwable instances and returns HTTP 500.
 *
 * Cybersecurity Risk of Exposing Stack Traces:
 * Returning raw Java stack traces to external clients is a serious security
 * vulnerability. An attacker can harvest:
 * 1. Internal class/package names, revealing the application's architecture.
 * 2. Framework versions (e.g., "Jersey 2.39.1") enabling targeted CVE exploits.
 * 3. Database query fragments or file system paths if errors originate there.
 * 4. Logic flow information that can be used to craft precise injection attacks.
 * 5. Server technology stack, narrowing the attack surface.
 * This mapper ensures ALL unexpected errors return only a generic message
 * while logging the full detail server-side for developer diagnostics.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log full detail server-side — never expose to client
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper: " + exception.getMessage(), exception);

        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred. Please contact the API administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
