# FitFuel

FitFuel is organized as two sibling applications under one parent repository.

```text
FitFuel/
├── backend/   Spring Boot REST API
└── frontend/  Static HTML/CSS/JavaScript web application
```

## Backend

Run without Docker using the local in-memory database:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Or run with PostgreSQL:

```bash
cd backend
docker compose up -d
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

Full backend documentation: [backend/docs/backend-code-documentation.md](backend/docs/backend-code-documentation.md)

## Frontend

The frontend entry point is [frontend/index.html](frontend/index.html). It displays login/signup first and reveals the main FitFuel page after successful authentication.

Serve it locally from the repository root:

```bash
python3 -m http.server 8090
```

Then open:

```text
http://localhost:8090/frontend/
```

The frontend expects the backend at `http://localhost:8080`.
