package com.smartcampus.resource;

import com.smartcampus.exception.BadRequestException;
import com.smartcampus.exception.ConflictException;
import com.smartcampus.exception.NotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collection;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    @GET
    public Collection<Room> getAllRooms() {
        return dataStore.getAllRooms();
    }

    @GET
    @Path("/{id}")
    public Response getRoomById(@PathParam("id") String id) {
        Room room = dataStore.getRoom(id);
        if (room == null) {
            throw new NotFoundException("Room not found");
        }
        return Response.ok(room).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            throw new BadRequestException("Room id is required");
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            throw new BadRequestException("Room name is required");
        }
        if (room.getCapacity() < 0) {
            throw new BadRequestException("Room capacity must be >= 0");
        }

        // Keep JSON consistent: ensure sensorIds is never null.
        if (room.getSensorIds() == null) {
            room.setSensorIds(null);
        }

        boolean alreadyExists = dataStore.getRoom(room.getId()) != null;
        if (alreadyExists) {
            throw new ConflictException("Room with id already exists");
        }

        dataStore.upsertRoom(room);

        return Response.created(URI.create("/api/v1/rooms/" + room.getId()))
                .entity(room)
                .build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String id) {
        Room room = dataStore.getRoom(id);
        if (room == null) {
            throw new NotFoundException("Room not found");
        }

        // a room cannot be deleted if it has sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new ConflictException("Room cannot be deleted while sensors are assigned");
        }

        dataStore.deleteRoom(id);
        return Response.noContent().build();
    }
}