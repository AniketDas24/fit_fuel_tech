import { readFile, writeFile } from 'node:fs/promises';

const filePath = process.argv[2];
if (!filePath) {
  throw new Error('Usage: node tools/enhance-frontend.mjs <html-file>');
}

let html = await readFile(filePath, 'utf8');

const backendCss = String.raw`

/* -- BACKEND CONNECTED APP -------------------------------------------------- */
.app-shell {
  padding: 1.5rem;
  background: #0a0a0a;
  border-top: 1px solid var(--border);
}
.app-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(320px, 1.2fr);
  gap: 12px;
  max-width: 1180px;
  margin: 0 auto;
}
.app-panel {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
}
.app-panel.full { grid-column: 1 / -1; }
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
.panel-title {
  font-family: 'Bebas Neue', sans-serif;
  color: var(--orange);
  font-size: 24px;
  letter-spacing: 2px;
}
.panel-sub {
  color: #777;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 2px;
}
.app-form {
  display: grid;
  gap: 8px;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.app-input,
.app-select,
.app-textarea {
  width: 100%;
  background: #0d0d0d;
  color: var(--text);
  border: 1px solid #2a2a2a;
  border-radius: 6px;
  padding: 9px 10px;
  font-family: 'Barlow', sans-serif;
  font-size: 13px;
  outline: none;
}
.app-textarea {
  min-height: 76px;
  resize: vertical;
}
.app-input:focus,
.app-select:focus,
.app-textarea:focus {
  border-color: var(--orange);
  box-shadow: 0 0 0 2px var(--orange-dim);
}
.app-btn {
  font-family: 'Barlow Condensed', sans-serif;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  border: 1px solid var(--orange);
  background: var(--orange);
  color: #0a0a0a;
  border-radius: 6px;
  padding: 9px 12px;
  cursor: pointer;
}
.app-btn.secondary {
  background: transparent;
  color: var(--orange);
}
.app-btn.ghost {
  border-color: #333;
  background: #101010;
  color: #aaa;
}
.app-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.app-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  margin-bottom: 10px;
}
.app-tab {
  border: 1px solid #333;
  background: #101010;
  color: #888;
  border-radius: 6px;
  padding: 8px;
  font-family: 'Barlow Condensed', sans-serif;
  font-weight: 700;
  letter-spacing: 1px;
  cursor: pointer;
}
.app-tab.active {
  background: var(--orange);
  border-color: var(--orange);
  color: #0a0a0a;
}
.status-line {
  min-height: 18px;
  margin-top: 10px;
  color: #888;
  font-size: 12px;
}
.status-line.good { color: #7ed957; }
.status-line.bad { color: #ff7a7a; }
.profile-card {
  border: 1px solid #242424;
  background: #0d0d0d;
  border-radius: 8px;
  padding: 10px;
  color: #aaa;
  font-size: 12px;
  line-height: 1.6;
}
.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}
.food-card {
  border: 1px solid #242424;
  background: #0d0d0d;
  border-radius: 8px;
  padding: 12px;
  display: grid;
  gap: 8px;
}
.food-name {
  font-family: 'Barlow Condensed', sans-serif;
  color: var(--text);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
}
.food-meta {
  color: #777;
  font-size: 12px;
  line-height: 1.45;
}
.food-price {
  color: var(--orange);
  font-family: 'Bebas Neue', sans-serif;
  font-size: 24px;
  letter-spacing: 1px;
}
.compact-list {
  display: grid;
  gap: 8px;
}
.compact-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  border-bottom: 1px solid #222;
  padding-bottom: 8px;
  color: #aaa;
  font-size: 12px;
}
.compact-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.cart-total {
  font-family: 'Bebas Neue', sans-serif;
  color: var(--orange);
  font-size: 28px;
  letter-spacing: 1px;
}
.hidden { display: none !important; }
@media (max-width: 860px) {
  .app-grid { grid-template-columns: 1fr; }
  .field-row { grid-template-columns: 1fr; }
}
`;

