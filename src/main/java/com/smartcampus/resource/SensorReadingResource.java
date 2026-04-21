package com.smartcampus.resource;

import com.smartcampus.exception.BadRequestException;
import com.smartcampus.exception.NotFoundException;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path("/readings")
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore dataStore = DataStore.getInstance();

    public SensorReadingResource(@PathParam("sensorId") String sensorId) {
        this.sensorId = sensorId;
    }

    public String getSensorId() {
        return sensorId;
    }


    @GET
    public Response getReadingHistory() {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found");
        }

        List<SensorReading> history = dataStore.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found");
        }

        //Offline sensors reject new readings.
        if (sensor.getStatus() != null && sensor.getStatus().equalsIgnoreCase("OFFLINE")) {
            throw new SensorUnavailableException("Sensor is OFFLINE and cannot accept readings");
        }

        if (reading == null || reading.getId() == null || reading.getId().trim().isEmpty()) {
            throw new BadRequestException("Reading id is required");
        }

        dataStore.addReading(sensorId, reading);

        //currentValue consistent with latest reading.
        sensor.setCurrentValue(reading.getValue());
        dataStore.upsertSensor(sensor);

        return Response.created(URI.create("/api/v1/sensors/" + sensorId + "/readings/" + reading.getId()))
                .entity(reading)
                .build();
    }
}