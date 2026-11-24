# ShopNest - E-Commerce Backend

ShopNest is the backend for an online store, built with Spring Boot as my capstone project for the MS in Computer Science (Scaler Neovarsity - Woolf).

It covers the main flow of a shopping site: users sign up, browse and search products, add items to a cart, check out, and pay through Razorpay or Stripe. Order updates are published to Kafka and a consumer sends notifications.

## Tech stack

- Java 17, Spring Boot 3.3
- Spring Data JPA / Hibernate with MySQL
- Spring Security with JWT
- Redis for caching
- Kafka for order events
- Razorpay and Stripe for payments
- Docker Compose for local setup, AWS (EC2, RDS, ElastiCache) for deployment

## Features

- Signup and login with JWT, roles `CUSTOMER` and `ADMIN`
- Product and category management (admin only for writes)
- Pagination, sorting and search on products
- Cart: add, update quantity, remove, clear
- Checkout that reserves stock with an atomic update so two users can't buy the last item
- Payment links through Razorpay or Stripe, confirmed by verified webhooks
- Duplicate webhooks are ignored (idempotent)
- Order events sent to Kafka after the DB transaction commits
- Product reads cached in Redis with eviction on updates and stock changes
- Global error handling with consistent error responses

## Project structure

```
src/main/java/com/akhilesh/shopnest
├── config          security, redis cache, kafka topic, admin user setup
├── controllers     REST endpoints
├── dtos            request and response objects
├── events          kafka producer and notification consumer
├── exceptions      custom exceptions and global handler
├── models          JPA entities
├── repositories    Spring Data repositories
├── security        JWT service and filter
└── services        business logic
    └── payment     Razorpay and Stripe gateways
```

## Running locally

You need Java 17+, Maven and Docker.

```bash
git clone https://github.com/akhileshvs/shopnest.git
cd shopnest
docker compose up -d
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. An admin account is created on first start (`admin@shopnest.com` / `admin123`, change these with `ADMIN_EMAIL` and `ADMIN_PASSWORD`).

Configuration is read from environment variables, see `.env.example`. For payments, put your Razorpay or Stripe test keys there.

Run tests:

```bash
mvn test
```

## API

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/signup` | Public | Register |
| POST | `/auth/login` | Public | Login, returns JWT |
| GET | `/products` | Public | List products (`page`, `size`, `sortBy`, `direction`) |
| GET | `/products/{id}` | Public | Product details (cached) |
| GET | `/products/search?query=` | Public | Search by name |
| POST | `/products` | Admin | Create product |
| PUT | `/products/{id}` | Admin | Update product |
| DELETE | `/products/{id}` | Admin | Delete product |
| GET | `/categories` | Public | List categories |
| GET | `/categories/{id}/products` | Public | Products in a category |
| POST/PUT/DELETE | `/categories/**` | Admin | Manage categories |
| GET | `/cart` | User | View cart |
| POST | `/cart/items` | User | Add item |
| PUT | `/cart/items/{productId}?quantity=` | User | Change quantity |
| DELETE | `/cart/items/{productId}` | User | Remove item |
| POST | `/orders/checkout` | User | Place order, returns payment link |
| GET | `/orders` | User | Order history |
| GET | `/orders/{id}` | User | Order details |
| POST | `/orders/{id}/cancel` | User | Cancel unpaid order |
| PATCH | `/orders/{id}/status?status=` | Admin | Update order status |
| POST | `/payments/webhook/razorpay` | Razorpay | Payment webhook |
| POST | `/payments/webhook/stripe` | Stripe | Payment webhook |

Sample requests are in `docs/api-requests.http`.

## Checkout flow

1. User calls `/orders/checkout`.
2. For each cart item, stock is reduced with `UPDATE ... WHERE stock >= qty`. If any item fails, the whole transaction rolls back.
3. The order is saved as `PENDING_PAYMENT` and a payment link is created with the chosen gateway.
4. The gateway calls the webhook. The signature is verified, the payment is marked `SUCCESS`, and the order becomes `CONFIRMED`. On expiry the order is cancelled and stock is released.
5. Each status change is published to the `order-events` topic after commit, and the notification consumer picks it up.

## Caching

`GET /products/{id}` and the paged product list are cached in Redis. Single products have a 10 minute TTL and pages a 2 minute TTL. Product updates and stock changes evict the affected keys. `scripts/benchmark.sh` measures average response time with and without the cache.

## Deployment

- App on EC2 (or Elastic Beanstalk) behind a load balancer
- MySQL on RDS in a private subnet
- Redis on ElastiCache
- Kafka on Amazon MSK
- Security groups allow DB and cache access only from the app instances

Build the jar with `mvn clean package` and run it with `java -jar target/shopnest-1.0.0.jar`, or build the Docker image with the included `Dockerfile`.
