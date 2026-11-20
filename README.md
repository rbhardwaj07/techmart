# TechMart - Full-Stack E-Commerce Application

An electronics store built in three parts:

| Part | What it adds |
|------|--------------|
| **Part 1** | Product listing with search, category filter, price filter, sorting and a product details page |
| **Part 2** | User registration and login, per-user persistent shopping cart, authorization, database migrations |
| **Part 3** | Structured JSON logging, Prometheus metrics, and a Grafana dashboard |

| Layer | Technology |
|-------|------------|
| Frontend | React 18 (Vite) + React Router |
| Backend | Java 17, Spring Boot 3 (REST API), Spring Data JPA |
| Database | MySQL 8 |
| Authentication | Spring Security + JWT (jjwt), BCrypt password hashing |
| Migrations | Flyway |
| Logging | SLF4J / Logback with JSON output (logstash-logback-encoder) |
| Metrics and dashboards | Spring Boot Actuator + Micrometer, Prometheus, Grafana (Docker Compose) |

---

## 1. Architecture

```
                                                    +------------------------+
 Browser                                            |  logs/techmart.log     |
 React app (:5173)                                  |  (JSON, 1 line/event)  |
      |  fetch("/api/...")                          +-----------^------------+
      v  (Vite dev server proxies /api)                         |
 Spring Boot backend (:8080) ----------------------------------+
   |  Controller -> Service -> Repository
   |        |
   |        +--JPA / JDBC--> MySQL (:3306)  [products, categories, users, carts, cart_items]
   |
   +-- GET /actuator/prometheus <----(scrape every 5 s)---- Prometheus (:9090) <---- Grafana (:3000)
```

### Request flow
1. The React app calls `/api/...`. In development, Vite proxies these calls to the backend (no CORS problems).
2. `RequestLoggingFilter` gives the request an id (`X-Request-Id`) and logs it.
3. `JwtAuthFilter` reads `Authorization: Bearer <token>`; if the token is valid, the request is authenticated as that user.
4. Spring Security decides whether the endpoint needs a login (`SecurityConfig`).
5. Controller -> Service (validation and business rules) -> Repository -> MySQL.
6. Errors become JSON responses with the correct status code (`GlobalExceptionHandler`); metrics are updated along the way.

### Important implementation decisions

**Authentication and authorization (Part 2)**
- **BCrypt** hashes passwords (salted and slow), so plain passwords are never stored or logged.
- **JWT** access tokens keep sessions stateless: the server stores no session, and the React app sends the token in the `Authorization` header.
- **Authorization is enforced on the backend.** Cart endpoints take the user id from the verified token, never from the URL or request body. Each cart item id is checked against the caller's cart; another user's item returns **403 Forbidden**. Hiding buttons in the UI is not relied on.
- **Prices come from the database only.** Cart requests contain just `productId` and `quantity`; a `price` sent by a client is ignored. Totals are computed on the server.
- **Backend validation:** quantity must be 1-100, the product must exist and the quantity cannot exceed stock.
- **Carts are stored in MySQL** (`carts`, `cart_items`), so they survive restarts and work across browsers.
- **Flyway migrations** extend the existing Part 1 database without touching `products` or `categories`. The Part 1 schema is treated as baseline version 1; `V2` adds the new tables.

**Logging and monitoring (Part 3)**
- **One JSON object per log line** with `@timestamp`, `level`, `logger_name`, `message`, `requestId`, `userId` (when logged in) and event fields (`method`, `path`, `status`, `durationMs`, ...).
- **Request ids** are created before Spring Security runs, so every log line of a request (even rejected 401s) can be followed with a single id. The id is also returned in the `X-Request-Id` response header.
- **No secrets in logs.** The logging filter never reads headers, query strings or bodies. Failed logins log no email or password. Tokens are never logged. A test (`passwordsAndTokensAreNeverLogged`) checks this.
- **Metrics use URI templates** (for example `/api/products/{id}`), so product ids do not create thousands of Prometheus series.
- **Custom (application) metrics** are created at 0 on startup so the dashboard shows 0 instead of "no data".
- **Actuator endpoints** `health` and `prometheus` are open so Prometheus can scrape without a token. This is acceptable for a single-computer lab; in production they should be restricted by network or authentication.
- **Controlled-failure endpoints** (`/api/debug/error`, `/api/debug/slow`) exist only while `app.debug-endpoints.enabled=true` and are used for the failure demonstration. Set `DEBUG_ENDPOINTS=false` to remove them.
- Database problems return **503** and unexpected exceptions return **500**; both are logged with a stack trace and counted in `techmart_errors_total`.

