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

# Smart Campus API (5COSC022W Coursework)

Part 1 – Service Architecture & Setup

### Q1 — Default lifecycle of a JAX-RS Resource class and its impact on in-memory data management

1. The Default Lifecycle: Per-Request

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP
request. This is called per-request lifecycle. The consequence of this design is that resource classes
must never store shared application state in their own instance fields, because each request gets a
fresh object and any data saved on one instance is invisible to the next request.
This means that every time a user sends a request to an endpoint, the server creates a brand new instance of the Resource class. Once the server sends the JSON response back, that instance is destroyed.

2. Impact on Data Management

In this project that problem is solved by using a singleton DataStore. The DataStore class is
implemented as a static singleton (DataStore.getInstance()) and its maps are ConcurrentHashMap
instances, which are thread safe by design. Every resource class — RoomResource,
SensorResource, SensorReadingResource calls DataStore.getInstance() in its constructor, so all
requests share the same in memory maps regardless of how many resource instances are created.

3. Synchronization and Race Conditions

The synchronized list used for sensor readings (Collections.synchronizedList) prevents race
conditions when multiple threads append readings at the same time.

### Q2 — Why is HATEOAS considered a hallmark of advanced RESTful design?

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links
pointing to related resources and available next actions. Instead of hard coding URLs in client code,
a client can start at the discovery endpoint and follow links to navigate the API dynamically.

In this project the GET /api/v1 discovery endpoint returns a 'resources' map containing the paths to
/api/v1/rooms and /api/v1/sensors. This gives client developers a single entry point from which the
whole API can be explored. 

**Benefits vs static documentation:**

- The benefit over static documentation is that if a path changes, clients that follow links rather than hard code them will still work correctly without needing an update. 
- It also makes the API self describing, which reduces the chance of integration errors.
- It reduces coupling between client and server and improves long-term maintainability.

Part 2 – Room Management

### Q3 — Implications of returning only IDs versus full room objects in a list response

If `GET /rooms` returns "only room IDs", the response is smaller and cheaper to transmit (lower bandwidth) and can be faster for clients that only need a picker/list.

If `GET /rooms` returns the "full room objects", the client gets everything in one call (less round-trips) but the payload is larger, which increases bandwidth usage and client-side parsing costs.

A common compromise is to return a summary representation (e.g., id + name) or use pagination. For this SmartCampusAPI, returning full objects is acceptable for simplicity, but the trade-off is larger responses as the number of rooms grows.

### Q4 — Is the DELETE operation idempotent in this implementation?

A DELETE request is **idempotent** if repeating the exact same request results in the same server state.

In this implementation:

- First `DELETE /rooms/{id}` removes the room (server state changes).
- Repeating the same DELETE again returns **404 Not Found** because the room is already gone and the server state remains unchanged.

Therefore, the operation is idempotent with respect to server state.

Part 3 – Sensor Operations & Linking

### Q5 — Consequences of a client sending data in a format other than application/json

The POST /api/v1/sensors method is annotated with
@Consumes(MediaType.APPLICATION_JSON). If a client sends a request with a Content-Type
header of text/plain or application/xml, JAX-RS cannot find a matching method because the
declared media type does not match the request media type. The JAX-RS runtime (Jersey in this
project) automatically rejects the request and returns an HTTP 415 Unsupported Media Type
response before the resource method is ever called. No application code runs, so there is no risk of
partial processing.

### Q6 — Why is @QueryParam preferred over a path segment for filtering?

Using @QueryParam for filtering (e.g., GET /api/v1/sensors?type=CO2) is the standard RESTful
approach because query parameters are understood to be optional refinements of a collection
request. The base path /api/v1/sensors still identifies the sensors collection clearly, and omitting the
parameter returns all sensors.
An alternative path-based approach such as /api/v1/sensors/type/CO2 treats 'type' as a
sub-resource, which implies it is a permanent part of the URL hierarchy rather than a filter. It also
makes caching and URL generation more complicated. With query parameters multiple filters can
be combined easily (e.g., ?type=CO2&status=ACTIVE) without restructuring the URL. For these
reasons the query parameter approach was used in SensorResource.

Part 4 – Deep Nesting with Sub-Resources

### Q7 — Architectural benefits of the Sub-Resource Locator pattern

The sub-resource locator pattern allows a parent resource to delegate handling of a nested path to a
separate, dedicated class. In this project SensorResource contains a method annotated with
@Path('{sensorId}/readings') that simply returns a new instance of SensorReadingResource.
JAX-RS then dispatches subsequent path segments to that class.
The main benefit is separation of concerns. SensorResource focuses on sensor CRUD operations
and SensorReadingResource focuses entirely on reading history for one sensor. If all endpoints
were defined in a single massive resource class, the file would grow very large and changes to
reading logic could accidentally break sensor logic. Having separate classes also makes unit testing
simpler because each class has a clear, limited responsibility.

Part 5 – Advanced Error Handling, Exception Mapping & Logging

### Q8— Why is HTTP 422 more semantically accurate than 404 for a missing roomId reference?

A 404 Not Found response conventionally means the requested URL itself does not exist on the
server. When a client POSTs a sensor with a roomId that does not exist, the request URL
/api/v1/sensors is perfectly valid the resource is there. 

The problem is inside the request body:

the referenced roomId is a foreign key that cannot be resolved.
HTTP 422 Unprocessable Entity is designed precisely for this situation. It signals that the request
was syntactically correct JSON (the server could parse it), but the content fails a semantic or
business rule validation. Returning 422 makes it clear to the client that it needs to fix the data in the
body specifically the roomId field rather than thinking the API endpoint itself is missing. This is
why LinkedResourceNotFoundException is mapped to 422 in this project's APIExceptionMapper.

### Q9 — Cybersecurity risks of exposing internal Java stack traces to external consumers

A Java stack trace reveals a significant amount of internal system information. An attacker can read
the exact class names, package structure, method names, and line numbers of the application. This
information can be used to identify the frameworks and libraries in use (and their versions), look up
known CVEs for those specific versions, and craft targeted payloads that exploit known weaknesses
in identified code paths.
Stack traces can also reveal file paths on the server, database query fragments, and internal
variable names that give insight into business logic. For these reasons this project implements a
GenericExceptionMapper that catches all Throwable instances and returns only a generic 500
Internal Server Error response with a fixed, safe message. No internal detail is ever sent to the
client.

### Q10 — Why use JAX-RS filters for logging rather than manual Logger calls in every resource method?

Inserting Logger.info() statements in every resource method is a cross-cutting concern — it is
repeated, identical code scattered across many classes. If the logging format needs to change,
every method must be edited, which increases the risk of inconsistency and bugs.
JAX-RS filters implement the same logic in one place. The LoggingFilter class in this project
implements both ContainerRequestFilter and ContainerResponseFilter. Every request and response
passes through this single class automatically because it is annotated with @Provider, which tells
Jersey to register it globally. This keeps resource classes clean and focused on business logic, and
guarantees that every endpoint is logged consistently without any risk of a developer forgetting to
add a log statement to a new method.