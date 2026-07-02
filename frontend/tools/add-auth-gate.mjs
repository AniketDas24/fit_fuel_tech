import { readFile, writeFile } from 'node:fs/promises';

const filePath = process.argv[2];
if (!filePath) {
  throw new Error('Usage: node tools/add-auth-gate.mjs <html-file>');
}

let html = await readFile(filePath, 'utf8');

const logoMatch = html.match(/<img class="logo-img" src="([^"]+)"/);
const logoSrc = logoMatch?.[1] || '';

const css = String.raw`

/* -- AUTH GATE -------------------------------------------------------------- */
.hidden { display: none !important; }
.auth-screen {
  min-height: 100vh;
  background:
    repeating-linear-gradient(45deg, transparent, transparent 20px, rgba(255,107,26,0.025) 20px, rgba(255,107,26,0.025) 21px),
    linear-gradient(135deg, #0a0a0a 0%, #1a0f00 50%, #0a0a0a 100%);
  display: grid;
  place-items: center;
  padding: clamp(1rem, 4vw, 2rem);
}
.auth-card {
  width: min(100%, 440px);
  background: rgba(20,20,20,0.96);
  border: 1px solid #2a2a2a;
  border-radius: 14px;
  box-shadow: 0 0 40px rgba(255,107,26,0.12);
  overflow: hidden;
}
.auth-brand {
  text-align: center;
  padding: 1.4rem 1.2rem 1rem;
  border-bottom: 1px solid #242424;
}
.auth-logo {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  border: 2px solid var(--orange);
  box-shadow: 0 0 28px var(--orange-glow);
  object-fit: cover;
  margin-bottom: 0.7rem;
}
.auth-title {
  font-family: 'Bebas Neue', sans-serif;
  font-size: clamp(42px, 12vw, 58px);
  letter-spacing: 4px;
  line-height: 0.95;
}
.auth-title .fit { color: var(--text); }
.auth-title .fuel { color: var(--orange); text-shadow: 0 0 36px var(--orange-glow); }
.auth-kicker {
  font-family: 'Barlow Condensed', sans-serif;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 4px;
  font-size: 12px;
  margin-top: 8px;
}
.auth-body {
  padding: 1rem;
}
.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}
.auth-tab {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  border: 1px solid #333;
  background: #0d0d0d;
  color: #888;
  border-radius: 8px;
  padding: 10px;
  cursor: pointer;
}
.auth-tab.active {
  background: var(--orange);
  border-color: var(--orange);
  color: #0a0a0a;
}
.auth-form {
  display: grid;
  gap: 9px;
}
.auth-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}
.auth-input {
  width: 100%;
  min-height: 42px;
  background: #0b0b0b;
  border: 1px solid #303030;
  border-radius: 8px;
  color: var(--text);
  font-family: 'Barlow', sans-serif;
  font-size: 14px;
  padding: 10px 11px;
  outline: none;
}
.auth-input:focus {
  border-color: var(--orange);
  box-shadow: 0 0 0 2px var(--orange-dim);
}
.auth-btn {
  min-height: 44px;
  border: 1px solid var(--orange);
  border-radius: 8px;
  background: var(--orange);
  color: #0a0a0a;
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 1.5px;
  text-transform: uppercase;
  cursor: pointer;
}
.auth-btn.secondary {
  background: transparent;
  color: var(--orange);
}
.auth-status {
  min-height: 18px;
  margin-top: 10px;
  color: #888;
  font-size: 12px;
  line-height: 1.4;
}
.auth-status.bad { color: #ff7a7a; }
.auth-status.good { color: #7ed957; }
.auth-note {
  color: #666;
  font-size: 12px;
  line-height: 1.45;
  margin-top: 10px;
}
.session-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: #0f0f0f;
  border-bottom: 1px solid #242424;
  padding: 9px clamp(0.75rem, 4vw, 1.5rem);
}
.session-user {
  color: #aaa;
  font-size: 12px;
}
.session-user strong {
  color: var(--orange);
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 15px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
.logout-btn {
  border: 1px solid #333;
  background: #141414;
  color: #aaa;
  border-radius: 7px;
  padding: 7px 11px;
  font-family: 'Barlow Condensed', sans-serif;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  cursor: pointer;
}
.logout-btn:hover {
  border-color: var(--orange);
  color: var(--orange);
}
@media (max-width: 520px) {
  .auth-row { grid-template-columns: 1fr; }
  .session-bar { align-items: flex-start; flex-direction: column; }
}
`;

