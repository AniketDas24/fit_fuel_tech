# FitFuel Backend Code Documentation

This document describes the current Spring Boot backend implementation for FitFuel. The application is built as a modular monolith: one deployable Spring Boot service, with separate Java packages for business capabilities that can later be split into independent services.

## 1. Technical Overview

| Area | Current Choice |
| --- | --- |
| Language | Java 21 target |
| Framework | Spring Boot 3.3.5 |
| Web | Spring MVC |
| Security | Spring Security with custom JWT filter |
| Persistence | Spring Data JPA / Hibernate |
| Production database | PostgreSQL |
| Test database | H2 in PostgreSQL compatibility mode |
| Build tool | Maven |

Application entry point:

```text
src/main/java/com/fitfuel/FitFuelApplication.java
```

Main config:

```text
src/main/resources/application.yml
```

Test config:

```text
src/test/resources/application-test.yml
```

## 2. Project Structure

```text
com.fitfuel
├── common
│   ├── ApiExceptionHandler.java
│   └── NotFoundException.java
├── config
│   ├── JwtService.java
│   └── SecurityConfig.java
├── feedback
├── menu
├── order
├── payment
├── subscription
└── user
```

Each feature package contains its entity, repository, DTO records, and controller or service classes. This keeps domain boundaries clear while still running as one application.

## 3. Runtime Configuration

Configuration is loaded from `application.yml`.

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/fitfuel}
    username: ${DATABASE_USERNAME:fitfuel}
    password: ${DATABASE_PASSWORD:fitfuel}
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: ${PORT:8080}

fitfuel:
  jwt:
    secret: ${JWT_SECRET:change-this-development-secret-at-least-32-chars}
    ttl-minutes: ${JWT_TTL_MINUTES:1440}
```

Important environment variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `DATABASE_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://localhost:5432/fitfuel` |
| `DATABASE_USERNAME` | DB username | `fitfuel` |
| `DATABASE_PASSWORD` | DB password | `fitfuel` |
| `PORT` | HTTP server port | `8080` |
| `JWT_SECRET` | HMAC signing secret | development secret |
| `JWT_TTL_MINUTES` | JWT lifetime | `1440` |

For local PostgreSQL:

```bash
docker compose up -d
mvn spring-boot:run
```

For local testing without PostgreSQL:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

## 4. Security Architecture

Security is implemented in:

```text
src/main/java/com/fitfuel/config/SecurityConfig.java
src/main/java/com/fitfuel/config/JwtService.java
```

### Public Endpoints

These endpoints do not require authentication:

```text
/auth/**
/menu/**
```

All other endpoints require:

```text
Authorization: Bearer <jwt>
```

### JWT Flow

1. User signs up or logs in.
2. Backend returns an HMAC-SHA256 JWT.
3. JWT subject is the user email.
4. `JwtAuthenticationFilter` reads the `Authorization` header.
5. The filter validates the token and loads the user by email.
6. Spring Security receives an authenticated principal using the user email as `Authentication#getName()`.

### Roles

Roles are defined in:

```text
src/main/java/com/fitfuel/user/Role.java
```

Current roles:

```text
CUSTOMER
ADMIN
```

Admin-only operations:

```text
POST /menu
PUT /menu/{id}
```

These use `@PreAuthorize("hasRole('ADMIN')")`.

### CORS

The backend currently allows cross-origin calls with:

```text
Allowed origins: *
Allowed methods: GET, POST, PUT, DELETE, OPTIONS
Allowed headers: *
```

This was added so the static HTML frontend can call `http://localhost:8080`. For production, restrict origins to the deployed frontend domain.

## 5. Common Layer

Package:

```text
com.fitfuel.common
```

### `NotFoundException`

Simple runtime exception used when a requested entity does not exist.

### `ApiExceptionHandler`

Centralized REST exception handling.

| Exception | HTTP Status |
| --- | --- |
| `NotFoundException` | `404 Not Found` |
| `IllegalArgumentException` | `400 Bad Request` |
| `ConstraintViolationException` | `400 Bad Request` |
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `BadCredentialsException` | `401 Unauthorized` |

