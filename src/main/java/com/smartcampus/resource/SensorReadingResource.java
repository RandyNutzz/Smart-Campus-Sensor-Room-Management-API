package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sub-resource for sensor readings.
 * Accessed via /api/v1/sensors/{sensorId}/readings
 *
 * Note: This class is NOT annotated with @Path at the class level because
 * it is instantiated by the sub-resource locator in SensorResource. JAX-RS
 * discovers method-level @Path annotations on its own once the locator
 * returns this instance.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings — Retrieve all readings for sensor
    @GET
    public Response getReadings() {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            error.put("status", "404");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        List<SensorReading> readings = store.getReadings(sensorId);
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        // First verify that the parent sensor actually exists
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Sensor not found: " + sensorId);
            error.put("status", "404");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // State constraint: If the sensor is offline or under MAINTENANCE, it cannot accept readings
        // This validates the Part 5 requirement (403 Forbidden Exception)
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot accept new readings."
            );
        }

        // Validate that a reading body was actually sent in the JSON payload
        if (reading == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Reading body is required.");
            error.put("status", "400");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // Auto-generate UUID and timestamp if the client didn't provide them
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(java.util.UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Add the reading to the datastore
        // Side effect: Inside this addReading method, the parent Sensor's currentValue is updated!
        store.addReading(sensorId, reading);

        // Return 201 Created
        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