### Project structure

```
techmart/
├── database/
│   ├── schema.sql                 # Part 1 schema: categories, products (re-running it resets data)
│   └── seed.sql                   # 6 categories, 14 products
├── backend/                       # Spring Boot (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/techmart/{controller,service,repository,model,dto,exception,config,security}
│       ├── main/resources/application.properties   # configuration
│       ├── main/resources/logback-spring.xml       # JSON logging
│       ├── main/resources/db/migration/            # Flyway: V2__create_users_carts_cart_items.sql
│       └── test/                                   # API tests (in-memory H2, MySQL not needed)
├── frontend/                      # React (Vite)
│   └── src/{pages,components,context,api.js,App.jsx,main.jsx,index.css}
├── monitoring/                    # Prometheus + Grafana
│   ├── docker-compose.yml
│   ├── prometheus/prometheus.yml
│   └── grafana/{provisioning/, dashboards/techmart-dashboard.json}
├── scripts/                       # generate-traffic.sh, trigger-failure.sh
└── screenshots/                   # submission screenshots
```

---

## 2. Prerequisites (Ubuntu)

Tested on Ubuntu (also works in WSL). Run these once:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven mysql-server curl unzip docker.io docker-compose-v2

# Node.js 20 (Ubuntu's default Node is too old for Vite)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Allow your user to run Docker (then close and reopen the terminal)
sudo usermod -aG docker $USER
```

If `docker-compose-v2` is not available, use `sudo apt install -y docker-compose` and type `docker-compose` instead of `docker compose` below.

Start the services (needed on WSL; harmless on normal Ubuntu):

```bash
sudo service mysql start
sudo service docker start
```

Check the versions:

```bash
java -version      # 17 or newer
mvn -version
node -v            # 18 or newer
mysql --version
docker --version && docker compose version
```

**Ports used:** 3306 (MySQL), 8080 (backend), 5173 (frontend), 9090 (Prometheus), 3000 (Grafana).

---

## 3. Configuration

### MySQL root password
On Ubuntu, MySQL's `root` user often logs in through the operating system and has no password. Give it a password so the backend can connect (replace `your_password`):

```bash
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH caching_sha2_password BY 'your_password'; FLUSH PRIVILEGES;"
```

### Backend settings
The backend reads these environment variables (defaults are in `backend/src/main/resources/application.properties`):

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:mysql://localhost:3306/techmart?...` | database address |
| `DB_USER` | `root` | database user |
| `DB_PASSWORD` | `root` | database password |
| `JWT_SECRET` | a development value | key that signs login tokens (use 32+ characters for real use) |
| `DEBUG_ENDPOINTS` | `true` | enables the controlled-failure endpoints used in the demo |

Set the password in **every terminal where you start the backend**. Keep the single quotes (a `#` in a password breaks without them):

```bash
export DB_PASSWORD='your_password'
```

### Monitoring settings
- `monitoring/prometheus/prometheus.yml`: Prometheus scrapes `host.docker.internal:8080/actuator/prometheus` every 5 seconds.
- `monitoring/docker-compose.yml`: Prometheus on port 9090 and Grafana on port 3000 (login `admin` / `admin`).
- `monitoring/grafana/provisioning/`: the Prometheus data source and the dashboard are loaded automatically; the dashboard itself is `monitoring/grafana/dashboards/techmart-dashboard.json` (this is also the export).

