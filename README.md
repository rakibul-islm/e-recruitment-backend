# E-Recruitment Backend

A Spring Boot REST API powering the E-Recruitment platform — JWT + Google authentication, role-based administration, OTP-driven account flows, and centralized exception logging.

The companion Angular client for this API lives in `e-recruitment-web`.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Database Profiles](#database-profiles)
- [Default Seeded Accounts](#default-seeded-accounts)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Docker](#docker)
- [Deployment](#deployment)

## Features

- **Authentication** — username/password login, Google Sign-In, and stateless JWT session handling (`AuthenticationController`, `JwtUtil`, `JwtAutenticationFilter`)
- **Account lifecycle via email OTP** — sign-up verification, forgot-password, change-password, and admin-initiated account setup, each with its own HTML email template (`templates/email/`)
- **Role-based access control** — users, roles, permissions, and user groups, with method-level security (`@EnableMethodSecurity`) enforced per permission
- **System configuration & password policy** — runtime-configurable settings and a centrally enforced password policy, both exposed via REST for the admin UI
- **Centralized exception logging** — `GlobalExceptionHandler` persists unhandled exceptions to the database (toggleable via the `EXCEPTION_LOG_TO_DB` system config key) and exposes them through `ExceptionLogController` for the admin exception-log viewer
- **Audit logging** — an append-only audit trail (security events, entity changes) written asynchronously off the request thread by `AuditLogWriter` on a dedicated executor, exposed read-only via `AuditLogController` (`/audit-log`); enabled/disabled and retention-purged (nightly) via the `AUDIT_LOG_ENABLED` / `AUDIT_LOG_RETENTION_DAYS` system config keys
- **Session management** — active JWT sessions are tracked in `UserSession` (plus guest/unauthenticated session counts via `GuestSessionTracker`), searchable per user and force-logout-able individually or globally via `UserSessionController` (`/session`)
- **Google avatar fetching** — on Google Sign-In, the user's profile picture is downloaded and stored asynchronously (`GoogleAvatarFetcher`) so it doesn't delay login
- **Job circulars** — endpoints for listing/filtering job circulars (`JobCircularController`)
- **Auto-seeded reference data** — roles, permissions, user groups, a password policy, system config, and two starter accounts are seeded on startup (`seed/` package)
- **API documentation** — interactive Swagger UI via springdoc-openapi
- **Multi-database support** — Spring profiles for H2 (dev, in-memory), PostgreSQL (prod), and Oracle

## Tech Stack

| Layer               | Technology |
|----------------------|------------|
| Language / runtime     | Java 21 |
| Framework              | Spring Boot 3.3.5 (Web, Data JPA, Security, Mail, JDBC) |
| Auth                   | JWT ([jjwt](https://github.com/jwtk/jjwt) 0.12.x) + Google Identity Services |
| Persistence             | Spring Data JPA / Hibernate — H2, PostgreSQL, or Oracle |
| API docs               | springdoc-openapi (Swagger UI) |
| Object mapping          | ModelMapper |
| Build                  | Gradle (wrapper included) |
| Boilerplate reduction    | Lombok |

## Prerequisites

- JDK 21
- No local Gradle install needed — use the included wrapper (`./gradlew` / `gradlew.bat`)
- A database if not using the default in-memory H2 profile (PostgreSQL or Oracle)
- SMTP credentials for outbound email (OTP / account-setup emails)
- A Google OAuth 2.0 client ID (for Google Sign-In)

## Getting Started

```bash
# Run with the default (dev / H2) profile
./gradlew bootRun
```

The API starts on `http://localhost:8041/e-recruitment` (context path `/e-recruitment`, default port `8041`, overridable via `PORT`).

At minimum, set the mail and Google/frontend variables below before starting (see [Configuration](#configuration)) — mail settings have no defaults and the app will fail to start without them.

## Configuration

Configuration is externalized via environment variables, read in `application.yml` and the per-database profile files.

| Variable            | Required | Description |
|-----------------------|----------|-------------|
| `PORT`                 | No       | HTTP port (default `8041`) |
| `MAIL_FROM`            | Yes      | "From" address on outgoing emails — must be the Gmail account the OAuth credentials below authorize |
| `GMAIL_CLIENT_ID`      | Yes      | OAuth 2.0 client ID for the Gmail API (SMTP is blocked on Render, so email goes through the Gmail API instead) |
| `GMAIL_CLIENT_SECRET`  | Yes      | OAuth 2.0 client secret paired with `GMAIL_CLIENT_ID` |
| `GMAIL_REFRESH_TOKEN`  | Yes      | Long-lived refresh token authorizing `gmail.send` for the `MAIL_FROM` account |
| `GOOGLE_CLIENT_ID`     | Yes      | Google OAuth 2.0 client ID, validated against Google Sign-In tokens |
| `FRONTEND_BASE_URL`    | Yes      | Base URL of the Angular client, used to build links in emails (e.g. account setup, password reset) |
| `DB_URL`               | Prod/Oracle only | JDBC URL — see [Database Profiles](#database-profiles) for defaults |
| `DB_USERNAME`          | Prod/Oracle only | Database username |
| `DB_PASSWORD`          | Prod/Oracle only | Database password |

Other tunables (OTP expiry/attempts, account-setup link expiry) are set in `application.yml` under `app.otp` / `app.account-setup` rather than env vars.

### Generating `GMAIL_REFRESH_TOKEN`

The refresh token can expire or be revoked (commonly because the Google Cloud OAuth consent screen is left in "Testing" status, which auto-expires tokens after 7 days). Emails will fail with `invalid_grant: Token has been expired or revoked.` when this happens — regenerate it:

1. In [Google Cloud Console](https://console.cloud.google.com/apis/credentials), confirm the OAuth client matching `GMAIL_CLIENT_ID` still exists, and check the OAuth consent screen's publishing status. If it's "Testing," publish it (or re-add the `MAIL_FROM` account as a test user) so the token doesn't keep expiring after a week.
2. Open the [OAuth 2.0 Playground](https://developers.google.com/oauthplayground), click the gear icon, and check "Use your own OAuth credentials," pasting in `GMAIL_CLIENT_ID` / `GMAIL_CLIENT_SECRET`.
3. Authorize the `https://www.googleapis.com/auth/gmail.send` scope, signed in as the `MAIL_FROM` account.
4. Exchange the authorization code for tokens and copy the resulting `refresh_token`.
5. Update `GMAIL_REFRESH_TOKEN` in the deployment environment (e.g. Render dashboard) and restart the service.

## Database Profiles

Spring profiles are grouped under `dev` (default) and `prod` in `application.yml`:

| Profile     | Group | Datastore | Config file |
|--------------|-------|-----------|--------------|
| `h2`          | `dev`  | In-memory H2, schema auto-created, H2 console at `/h2-console` | `application-h2.yml` |
| `postgres`    | `prod` | PostgreSQL | `application-postgres.yml` |
| `oracle`      | —      | Oracle | `application-oracle.yml` |

Activate a profile group with `SPRING_PROFILES_ACTIVE=prod` (uses PostgreSQL), or select a specific profile with `SPRING_PROFILES_ACTIVE=oracle`. All profiles currently run with `ddl-auto: update`.

## Default Seeded Accounts

On every startup, `SeedingRunner` seeds roles, permissions, user groups, password policy, system config, and two accounts if they don't already exist (`seed/data/UserData.java`):

| Email                     | Password | Role         |
|----------------------------|----------|--------------|
| `admin@e-recruitment.com`  | `a`      | `SUPER_ADMIN` |
| `test@e-recruitment.com`   | `t`      | *(none — Normal User group)* |

These are intended for local development/testing only — change or remove them before exposing an environment publicly.

## API Documentation

With the app running, Swagger UI is available at:

```
http://localhost:8041/e-recruitment/swagger-ui/index.html
```

Raw OpenAPI spec: `http://localhost:8041/e-recruitment/v3/api-docs`.

## Project Structure

```
src/main/java/com/bd/erecruitment/
├── controller/         REST controllers (Authentication, User, Role, Permission, UserGroup,
│                        SystemConfig, PasswordPolicy, ExceptionLog, AuditLog, UserSession,
│                        JobCircular, Profile)
├── service/             Business logic interfaces (incl. UserSessionService, GuestSessionTracker)
│   └── impl/             Implementations (incl. AuditLogServiceImpl, GoogleAvatarFetcher)
├── audit/               Audit action constants, AuditLogWriter (async), exemption annotations
│                        (@AuditExempt, @AuditIgnore)
├── repository/          Spring Data JPA repositories
├── entity/               JPA entities (incl. AuditLog, UserSession)
├── dto/
│   ├── req/               Request DTOs
│   └── res/               Response DTOs
├── security/             Spring Security config, JWT entry point
├── filter/               JWT authentication filter
├── util/                 JWT utility, response wrapper helpers, request utils
├── exception/            Custom exceptions + GlobalExceptionHandler (also persists to exception log)
├── specification/        JPA Specifications for dynamic search/filtering
├── annotation/           Custom annotations
├── model/                Auth/user-detail models used by Spring Security
├── enums/                Shared enums
├── config/               CORS, Swagger, async executor (AsyncConfig), and general web config
└── seed/                 Startup data seeding (roles, permissions, users, groups, config, policy)

src/main/resources/
├── application.yml              Base configuration (mail, OTP, Google, frontend URL, springdoc)
├── application-h2.yml           Dev / H2 datasource
├── application-postgres.yml     Prod / PostgreSQL datasource
├── application-oracle.yml       Oracle datasource
└── templates/email/             HTML email templates (signup OTP, forgot/change password OTP, account setup)
```

## Testing

```bash
./gradlew test
```

Runs the JUnit 5 test suite (Spring Boot Test + Spring Security Test).

## Docker

A multi-stage `Dockerfile` is included (Gradle build stage → `eclipse-temurin:21-jre-alpine` runtime), exposing port `8041`:

```bash
docker build -t e-recruitment-backend .
docker run -p 8041:8041 --env-file .env e-recruitment-backend
```

Provide the [required environment variables](#configuration) via `--env-file` or `-e` flags.

## Deployment

`render.yaml` configures this service for deployment on [Render](https://render.com) as a Docker web service, building with `./gradlew build` and running the generated `build/libs/e-recruitment-0.0.1-SNAPSHOT.jar`. Set the environment variables from [Configuration](#configuration) (plus `SPRING_PROFILES_ACTIVE=prod` and PostgreSQL credentials) in the Render service settings.
