# FitFuel Frontend

The frontend is a responsive static web application in [index.html](index.html).

Features currently wired to the backend:

- Landing page (menu + meal plans) is shown by default — no login wall
- Login / Register buttons in the top-right corner open a modal
- Once signed in, the top-right corner shows the user's name, age, weight, email and a Logout button
- JWT persistence in local storage
- Real "Order Now" menu fetched from the backend, with one-click ordering (adds to cart and checks out)
- Ordering while signed out prompts an alert and opens the login/register modal
- Existing FitFuel meal-plan page and theme

The backend is expected at `http://localhost:8080`.

From the repository root, serve the frontend with:

```bash
python3 -m http.server 8090
```

Open `http://localhost:8090/frontend/`.