const backendHtml = String.raw`

  <div class="app-shell" id="fitfuel-app">
    <div class="app-grid">
      <section class="app-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">Account</div>
            <div class="panel-sub">Sign up once, then order meals or create a mess subscription.</div>
          </div>
          <button class="app-btn ghost hidden" id="logout-btn">Logout</button>
        </div>

        <div class="app-tabs">
          <button class="app-tab active" data-auth-tab="signup">Signup</button>
          <button class="app-tab" data-auth-tab="login">Login</button>
        </div>

        <form class="app-form" id="signup-form">
          <input class="app-input" name="name" placeholder="Name" required />
          <div class="field-row">
            <input class="app-input" name="email" type="email" placeholder="Email" required />
            <input class="app-input" name="phone" placeholder="Phone" required />
          </div>
          <div class="field-row">
            <input class="app-input" name="password" type="password" placeholder="Password" minlength="8" required />
            <input class="app-input" name="age" type="number" placeholder="Age" />
          </div>
          <input class="app-input" name="weight" type="number" step="0.1" placeholder="Weight in kg" />
          <button class="app-btn" type="submit">Create Account</button>
        </form>

        <form class="app-form hidden" id="login-form">
          <input class="app-input" name="email" type="email" placeholder="Email" required />
          <input class="app-input" name="password" type="password" placeholder="Password" required />
          <button class="app-btn" type="submit">Login</button>
        </form>

        <div class="status-line" id="auth-status"></div>
      </section>

      <section class="app-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">Profile</div>
            <div class="panel-sub">Profile stays separate from mess plan goals.</div>
          </div>
          <button class="app-btn secondary" id="refresh-profile-btn">Refresh</button>
        </div>
        <div class="profile-card" id="profile-card">Login to view your profile.</div>
        <form class="app-form" id="profile-form" style="margin-top:10px;">
          <div class="field-row">
            <input class="app-input" name="name" placeholder="Update name" />
            <input class="app-input" name="phone" placeholder="Update phone" />
          </div>
          <div class="field-row">
            <input class="app-input" name="age" type="number" placeholder="Age" />
            <input class="app-input" name="weight" type="number" step="0.1" placeholder="Weight" />
          </div>
          <button class="app-btn secondary" type="submit">Save Profile</button>
        </form>
      </section>

      <section class="app-panel full">
        <div class="panel-head">
          <div>
            <div class="panel-title">Mess Subscription</div>
            <div class="panel-sub">Choose goal, meals, protein tier, and dates only when subscribing.</div>
          </div>
          <button class="app-btn secondary" id="load-subscriptions-btn">My Plans</button>
        </div>
        <form class="app-form" id="subscription-form">
          <div class="field-row">
            <select class="app-select" name="planType" required>
              <option value="WEIGHT_LOSS">Weight Loss</option>
              <option value="WEIGHT_GAIN">Weight Gain</option>
              <option value="HEALTHY_DIET">Healthy Diet</option>
            </select>
            <select class="app-select" name="mealType" required>
              <option value="BREAKFAST">Breakfast</option>
              <option value="LUNCH">Lunch</option>
              <option value="DINNER">Dinner</option>
              <option value="FULL_DAY">Full Day</option>
            </select>
          </div>
          <div class="field-row">
            <select class="app-select" name="proteinTier" required>
              <option value="G100">100g protein</option>
              <option value="G120">120g protein</option>
              <option value="G150">150g protein</option>
              <option value="G200">200g protein</option>
            </select>
            <input class="app-input" name="startDate" type="date" required />
          </div>
          <input class="app-input" name="endDate" type="date" required />
          <button class="app-btn" type="submit">Create Subscription</button>
        </form>
        <div class="status-line" id="subscription-status"></div>
        <div class="compact-list" id="subscription-list" style="margin-top:10px;"></div>
      </section>

      <section class="app-panel full">
        <div class="panel-head">
          <div>
            <div class="panel-title">Regular Menu</div>
            <div class="panel-sub">For one-time orders: browse, add to cart, checkout, then create payment.</div>
          </div>
          <button class="app-btn secondary" id="load-menu-btn">Load Menu</button>
        </div>
        <div class="menu-grid" id="regular-menu"></div>
      </section>

      <section class="app-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">Cart</div>
            <div class="panel-sub">Regular-menu items ready for checkout.</div>
          </div>
          <button class="app-btn secondary" id="refresh-cart-btn">Refresh</button>
        </div>
        <div class="compact-list" id="cart-list"></div>
        <div class="cart-total" id="cart-total">Rs 0</div>
        <button class="app-btn" id="checkout-btn" style="margin-top:10px;width:100%;">Checkout</button>
        <div class="status-line" id="order-status"></div>
      </section>

      <section class="app-panel">
        <div class="panel-head">
          <div>
            <div class="panel-title">Orders & Feedback</div>
            <div class="panel-sub">Pay for latest order and submit rating.</div>
          </div>
          <button class="app-btn secondary" id="load-orders-btn">My Orders</button>
        </div>
        <div class="compact-list" id="orders-list"></div>
        <form class="app-form" id="feedback-form" style="margin-top:10px;">
          <input class="app-input" name="orderId" type="number" placeholder="Order ID" required />
          <select class="app-select" name="rating" required>
            <option value="5">5 - Excellent</option>
            <option value="4">4 - Good</option>
            <option value="3">3 - Okay</option>
            <option value="2">2 - Poor</option>
            <option value="1">1 - Bad</option>
          </select>
          <textarea class="app-textarea" name="comment" placeholder="Comment"></textarea>
          <button class="app-btn secondary" type="submit">Send Feedback</button>
        </form>
        <div class="status-line" id="feedback-status"></div>
      </section>
    </div>
  </div>
`;

