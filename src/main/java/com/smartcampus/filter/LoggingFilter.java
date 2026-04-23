package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Logging Filter - implements both request and response filtering.
 *
 * Why use JAX-RS filters for cross-cutting concerns like logging:
 * Inserting Logger.info() calls manually into every resource method violates
 * the DRY (Don't Repeat Yourself) principle and scatters infrastructure code
 * throughout business logic. Filters are applied automatically to EVERY
 * request/response by the JAX-RS runtime — a single class handles logging
 * for the entire API. This also means adding new resource endpoints
 * automatically gets logging for free, without any developer action.
 * Filters can also be toggled, ordered, and applied selectively via
 * @NameBinding annotations, making them far more flexible than inline logging.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("[REQUEST]  %s %s", method, uri));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String uri = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info(String.format("[RESPONSE] %s %s -> HTTP %d", method, uri, status));
    }
}
