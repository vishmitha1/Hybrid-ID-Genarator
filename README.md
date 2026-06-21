# HybridIDGen

A Spring Boot demo project exploring different strategies for hybrid ID generation — where a single entity supports both PostgreSQL auto-increment and manually supplied IDs at runtime, decided per request at persist time.

---

## Table of Contents

- [Overview](#overview)
- [Approach 1 — Custom Hibernate Generator](#approach-1--custom-hibernate-generator)
  - [CustomIdentityGenerator](#customidentitygenerator)
  - [IdentityOrAssignedGenerator](#identityorassignedgenerator)
- [Approach 2 — @PrePersist Entity Listener](#approach-2--prepersist-entity-listener)
  - [ItemIdListener](#itemidlistener)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Database Setup](#database-setup)
  - [Run the Application](#run-the-application)
- [API Reference](#api-reference)
  - [Item Endpoints](#item-endpoints)
- [Configuration](#configuration)

---

## Overview

Most applications pick one ID strategy — either auto-increment or manually assigned. This project explores two different approaches that both handle **both** on the same entity, decided at persist time based on whether the caller supplies an `id` or not.

| Approach | Mechanism | Auto source | Manual source |
|---|---|---|---|
| `CustomIdentityGenerator` | Custom Hibernate `BeforeExecutionGenerator` | PostgreSQL `IDENTITY` column | Caller-supplied value via `generate()` |
| `ItemIdListener` | JPA `@PrePersist` + Spring entity listener | PostgreSQL sequence (`items_id_seq`) | Caller-supplied value, listener skips |

---

## Approach 1 — Custom Hibernate Generator

### CustomIdentityGenerator

**Location:** `com.visal.hybridIDGen.entity.CustomIdentityGenerator`

Extends Hibernate's `IdentityGenerator` and implements `BeforeExecutionGenerator`. It decides per entity instance at INSERT time which path to take:

| Scenario | `id` at save time | `generatedOnExecution(entity, session)` | What happens |
|---|---|---|---|
| **Auto** | `null` | returns `true` | Hibernate omits the `id` column; PostgreSQL `IDENTITY` assigns the next value |
| **Manual** | set (e.g. `500`) | returns `false` | `generate()` returns the existing id; Hibernate includes it in the INSERT |

The no-arg `generatedOnExecution()` always returns `true` — required by Hibernate to register this as a "mixed-timing" generator that can switch behavior per entity instance.

> **Known limitation (Hibernate 6.6.x):** When `@Version` is present on the entity, persisting with a manually assigned id throws `PropertyValueException: Detached entity with generated id has an uninitialized version value 'null'` at `AbstractEntityPersister.isTransient()`. This is a regression from Hibernate 6.4.x where the per-instance `generatedOnExecution(entity, session)` was consulted during entity state detection. Tracked at [HHH-20567](https://hibernate.atlassian.net/browse/HHH-20567).

### IdentityOrAssignedGenerator

**Location:** `com.visal.hybridIDGen.entity.IdentityOrAssignedGenerator`

An alternative implementation of the same mixed-timing pattern, kept as a reference. Currently commented out in `Item.java`. Functionally equivalent to `CustomIdentityGenerator`.

---

## Approach 2 — @PrePersist Entity Listener

### ItemIdListener

**Location:** `com.visal.hybridIDGen.entity.ItemIdListener`

A JPA entity listener that hooks into the `@PrePersist` lifecycle event to apply the same hybrid logic — without touching Hibernate's generator API. Avoids the HHH-20567 bug because no `@GeneratedValue` is registered on the entity.

**How the decision is made:**

| Scenario | `id` at save time | What the listener does |
|---|---|---|
| **Auto** | `null` | Calls `SELECT nextval('items_id_seq')` and sets the id before the INSERT |
| **Manual** | set (e.g. `500`) | Skips generation — uses the caller-supplied value as-is |

**Why `@PersistenceContext` cannot be used directly on the entity:**

Spring's IoC container only injects into Spring-managed beans. JPA entity instances are created by Hibernate via reflection — `@PersistenceContext` on an entity field is never processed, so `EntityManager` would always be `null`.

**Solution — static self-reference pattern:**

```java
@Component
public class ItemIdListener {

    private static ItemIdListener INSTANCE;

    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    public void init() {
        INSTANCE = this;           // Spring bean stores itself after injection
    }

    @PrePersist
    public void prePersist(Item item) {
        if (item.getId() == null && INSTANCE != null) {
            Integer nextId = ((Number) INSTANCE.em
                    .createNativeQuery("SELECT nextval('items_id_seq')")
                    .getSingleResult())
                    .intValue();
            item.setId(nextId);
        }
    }
}
```

Spring creates **one** `ItemIdListener` bean, injects `EntityManager` into it, and stores it in `INSTANCE`. Hibernate creates a **separate** `ItemIdListener` instance (via reflection) when it processes entity lifecycle events. That Hibernate-created instance's `prePersist()` reads `INSTANCE.em` — the Spring-injected one — not its own null field.

Wire the listener to the entity with `@EntityListeners`:

```java
@Entity
@Table(name = "items")
@EntityListeners(ItemIdListener.class)
public class Item {
    @Id
    @Column(name = "id")
    private Integer id;

    @Version
    @Column(name = "version")
    private Long version;
    // ...
}
```

**Sequence setup:**

`ddl-auto=update` does not create the sequence because there is no `@GeneratedValue`. `src/main/resources/sequence.sql` handles this at startup:

```sql
CREATE SEQUENCE IF NOT EXISTS items_id_seq START 1 INCREMENT 1;
SELECT setval('items_id_seq', COALESCE((SELECT MAX(id) FROM items), 0) + 1, false);
```

The `setval` call ensures the sequence starts above the current maximum `id` already in the table, so no conflicts occur if rows were inserted before the sequence existed. `spring.jpa.defer-datasource-initialization=true` ensures the `items` table is created by Hibernate first before this script runs.

**`@Version` compatibility:**

Without `@GeneratedValue`, Spring Data JPA's `isNew()` uses the `@Version` null check (`version == null` → `persist()`) and Hibernate's `isTransient()` no longer applies the HHH-20567 strict validation. Optimistic locking works correctly: Hibernate sets `version = 0` on INSERT and increments it on each UPDATE.

---

## Project Structure

```
src/main/java/com/visal/hybridIDGen/
├── HybridIDGenApplication.java              Spring Boot entry point
├── controller/
│   └── ItemController.java                  POST /items/auto, POST /items/manual, PUT /items/{id}
├── entity/
│   ├── Item.java                            Entity (Integer PK, @Version)
│   ├── ItemIdListener.java                  @PrePersist listener — Approach 2 (sequence-based)
│   ├── CustomIdentityGenerator.java         Approach 1 — active mixed-timing Hibernate generator
│   └── IdentityOrAssignedGenerator.java     Approach 1 alternative (reference, not active)
├── service/
│   └── ItemService.java                     Validation and repository calls
└── repository/
    └── ItemRepository.java

src/main/resources/
├── application.properties
└── sequence.sql                             Creates items_id_seq on startup (Approach 2)
```

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Hibernate ORM | 6.4.4.Final |
| PostgreSQL | 17 |
| Lombok | managed by Spring Boot |
| Maven | 3.x |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 17 running on `localhost:5432`

### Database Setup

Create the database once before the first run:

```sql
psql -U postgres -c "CREATE DATABASE hybrid_id_gen_db;"
```

Hibernate's `ddl-auto=update` will create and maintain all tables automatically on startup.

### Run the Application

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

---

## API Reference

### Item Endpoints

#### POST /items/auto — Auto-assigned ID

The `id` field is omitted. The active ID strategy assigns the next value automatically.

```bash
curl -s -X POST http://localhost:8080/items/auto \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple"}'
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "name": "Apple",
  "version": 0
}
```

#### POST /items/manual — Manually supplied ID

The `id` field is provided. The active ID strategy preserves the caller-supplied value.

```bash
curl -s -X POST http://localhost:8080/items/manual \
  -H "Content-Type: application/json" \
  -d '{"id": 500, "name": "Banana"}'
```

**Response `201 Created`:**
```json
{
  "id": 500,
  "name": "Banana",
  "version": 0
}
```

#### PUT /items/{id} — Update an item

Fetches the existing item by id, updates the name, and saves via JPA. The `@Version` field is incremented by Hibernate automatically on each update.

```bash
curl -s -X PUT http://localhost:8080/items/500 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Banana"}'
```

**Response `200 OK`:**
```json
{
  "id": 500,
  "name": "Updated Banana",
  "version": 1
}
```

**Error responses:**

| Status | Reason |
|---|---|
| `400 Bad Request` | Missing or blank `name` / `id` field, or `id` is not an integer |
| `409 Conflict` | An item with the given `id` already exists |
| `404 Not Found` | No item found for the given `id` on update |

---

## Configuration

All settings are in `src/main/resources/application.properties`.

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/hybrid_id_gen_db
spring.datasource.username=postgres
spring.datasource.password=password

# Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Required for Approach 2 (@PrePersist listener):
# Runs sequence.sql after Hibernate creates the schema, so items table exists when setval runs
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:sequence.sql
```

To change the database password, update `spring.datasource.password`.  
To pin a specific Hibernate version, add `hibernate-core` as an explicit dependency in `pom.xml`.