const backendScript = String.raw`

const API_BASE = 'http://localhost:8080';
let authToken = localStorage.getItem('fitfuelToken') || '';
let lastOrderId = localStorage.getItem('fitfuelLastOrderId') || '';
let lastOrderAmount = Number(localStorage.getItem('fitfuelLastOrderAmount') || '0');

const money = value => 'Rs ' + Number(value || 0).toFixed(0);
const el = id => document.getElementById(id);
const escapeText = value => String(value ?? '').replace(/[&<>"']/g, ch => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[ch]));

function setStatus(id, message, good = true) {
  const target = el(id);
  if (!target) return;
  target.textContent = message || '';
  target.classList.toggle('good', Boolean(message) && good);
  target.classList.toggle('bad', Boolean(message) && !good);
}

function authHeaders() {
  return authToken ? { Authorization: 'Bearer ' + authToken } : {};
}

async function api(path, options = {}) {
  const response = await fetch(API_BASE + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(data?.message || 'Request failed: ' + response.status);
  }
  return data;
}

function formJson(form) {
  const data = new FormData(form);
  const json = {};
  for (const [key, value] of data.entries()) {
    if (value === '') continue;
    json[key] = ['age', 'rating'].includes(key) ? Number(value)
      : ['weight', 'amount'].includes(key) ? Number(value)
      : value;
  }
  return json;
}

function setAuthenticated(token) {
  authToken = token;
  localStorage.setItem('fitfuelToken', token);
  el('logout-btn')?.classList.remove('hidden');
}

function clearAuth() {
  authToken = '';
  localStorage.removeItem('fitfuelToken');
  el('logout-btn')?.classList.add('hidden');
  el('profile-card').textContent = 'Login to view your profile.';
  el('cart-list').innerHTML = '';
  el('orders-list').innerHTML = '';
}

function renderProfile(user) {
  el('profile-card').innerHTML = [
    '<strong>' + escapeText(user.name) + '</strong>',
    escapeText(user.email),
    'Phone: ' + escapeText(user.phone),
    'Age: ' + escapeText(user.age ?? 'Not set'),
    'Weight: ' + escapeText(user.weight ?? 'Not set') + ' kg',
    'Role: ' + escapeText(user.role)
  ].join('<br>');
}

async function loadProfile() {
  if (!authToken) return;
  const user = await api('/users/me');
  renderProfile(user);
}

async function loadMenu() {
  const items = await api('/menu?type=REGULAR_MENU', { headers: {} });
  el('regular-menu').innerHTML = items.map(item => \`
    <article class="food-card">
      <div class="food-name">\${escapeText(item.name)}</div>
      <div class="food-meta">\${escapeText(item.description || '')}</div>
      <div class="food-meta">\${escapeText(item.category)} · \${item.protein || 0}g protein · \${item.calories || 0} cal</div>
      <div class="food-price">\${money(item.price)}</div>
      <button class="app-btn secondary" data-add-item="\${item.id}">Add To Cart</button>
    </article>
  \`).join('') || '<div class="profile-card">No regular menu items found.</div>';
}

async function addToCart(foodItemId) {
  await api('/cart/items', {
    method: 'POST',
    body: JSON.stringify({ foodItemId: Number(foodItemId), quantity: 1 })
  });
  await loadCart();
}

async function loadCart() {
  if (!authToken) return;
  const cart = await api('/cart');
  el('cart-list').innerHTML = cart.items.map(item => \`
    <div class="compact-row">
      <span>\${escapeText(item.name)} x \${item.quantity}</span>
      <strong>\${money(Number(item.price) * item.quantity)}</strong>
    </div>
  \`).join('') || '<div class="profile-card">Cart is empty.</div>';
  el('cart-total').textContent = money(cart.totalAmount);
}

async function checkout() {
  const order = await api('/orders/checkout', { method: 'POST', body: '{}' });
  lastOrderId = order.id;
  lastOrderAmount = Number(order.totalAmount || 0);
  localStorage.setItem('fitfuelLastOrderId', lastOrderId);
  localStorage.setItem('fitfuelLastOrderAmount', String(lastOrderAmount));
  el('feedback-form').orderId.value = order.id;
  setStatus('order-status', 'Order #' + order.id + ' created. Payment is pending.');
  await loadCart();
  await loadOrders();
}

async function createPayment(orderId, amount) {
  const payment = await api('/payments', {
    method: 'POST',
    body: JSON.stringify({ orderId: Number(orderId), amount: Number(amount), provider: 'RAZORPAY' })
  });
  setStatus('order-status', 'Payment request created: ' + payment.gatewayReference);
  await loadOrders();
}

async function loadOrders() {
  if (!authToken) return;
  const orders = await api('/orders');
  el('orders-list').innerHTML = orders.map(order => \`
    <div class="compact-row">
      <span>#\${order.id} · \${order.status} · \${order.paymentStatus}<br>\${order.items.map(i => escapeText(i.name) + ' x ' + i.quantity).join(', ')}</span>
      <span>
        <strong>\${money(order.totalAmount)}</strong><br>
        <button class="app-btn ghost" data-pay-order="\${order.id}" data-pay-amount="\${order.totalAmount}">Pay</button>
      </span>
    </div>
  \`).join('') || '<div class="profile-card">No orders yet.</div>';
}

async function loadSubscriptions() {
  if (!authToken) return;
  const subscriptions = await api('/subscriptions');
  el('subscription-list').innerHTML = subscriptions.map(plan => \`
    <div class="compact-row">
      <span>\${plan.planType} · \${plan.mealType} · \${plan.proteinTier}<br>\${plan.startDate} to \${plan.endDate}</span>
      <strong>\${plan.status}</strong>
    </div>
  \`).join('') || '<div class="profile-card">No subscriptions yet.</div>';
}

function wireBackendApp() {
  if (authToken) {
    el('logout-btn')?.classList.remove('hidden');
    loadProfile().catch(error => setStatus('auth-status', error.message, false));
    loadCart().catch(() => {});
    loadOrders().catch(() => {});
    loadSubscriptions().catch(() => {});
  }

  document.querySelectorAll('[data-auth-tab]').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('[data-auth-tab]').forEach(tab => tab.classList.remove('active'));
      btn.classList.add('active');
      const mode = btn.dataset.authTab;
      el('signup-form').classList.toggle('hidden', mode !== 'signup');
      el('login-form').classList.toggle('hidden', mode !== 'login');
      setStatus('auth-status', '');
    });
  });

  el('signup-form')?.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const response = await api('/auth/signup', { method: 'POST', body: JSON.stringify(formJson(event.currentTarget)) });
      setAuthenticated(response.token);
      renderProfile(response.user);
      setStatus('auth-status', 'Account created. You are logged in.');
      await Promise.allSettled([loadCart(), loadOrders(), loadSubscriptions()]);
    } catch (error) {
      setStatus('auth-status', error.message, false);
    }
  });

  el('login-form')?.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const response = await api('/auth/login', { method: 'POST', body: JSON.stringify(formJson(event.currentTarget)) });
      setAuthenticated(response.token);
      renderProfile(response.user);
      setStatus('auth-status', 'Logged in.');
      await Promise.allSettled([loadCart(), loadOrders(), loadSubscriptions()]);
    } catch (error) {
      setStatus('auth-status', error.message, false);
    }
  });

  el('logout-btn')?.addEventListener('click', clearAuth);
  el('refresh-profile-btn')?.addEventListener('click', () => loadProfile().catch(error => setStatus('auth-status', error.message, false)));
  el('load-menu-btn')?.addEventListener('click', () => loadMenu().catch(error => setStatus('order-status', error.message, false)));
  el('refresh-cart-btn')?.addEventListener('click', () => loadCart().catch(error => setStatus('order-status', error.message, false)));
  el('load-orders-btn')?.addEventListener('click', () => loadOrders().catch(error => setStatus('order-status', error.message, false)));
  el('load-subscriptions-btn')?.addEventListener('click', () => loadSubscriptions().catch(error => setStatus('subscription-status', error.message, false)));
  el('checkout-btn')?.addEventListener('click', () => checkout().catch(error => setStatus('order-status', error.message, false)));

  el('profile-form')?.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const user = await api('/users/me', { method: 'PUT', body: JSON.stringify(formJson(event.currentTarget)) });
      renderProfile(user);
      setStatus('auth-status', 'Profile updated.');
    } catch (error) {
      setStatus('auth-status', error.message, false);
    }
  });

  el('subscription-form')?.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const plan = await api('/subscriptions', { method: 'POST', body: JSON.stringify(formJson(event.currentTarget)) });
      setStatus('subscription-status', 'Subscription #' + plan.id + ' created with status ' + plan.status + '.');
      await loadSubscriptions();
    } catch (error) {
      setStatus('subscription-status', error.message, false);
    }
  });

  el('feedback-form')?.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const feedback = await api('/feedbacks', { method: 'POST', body: JSON.stringify(formJson(event.currentTarget)) });
      setStatus('feedback-status', 'Feedback saved for order #' + feedback.orderId + '.');
    } catch (error) {
      setStatus('feedback-status', error.message, false);
    }
  });

  document.addEventListener('click', event => {
    const addBtn = event.target.closest('[data-add-item]');
    if (addBtn) {
      addToCart(addBtn.dataset.addItem).catch(error => setStatus('order-status', error.message, false));
      return;
    }
    const payBtn = event.target.closest('[data-pay-order]');
    if (payBtn) {
      createPayment(payBtn.dataset.payOrder, payBtn.dataset.payAmount).catch(error => setStatus('order-status', error.message, false));
    }
  });

  loadMenu().catch(() => {});
}
wireBackendApp();
`;

if (!html.includes('/* -- BACKEND CONNECTED APP')) {
  html = html.replace('</style>', `${backendCss}\n</style>`);
}

if (!html.includes('id="fitfuel-app"')) {
  html = html.replace('\n  <div class="footer-strip">', `${backendHtml}\n  <div class="footer-strip">`);
}

if (!html.includes("const API_BASE = 'http://localhost:8080'")) {
  html = html.replace("renderTier('90');\n</script>", `renderTier('90');\n${backendScript}\n</script>`);
}

await writeFile(filePath, html, 'utf8');
