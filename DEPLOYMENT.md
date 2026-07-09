# Deploying FitFuel (all free tier)

Three pieces, each on its own free service:

| Piece | Platform | Why |
|-------|----------|-----|
| **Database** | [Neon](https://neon.com) — serverless Postgres | Genuinely always-free, no 7-day pause, scales to zero |
| **Backend** | [Render](https://render.com) — Docker web service (free plan) | Easiest for Spring Boot; blueprint included (`render.yaml`) |
| **Frontend** | [Cloudflare Pages](https://pages.cloudflare.com) or [Netlify](https://netlify.com) | Static, no cold starts |

> **Free-tier trade-off:** Render's free service sleeps after ~15 min idle and takes ~30s to wake on the next request. Neon also scales to zero and wakes in ~1s. Fine for a demo/portfolio; upgrade the backend to a paid instance or Google Cloud Run if you need it always-warm.

---

## 1. Database — Neon

1. Create a project at [neon.com](https://neon.com). Choose a region near your users.
2. Open **Connection Details** and note the host, database, user, and password.
3. You'll plug these into the backend as:
   - `DATABASE_URL` = `jdbc:postgresql://<host>/<db>?sslmode=require`  *(note the `jdbc:` prefix and `sslmode=require` — Neon requires SSL)*
   - `DATABASE_USERNAME` = the user
   - `DATABASE_PASSWORD` = the password

The schema is created automatically on first boot (`ddl-auto: update`).

## 2. Backend — Render

1. Push this repo to GitHub.
2. In Render: **New + → Blueprint**, connect the repo. Render reads [`render.yaml`](render.yaml) and creates the `fitfuel-backend` Docker service.
3. Open the service's **Environment** tab and set every variable (all are `sync: false`, i.e. not stored in git):

   | Variable | Value |
   |----------|-------|
   | `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from Neon (step 1) |
   | `JWT_SECRET` | a long random string — `openssl rand -base64 48` |
   | `ADMIN_EMAIL` / `ADMIN_PASSWORD` | your real first-admin login (prod **won't boot** without `ADMIN_PASSWORD`) |
   | `CORS_ALLOWED_ORIGINS` | your frontend URL, e.g. `https://fitfuel.pages.dev` |
   | `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay **Live** keys |
   | `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | Twilio |
   | `TWILIO_WHATSAPP_FROM` | your approved WhatsApp Business number |
   | `TWILIO_WHATSAPP_CONTENT_SID` | your Meta-approved template SID (see WhatsApp note below) |

   `SPRING_PROFILES_ACTIVE=prod` is already set by the blueprint.
4. Deploy. Watch the logs for `Started FitFuelApplication`. Health check: `https://<your-service>.onrender.com/actuator/health` → `{"status":"UP"}`.

## 3. Frontend — Cloudflare Pages / Netlify

1. Edit [`frontend/index.html`](frontend/index.html): set the API-base meta tag to your Render URL:
   ```html
   <meta name="fitfuel-api-base" content="https://fitfuel-backend.onrender.com">
   ```
2. Deploy the `frontend/` directory as the site root (drag-and-drop, or connect the repo and set the output/publish directory to `frontend`). No build command needed — it's a single static file.
3. Copy the resulting URL (e.g. `https://fitfuel.pages.dev`) back into the backend's `CORS_ALLOWED_ORIGINS` and redeploy the backend.

---

## Production checklist (what must be set up to run smoothly)

- [ ] **Neon Postgres** created; `DATABASE_URL` uses `jdbc:...?sslmode=require`.
- [ ] **`JWT_SECRET`** set to a strong random value (prod refuses to boot without it).
- [ ] **`ADMIN_PASSWORD`** set (prod refuses to boot without it — prevents seeding the well-known dev password). Log in once and change it via the profile flow if you like.
- [ ] **`CORS_ALLOWED_ORIGINS`** set to the exact frontend origin (no trailing slash). Not `*` in production.
- [ ] **Frontend meta tag** points at the backend URL; **backend CORS** points back at the frontend URL.
- [ ] **Razorpay** switched to **Live** keys (and you've completed Razorpay KYC to accept real payments).
- [ ] **WhatsApp**: production uses a Meta-approved **template**, not sandbox free-text — see below.
- [ ] Health check green at `/actuator/health`.

### Razorpay webhook (payment reliability)

Belt-and-suspenders so a paid order is never stuck if the customer's browser drops before the in-page callback runs:
1. Razorpay Dashboard → **Settings → Webhooks → Add New Webhook**.
2. URL: `https://<your-backend>.onrender.com/payments/webhook`
3. Subscribe to events **`payment.captured`** and **`order.paid`**.
4. Set a **secret**, and put the same value in `RAZORPAY_WEBHOOK_SECRET`.

The endpoint verifies Razorpay's signature, then confirms the order server-side. It's idempotent, so it's harmless when the normal in-page `/verify` already confirmed the order.

### WhatsApp in production (important)

The sandbox sends free-text and requires each recipient to join with a code. Production uses an approved template and can message anyone, anytime:
1. Get a verified **WhatsApp Business Account** (via Twilio) with a real sender number → `TWILIO_WHATSAPP_FROM`.
2. Register a **utility template** in Twilio's Content Template Builder and paste **exactly this body** (the app fills `{{1}}`–`{{4}}`):
   ```
   Hi {{1}}! 🍽️

   Your FitFuel order #{{2}} is now *{{3}}*.

   📋 {{4}}

   Eat clean, stay strong! 💪
   — Team FitFuel
   ```
   The variables the app sends are: `{{1}}` = customer first name, `{{2}}` = order number, `{{3}}` = status (e.g. "Out for Delivery"), `{{4}}` = dishes (e.g. "2× Paneer Protein Bowl"). The newlines above live in the **template body** (allowed); the variables themselves are single-line (required by Meta). One template covers every status.
3. Copy the template's **Content SID** (`HX...`) → `TWILIO_WHATSAPP_CONTENT_SID`.
4. With that set, the app automatically switches from free-text to template mode (`TwilioWhatsAppSender`) — no code change, no 24-hour-window or join-code limits.

---

## Statelessness (how this scales / restarts cleanly)

The backend is stateless — nothing that must survive a restart lives in memory:

- **Auth** is JWT-only. Sessions are disabled (`SessionCreationPolicy.STATELESS`), so there's no `JSESSIONID` and no server-side session store.
- **All data** (users, orders, cart, payments, feedback) lives in Postgres, not the app.
- **The one in-memory piece** is the live SSE connection registry in `NotificationService` (open browser notification streams). This is deliberately ephemeral: if the instance sleeps/restarts, browsers reconnect automatically via `EventSource`, and the registry repopulates. No data is lost.

This means you can safely run a single free instance that sleeps and wakes. **If you ever run more than one backend instance**, SSE push would only reach clients connected to the same instance — at that point add a Redis pub/sub fan-out (not needed on the free single-instance setup).

---

## Local development (unchanged)

`./run-local.sh` still runs everything locally with the in-memory H2 database and `.env` secrets — see [`.env.example`](.env.example). Production config lives in [`.env.prod.example`](.env.prod.example) / `run-prod.sh` for testing the prod profile locally.