---

## 4. Setup and run (everything on one computer)

Run all commands from the project root (the folder containing `backend`, `frontend`, `database`, `monitoring`) unless stated otherwise.

### Step 1 - Create the database (Part 1 schema and data)

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
mysql -u root -p -e "SELECT COUNT(*) FROM techmart.products;"
```

Enter your MySQL password when asked. The last command must print **14**.

### Step 2 - Start the backend (Terminal 1)

```bash
cd backend
export DB_PASSWORD='your_password'
mvn test                 # optional: runs all API tests (uses in-memory H2, no MySQL needed)
mvn spring-boot:run
```

The first run downloads dependencies and takes a few minutes. Wait for these log lines:
- `Successfully applied 1 migration` - Flyway has created the Part 2 tables (see "Database migrations" below),
- `Started TechMartApplication`.

Check it: <http://localhost:8080/api/products> shows JSON, and <http://localhost:8080/actuator/prometheus> shows metrics.

### Step 3 - Start the frontend (Terminal 2)

```bash
cd frontend
npm install
npm run dev
```

Open **<http://localhost:5173>**.

### Step 4 - Start the monitoring stack (Terminal 3)

```bash
cd monitoring
docker compose up -d
```

| Service | URL | Notes |
|---------|-----|-------|
| Web shop | <http://localhost:5173> | register an account to use the cart |
| Backend API | <http://localhost:8080> | |
| Raw metrics | <http://localhost:8080/actuator/prometheus> | |
| Prometheus | <http://localhost:9090> | Status -> Targets: `techmart-backend` must be **UP** |
| Grafana | <http://localhost:3000> | `admin` / `admin`; open Dashboards -> **TechMart Backend Monitoring** |

### Stopping everything
- Backend and frontend: press `Ctrl+C` in their terminals.
- Monitoring: `cd monitoring && docker compose down`.

---

## 5. Database migrations (Part 2)

Migration tool: **Flyway** (the standard choice for Spring Boot). The migration is `backend/src/main/resources/db/migration/V2__create_users_carts_cart_items.sql`. It only **adds** the tables `users`, `carts` and `cart_items`; `categories` and `products` are not changed.

**How it is applied:** automatically when the backend starts. The Part 1 database has no migration history, so `application.properties` sets `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1`: Flyway treats the existing Part 1 schema as version 1 and applies only V2. Hibernate is set to `validate`, so the backend refuses to start if the tables do not match the code.

**Demonstrate that existing product records are preserved:**

```bash
# BEFORE the first backend start (after Step 1)
mysql -u root -p -e "SELECT COUNT(*) AS products_before FROM techmart.products;"     # 14

# start the backend once (Step 2), then:
mysql -u root -p -e "SELECT COUNT(*) AS products_after FROM techmart.products; SHOW TABLES FROM techmart; SELECT version, description, success FROM techmart.flyway_schema_history;"
```

Expected: 14 products both times, the new tables `users`, `carts`, `cart_items`, and history rows for version 1 (baseline) and version 2.

> Re-running `database/schema.sql` drops the Part 2 tables and the Flyway history too, so the next backend start re-applies V2 cleanly.

---

## 6. Using and testing the application

### Shop features
1. **Browse** products, search, filter by category and price, sort, open a product page (no login needed).
2. **Register / Log in** from the header. After login the header shows your name and a cart badge.
3. **Add to cart** from a product card or the product details page (choose a quantity there).
4. **Cart page** (`/cart`): change quantities with + / -, remove items, see line totals and the cart total. Visiting `/cart` while logged out redirects to the login page.
5. The cart is stored in MySQL: stop and restart the backend, log in again, and the items are still there.

### Authorization demonstration (one user cannot access another user's cart)

```bash
# 1. Register two users (copy each "token" from the responses)
curl -s -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","password":"password123"}'
curl -s -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Bob","email":"bob@example.com","password":"password123"}'

