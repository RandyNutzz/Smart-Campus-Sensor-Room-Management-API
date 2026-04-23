package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Room Resource - manages /api/v1/rooms
 *
 * Full Object vs ID-Only Responses:
 * Returning full room objects (as done here) gives clients all the data they
 * need in one round-trip, reducing network calls. However, for very large
 * collections, this increases bandwidth usage significantly. Returning only
 * IDs is more bandwidth-efficient but forces clients to issue N+1 requests
 * to fetch details. The ideal approach (HATEOAS) is to return a summary list
 * with embedded links, letting clients fetch full detail only when needed.
 *
 * DELETE Idempotency:
 * In this implementation, DELETE is idempotent from the server state
 * perspective — deleting an already-deleted room does not change the server
 * state further. However, subsequent DELETE calls return 404 (Not Found)
 * rather than 200/204, which is technically acceptable per RFC 7231. Strict
 * idempotency refers to the *effect* on state, not the response code. The
 * room is gone after the first DELETE regardless of how many times you repeat
 * the call, so the idempotency contract is upheld.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/rooms — List all rooms
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    // POST /api/v1/rooms — Create a new room
    @POST
    public Response createRoom(Room room) {
        // Validate that the request payload correctly provides an ID
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room ID is required.");
            error.put("status", "400");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
        
        // Prevent duplicate creation by checking if the room ID already exists in the datastore
        if (store.getRoom(room.getId()) != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "A room with ID '" + room.getId() + "' already exists.");
            error.put("status", "409");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }
        
        // Save the new room to the datastore
        store.addRoom(room);
        
        // Return 201 Created and send the created room object back in the response
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // GET /api/v1/rooms/{roomId} — Get a specific room
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room not found: " + roomId);
            error.put("status", "404");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        return Response.ok(room).build();
    }

    // DELETE /api/v1/rooms/{roomId} — Delete a room (blocked if sensors exist)
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        // Attempt to find the room in the datastore
        Room room = store.getRoom(roomId);
        if (room == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Room not found: " + roomId);
            error.put("status", "404");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build(); // Return 404 if it doesn't exist
        }

        // Business rule constraint: cannot delete room that still holds active sensors
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Room '" + roomId + "' cannot be deleted. It still has " +
                room.getSensorIds().size() + " sensor(s) assigned: " + room.getSensorIds()
            );
        }

        // Safe to delete room
        store.deleteRoom(roomId);
        
        // Build and return success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "Room '" + roomId + "' has been successfully deleted.");
        return Response.ok(response).build();
    }
}
