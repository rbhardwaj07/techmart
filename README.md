# TechMart - Electronic Gadgets Store (Part 1: Product Listing)

A full-stack product listing website.

| Layer    | Technology                                  |
|----------|---------------------------------------------|
| Frontend | React 18 (Vite) + React Router              |
| Backend  | Java 17+ with Spring Boot 3 (REST API)      |
| Database | MySQL 8                                     |

## Architecture

```
 Browser (React, :5173)  --/api-->  Spring Boot (:8080)  --JPA/JDBC-->  MySQL (:3306)
```

- The **React app** shows the product list, search/filters and a product details page. It only talks to the backend through `fetch("/api/...")`; it never touches the database.
- During development, Vite's dev server proxies every `/api` request to the Spring Boot backend (see `frontend/vite.config.js`), so there are no CORS problems.
- The **Spring Boot backend** is split into layers: `controller` (HTTP) -> `service` (validation and logic) -> `repository` (Spring Data JPA) -> MySQL. Errors are turned into JSON responses with the right status code by `GlobalExceptionHandler`.
- The **MySQL database** has two tables: `categories` and `products` (with a foreign key from products to categories).

```
techmart/
├── database/
│   ├── schema.sql        # creates the techmart database and tables
│   └── seed.sql          # 6 categories and 14 sample products
├── backend/              # Spring Boot project (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/techmart/{controller,service,repository,model,dto,exception,config}
│       ├── main/resources/application.properties
│       └── test/                  # API tests (use an in-memory H2 database)
├── frontend/             # React project (Vite)
│   ├── package.json
│   ├── vite.config.js
│   ├── public/images/             # product images
│   └── src/{pages,components,api.js,App.jsx,main.jsx,index.css}
└── screenshots/          # add your screenshots here
```

## API endpoints

Base URL: `http://localhost:8080`

| Method | Endpoint             | Description                                  |
|--------|----------------------|----------------------------------------------|
| GET    | `/api/products`      | List products (all query parameters optional) |
| GET    | `/api/products/{id}` | Get one product                              |
| GET    | `/api/categories`    | List categories                              |

Query parameters for `GET /api/products`:

| Parameter    | Meaning                                                   |
|--------------|-----------------------------------------------------------|
| `search`     | Text matched against product name and description         |
| `categoryId` | Only products in this category                            |
| `minPrice`   | Minimum price (>= 0)                                      |
| `maxPrice`   | Maximum price (>= 0 and not below `minPrice`)             |
| `sort`       | `name`, `price_asc` or `price_desc` (default: by id)      |

Example: `GET /api/products?search=wireless&categoryId=3&sort=price_asc`

Status codes:

| Code | When                                                                                       |
|------|--------------------------------------------------------------------------------------------|
| 200  | Request succeeded                                                                          |
| 400  | Invalid input: non-numeric or non-positive id, negative price, minPrice > maxPrice, bad sort |
| 404  | No product with the given id                                                               |
| 500  | Unexpected server error                                                                    |

Error responses look like:

```json
{ "timestamp": "2026-10-29T09:00:00Z", "status": 404, "error": "Not Found", "message": "Product with id 9999 not found" }
```

## Prerequisites

- **JDK 17 or newer** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- **Node.js 18+ and npm** (`node -v`)
- **MySQL 8** server running on `localhost:3306`

## Setup and run

### 1. Create the database

From the project root (you will be asked for your MySQL password):

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

Check it worked:

```bash
mysql -u root -p -e "SELECT COUNT(*) FROM techmart.products;"
```

It should print 14. Re-running `schema.sql` resets the data.

### 2. Configure the backend

The backend reads its database settings from environment variables, with these defaults (see `backend/src/main/resources/application.properties`):

| Variable      | Default                                   |
|---------------|-------------------------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/techmart?...` |
| `DB_USER`     | `root`                                    |
| `DB_PASSWORD` | `root`                                    |

If your MySQL password is not `root`, set it before starting the backend:

```bash
# Linux / macOS
export DB_PASSWORD=your_password

# Windows PowerShell
$env:DB_PASSWORD="your_password"

# Windows Command Prompt
set DB_PASSWORD=your_password
```

### 3. Start the backend (terminal 1)

```bash
cd backend
mvn spring-boot:run
```

Wait for `Started TechMartApplication`. Then open <http://localhost:8080/api/products> - you should see JSON.

### 4. Start the frontend (terminal 2)

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> in your browser.

### Run the backend tests (optional)

```bash
cd backend
mvn test
```

The tests call every endpoint using an in-memory H2 database, so MySQL is not needed for them.

## Troubleshooting

- **`Access denied for user 'root'`** - set `DB_USER` / `DB_PASSWORD` as shown above.
- **`Unknown database 'techmart'`** - run the two `mysql` commands in step 1.
- **Page shows "Request failed" / no products** - make sure the backend is running on port 8080 (step 3).
- **Port already in use** - change `server.port` in `application.properties` (and the proxy target in `frontend/vite.config.js`).

## Screenshots

Add your screenshots to the `screenshots/` folder:

- `screenshots/product-listing.png` - the home page with the product grid
- `screenshots/product-details.png` - a product details page