# 2. Alice adds a product; the response shows the cart item "id" (for example 1)
curl -s -X POST localhost:8080/api/cart/items -H "Authorization: Bearer ALICE_TOKEN" \
  -H "Content-Type: application/json" -d '{"productId":1,"quantity":1}'

# 3. Bob tries to change / delete Alice's item by using its id  ->  403 Forbidden
curl -i -X PUT    localhost:8080/api/cart/items/1 -H "Authorization: Bearer BOB_TOKEN" \
  -H "Content-Type: application/json" -d '{"quantity":5}'
curl -i -X DELETE localhost:8080/api/cart/items/1 -H "Authorization: Bearer BOB_TOKEN"

# 4. No token at all  ->  401 Unauthorized
curl -i localhost:8080/api/cart
```

These cases are also automated tests (`AuthCartTests`, run with `mvn test`).

### Monitoring demonstration

1. Open the Grafana dashboard. Set the time range to **Last 15 minutes** (auto-refresh is 5 s).
2. **Generate normal traffic** - use the shop in the browser, or run (from the project root):
   ```bash
   bash scripts/generate-traffic.sh 30
   ```
   It registers a test user and browses, adds, changes and removes cart items, plus a few 404 / 401 requests. Request volume, latency, per-endpoint and cart panels change.
3. **Trigger a controlled failure:**
   ```bash
   bash scripts/trigger-failure.sh error     # 20 calls to /api/debug/error -> HTTP 500
   ```
   - On the dashboard: **5xx server errors**, **HTTP error rate** and **Backend errors (unexpected)** rise.
   - In the logs:
     ```bash
     grep unexpected_exception backend/logs/techmart.log | tail -3
     ```
     shows ERROR lines with the stack trace and the request id.
4. **Latency:** `bash scripts/trigger-failure.sh slow` makes the latency percentiles jump.
5. **Optional database failure:** `sudo service mysql stop`, then try to log in. The API answers **503**, logs `database_error`, and `techmart_errors_total{type="database"}` increases. Start MySQL again with `sudo service mysql start`.

**Dashboard panels:** backend status, total requests, 4xx/5xx count, products added to carts, failed auth attempts, request volume over time (total and by status), requests per endpoint, latency p50/p95/p99, average latency per endpoint, HTTP error rate (%), errors by status code, cart operations per minute, authentication attempts, backend errors (database / unexpected), JVM heap.

**Metrics exposed** (in addition to the standard `http_server_requests_seconds_*` and JVM metrics):

| Metric | Meaning |
|--------|---------|
| `techmart_cart_operations_total{operation="add\|update\|remove"}` | cart operations completed |
| `techmart_cart_items_added_total` | products (units) added to carts |
| `techmart_auth_attempts_total{type="login\|register\|token", result="success\|failure"}` | authentication attempts |
| `techmart_errors_total{type="database\|unexpected"}` | backend errors |

**Useful log commands:**

```bash
tail -f backend/logs/techmart.log                   # live log
grep '"level":"ERROR"' backend/logs/techmart.log    # errors only
grep login_failed backend/logs/techmart.log         # failed logins
grep THE_REQUEST_ID backend/logs/techmart.log       # follow one request (use an X-Request-Id value)
```

Example log line:

```json
{"@timestamp":"2026-11-20T10:15:02.114Z","level":"WARN","logger_name":"http.access","message":"http_request","requestId":"3f0c...","method":"GET","path":"/api/cart","status":401,"durationMs":3,"app":"techmart-backend"}
```

---

## 7. API reference

Base URL: `http://localhost:8080`

### Products (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | list products (query parameters below, all optional) |
| GET | `/api/products/{id}` | one product |
| GET | `/api/categories` | list categories |

Query parameters for `GET /api/products`: `search` (name or description), `categoryId`, `minPrice`, `maxPrice`, `sort` (`name`, `price_asc`, `price_desc`).
Example: `GET /api/products?search=wireless&categoryId=3&sort=price_asc`

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | public | `{name, email, password}` -> `{token, user}` (201; 409 if the email exists; password 8-72 characters) |
| POST | `/api/auth/login` | public | `{email, password}` -> `{token, user}` (401 on wrong credentials) |
| GET | `/api/auth/me` | token | the current user |

