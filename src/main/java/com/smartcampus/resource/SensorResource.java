package com.smartcampus.resource;

import com.smartcampus.exception.BadRequestException;
import com.smartcampus.exception.ConflictException;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.NotFoundException;
import com.smartcampus.exception.UnprocessableEntityException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    @GET
    public Collection<Sensor> getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = dataStore.getAllSensors();
        if (type == null || type.trim().isEmpty()) {
            return all;
        }

        String wanted = type.trim();
        List<Sensor> filtered = new ArrayList<>();
        for (Sensor s : all) {
            if (s != null && s.getType() != null && s.getType().equalsIgnoreCase(wanted)) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found");
        }
        return Response.ok(sensor).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        validateSensorForCreate(sensor);

        Room room = dataStore.getRoom(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Referenced roomId does not exist");
        }

        boolean alreadyExists = dataStore.getSensor(sensor.getId()) != null;
        if (alreadyExists) {
            throw new ConflictException("Sensor with id already exists");
        }

        dataStore.upsertSensor(sensor);

        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
            dataStore.upsertRoom(room);
        }

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    
    @PUT
    @Path("/{sensorId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSensor(@PathParam("sensorId") String sensorId, Sensor updated) {
        Sensor existing = dataStore.getSensor(sensorId);
        if (existing == null) {
            throw new NotFoundException("Sensor not found");
        }
        if (updated == null) {
            throw new BadRequestException("Sensor body is required");
        }

        
        if (updated.getId() != null && !updated.getId().trim().isEmpty() && !sensorId.equals(updated.getId())) {
            throw new BadRequestException("Sensor id cannot be changed");
        }

        
        if (updated.getType() == null || updated.getType().trim().isEmpty()) {
            throw new BadRequestException("Sensor type is required");
        }
        if (updated.getStatus() == null || updated.getStatus().trim().isEmpty()) {
            throw new BadRequestException("Sensor status is required");
        }
        if (updated.getRoomId() == null || updated.getRoomId().trim().isEmpty()) {
            throw new BadRequestException("Sensor roomId is required");
        }

        
        Room newRoom = dataStore.getRoom(updated.getRoomId());
        if (newRoom == null) {
            throw new LinkedResourceNotFoundException("Referenced roomId does not exist");
        }

        
        if (existing.getRoomId() != null && !existing.getRoomId().equals(updated.getRoomId())) {
            Room oldRoom = dataStore.getRoom(existing.getRoomId());
            if (oldRoom != null && oldRoom.getSensorIds() != null) {
                oldRoom.getSensorIds().remove(sensorId);
                dataStore.upsertRoom(oldRoom);
            }
        }

        if (newRoom.getSensorIds() != null && !newRoom.getSensorIds().contains(sensorId)) {
            newRoom.getSensorIds().add(sensorId);
            dataStore.upsertRoom(newRoom);
        }

        updated.setId(sensorId);
        dataStore.upsertSensor(updated);

        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor existing = dataStore.getSensor(sensorId);
        if (existing == null) {
            throw new NotFoundException("Sensor not found");
        }


        if (existing.getRoomId() != null) {
            Room room = dataStore.getRoom(existing.getRoomId());
            if (room != null && room.getSensorIds() != null) {
                room.getSensorIds().remove(sensorId);
                dataStore.upsertRoom(room);
            }
        }

        dataStore.deleteSensor(sensorId);
        return Response.noContent().build();
    }

    private void validateSensorForCreate(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            throw new BadRequestException("Sensor id is required");
        }
        if (sensor.getType() == null || sensor.getType().trim().isEmpty()) {
            throw new BadRequestException("Sensor type is required");
        }
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            throw new BadRequestException("Sensor status is required");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            throw new BadRequestException("Sensor roomId is required");
        }
    }
}