Response shape:

```json
{
  "message": "Error message"
}
```

## 6. User Module

Package:

```text
com.fitfuel.user
```

Responsibilities:

- Signup
- Login
- Password hashing
- Profile read/update
- Role storage

### Entity: `AppUser`

Database table:

```text
users
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `name` | `String` | Required |
| `email` | `String` | Required, unique |
| `phone` | `String` | Required, unique |
| `passwordHash` | `String` | BCrypt hash |
| `age` | `Integer` | Optional |
| `weight` | `Double` | Optional |
| `role` | `Role` | Defaults to `CUSTOMER` |
| `createdAt` | `Instant` | Set on creation |

Important design choice:

Goal selection is not stored on the user. Goals belong to subscriptions so a user can sign up once and later choose Weight Loss, Weight Gain, or Healthy Diet.

### Repository: `UserRepository`

Methods:

```java
Optional<AppUser> findByEmail(String email);
boolean existsByEmail(String email);
boolean existsByPhone(String phone);
```

### APIs

#### `POST /auth/signup`

Creates a user and returns a JWT.

Request:

```json
{
  "name": "Aniket",
  "email": "aniket@example.com",
  "phone": "9999999999",
  "password": "password123",
  "age": 24,
  "weight": 72.5
}
```

Response:

```json
{
  "token": "<jwt>",
  "user": {
    "id": 1,
    "name": "Aniket",
    "email": "aniket@example.com",
    "phone": "9999999999",
    "age": 24,
    "weight": 72.5,
    "role": "CUSTOMER"
  }
}
```

Validation:

- `name` required
- `email` required and email-shaped
- `phone` required
- `password` minimum length 8
- duplicate email rejected
- duplicate phone rejected

#### `POST /auth/login`

Authenticates an existing user.

Request:

```json
{
  "email": "aniket@example.com",
  "password": "password123"
}
```

Response shape is the same as signup.

#### `GET /users/me`

Returns the authenticated user profile.

Headers:

```text
Authorization: Bearer <jwt>
```

#### `PUT /users/me`

Updates the authenticated user profile.

Request:

```json
{
  "name": "Aniket Das",
  "phone": "9999999999",
  "age": 25,
  "weight": 73
}
```

All fields are optional. Only non-null fields are updated.

## 7. Menu Module

Package:

```text
com.fitfuel.menu
```

Responsibilities:

- Food catalog
- Mess menu vs regular menu separation
- Pricing and nutrition metadata
- Seed data for local/demo usage

### Entity: `FoodItem`

Database table:

```text
food_items
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `name` | `String` | Required |
| `description` | `String` | Optional |
| `category` | `String` | Required |
| `menuType` | `MenuType` | `MESS_MENU` or `REGULAR_MENU` |
| `protein` | `Integer` | Optional grams |
| `calories` | `Integer` | Optional |
| `price` | `BigDecimal` | Required |
| `active` | `boolean` | Defaults true |

### Enum: `MenuType`

```text
MESS_MENU
REGULAR_MENU
```

### Repository: `FoodItemRepository`

Methods:

```java
List<FoodItem> findByMenuTypeAndActiveTrue(MenuType menuType);
List<FoodItem> findByActiveTrue();
```

### Seed Data

`MenuDataInitializer` inserts sample food items if the database is empty.

Seeded regular menu examples:

- Paneer Protein Bowl
- Chicken Lean Box
- Egg Power Sandwich
- Quinoa Veg Plate

Seeded mess menu examples:

- Lean & Clean Mess Menu
- Gainz Mess Menu

### APIs

#### `GET /menu`

Lists active food items.

Optional query:

```text
type=REGULAR_MENU
type=MESS_MENU
```

Example:

```text
GET /menu?type=REGULAR_MENU
```

#### `GET /menu/{id}`

Returns one food item.

#### `POST /menu`

Admin only. Creates a food item.

Request:

