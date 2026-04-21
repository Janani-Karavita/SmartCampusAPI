package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory data store.
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    private final Map<String, List<SensorReading>> readingsBySensorId = new ConcurrentHashMap<>();

    private DataStore() {
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }


    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public Room upsertRoom(Room room) {
        if (room == null || room.getId() == null) {
            throw new IllegalArgumentException("Room and room.id must not be null");
        }
        return rooms.put(room.getId(), room);
    }

    public Room deleteRoom(String id) {
        return rooms.remove(id);
    }


    public Collection<Sensor> getAllSensors() {
        return sensors.values();
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }


    public Sensor upsertSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null) {
            throw new IllegalArgumentException("Sensor and sensor.id must not be null");
        }
        return sensors.put(sensor.getId(), sensor);
    }

    public Sensor deleteSensor(String id) {
        readingsBySensorId.remove(id);
        return sensors.remove(id);
    }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        if (sensorId == null) {
            throw new IllegalArgumentException("sensorId must not be null");
        }
        List<SensorReading> list = readingsBySensorId.get(sensorId);
        if (list == null) {
            return Collections.emptyList();
        }

        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public void addReading(String sensorId, SensorReading reading) {
        if (sensorId == null) {
            throw new IllegalArgumentException("sensorId must not be null");
        }
        if (reading == null) {
            throw new IllegalArgumentException("reading must not be null");
        }

        List<SensorReading> list = readingsBySensorId.computeIfAbsent(
                sensorId,
                k -> Collections.synchronizedList(new ArrayList<>())
        );
        list.add(reading);
    }
}