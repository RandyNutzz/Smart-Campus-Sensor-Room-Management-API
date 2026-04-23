package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Discovery / root endpoint at GET /api/v1
 *
 * HATEOAS Note: Providing hypermedia links within API responses is the
 * hallmark of advanced RESTful design (HATEOAS - Hypermedia As The Engine
 * of Application State). Instead of clients relying on out-of-band static
 * documentation to know available endpoints, the API itself advertises
 * navigable links. This decouples clients from hardcoded URLs, allows the
 * API to evolve its URL structure without breaking clients, and enables
 * self-discovery — a client can start at the root and traverse the entire
 * API just by following links, similar to how a browser navigates the web.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        // Construct the base metadata object describing the API
        Map<String, Object> response = new HashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("contact", "admin@smartcampus.university.ac.uk");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");

        // HATEOAS Implementation: Embed navigation links
        // This is a core coursework requirement (Part 1.2) - it allows clients 
        // to discover available endpoints without hardcoded logic.
        Map<String, String> links = new HashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);

        // Deprecated standard resources array (kept for backward compatibility)
        Map<String, String> resources = new HashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        response.put("resources", resources);

        // Return HTTP 200 OK with the generated JSON discovery payload
        return Response.ok(response).build();
    }
}
