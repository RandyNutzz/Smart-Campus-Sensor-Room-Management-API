# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) for managing university campus rooms and IoT sensors.

---

## API Design Overview

The API follows a hierarchical resource model that mirrors the physical campus structure:

```
/api/v1                          → Discovery endpoint
/api/v1/rooms                    → Room collection
/api/v1/rooms/{roomId}           → Individual room
/api/v1/sensors                  → Sensor collection
/api/v1/sensors/{sensorId}       → Individual sensor
/api/v1/sensors/{sensorId}/readings  → Historical readings (sub-resource)
```

All data is stored in-memory using `ConcurrentHashMap` and `ArrayList`. No database is used.

---

## Tech Stack

- **Java 11**
- **JAX-RS 2.1** (Jersey 2.39.1 implementation)
- **Jackson** for JSON serialisation
- **Maven** for build management
- **Apache Tomcat 9** as the servlet container

---

## How to Build and Run

### Prerequisites

- JDK 11 or later
- Maven 3.6+
- Apache Tomcat 9 (bundled with NetBeans, or download from https://tomcat.apache.org)
- NetBeans IDE 12+ (recommended)

### Option A: NetBeans IDE

1. Open NetBeans → **File → Open Project**
2. Navigate to and select the `smartcampus-api` folder
3. Right-click the project → **Clean and Build**
4. Ensure a Tomcat 9 server is configured: **Tools → Servers → Add Server → Apache Tomcat**
5. Right-click the project → **Run** (deploys to Tomcat automatically)
6. API is available at: `http://localhost:8080/smartcampus-api/api/v1`

### Option B: Maven Command Line + Tomcat

```bash
# 1. Clone or navigate to project root
cd smartcampus-api

# 2. Build the WAR file
mvn clean package

# 3. Copy WAR to Tomcat webapps directory
cp target/smartcampus-api.war /path/to/tomcat/webapps/

# 4. Start Tomcat
/path/to/tomcat/bin/startup.sh   # Linux/macOS
/path/to/tomcat/bin/startup.bat  # Windows

# 5. Access the API
curl http://localhost:8080/smartcampus-api/api/v1
```

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1
```

### 2. Get All Rooms
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/rooms
```

### 3. Create a New Room
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-001","name":"Main Hall","capacity":200}'
```

### 4. Get Specific Room
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/rooms/LIB-301
```

### 5. Delete a Room (no sensors)
```bash
curl -X DELETE http://localhost:8080/smartcampus-api/api/v1/rooms/HALL-001
```

### 6. Delete a Room that has sensors (triggers 409)
```bash
curl -X DELETE http://localhost:8080/smartcampus-api/api/v1/rooms/LIB-301
```

### 7. Get All Sensors
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/sensors
```

### 8. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/smartcampus-api/api/v1/sensors?type=CO2"
```

### 9. Create a New Sensor (valid room)
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LAB-101"}'
```

### 10. Create a Sensor with Invalid Room (triggers 422)
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

### 11. Get Sensor Readings
```bash
curl -X GET http://localhost:8080/smartcampus-api/api/v1/sensors/TEMP-001/readings
```

### 12. Post a New Sensor Reading
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.7}'
```

### 13. Post Reading to MAINTENANCE Sensor (triggers 403)
```bash
curl -X POST http://localhost:8080/smartcampus-api/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15.0}'
```

---

## Conceptual Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request scope). This is the opposite of a singleton. The runtime instantiates the resource, processes the request, and discards the instance afterwards.

**Impact on in-memory data management:** Because each request gets a fresh resource object, any state stored as an instance variable inside a resource class would be lost after the request ends. To safely share data across all requests, this project uses a **singleton `DataStore` class** accessed via a static `getInstance()` method. The `DataStore` internally uses `ConcurrentHashMap`, which is thread-safe and handles concurrent access from multiple simultaneous requests without race conditions or data corruption. This cleanly separates the stateless HTTP layer (resource classes) from the stateful data layer (DataStore).

---

### Part 1.2 — HATEOAS and Hypermedia

**HATEOAS (Hypermedia As The Engine Of Application State)** is considered a hallmark of advanced RESTful design because it makes an API self-describing and navigable — much like the web itself.

In a HATEOAS-compliant API, every response includes links to related actions and resources. For example, a GET /rooms response might embed a `_links` object containing URIs to create a room, fetch sensors, or navigate to sub-resources.

**Benefits over static documentation:**
- Clients do not need to hardcode URLs — they follow links embedded in responses
- The API can change URL structures without breaking clients that follow links rather than assume paths
- New capabilities can be discovered at runtime rather than requiring documentation updates
- Reduces client-side coupling, enabling the server to evolve independently

---

### Part 2.1 — IDs vs Full Objects in List Responses

**Returning only IDs** is bandwidth-efficient and fast for large collections, but forces clients to issue one additional GET request per item (the N+1 problem), increasing latency and server load.

**Returning full objects** provides all data in a single round-trip, eliminating follow-up requests. However, for collections of thousands of rooms with deeply nested sensor arrays, this can produce very large payloads that stress network and memory resources.

**Best practice:** Return a compact summary representation (e.g., id, name, capacity) in list responses, and reserve the full object (including sensorIds and metadata) for individual GET /{roomId} requests. This balances network efficiency with client usability.

---

### Part 2.2 — DELETE Idempotency

Yes, **DELETE is idempotent** in this implementation. Idempotency means that making the same request multiple times produces the same server state as making it once — not necessarily the same response code.

- **First DELETE on an existing room:** The room is removed. Returns `200 OK`.
- **Second DELETE on the same (now deleted) room:** The room no longer exists. Returns `404 Not Found`.

The server state is identical after both calls — the room is absent. The response code differs, but idempotency is defined by effect on state, not HTTP status. This is consistent with RFC 7231, which states DELETE is idempotent in terms of intended effect.

---

### Part 3.1 — @Consumes and Media Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation declares a contract: this endpoint only processes requests with `Content-Type: application/json`.

If a client sends `text/plain` or `application/xml`, JAX-RS performs content negotiation before invoking the method. Finding no matching `@Consumes` handler, it immediately returns **HTTP 415 Unsupported Media Type** — the method body is never executed. This is enforced entirely by the JAX-RS runtime, requiring no manual content-type checking inside the method, making the API safer and cleaner.

---

### Part 3.2 — @QueryParam vs Path-Based Filtering

`@QueryParam` (`/sensors?type=CO2`) is superior to path-based filtering (`/sensors/type/CO2`) for several reasons:

1. **Semantic correctness:** Path segments identify a specific resource. `/sensors` is the collection — it always refers to the same resource regardless of filtering. Query params *modify* how that resource is presented.
2. **Composability:** Multiple filters combine naturally: `?type=CO2&status=ACTIVE`. Path-based equivalents would require deeply nested or awkward URLs.
3. **Optionality:** Query params are inherently optional. Path segments imply a required identifier, leading to conflicts when filtering is not needed.
4. **Caching compatibility:** Caches and proxies treat the path as the resource key. Path-based filters create separate cache entries for the same underlying collection.
5. **REST convention:** Query params are the universally accepted convention for search, filter, sort, and paginate operations.

---

### Part 4.1 — Sub-Resource Locator Pattern

The sub-resource locator pattern (where a resource method returns an instance of another resource class rather than a response) offers significant architectural benefits:

1. **Single Responsibility Principle:** `SensorResource` manages sensor CRUD; `SensorReadingResource` manages historical data. Each class has one clear purpose.
2. **Maintainability:** In large APIs, placing every nested endpoint in one controller creates unmaintainable "god classes" with hundreds of methods. Sub-resources create a navigable class hierarchy that mirrors the URL hierarchy.
3. **Testability:** Sub-resource classes can be unit-tested in isolation with a known `sensorId`, without needing to bootstrap the full resource tree.
4. **Reusability:** A sub-resource class could theoretically be reused in multiple parent contexts.
5. **Clarity:** The code structure visually communicates the API hierarchy — reading `SensorResource` clearly shows that readings are delegated to another class.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Reference

When a client POSTs a sensor with a `roomId` that doesn't exist, **HTTP 422 Unprocessable Entity** is more semantically accurate than 404 for these reasons:

- **404 Not Found** means the *requested URL* (`/api/v1/sensors`) was not found. But the URL is perfectly valid and was found — the endpoint exists and processed the request.
- The problem is not the URL, but a **broken reference inside the request body**. The JSON is syntactically valid, but semantically invalid because it declares a dependency (`roomId`) that doesn't exist.
- **422** explicitly signals: "I understood your request, I can parse it, but I cannot fulfil it because the instructions are semantically invalid."
- Using 404 here would mislead clients into thinking the sensors endpoint itself doesn't exist, causing incorrect error-handling logic on the client side.

---

### Part 5.4 — Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a critical security vulnerability:

1. **Technology fingerprinting:** Class names, package structures, and framework names (e.g., `org.glassfish.jersey`) reveal the exact tech stack, allowing attackers to research known CVEs.
2. **Architecture disclosure:** Internal package naming conventions (e.g., `com.smartcampus.store.DataStore`) reveal application structure, making targeted attacks easier.
3. **Path traversal hints:** File system paths sometimes appear in traces, exposing server directory structures.
4. **Logic flow exposure:** Exception messages and stack frames reveal which code paths exist, enabling attackers to craft precise inputs that trigger vulnerable branches.
5. **Version-specific exploits:** Framework version numbers in traces allow attackers to look up unpatched vulnerabilities for that exact version.

The global `ExceptionMapper<Throwable>` ensures all unexpected errors return a generic 500 message to clients, while logging full detail server-side only.

---

### Part 5.5 — JAX-RS Filters vs Inline Logging

Using JAX-RS filters for cross-cutting concerns like logging is superior to manual `Logger.info()` calls because:

1. **DRY Principle:** One filter class handles logging for every endpoint. Adding a new resource automatically gets logging for free.
2. **Separation of concerns:** Business logic in resource classes stays clean and focused. Infrastructure concerns (logging, authentication, CORS) live in filters.
3. **Consistency:** Every request is logged identically. Manual logging is prone to inconsistency — developers may forget to add it, use different formats, or log at different levels.
4. **Maintainability:** Changing the log format requires editing one filter, not hunting through dozens of resource methods.
5. **Ordering and control:** Multiple filters can be chained and ordered using `@Priority`, giving fine-grained control over the filter pipeline.
