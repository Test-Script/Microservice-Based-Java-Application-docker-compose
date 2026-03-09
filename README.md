# Spring Boot Microservices — Interdependent Architecture

Hey there! This is a setup with three Spring Boot services that talk to each other over REST HTTP calls. It's all about how they depend on one another to get things done.

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

Here's what each service does:

| Service         | Port | What it handles                            |
|----------------|------|-------------------------------------------|
| user-service   | 8081 | Manages users, checks if they're active   |
| product-service| 8082 | Handles products, keeps track of stock    |
| order-service  | 8083 | Places and cancels orders, coordinates the others |

---

## Quick Start (local, no Docker)

### What you need first
- Java 17 or higher
- Maven 3.8 or up

### 1. Fire up the User Service
```bash
cd user-service
mvn spring-boot:run
# It'll run on http://localhost:8081
# Check the H2 console at http://localhost:8081/h2-console
```

### 2. Start the Product Service
```bash
cd product-service
mvn spring-boot:run
# Runs on http://localhost:8082
```

### 3. Launch the Order Service
```bash
cd order-service
mvn spring-boot:run
# Runs on http://localhost:8083
```

---

## Quick Start (with Docker Compose)

Just run this and you're good:

```bash
docker-compose up --build
```

The Order Service waits for the other two to be ready before starting up.

---

## API Examples

### User Service

```bash
# See all users (starts with 3 pre-loaded)
curl http://localhost:8081/api/users

# Grab a specific user
curl http://localhost:8081/api/users/1

# Add a new user
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dave","email":"dave@example.com","phone":"555-9999"}'

# Check if a user is active (Order Service uses this)
curl http://localhost:8081/api/users/1/active
```

### Product Service

```bash
# List all products (3 seeded at start)
curl http://localhost:8082/api/products

# Show only available ones
curl http://localhost:8082/api/products/available

# Create a product
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Mechanical Keyboard","price":149.99,"stockQuantity":30,"category":"ELECTRONICS"}'
```

### Order Service

```bash
# Place an order (talks to User and Product Services behind the scenes)
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productId":2,"quantity":1,"notes":"Please gift wrap"}'

# Get all orders
curl http://localhost:8083/api/orders

# Orders for a specific user
curl http://localhost:8083/api/orders/user/1

# Cancel an order (frees up stock in Product Service)
curl -X POST http://localhost:8083/api/orders/1/cancel

# Move order status forward (CONFIRMED → SHIPPED → DELIVERED)
curl -X POST http://localhost:8083/api/orders/1/advance
```

---

## How Services Talk to Each Other

The Order Service uses Spring WebFlux WebClient for HTTP calls:

| What happens          | Who calls      | Who gets called | Endpoint                      |
|------------------------|--------------|-----------------|-------------------------------|
| Check user on order    | Order Service | User Service    | `GET /api/users/{id}/active`  |
| Get product details    | Order Service | Product Service | `GET /api/products/{id}`      |
| Reserve stock          | Order Service | Product Service | `POST /api/products/{id}/reserve` |
| Release stock (cancel) | Order Service | Product Service | `POST /api/products/{id}/release` |

---

## Health Checks

All services have Spring Actuator health endpoints:

```bash
curl http://localhost:8081/actuator/health   # User Service
curl http://localhost:8082/actuator/health   # Product Service
curl http://localhost:8083/actuator/health   # Order Service
```

---

## Some Notes on the Setup

- **No service registry** like Eureka or Consul: Just hardcoded URLs in application.properties
- **No API Gateway**: Direct calls between services to keep it simple
- **In-memory H2 databases**: Each service has its own DB (resets when you restart)
- **Stock handling**: Order Service works with Product Service to reserve stock atomically
- **Error handling**: Order Service has a global exception handler for clean error responses.
