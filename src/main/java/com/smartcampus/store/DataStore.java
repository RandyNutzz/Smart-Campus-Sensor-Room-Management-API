package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store.
 *
 * Because JAX-RS resource classes are per-request by default (a new instance
 * is created for every HTTP request), we cannot store state inside resource
 * classes. This DataStore class uses a static singleton pattern with
 * ConcurrentHashMap to ensure thread-safe access to shared data across all
 * resource instances and concurrent requests.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Thread-safe maps for each entity type
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ---- Rooms ----
    public Map<String, Room> getRooms() { return rooms; }

    public Room getRoom(String id) { return rooms.get(id); }

    public void addRoom(Room room) { rooms.put(room.getId(), room); }

    public boolean deleteRoom(String id) {
        return rooms.remove(id) != null;
    }

    // ---- Sensors ----
    public Map<String, Sensor> getSensors() { return sensors; }

    public Sensor getSensor(String id) { return sensors.get(id); }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        sensorReadings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean deleteSensor(String id) {
        sensorReadings.remove(id);
        return sensors.remove(id) != null;
    }

    // ---- Sensor Readings ----
    public List<SensorReading> getReadings(String sensorId) {
        return sensorReadings.getOrDefault(sensorId, new ArrayList<>());
    }

    public synchronized void addReading(String sensorId, SensorReading reading) {
        sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
        // Update parent sensor's currentValue
        Sensor sensor = sensors.get(sensorId);
        if (sensor != null) {
            sensor.setCurrentValue(reading.getValue());
        }
    }

    // ---- Seed Data ----
    private void seedData() {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab A", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        sensorReadings.put(s1.getId(), new ArrayList<>());
        sensorReadings.put(s2.getId(), new ArrayList<>());
        sensorReadings.put(s3.getId(), new ArrayList<>());

        // Seed some readings
        SensorReading sr1 = new SensorReading(22.5);
        SensorReading sr2 = new SensorReading(23.1);
        sensorReadings.get(s1.getId()).add(sr1);
        sensorReadings.get(s1.getId()).add(sr2);
    }
}
