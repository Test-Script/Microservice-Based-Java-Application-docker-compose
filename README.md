# Spring Boot Microservices — Interdependent Architecture

Three Spring Boot services that depend on each other through REST HTTP calls.

```
┌──────────────────────────────────────────────────────────┐
│                     ORDER SERVICE                         │
│                      :8083                               │
│                                                          │
│  POST /api/orders  ──┬──► User Service  (validate user)  │
│                      └──► Product Service (reserve stock) │
│  POST /api/orders/{id}/cancel                            │
│          └──────────────► Product Service (release stock) │
└──────────────────────────────────────────────────────────┘
         │                         │
         ▼                         ▼
┌────────────────┐       ┌──────────────────┐
│  USER SERVICE  │       │ PRODUCT SERVICE  │
│    :8081       │       │    :8082         │
│                │       │                  │
│ GET  /users    │       │ GET  /products   │
│ POST /users    │       │ POST /products   │
│ GET  /users    │       │ POST /{id}/reserve│
│   /{id}/active │       │ POST /{id}/release│
└────────────────┘       └──────────────────┘
```

## Services

| Service         | Port | Responsibility                            |
|----------------|------|-------------------------------------------|
| user-service   | 8081 | CRUD users, validate active status        |
| product-service| 8082 | CRUD products, manage stock reservation   |
| order-service  | 8083 | Place/cancel orders, orchestrates the two above |

---

## Quick Start (local, no Docker)

### Prerequisites
- Java 17+
- Maven 3.8+

### 1. Start User Service
```bash
cd user-service
mvn spring-boot:run
# Runs on http://localhost:8081
# H2 console: http://localhost:8081/h2-console
```

### 2. Start Product Service
```bash
cd product-service
mvn spring-boot:run
# Runs on http://localhost:8082
```

### 3. Start Order Service
```bash
cd order-service
mvn spring-boot:run
# Runs on http://localhost:8083
```

---

## Quick Start (Docker Compose)

```bash
docker-compose up --build
```

Order Service will wait for User Service and Product Service to be healthy before starting.

---

## API Examples

### User Service

```bash
# List all users (3 seeded on startup)
curl http://localhost:8081/api/users

# Get a specific user
curl http://localhost:8081/api/users/1

# Create a new user
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dave","email":"dave@example.com","phone":"555-9999"}'

# Check if user is active (used internally by Order Service)
curl http://localhost:8081/api/users/1/active
```

### Product Service

```bash
# List all products (3 seeded on startup)
curl http://localhost:8082/api/products

# List only available products
curl http://localhost:8082/api/products/available

# Create a product
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Mechanical Keyboard","price":149.99,"stockQuantity":30,"category":"ELECTRONICS"}'
```

### Order Service

```bash
# Place a new order (calls User Service + Product Service internally)
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":2,"quantity":1,"notes":"Please gift wrap"}'

# Get all orders
curl http://localhost:8083/api/orders

# Get orders for a specific user
curl http://localhost:8083/api/orders/user/1

# Cancel an order (releases stock back to Product Service)
curl -X POST http://localhost:8083/api/orders/1/cancel

# Advance order status (CONFIRMED → SHIPPED → DELIVERED)
curl -X POST http://localhost:8083/api/orders/1/advance
```

---

## Inter-Service Communication

Order Service uses Spring WebFlux `WebClient` to make HTTP calls:

| Flow                   | Caller        | Callee          | Endpoint                      |
|------------------------|--------------|-----------------|-------------------------------|
| Validate user on order | Order Service | User Service    | `GET /api/users/{id}/active`  |
| Fetch product info     | Order Service | Product Service | `GET /api/products/{id}`      |
| Reserve stock          | Order Service | Product Service | `POST /api/products/{id}/reserve` |
| Release stock (cancel) | Order Service | Product Service | `POST /api/products/{id}/release` |

---

## Health Checks

All services expose Spring Actuator endpoints:

```bash
curl http://localhost:8081/actuator/health   # User Service
curl http://localhost:8082/actuator/health   # Product Service
curl http://localhost:8083/actuator/health   # Order Service
```

---

## Architecture Notes

- **No service registry** (Eureka/Consul): URLs are configured in `application.properties`
- **No API Gateway**: Direct service-to-service calls for simplicity
- **In-memory H2 databases**: Each service has its own isolated DB (data resets on restart)
- **Stock management**: Order Service coordinates with Product Service for atomic stock reservation
- **Error handling**: Global `@RestControllerAdvice` in Order Service returns structured errors

To productionise, consider adding:
- Service discovery (Spring Cloud Netflix Eureka or Consul)
- API Gateway (Spring Cloud Gateway)
- Persistent databases (PostgreSQL per service)
- Distributed tracing (Zipkin/Sleuth)
- Circuit breakers (Resilience4j)
- Message queues (Kafka/RabbitMQ) for async events
