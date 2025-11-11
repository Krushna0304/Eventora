
# Eventora

Eventora is a Spring Boot-based backend for event management. It enables users to register, log in, create and discover events, register for events, and receive personalized recommendations. The backend supports OAuth, JWT authentication, and AWS S3 for asset storage.

**Example use case:**

> Alice wants to host a tech meetup. She registers, creates an event, uploads a banner, and shares the event link. Bob discovers the event, registers, and receives a confirmation. Both can manage their events and registrations via the API.


## Table of Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Build & Run (Windows)](#build--run-windows)
- [Running tests](#running-tests)
- [Common endpoints & structure](#common-endpoints--structure)
- [Development notes](#development-notes)
- [Contributing](#contributing)
- [License](#license)


## Features

- User registration and authentication (JWT-based)
- OAuth integration
- Event creation, listing, filtering, and detail views
- Registration management for events
- Recommendation engine for suggesting events
- AWS S3 support for storing event assets (images, attachments)


## Tech Stack

- Java 11+ (project uses Spring Boot)
- Spring Security (JWT filter and custom user details)
- Spring Data JPA (repositories under `src/main/java/com/Eventora/repository`)
- AWS SDK (S3 service integration)
- Maven wrapper for builds


## Prerequisites

- JDK 11 or newer installed and JAVA_HOME configured
- Maven wrapper is included (`mvnw` / `mvnw.cmd`) so you don't need a separate Maven install
- A database (H2, PostgreSQL, MySQL, etc.) configured in `application.yml`
- (Optional) AWS credentials if using S3 features


## Configuration

Most configuration is in `src/main/resources/application.yml`. Typical values you will need to set before running in a non-development environment:

- Datasource (URL, username, password)
- JWT secret and token expiration
- AWS S3 credentials and bucket name (or configure environment credentials)


### Environment Variables & Secrets

You can set sensitive values as environment variables or in `application.yml`. For production, avoid hardcoding secrets.

**Common environment variables:**

- `SPRING_DATASOURCE_URL` — JDBC URL for your database
- `SPRING_DATASOURCE_USERNAME` — DB username
- `SPRING_DATASOURCE_PASSWORD` — DB password
- `JWT_SECRET` — Secret for signing JWT tokens
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` — AWS credentials for S3

Example settings you might add (pseudo YAML):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/eventora
    username: your_db_user
    password: your_db_pass

jwt:
  secret: your_jwt_secret_here
  expirationMs: 3600000

aws:
  s3:
    bucket: your-bucket-name
    region: us-east-1
```


For local development you can also use an in-memory DB or point `spring.datasource.url` to an H2 instance.


## Build & Run (Windows)

From the project root (where `mvnw.cmd` and `pom.xml` live), run in PowerShell:

```powershell
# Build the project
.\mvnw.cmd clean package -DskipTests

# Run the app
.\mvnw.cmd spring-boot:run
```

Or run the packaged JAR produced in `target/`:

```powershell
java -jar target\*.jar
```

By default the app starts on port 8080 (unless configured otherwise in `application.yml`).


## Running Tests

Run unit and integration tests using the Maven wrapper:

```powershell
.\mvnw.cmd test
```


## Example API Usage

**Register a new user:**

Request:
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "token": "<JWT_TOKEN>"
}
```

**Get all events:**
```http
GET /api/events
Authorization: Bearer <JWT_TOKEN>
```

See controller classes for more endpoints and request/response formats.

## Common Endpoints & Project Structure

The controllers live in `src/main/java/com/Eventora/controller`. The main controllers include:

- `AuthController` — handles registration/login related endpoints
- `OAuthController` — OAuth related endpoints
- `EventController` — create, update, list, and fetch event details
- `RegistrationController` — register/unregister for events
- `RecommendationController` — event recommendation endpoints


### Folder Structure

```
src/
  main/
    java/com/Eventora/
      controller/      # REST controllers (API endpoints)
      dto/             # Data transfer objects
      entity/          # JPA entities (database models)
      repository/      # Spring Data repositories
      service/         # Business logic
      security/        # JWT, OAuth, and security config
      Utils/           # Utility classes
    resources/
      application.yml  # Main configuration
      static/          # Static assets (if any)
      templates/       # Email or web templates
  test/
    java/com/Eventora/ # Unit and integration tests
```

Check the controller classes for exact endpoint paths and request/response shapes.


## Development Notes

- Security: JWT handling is implemented via `JwtFilter`, `JwtUtils`, and `SecurityConfig`. If you change token formats or auth flows, update these classes.
- AWS S3: `AWSS3Service` encapsulates S3 interactions. Provide proper credentials and bucket details.
- For application-wide utility functions see `src/main/java/com/Eventora/Utils`.


## Contact & Support

For questions, issues, or feature requests, please open a GitHub Issue or contact the maintainer at [your-email@example.com].

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Make changes and add tests
4. Run tests locally: `.\mvnw.cmd test`
5. Open a pull request with a clear description of the change

Follow existing code style and Java conventions used in the project.


## Troubleshooting

- Port already in use: change `server.port` in `application.yml` or free the port.
- DB connection issues: confirm `spring.datasource.*` settings and that the DB is reachable.
- AWS S3 permissions: ensure the credentials used have rights to write/read the configured bucket.


## License

This repository does not contain an explicit license file. To allow open-source use, add a `LICENSE` file (e.g., MIT, Apache 2.0, GPL) to clarify reuse terms.

---

If you'd like, I can also:
- Add a short `docs/` folder describing the API endpoints discovered from the controllers
- Add a sample `application.yml.example` with placeholder values
- Add a basic GitHub Actions workflow to run `mvnw.cmd test` on push

