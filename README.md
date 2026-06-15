# HybridIDGen

A Spring Boot demo project showcasing a custom Hibernate mixed-timing ID generation strategy — a single generator that supports both PostgreSQL auto-increment and manually supplied integer IDs on the same entity at runtime.

---

## Table of Contents

- [Overview](#overview)
- [ID Generator](#id-generator)
  - [CustomIdentityGenerator](#customidentitygenerator)
  - [IdentityOrAssignedGenerator](#identityorassignedgenerator)
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

Most applications pick one ID strategy — either auto-increment or manually assigned. This project explores a single Hibernate generator that handles **both** on the same entity, decided at persist time based on whether the caller supplies an `id` or not.

| Generator | Entity | ID Type | Auto example | Manual example |
|---|---|---|---|---|
| `CustomIdentityGenerator` | `Item` | `Integer` | `1` (PostgreSQL auto-increment) | `500` (caller-supplied) |

---

## ID Generator

### CustomIdentityGenerator

**Location:** `com.visal.hybridIDGen.entity.CustomIdentityGenerator`

Extends Hibernate's `IdentityGenerator` and implements `BeforeExecutionGenerator`. It decides per entity instance at INSERT time which path to take:

| Scenario | `id` at save time | `generatedOnExecution(entity, session)` | What happens |
|---|---|---|---|
| **Auto** | `null` | returns `true` | Hibernate omits the `id` column; PostgreSQL auto-increment assigns the next value |
| **Manual** | set (e.g. `500`) | returns `false` | `generate()` is called; returns the existing id; Hibernate includes it in the INSERT |

The no-arg `generatedOnExecution()` always returns `true` — required by Hibernate to register this as a "mixed-timing" generator that can switch behavior per entity instance.

> **Known limitation (Hibernate 6.6.x):** When `@Version` is present on the entity, persisting with a manually assigned id throws `PropertyValueException: Detached entity with generated id has an uninitialized version value 'null'` at `AbstractEntityPersister.isTransient()`. This is a regression from Hibernate 6.4.x where the per-instance `generatedOnExecution(entity, session)` was consulted during entity state detection. Tracked at [HHH-20567](https://hibernate.atlassian.net/browse/HHH-20567).

### IdentityOrAssignedGenerator

**Location:** `com.visal.hybridIDGen.entity.IdentityOrAssignedGenerator`

An alternative implementation of the same mixed-timing pattern, kept as a reference. Currently commented out in `Item.java`. Functionally equivalent to `CustomIdentityGenerator`.

---

## Project Structure

```
src/main/java/com/visal/hybridIDGen/
├── HybridIDGenApplication.java              Spring Boot entry point
├── controller/
│   └── ItemController.java                  POST /items/auto, POST /items/manual, PUT /items/{id}
├── entity/
│   ├── Item.java                            Entity using CustomIdentityGenerator (Integer PK, @Version)
│   ├── CustomIdentityGenerator.java         Active mixed-timing generator
│   └── IdentityOrAssignedGenerator.java     Alternative generator (reference, not active)
├── service/
│   └── ItemService.java                     Validation and repository calls
└── repository/
    └── ItemRepository.java
```

---

## Tech Stack

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Hibernate ORM | 6.5.3.Final |
| PostgreSQL | 16 |
| Lombok | managed by Spring Boot |
| Maven | 3.x |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 16 running on `localhost:5432`

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

Uses `CustomIdentityGenerator` — supports both PostgreSQL auto-increment and manual integer IDs.

#### Scenario 1 — Auto increment

The `id` field is omitted. `generatedOnExecution(entity, session)` returns `true`, so PostgreSQL auto-increment assigns the next integer.

```http
POST /items/auto
Content-Type: application/json

{
  "name": "Apple"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "name": "Apple",
  "version": 0
}
```

#### Scenario 2 — Manual ID

The `id` field is provided. `generatedOnExecution(entity, session)` returns `false`, so `generate()` returns the supplied value and Hibernate includes it in the INSERT statement.

```http
POST /items/manual
Content-Type: application/json

{
  "id": 500,
  "name": "Banana"
}
```

**Response `201 Created`:**
```json
{
  "id": 500,
  "name": "Banana",
  "version": 0
}
```

#### Update an item

Fetches the existing item by id, updates the name, and saves via JPA. The `@Version` field is incremented by Hibernate automatically on each update.

```http
PUT /items/{id}
Content-Type: application/json

{
  "name": "Updated Banana"
}
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
spring.datasource.password=postgres

# Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

To change the database password, update `spring.datasource.password`.  
To pin a specific Hibernate version, add `hibernate-core` as an explicit dependency in `pom.xml`.

# Hybrid-ID-Genarator
