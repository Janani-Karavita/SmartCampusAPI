# Smart Campus API

A RESTful API built with JAX-RS (Jersey) for managing campus Rooms and Sensors. All data is stored in-memory using `ConcurrentHashMap` and `ArrayList` — no database is used.

---

## API Design Overview

The API is versioned at `/api/v1` using the `@ApplicationPath("/api/v1")` annotation on the `SmartCampusApplication` class.

**Three core resources:**
- `Room` – Use CRUD for represents a physical room with an ID, name, capacity, and a list of sensor IDs.
- `Sensor` – Use CRUD for represents a sensor deployed in a room (type, status, current value, roomId).
- `SensorReading` – a timestamped reading logged for a specific sensor. Sub-resource for SensorReading objects (id, timestamp, value)
- Storage - in-memory DataStore
- Error handling - 

**Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1` | Discovery endpoint (API metadata) |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a room |
| GET | `/api/v1/rooms/{id}` | Get a room by ID |
| DELETE | `/api/v1/rooms/{id}` | Delete a room (blocked if sensors exist) |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=` filter) |
| POST | `/api/v1/sensors` | Create a sensor |
| GET | `/api/v1/sensors/{id}` | Get a sensor by ID |
| PUT | `/api/v1/sensors/{id}` | Update a sensor |
| DELETE | `/api/v1/sensors/{id}` | Delete a sensor |
| GET | `/api/v1/sensors/{id}/readings` | Get reading history for a sensor |
| POST | `/api/v1/sensors/{id}/readings` | Add a new reading for a sensor |

**Error handling is fully implemented** — every error returns a structured JSON body with `code`, `message`, `status`, and `path`. No stack traces are ever exposed.(400 Bad Request, 404 Not Found, 409 Conflict, 422 Unprocessable Entity, 403 Forbidden.)

**Package structure:**
```
com.smartcampus
├── SmartCampusApplication.java       # JAX-RS entry point
├── api/
│   └── ApiError.java                 # Standard error response model
├── exception/                        # Custom exceptions
├── filter/
│   └── LoggingFilter.java            # Request/response logging
├── mapper/                           # Exception mappers
├── model/                            # Room, Sensor, SensorReading POJOs
├── resource/                         # JAX-RS resource classes
└── store/
    └── DataStore.java                # Singleton in-memory data store
```

## How to Build and Run

**Prerequisites:**
- Java 11+
- Maven 3.6+

**Step 1 – Build the project:**
- Open PowerShell in the project folder.
- Build: mvn clean package
```bash
mvn clean package
```

**Step 2 – Run the embedded Jetty server:**
- Run Jetty: mvn jetty:run
```bash
mvn jetty:run
```

**Step 3 – NetBeans built-in Tomcat:**
- Open the Maven project in NetBeans.
- Clean & Build or Deploy from NetBeans, then Start the server.

The server starts on `http://localhost:8080`. The API base path is `http://localhost:8080/api/v1`.

> Alternatively, deploy the generated `target/ROOT.war` to any Servlet container such as Apache Tomcat.

## Sample curl Commands

**1. Discovery endpoint**
```bash
curl -X GET http://localhost:8080/api/v1
```

**2. Create a room**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

**3. Create a sensor (linked to the room above)**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

**4. List sensors**
```bash
curl -X GET http://localhost:8080/api/v1/sensors
```

**5. Add a sensor reading**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"id":"READ-001","timestamp":1713780000000,"value":22.5}'
```

**6. Delete a sensor**
```bash
curl -X DELETE http://localhost:8080/api/v1/sensors/TEMP-001
```

**7. Attempt to delete a room that still has sensors (expects 409)**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```
