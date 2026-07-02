# FitFuel Backend

Spring Boot modular monolith containing user, menu, order, payment, subscription, and feedback modules.

## Run

Without Docker:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

With PostgreSQL:

```bash
docker compose up -d
mvn spring-boot:run
```

## Test

```bash
mvn test
```

See [docs/backend-code-documentation.md](docs/backend-code-documentation.md) for full documentation.