```json
{
  "name": "Paneer Protein Bowl",
  "description": "Paneer, rice, vegetables, curd, and salad",
  "category": "BOWL",
  "menuType": "REGULAR_MENU",
  "protein": 42,
  "calories": 620,
  "price": 199,
  "active": true
}
```

#### `PUT /menu/{id}`

Admin only. Replaces editable food item fields.

## 8. Order Module

Package:

```text
com.fitfuel.order
```

Responsibilities:

- Cart management
- Non-mess one-time order checkout
- Order history
- Order item price snapshotting

This module is specifically for non-mess users ordering from the regular menu.

### Entities

#### `Cart`

Database table:

```text
carts
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `user` | `AppUser` | One cart per user |
| `items` | `List<CartItem>` | Cascade and orphan removal |

#### `CartItem`

Database table:

```text
cart_items
```

Unique constraint:

```text
cart_id + food_item_id
```

Fields:

| Field | Type |
| --- | --- |
| `id` | `Long` |
| `cart` | `Cart` |
| `foodItem` | `FoodItem` |
| `quantity` | `int` |

#### `CustomerOrder`

Database table:

```text
orders
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `user` | `AppUser` | Owner |
| `status` | `OrderStatus` | Defaults `CREATED` |
| `totalAmount` | `BigDecimal` | Calculated at checkout |
| `paymentStatus` | `PaymentStatus` | Defaults `PENDING` |
| `items` | `List<OrderItem>` | Order item snapshot |
| `createdAt` | `Instant` | Set on creation |

#### `OrderItem`

Database table:

```text
order_items
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `order` | `CustomerOrder` | Parent |
| `foodItem` | `FoodItem` | Catalog item |
| `quantity` | `int` | Quantity ordered |
| `price` | `BigDecimal` | Price snapshot at checkout |

### Enums

`OrderStatus`:

```text
CREATED
CONFIRMED
PREPARING
DELIVERED
CANCELLED
```

`PaymentStatus`:

```text
PENDING
PAID
FAILED
REFUNDED
```

### Service: `OrderService`

Important behavior:

- `addToCart` only accepts active `REGULAR_MENU` items.
- If the user has no cart, a cart is created automatically.
- Adding an existing food item increases quantity.
- Checkout converts cart items into order items and clears the cart.
- Order item price is copied from the food item at checkout time.
- `orders` is transactional so lazy order items can be serialized safely.

### APIs

#### `GET /cart`

Returns current authenticated user's cart. Creates an empty cart if none exists.

#### `POST /cart/items`

Adds an item to the cart.

Request:

```json
{
  "foodItemId": 1,
  "quantity": 2
}
```

Validation:

- `foodItemId` required
- `quantity` positive
- item must exist
- item must be active
- item must be `REGULAR_MENU`

#### `POST /orders/checkout`

Creates an order from the current cart and clears the cart.

Request body is currently optional. Coupon support is represented in DTOs but not applied yet.

Response:

```json
{
  "id": 1,
  "status": "CREATED",
  "totalAmount": 398.00,
  "paymentStatus": "PENDING",
  "createdAt": "2026-06-09T16:38:08Z",
  "items": [
    {
      "foodItemId": 1,
      "name": "Paneer Protein Bowl",
      "quantity": 2,
      "price": 199.00
    }
  ]
}
```

#### `GET /orders`

Returns authenticated user's orders ordered newest first.

## 9. Payment Module

Package:

```text
com.fitfuel.payment
```

Responsibilities:

- Payment records
- Gateway placeholder
- Coupon data model foundation

Current implementation does not integrate Razorpay or Stripe yet. It creates mock payment references.

### Entity: `Payment`

Database table:

```text
payments
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `user` | `AppUser` | Required |
| `order` | `CustomerOrder` | Optional |
| `amount` | `BigDecimal` | Required |
| `status` | `PaymentEntityStatus` | Defaults `CREATED` |
| `provider` | `PaymentProvider` | Defaults `RAZORPAY` |
| `gatewayReference` | `String` | Mock reference |
| `createdAt` | `Instant` | Set on creation |

