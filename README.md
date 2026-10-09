# E-Commerce Backend — Spring Boot

A RESTful e-commerce backend built with Java, Spring Boot, PostgreSQL, Spring Data JPA, and Spring Security. The application supports user authentication, product and category management, shopping carts, order processing, and role-based administration.

## Features

- **Authentication & Authorization:** JWT-based authentication and role-based access control.
- **User Management:** User registration, authentication, profile management, and administrative user management.
- **Product Catalog:** Product and category management with request validation.
- **Shopping Cart:** Add products to a cart and manage cart items.
- **Order Management:** Checkout, order retrieval, cancellation, and order status administration.
- **Security:** Spring Security, password hashing, and protected admin endpoints.
- **Database Persistence:** PostgreSQL with Spring Data JPA and Hibernate.
- **Database Migrations:** Flyway for schema migrations.
- **Exception Handling:** Centralized exception handling and structured validation errors.
- **API Documentation:** OpenAPI/Swagger UI.
- **Automated Testing:** Maven-based service, controller, authentication, and security tests.

## Tech Stack

| Technology | Purpose |
|---|---|
| Java | Backend programming |
| Spring Boot | Application framework |
| Spring MVC | REST API development |
| Spring Security | Authentication and authorization |
| JWT | Stateless authentication |
| Spring Data JPA | Database access |
| Hibernate | ORM |
| PostgreSQL | Relational database |
| Flyway | Database migrations |
| Maven | Build and dependency management |
| JUnit & Mockito | Automated testing |
| OpenAPI / Swagger | API documentation |

## Architecture

The application follows a layered architecture that separates HTTP handling, business logic, and database access.

```text
Client / API Consumer
        |
        v
REST Controllers
        |
        v
Service Layer
        |
        v
Repository Layer
        |
        v
JPA / Hibernate
        |
        v
PostgreSQL
```

- **Controller:** Handles HTTP requests and responses.
- **Service:** Implements business rules and application logic.
- **Repository:** Performs database operations.
- **Entity:** Represents persisted domain objects.
- **DTO:** Defines request and response data structures.
- **Security configuration:** Protects endpoints using JWT authentication and role-based authorization.

## Main Modules

| Module | Responsibility |
|---|---|
| Authentication | Registration, login, and JWT handling |
| Users | User accounts and administration |
| Categories | Product category management |
| Products | Product catalog and inventory information |
| Cart | Shopping cart operations |
| Orders | Checkout, order lifecycle, and cancellation |
| Admin | Order status and user administration |

## API Overview

The application exposes REST endpoints under `/api`.

| Area | Example endpoints | Access |
|---|---|---|
| Registration | `POST /api/users/register` | Public |
| Login | `POST /api/auth/login` | Public |
| Categories | `/api/categories` | Read access; mutations restricted |
| Products | `/api/products` | Read access; mutations restricted |
| Cart | `/api/cart/**` | Authenticated |
| Orders | `/api/orders/**` | Authenticated |
| Admin | `/api/admin/**` | Admin only |

The API also includes role-management endpoints under `/api/role/**`. Exact request methods, request bodies, and response formats are defined by the controllers and DTOs.

### API Documentation

When the application is running, open:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI specification: `http://localhost:8080/v3/api-docs`

## Prerequisites

- A compatible JDK installed
- PostgreSQL running locally
- Git
- Maven, or use the included Maven Wrapper

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/ayushdubey001/ecommerce-backend-spring-boot.git
cd ecommerce-backend-spring-boot
```

### 2. Create the database

Create a PostgreSQL database named `ecommerce_db`, or configure another database name using the environment variables below.

### 3. Configure environment variables

The application reads its database and JWT configuration from environment variables.

Example PowerShell configuration for local development:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/ecommerce_db"
$env:DB_USERNAME="your_postgres_username"
$env:DB_PASSWORD="your_postgres_password"
$env:JWT_SECRET="your_base64_encoded_jwt_signing_secret"
$env:JWT_ISSUER="ayush-api"
$env:JWT_EXPIRY="900"
```

Use a strong, randomly generated JWT signing secret encoded in Base64. Keep actual credentials out of source control.

### 4. Run the application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
./mvnw spring-boot:run
```

The application uses port `8080` by default.

### 5. Run tests

On Windows:

```powershell
.\mvnw.cmd test
```

On Linux or macOS:

```bash
./mvnw test
```

## Configuration Profiles

The project includes separate configuration files:

- `application.properties` — local development configuration.
- `application-prod.properties` — production configuration.

Activate the production profile by setting:

```text
SPRING_PROFILES_ACTIVE=prod
```

Production configuration uses Hibernate schema validation and Flyway migrations. Ensure that the production database is configured correctly and the required migrations are available before starting the application.

## Security Notes

- Passwords are handled using Spring Security's password encoder.
- Protected endpoints require valid JWT authentication.
- Administrative operations require the appropriate role.
- Database credentials and JWT signing secrets should be supplied through environment variables.
- Never commit `.env` files, production credentials, or real signing secrets.

## Testing

The project includes automated tests for services, controllers, JWT handling, authentication, and security configuration.

Run `mvn test` to execute the test suite.

## Future Improvements

Potential enhancements include:

- Integration testing against a dedicated test database.
- Continuous integration with GitHub Actions.
- Deployment to a cloud platform.
- Containerization with Docker.
- Additional monitoring and production observability.

## Author

**Ayush Kumar Dubey**

- GitHub: [@ayushdubey001](https://github.com/ayushdubey001)
- Project: [E-Commerce Backend](https://github.com/ayushdubey001/ecommerce-backend-spring-boot)

---

Built as a backend engineering portfolio project to demonstrate REST API development, database integration, authentication, authorization, and automated testing.