package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sensor Resource - manages /api/v1/sensors
 *
 * @Consumes Annotation Behaviour:
 * The @Consumes(MediaType.APPLICATION_JSON) annotation tells JAX-RS this
 * method only accepts requests with Content-Type: application/json. If a
 * client sends text/plain or application/xml, JAX-RS will immediately reject
 * the request with HTTP 415 Unsupported Media Type — before the method body
 * even executes. This enforces a strict contract without any manual checking.
 *
 * @QueryParam vs Path Segment for Filtering:
 * Using @QueryParam for filtering (?type=CO2) is superior to path-based
 * filtering (/sensors/type/CO2) because query parameters are semantically
 * designed for optional modifications of a resource collection (filtering,
 * sorting, pagination), whereas path segments identify a unique resource.
 * /sensors represents "all sensors" — a query param simply narrows that set.
 * Path-based filters also pollute the URL namespace and are harder to combine
 * (e.g., filtering by type AND status would require awkward nested paths).
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        // Fetch all sensors from datastore
        Collection<Sensor> all = store.getSensors().values();

        // If the client provided a '?type=' query parameter, filter the results
        if (type != null && !type.trim().isEmpty()) {
            List<Sensor> filtered = all.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type)) // Filter by exact type match
                .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }

        // No filter provided, return the entire list
        return Response.ok(new ArrayList<>(all)).build();
    }

    @POST
    public Response createSensor(Sensor sensor) {
        // Validate payload exists and has an ID
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor ID is required.");
            error.put("status", "400");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Reject if sensor ID already exists
        if (store.getSensor(sensor.getId()) != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor with ID '" + sensor.getId() + "' already exists.");
            error.put("status", "409");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Validate that the referenced room actually exists in the database
        // This is a core coursework requirement (returns 422 Unprocessable Entity)
        if (sensor.getRoomId() == null || store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: Room with ID '" + sensor.getRoomId() + "' does not exist in the system."
            );
        }

        // Save sensor to the main collection
        store.addSensor(sensor);

        // Map sensor to its parent room logically
        Room room = store.getRoom(sensor.getRoomId());
        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // GET /api/v1/sensors/{sensorId} — Get a specific sensor
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            error.put("status", "404");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        return Response.ok(sensor).build();
    }

    // DELETE /api/v1/sensors/{sensorId} — Remove a sensor
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            error.put("status", "404");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Remove sensor from its room's sensor list
        if (sensor.getRoomId() != null) {
            Room room = store.getRoom(sensor.getRoomId());
            if (room != null) {
                room.getSensorIds().remove(sensorId);
            }
        }

        store.deleteSensor(sensorId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Sensor '" + sensorId + "' successfully deleted.");
        return Response.ok(response).build();
    }

    /**
     * Sub-resource locator for sensor readings.
     * Delegates to SensorReadingResource for all paths under /{sensorId}/readings.
     *
     * Sub-Resource Locator Pattern Benefits:
     * By returning a separate SensorReadingResource instance here, we delegate
     * all reading-related logic to a dedicated class. This follows the Single
     * Responsibility Principle — SensorResource handles sensor CRUD, while
     * SensorReadingResource handles historical data. In large APIs with many
     * nested resources, putting every endpoint in one controller leads to
     * thousands-of-line "god classes" that are hard to maintain and test.
     * The locator pattern creates a clean, navigable class hierarchy that
     * mirrors the URL hierarchy.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
        // Return an instance of the sub-resource. JAX-RS will now route all traffic 
        // starting with "/{sensorId}/readings" to the methods defined in SensorReadingResource.java!
        return new SensorReadingResource(sensorId);
    }
}
