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

The frontend entry point is [frontend/index.html](frontend/index.html). The landing page and menu are visible to everyone by default; Login/Register buttons sit in the top-right corner and open a modal. Once signed in, that corner shows the user's details and a Logout button, and the "Order Now" menu becomes orderable (guests get an alert prompting them to login/register).

Serve it locally from the repository root:

```bash
python3 -m http.server 8090
```

Then open:

```text
http://localhost:8090/frontend/
```

The frontend expects the backend at `http://localhost:8080`.