### Entity: `Coupon`

Database table:

```text
coupons
```

Fields:

| Field | Type |
| --- | --- |
| `id` | `Long` |
| `code` | `String` |
| `discountType` | `DiscountType` |
| `discountValue` | `BigDecimal` |
| `expiry` | `LocalDate` |
| `active` | `boolean` |

Coupon engine is modeled but not applied during checkout yet.

### Enums

`PaymentProvider`:

```text
RAZORPAY
STRIPE
CASH
```

`PaymentEntityStatus`:

```text
CREATED
SUCCESS
FAILED
```

`DiscountType`:

```text
PERCENTAGE
FLAT
```

### APIs

#### `POST /payments`

Creates a payment record.

Request:

```json
{
  "orderId": 1,
  "amount": 398,
  "provider": "RAZORPAY"
}
```

Response:

```json
{
  "id": 1,
  "orderId": 1,
  "amount": 398,
  "status": "CREATED",
  "provider": "RAZORPAY",
  "gatewayReference": "mock_1717930000000",
  "createdAt": "2026-06-09T16:38:08Z"
}
```

#### `GET /payments`

Returns authenticated user's payment records ordered newest first.

## 10. Subscription Module

Package:

```text
com.fitfuel.subscription
```

Responsibilities:

- Mess user subscription creation
- Goal selection
- Meal selection
- Protein tier selection
- Active subscription lookup

### Entity: `Subscription`

Database table:

```text
subscriptions
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `user` | `AppUser` | Owner |
| `planType` | `PlanType` | Goal |
| `proteinTier` | `ProteinTier` | Protein amount |
| `mealType` | `MealType` | Meal coverage |
| `startDate` | `LocalDate` | Required |
| `endDate` | `LocalDate` | Required |
| `status` | `SubscriptionStatus` | Defaults `PENDING_PAYMENT` |

### Enums

`PlanType`:

```text
WEIGHT_LOSS
WEIGHT_GAIN
HEALTHY_DIET
```

`ProteinTier`:

```text
G100
G120
G150
G200
```

`MealType`:

```text
BREAKFAST
LUNCH
DINNER
FULL_DAY
```

`SubscriptionStatus`:

```text
PENDING_PAYMENT
ACTIVE
PAUSED
EXPIRED
CANCELLED
```

### APIs

#### `POST /subscriptions`

Creates a mess subscription in `PENDING_PAYMENT` status.

Request:

```json
{
  "planType": "WEIGHT_LOSS",
  "proteinTier": "G150",
  "mealType": "FULL_DAY",
  "startDate": "2026-07-01",
  "endDate": "2026-08-01"
}
```

Validation:

- `planType` required
- `proteinTier` required
- `mealType` required
- `startDate` required and must be today or future
- `endDate` required
- `endDate` must be after `startDate`

#### `GET /subscriptions`

Lists authenticated user's subscriptions ordered newest first.

#### `GET /subscriptions/active`

Returns the latest active subscription for the user.

If none exists:

```http
404 Not Found
```

## 11. Feedback Module

Package:

```text
com.fitfuel.feedback
```

Responsibilities:

- Rating
- Comment
- Feedback history

### Entity: `Feedback`

Database table:

```text
feedbacks
```

Fields:

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key |
| `user` | `AppUser` | Owner |
| `order` | `CustomerOrder` | Related order |
| `rating` | `int` | 1 to 5 |
| `comment` | `String` | Optional |
| `createdAt` | `Instant` | Set on creation |

### APIs

#### `POST /feedbacks`

Creates feedback for an order.

Request:

```json
{
  "orderId": 1,
  "rating": 5,
  "comment": "Great meal"
}
```

Validation:

- `orderId` required
- `rating` minimum 1
- `rating` maximum 5

#### `GET /feedbacks`

Returns authenticated user's feedback ordered newest first.

## 12. Database Tables

Current tables created by JPA:

```text
users
food_items
carts
cart_items
orders
order_items
payments
coupons
subscriptions
feedbacks
```

High-level relationships:

```text
User 1 -> 1 Cart
Cart 1 -> many CartItems
CartItem many -> 1 FoodItem