### Cart (login required; header `Authorization: Bearer <token>`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/cart` | items, `totalItems`, `total` |
| POST | `/api/cart/items` | `{productId, quantity}` adds or increases an item (201) |
| PUT | `/api/cart/items/{itemId}` | `{quantity}` sets the quantity |
| DELETE | `/api/cart/items/{itemId}` | removes the item |

### Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | health (includes the database) |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/api/debug/error` | controlled 500 (demo only) |
| GET | `/api/debug/slow?ms=1500` | controlled slow response (demo only, max 5000 ms) |

### Status codes

| Code | When |
|------|------|
| 200 / 201 | success / created |
| 400 | invalid input (bad id, negative price, quantity outside 1-100, quantity above stock, malformed JSON) |
| 401 | missing, invalid or expired token; wrong login |
| 403 | trying to use another user's cart item |
| 404 | product, cart item or URL not found |
| 405 | wrong HTTP method |
| 409 | email already registered |
| 500 | unexpected server error (logged and counted) |
| 503 | database unavailable (logged and counted) |

Error responses are JSON, for example:
`{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "Product with id 9999 not found" }`

---

## 8. Tests

```bash
cd backend
mvn test
```

The tests use an in-memory H2 database, so MySQL is not needed. They cover the product API (Part 1), registration and login, cart add/update/remove with totals, stock and quantity validation, ignoring client-submitted prices, the cross-user authorization checks (Part 2), and request ids, custom metrics, the controlled failure and "no passwords or tokens in logs" (Part 3).

---

## 9. Screenshots

The screenshots for submission are in the `screenshots/` folder.

### Authentication flow
| Register | Login | Logged in (header) |
|----------|-------|--------------------|
| ![Register](screenshots/register.png) | ![Login](screenshots/login.png) | ![Logged in](screenshots/logged-in.png) |

### Shopping cart
![Shopping cart with items and total](screenshots/cart.png)

### Grafana dashboard
![Grafana dashboard with traffic](screenshots/grafana-dashboard.png)

After the controlled failure (`trigger-failure.sh error`):

![Grafana dashboard after the failure](screenshots/grafana-failure.png)

Log lines for the failure:

![Error logs](screenshots/error-logs.png)

---

## 10. Troubleshooting

| Problem | Fix |
|---------|-----|
| `mysql: Access denied for user 'root'` (error 1698) | root still uses OS login: run the `ALTER USER ... caching_sha2_password` command in section 3 |
| `Unknown database 'techmart'` | run the two `mysql` commands in Step 1 |
| Backend: `Access denied` at startup | `export DB_PASSWORD='your_password'` in that terminal (keep the quotes) |
| `Can't connect to local MySQL server` | `sudo service mysql start` |
| Backend: `Schema-validation: missing table` | the migration did not run; read the Flyway lines in the backend log and check the DB credentials |
| `Found non-empty schema(s) without schema history` | keep `spring.flyway.baseline-on-migrate=true` in `application.properties` |
| Shop shows "Request failed" / no products | the backend is not running on port 8080 |
| `npm` errors after copying the project from Windows | delete `frontend/node_modules` and run `npm install` again |
| Prometheus target `techmart-backend` is DOWN | open <http://localhost:8080/actuator/prometheus>; if it works, check Docker is 20.10 or newer (needed for `host.docker.internal` on Linux) |
| Grafana panels show "No data" | generate traffic (`bash scripts/generate-traffic.sh 20`) and wait about 10 seconds; time range must be "Last 15 minutes" |
| `permission denied` when running docker | `sudo usermod -aG docker $USER` and open a new terminal (or prefix commands with `sudo`) |
| Port already in use | stop the other program; for the backend change `server.port` in `application.properties` and the proxy in `frontend/vite.config.js`; for Docker change the left-hand ports in `monitoring/docker-compose.yml` |