const authHtml = String.raw`
<section class="auth-screen" id="auth-screen">
  <div class="auth-card">
    <div class="auth-brand">
      ${logoSrc ? `<img class="auth-logo" src="${logoSrc}" alt="FitFuel Logo">` : ''}
      <div class="auth-title"><span class="fit">FIT</span><span class="fuel"> FUEL</span></div>
      <div class="auth-kicker">Healthy Meals. Real Results.</div>
    </div>
    <div class="auth-body">
      <div class="auth-tabs">
        <button class="auth-tab active" type="button" data-auth-mode="login">Login</button>
        <button class="auth-tab" type="button" data-auth-mode="signup">Sign Up</button>
      </div>

      <form class="auth-form" id="login-form">
        <input class="auth-input" name="email" type="email" placeholder="Email" autocomplete="email" required>
        <input class="auth-input" name="password" type="password" placeholder="Password" autocomplete="current-password" required>
        <button class="auth-btn" type="submit">Login & View Menu</button>
      </form>

      <form class="auth-form hidden" id="signup-form">
        <input class="auth-input" name="name" placeholder="Full name" autocomplete="name" required>
        <div class="auth-row">
          <input class="auth-input" name="email" type="email" placeholder="Email" autocomplete="email" required>
          <input class="auth-input" name="phone" placeholder="Phone" autocomplete="tel" required>
        </div>
        <div class="auth-row">
          <input class="auth-input" name="password" type="password" placeholder="Password" autocomplete="new-password" minlength="8" required>
          <input class="auth-input" name="age" type="number" placeholder="Age">
        </div>
        <input class="auth-input" name="weight" type="number" step="0.1" placeholder="Weight in kg">
        <button class="auth-btn" type="submit">Create Account & View Menu</button>
      </form>

      <div class="auth-status" id="auth-status"></div>
      <div class="auth-note">Login or create an account first. Your plan goal stays flexible and can be chosen later during subscription.</div>
    </div>
  </div>
</section>
`;

const sessionBar = String.raw`
  <div class="session-bar">
    <div class="session-user" id="session-user">Signed in</div>
    <button class="logout-btn" type="button" id="logout-btn">Logout</button>
  </div>
`;

const script = String.raw`

const API_BASE = "http://localhost:8080";
const AUTH_TOKEN_KEY = "fitfuelToken";
const AUTH_USER_KEY = "fitfuelUser";

function authEl(id) {
  return document.getElementById(id);
}

function setAuthStatus(message, type) {
  const status = authEl("auth-status");
  if (!status) return;
  status.textContent = message || "";
  status.classList.toggle("bad", type === "bad");
  status.classList.toggle("good", type === "good");
}

function formToPayload(form) {
  const data = new FormData(form);
  const payload = {};
  for (const [key, value] of data.entries()) {
    if (value === "") continue;
    payload[key] = ["age"].includes(key) ? Number(value)
      : ["weight"].includes(key) ? Number(value)
      : value;
  }
  return payload;
}

async function authRequest(path, payload) {
  const response = await fetch(API_BASE + path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};
  if (!response.ok) {
    throw new Error(data.message || "Request failed. Please try again.");
  }
  return data;
}

function showMainApp(user) {
  authEl("auth-screen")?.classList.add("hidden");
  authEl("main-app")?.classList.remove("hidden");
  const session = authEl("session-user");
  if (session && user) {
    session.innerHTML = "Signed in as <strong>" + String(user.name || user.email || "FitFuel User") + "</strong>";
  }
  window.scrollTo({ top: 0, behavior: "instant" });
}

function showAuthScreen() {
  authEl("main-app")?.classList.add("hidden");
  authEl("auth-screen")?.classList.remove("hidden");
}

function saveSession(authResponse) {
  localStorage.setItem(AUTH_TOKEN_KEY, authResponse.token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(authResponse.user));
  showMainApp(authResponse.user);
}

function clearSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  setAuthStatus("");
  showAuthScreen();
}

function wireAuthGate() {
  document.querySelectorAll("[data-auth-mode]").forEach(button => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-auth-mode]").forEach(tab => tab.classList.remove("active"));
      button.classList.add("active");
      const mode = button.dataset.authMode;
      authEl("login-form")?.classList.toggle("hidden", mode !== "login");
      authEl("signup-form")?.classList.toggle("hidden", mode !== "signup");
      setAuthStatus("");
    });
  });

  authEl("login-form")?.addEventListener("submit", async event => {
    event.preventDefault();
    setAuthStatus("Logging you in...");
    try {
      const data = await authRequest("/auth/login", formToPayload(event.currentTarget));
      saveSession(data);
    } catch (error) {
      setAuthStatus(error.message, "bad");
    }
  });

  authEl("signup-form")?.addEventListener("submit", async event => {
    event.preventDefault();
    setAuthStatus("Creating your account...");
    try {
      const data = await authRequest("/auth/signup", formToPayload(event.currentTarget));
      saveSession(data);
    } catch (error) {
      setAuthStatus(error.message, "bad");
    }
  });

  authEl("logout-btn")?.addEventListener("click", clearSession);

  const existingToken = localStorage.getItem(AUTH_TOKEN_KEY);
  const existingUser = localStorage.getItem(AUTH_USER_KEY);
  if (existingToken && existingUser) {
    try {
      showMainApp(JSON.parse(existingUser));
    } catch {
      clearSession();
    }
  } else {
    showAuthScreen();
  }
}

wireAuthGate();
`;

if (!html.includes('/* -- AUTH GATE')) {
  html = html.replace('</style>', `${css}\n</style>`);
}

if (!html.includes('id="auth-screen"')) {
  html = html.replace('<body>\n', `<body>\n\n${authHtml}\n`);
}

if (!html.includes('id="main-app"')) {
  html = html.replace('<div class="ff-wrap">', '<div class="ff-wrap hidden" id="main-app">');
}

if (!html.includes('id="session-user"')) {
  html = html.replace('<div class="ff-wrap hidden" id="main-app">', `<div class="ff-wrap hidden" id="main-app">\n${sessionBar}`);
}

if (!html.includes('const API_BASE = "http://localhost:8080"')) {
  html = html.replace('renderTier("90");\n</script>', `renderTier("90");\n${script}\n</script>`);
}

await writeFile(filePath, html, 'utf8');