User 1 -> many Orders
Order 1 -> many OrderItems
OrderItem many -> 1 FoodItem

User 1 -> many Payments
Payment many -> 0/1 Order

User 1 -> many Subscriptions

User 1 -> many Feedbacks
Feedback many -> 1 Order
```

## 13. End-to-End Business Flows

### Non-Mess User Flow

```text
Signup/Login
  -> GET /menu?type=REGULAR_MENU
  -> POST /cart/items
  -> GET /cart
  -> POST /orders/checkout
  -> POST /payments
  -> POST /feedbacks
```

Current payment behavior:

- Checkout creates an order with `paymentStatus=PENDING`.
- `POST /payments` creates a mock payment record.
- Payment success does not yet update order `paymentStatus` to `PAID`.

### Mess Subscription Flow

```text
Signup/Login
  -> POST /subscriptions
  -> POST /payments
  -> Subscription should become ACTIVE after payment success
```

Current subscription behavior:

- Subscription starts as `PENDING_PAYMENT`.
- Activation after payment success is not implemented yet.

## 14. Testing

Integration test:

```text
src/test/java/com/fitfuel/ApiIntegrationTest.java
```

Test profile:

```text
src/test/resources/application-test.yml
```

The test uses:

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- H2 in PostgreSQL mode
- Full Spring Security filter chain
- Real JPA repositories
- Real controllers and services

Verified APIs:

```text
POST /auth/signup
POST /auth/login
GET /users/me
PUT /users/me
POST /menu
PUT /menu/{id}
GET /menu
GET /menu/{id}
POST /cart/items
GET /cart
POST /orders/checkout
GET /orders
POST /payments
GET /payments
POST /subscriptions
GET /subscriptions
GET /subscriptions/active
POST /feedbacks
GET /feedbacks
```

Run tests:

```bash
mvn test
```

Mockito compatibility file:

```text
src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
```

This uses `mock-maker-subclass` so tests run cleanly on the currently installed Java 24 runtime.

## 15. Frontend Integration Notes

The static HTML frontend at `../frontend/index.html` calls:

```text
http://localhost:8080
```

The backend must be running before using the connected frontend.

For demo without Docker:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

For PostgreSQL:

```bash
docker compose up -d
mvn spring-boot:run
```

The frontend stores JWT in:

```text
localStorage.fitfuelToken
```

## 16. Current Limitations

These are known MVP gaps, not accidental omissions.

### Payment

- Razorpay/Stripe SDK integration is not implemented.
- `gatewayReference` is a mock value.
- Webhook handling is not implemented.
- Payment success does not update order or subscription status.

### Coupon

- Coupon entity and repository exist.
- Coupon application during checkout is not implemented.

### Order

- Cart item removal/update quantity APIs are not implemented.
- Order cancellation is not implemented.
- Admin order management is not implemented.

### Subscription

- Pricing calculation is not implemented.
- Subscription activation after payment is not implemented.
- Pause/cancel/renew flows are not implemented.
- Daily mess meal delivery tracking is not implemented.

### Feedback

- Feedback currently checks that an order exists, but does not enforce that the order belongs to the authenticated user.

### Security

- JWT implementation is intentionally lightweight and custom.
- Production should use a well-tested JWT library and stronger key management.
- CORS should be restricted before deployment.

### Database

- `ddl-auto: update` is convenient for MVP but should be replaced with Flyway or Liquibase migrations before production.

## 17. Recommended Next Backend Tasks

1. Add Flyway migrations for all tables.
2. Implement payment success callback and status transitions.
3. Add cart update and remove endpoints.
4. Add coupon calculation during checkout.
5. Add subscription pricing and payment activation.
6. Add ownership checks for order payment and feedback.
7. Add admin APIs for menu, order, and subscription management.
8. Replace custom JWT code with a standard JWT library.
9. Add OpenAPI/Swagger documentation.
10. Add service-level unit tests around pricing, coupon, and payment status logic.
