# E-Recruitment Backend

A Spring Boot REST API powering the E-Recruitment platform — JWT + Google authentication, role-based administration, the end-to-end hiring workflow (postings, applications, interviews, offers, onboarding), and centralized exception/audit logging.

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
- [Database Backup & Restore](#database-backup--restore)
- [Testing](#testing)
- [Docker](#docker)
- [Deployment](#deployment)

## Features

- **Authentication** — username/password login, Google Sign-In, and stateless JWT session handling (`AuthenticationController`, `JwtUtil`, `JwtAutenticationFilter`)
- **Account lifecycle via email OTP** — sign-up verification, forgot-password, change-password, and admin-initiated account setup, each with its own HTML email template (`templates/email/`)
- **Role-based access control** — users, roles, permissions, and user groups, with method-level security (`@EnableMethodSecurity`) enforced per permission
- **System configuration & password policy** — runtime-configurable settings and a centrally enforced password policy, both exposed via REST for the admin UI
- **Centralized exception logging** — `GlobalExceptionHandler` persists unhandled exceptions to the database (toggleable via the `EXCEPTION_LOG_TO_DB` system config key) and exposes them through `ExceptionLogController` for the admin exception-log viewer
- **Audit logging** — an append-only audit trail written asynchronously off the request thread by `AuditLogWriter` on a dedicated executor, exposed read-only via `AuditLogController` (`/audit-log`); enabled/disabled and retention-purged (nightly) via the `AUDIT_LOG_ENABLED` / `AUDIT_LOG_RETENTION_DAYS` system config keys. Entries have a category: `ENTITY` (create/update/delete from `AbstractBaseService` and from the candidate-profile, job-alert, saved-job and notification services), `SECURITY` (login, logout, OTP, password events) and `ACTIVITY` (a short list of user actions that are not data changes: viewing a job posting or application, downloading a CV, offer letter or report, mark-all-notifications-read). `ACTIVITY` entries are only written for signed-in users, so every entry has the actor's email in `created_by`; the audit search and report filter by email with a partial, case-insensitive match. Each entry also stores the client's IP, city, country, device type, OS, browser and raw User-Agent
- **Session management** — active JWT sessions are tracked in `UserSession` with the login's IP, city, country, device type, OS and browser (plus live online-user and guest counts, taken from open SSE streams: `/notification/stream` for logged-in users, `/presence/guest-stream` for guests, pushed to admins on `/session/stream`), searchable per user and force-logout-able individually or globally via `UserSessionController` (`/session`). `GET /session/guests` lists the guests currently online (id, IP, city, country, device, OS, browser, connected-at) from an in-memory record kept by `SseEmitterRegistry`, which empties on restart. `PUT /profile/session-location` lets a signed-in user refine the city/country of their own current session once the browser's exact position is known
- **Google avatar fetching** — on Google Sign-In, the user's profile picture is downloaded and stored asynchronously (`GoogleAvatarFetcher`) so it doesn't delay login
- **User profile self-service** — view/update own profile and OTP-verified self password change, separate from admin-initiated flows (`ProfileController`, `/profile`)
- **Data archiving & retention** — runtime-configurable per-table archive policies (source table, archive schema/table, date column, retention days, optional SQL where-condition), run on a nightly schedule or on demand, with the archived data browsable through the admin UI (`ArchiveConfigController`, `GenericArchiveEngine`, `ArchiveScheduler`, `/archive-config`)
- **Database backup & restore** — database-independent logical backups (plain JDBC, so no `pg_dump`/`expdp` is needed and the same code runs on H2, PostgreSQL and Oracle): create on demand or on a schedule, upload, download, delete and restore from the admin UI, with a SHA-256 checksum per backup, a safety backup before every restore, and an all-or-nothing restore. See [Database Backup & Restore](#database-backup--restore)
- **Organizations & job circulars** — organization profiles (`OrganizationController`, `/organization`) and organization types (`OrganizationTypeController`, `/organization-type`) backing job postings; job circulars can be linked to a `Organization` or carry free-text organization details for backward compatibility (`JobCircularController`)
- **Recruiter self-registration** — prospective recruiters submit an organization/application request that an admin reviews and approves or rejects, promoting the requester into a recruiter role on approval (`RecruiterApplicationController`, `/recruiter-application`)
- **Candidate profiles & CV generation** — structured candidate profiles (education, work experience, skills, certifications, projects, languages) that can be rendered to a downloadable PDF CV via HTML-to-PDF (`CandidateProfileController`, `/candidate-profile`, `CvGenerationService`, `HtmlToPdfRenderer`)
- **CV completeness score** — `ProfileCompletenessCalculator` scores a profile out of 100 and returns it as `completeness` on the profile response: basic info 30 (headline 10, summary 10, phone 5, address 5 — phone and address are read from the user account because the CV prints them from there), work experience 20, education 15, skills 15 (three or more), languages 5, certifications 5, projects 5, LinkedIn/portfolio link 5. 80 or more is `COMPLETE` (CV ready), 50 or more `GOOD`, otherwise `INCOMPLETE`. It is advisory; applying is not blocked
- **CV–job match for recruiters** — `CvJobMatchCalculator` compares a candidate's profile with a job circular: skills 45 (the job's comma-separated `skills`, matched against the candidate's skills or any text in their work, project and certification entries), experience 25 (first number in the job's `experience` text versus the candidate's merged work history), education 15 (Diploma/Bachelor/Master/PhD detected in the job's requirement text versus the candidate's highest degree), requirement keywords 15. Only the parts the job actually specifies count; the result is a percent plus matched and missing skills and a per-part breakdown. The percent is stored on `Application.matchScore` when the candidate applies (list column and `matchScore_gte` filter) and recomputed from the current profile by `GET /application/{id}/match` (recruiters and admins only). Matching is by text, not meaning, so synonyms such as "Node" and "Node.js" are not equated
- **Job applications & hiring pipeline** — candidates apply to a job circular with a resume or generated CV, with status history tracked per application (`ApplicationController`, `/application`, `ApplicationStatusHistory`)
- **Interview scheduling & feedback** — scheduling interviews against an application and recording structured interviewer feedback (`InterviewController`, `/interview`)
- **Offers & offer letters** — creating and sending offers with a PDF offer letter (via the shared `HtmlToPdfRenderer`) and recording the candidate's response (`OfferController`, `/offer`)
- **Onboarding tasks** — post-hire onboarding checklists tracked per candidate (`OnboardingTaskController`, `/onboarding-task`)
- **In-app notifications & admin broadcast** — a generic per-user notification pipeline (`NotificationEvent` → `NotificationPublisher` → `NotificationListener`, async, after-commit) drives an unread-count badge and toasts across the app; delivered via poll (`/notification/poll`) and a live SSE stream (`/notification/stream`, `SseEmitterRegistry`, 5s heartbeat on its own `sse-keepalive` threads) so every notification type gets real-time push with no per-type code. Admins can also compose and send an ad-hoc notification — to all users, a role, or specific users, optionally also by email — gated by dedicated `notification-broadcast:read`/`write` permissions (`NotificationController`, `NotificationBroadcastController`, `/notification`, `/notification-broadcast`)
- **Saved jobs & job alerts** — candidates can bookmark job circulars (`SavedJobController`, `/saved-job`) and configure alerts for new matching postings (`JobAlertController`, `/job-alert`)
- **AI-assisted job posting** — "auto-fill by AI" drafts job posting fields from a short prompt via the Google Gemini API, with retry/backoff on transient failures (`JobPostingAiServiceImpl`)
- **Recruitment analytics** — aggregate metrics across postings, applications, and hiring stages for dashboards (`AnalyticsController`, `/analytics`)
- **Downloadable reports** — JasperReports-based PDF/Excel reports (job posting, application, MCQ result, MCQ violation, audit log — the audit report lists the user's email, IP, city, country, device, OS and browser), each independently permission-gated and filterable via the same `_like`/`_gte`/`_eq` query-param convention as the regular search endpoints (`ReportController`, `/report/{reportKey}/generate`, `ReportServiceImpl`, `.jrxml` templates under `resources/reports/`)
- **Client details on every record** — `BaseEntity` carries created/updated terminal (IP), location, device and User-Agent next to the existing created/updated by/on fields, filled in by `AbstractBaseService` and the audit, exception-log, MCQ-violation and stored-file writers. The frontend sends the visitor's place as `X-Client-City` / `X-Client-Country` headers (URL-encoded); `RequestUtils` / `ClientInfo` read those and parse device type, OS and browser out of the User-Agent. Location is whatever the client reports and is not verified server-side
- **Auto-seeded reference data** — roles, permissions, user groups, a password policy, system config, and two starter accounts are seeded on startup (`seed/` package)
- **API documentation** — interactive Swagger UI via springdoc-openapi
- **Multi-database support** — Spring profiles for H2 (dev, in-memory), PostgreSQL (prod), and Oracle

## Tech Stack

| Layer               | Technology |
|----------------------|------------|
| Language / runtime     | Java 21 |
| Framework              | Spring Boot 3.3.13 (Web, Data JPA, Security, Mail, JDBC) |
| Auth                   | JWT ([jjwt](https://github.com/jwtk/jjwt) 0.12.x) + Google Identity Services |
| Persistence             | Spring Data JPA / Hibernate — H2, PostgreSQL, or Oracle |
| API docs               | springdoc-openapi (Swagger UI) |
| Object mapping          | ModelMapper |
| PDF generation           | openhtmltopdf (HTML → PDF for generated CVs and offer letters) |
| Reporting               | JasperReports (PDF/Excel downloadable reports) |
| AI content assist        | Google Gemini API (job posting auto-fill) |
| Build                  | Gradle (wrapper included) |
| Boilerplate reduction    | Lombok |

## Prerequisites

- JDK 21
- No local Gradle install needed — use the included wrapper (`./gradlew` / `gradlew.bat`)
- A database if not using the default in-memory H2 profile (PostgreSQL or Oracle)
- Gmail API OAuth credentials for outbound email (OTP / account-setup emails) — see [Configuration](#configuration)
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
| `MOBILE_ORIGINS`       | No       | Comma-separated origins of the Capacitor Android app allowed by CORS. Defaults to `https://localhost,capacitor://localhost` |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | No | Firebase service-account key (raw JSON, or the same JSON base64-encoded) used to send FCM push notifications to the mobile app. Push is disabled if unset |
| `GEMINI_API_KEY`       | No       | Google Gemini API key (free tier, from [Google AI Studio](https://aistudio.google.com/apikey)) powering "auto-fill by AI" on the job posting form. Feature returns a 503 if unset |
| `GEMINI_MODEL`         | No       | Model used for AI features (job posting auto-fill, MCQ generation). Defaults to `gemma-4-26b` |
| `TOKEN_EXPIRY_HOURS`    | No       | How long a login session (JWT) stays valid, in hours. Defaults to `24` |
| `DB_URL`               | Prod/Oracle only | JDBC URL — see [Database Profiles](#database-profiles) for defaults |
| `DB_USERNAME`          | Prod/Oracle only | Database username |
| `DB_PASSWORD`          | Prod/Oracle only | Database password |
| `BACKUP_DIR`           | No       | Folder where database backups are written (default `./backups`, git-ignored). In Docker or on a host with an ephemeral disk, mount a persistent volume here or the backups disappear with the container |
| `DRIVE_REFRESH_TOKEN`  | No       | OAuth refresh token (scope `drive.file`) used to upload backups to Google Drive. Drive upload is unavailable if unset — see [Database Backup & Restore](#database-backup--restore) |
| `DRIVE_FOLDER_NAME`    | No       | Name of the Drive folder the backups go into. Defaults to `E-Recruitment Backups` |

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

A public, unauthenticated health check is exposed via Spring Boot Actuator at `http://localhost:8041/e-recruitment/actuator/health` (returns `{"status":"UP"}`, including datasource connectivity; used as the deploy pipeline's post-deploy health check).

## Project Structure

```
src/main/java/com/bd/erecruitment/
├── controller/         REST controllers (Authentication, User, Role, Permission, UserGroup,
│                        SystemConfig, PasswordPolicy, ExceptionLog, AuditLog, UserSession,
│                        Profile, ArchiveConfig, Organization, OrganizationType, JobCircular,
│                        RecruiterApplication, CandidateProfile, Application, Interview, Offer,
│                        OnboardingTask, Notification, NotificationBroadcast, SavedJob, JobAlert,
│                        Analytics, Report, DatabaseBackup, DatabaseRestore)
├── service/             Business logic interfaces (incl. UserSessionService,
│                        CvGenerationService)
│   └── impl/             Implementations (incl. AuditLogServiceImpl, GoogleAvatarFetcher,
│                          ArchiveConfigServiceImpl, JobPostingAiServiceImpl, HtmlToPdfRenderer,
│                          NotificationBroadcastServiceImpl, AdminMessageEmailSender)
├── retention/            Data archiving/retention engine (GenericArchiveEngine, ArchiveScheduler)
│                        and DatabaseBackupScheduler (scheduled backups)
├── backup/               Database backup engine (DatabaseExporter, DatabaseRestorer, RestoreState,
│                          GoogleDriveClient)
├── audit/               Audit action constants, AuditLogWriter (async), exemption annotations
│                        (@AuditExempt, @AuditIgnore)
├── notification/         Generic notification pipeline (NotificationEvent, NotificationPublisher,
│                          NotificationListener, SseEmitterRegistry, NotificationHeartbeatScheduler)
├── repository/          Spring Data JPA repositories
├── entity/               JPA entities (incl. AuditLog, UserSession)
├── dto/
│   ├── req/               Request DTOs
│   ├── res/               Response DTOs
│   └── report/            One row DTO per Jasper report (field names must match the .jrxml's <field> declarations)
├── security/             Spring Security config, JWT entry point
├── filter/               JWT authentication filter, RestoreMaintenanceFilter (503 while a restore runs)
├── util/                 JWT utility, response wrapper helpers, request utils (RequestUtils, ClientInfo)
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
├── templates/email/             HTML email templates (signup OTP, forgot/change password OTP, account setup)
└── reports/                     JasperReports .jrxml templates (one per report key)
```

## Database Backup & Restore

Backups are logical, not native dumps: `DatabaseExporter` reads every table in one read-only snapshot transaction and streams it into a single `.zip` (one `data/<table>.ndjson` file per table, one JSON object per row, plus a `manifest.json` with each table's columns and JDBC types). The file is written as `.part` and renamed only when complete, and its SHA-256 is stored with the backup record. The `database_backup` table itself is excluded, so the backup history survives a restore.

| Endpoint | Authority | Purpose |
|----------|-----------|---------|
| `GET /backup`, `GET /backup/{id}/download` | `backup:read` | List backups, download one |
| `POST /backup`, `POST /backup/upload` | `backup:write` | Start a backup (runs in the background), upload a `.zip` made elsewhere |
| `DELETE /backup/{id}` | `backup:delete` | Delete a backup and its file |
| `GET /backup-restore/status` | `backup-restore:read` | Progress and outcome of the latest restore |
| `POST /backup-restore/{id}` | `backup-restore:write` | Restore; body `{"confirmation":"RESTORE"}` |

Restore is under its own path so it needs its own authority rather than `backup:write`. All five authorities are seeded, and only `SUPER_ADMIN` has them until they are granted to a role. A backup contains password hashes and personal data, so keep that grant narrow and store downloads securely.

**Scheduled backups** are controlled from the System Config page and take effect without a restart (the scheduler checks every minute):

| Key | Default | Meaning |
|-----|---------|---------|
| `DB_BACKUP_SCHEDULE_ENABLED` | `Y` | Turns scheduled backups on or off |
| `DB_BACKUP_CRON` | `0 0 2 * * *` | 6-field cron (second minute hour day month weekday), Asia/Dhaka time; the default is every day at 02:00 |
| `DB_BACKUP_KEEP_COUNT` | `7` | Scheduled backups kept (a count, not days). Older scheduled backups are deleted; manual, uploaded and pre-restore backups never are |
| `DB_BACKUP_DRIVE_ENABLED` | `Y` | When `Y`, manual and scheduled backups are also uploaded to Google Drive, but only if `DRIVE_REFRESH_TOKEN` is set (see below) |

The seeder only inserts missing keys, so a database that already ran an earlier version keeps its old values. A failed scheduled backup notifies everyone with `backup:write` and all super admins. An invalid cron is logged once and scheduled backups pause until it is fixed.

**Restore** replaces all data with the backup's contents:

1. The request itself is only rejected for a wrong confirmation word, or a backup that is not `COMPLETED` or whose file is missing, so it answers at once even for a very large file. The slow checks then run in the background job, in a `VALIDATING` phase that does not lock anyone out: the file must pass its checksum, and every table and column in it must exist in the current schema (a circular foreign-key dependency also rejects it). A backup that fails these ends with phase `FAILED` and the reason in `message`, and nothing has changed.
2. A safety backup (`pre-restore-<time>.zip`) of the current data is taken first, so a restore can be undone by restoring that file.
3. In one transaction, every table is emptied in reverse foreign-key order and reloaded in foreign-key order. Any failure rolls back and leaves the current data untouched. Tables that exist in the database but not in the backup are emptied.
4. After commit, every sequence is restarted above the highest `id` so new inserts do not collide, and the user-details, permission and system-config caches are cleared.

Once validation passes the phase becomes `RUNNING`, and only then does `RestoreMaintenanceFilter` answer every request except `/backup-restore/status` and `/actuator/health` with `503`. Sessions come from the restored data, so users are normally signed out afterwards. A server restart during a backup marks it `FAILED` on the next start.

**Google Drive copy.** With `DB_BACKUP_DRIVE_ENABLED` set to `Y` (the default) and `DRIVE_REFRESH_TOKEN` configured (without the token nothing is uploaded), every manual and scheduled backup is also uploaded to Google Drive, so a copy survives the loss of the server's disk. Uploaded and pre-restore backups are not uploaded automatically, but any completed backup can be sent with the cloud button in the UI (`POST /backup/{id}/drive-upload`), which also retries a failed upload.

- **Setup:** enable the Google Drive API in the same Google Cloud project as the Gmail client, put `https://www.googleapis.com/auth/drive.file` on the OAuth consent screen, publish the app (in "Testing" status refresh tokens expire after 7 days), and create a refresh token in the OAuth Playground for the Google account that should hold the backups, using your own OAuth credentials and the `drive.file` scope. The client ID and secret are the existing `GMAIL_CLIENT_ID` / `GMAIL_CLIENT_SECRET`.
- **Folder:** the `drive.file` scope only reaches files the app created itself, so the app finds or creates a folder named `E-Recruitment Backups` (override with `DRIVE_FOLDER_NAME`) rather than using a folder you made by hand. Files are uploaded with Drive's resumable upload and are owned by the token's account.
- **Status:** each backup row shows `UPLOADING`, `UPLOADED` or `FAILED` for Drive. A failure (including a revoked token) notifies everyone with `backup:write` and all super admins, and the local backup is kept either way. Deleting a backup, manually or by the keep-count pruning, also deletes its Drive copy.
- **Plain upload:** the `.zip` is uploaded as it is, and it holds password hashes and personal data. Use a private account and do not share the folder.

**Performance notes.** Backups stream row by row, so memory stays flat except for a row that holds a large file: each binary cell is read whole and Base64-encoded, so a 100 MB file needs roughly 230 MB of heap at that moment. Base64 also makes binary data about a third larger, and PDFs and JPEGs barely compress. Restore deletes and reloads every table in one transaction, so it is slow on millions of rows and the system answers `503` for that time; on PostgreSQL the `postgres` profile enables `reWriteBatchedInserts` to speed the reload. Uploading a backup through the app is limited to 120 MB (`spring.servlet.multipart`), and the Drive upload is a single streaming request that starts over if it fails. Past a few GB of data, native `pg_dump`/`pg_restore` is the better tool.

The backup and restore code is tested on H2 only. The Drive client is tested against a fake Google server, never against real Google. On PostgreSQL and Oracle the foreign-key ordering and the sequence reset use each database's own syntax, so try a restore on a copy of the data before relying on it in production.

## Testing

`src/test` currently holds the database backup tests (`DatabaseExporterTest`, `DatabaseRestorerTest`, `GoogleDriveClientTest`), which run with no Spring context: export of typed values, a full restore round trip on in-memory H2 including a sequence reset, rollback when a restore fails, rejection of a schema mismatch, and the Drive client (token exchange, folder create or reuse, resumable upload, delete, revoked-token message) against a local fake Google server. Run them with `./gradlew test`. Apart from these, the rest of the application has no automated tests, so the deploy pipeline's test step checks only the backup code. The JUnit 5, Spring Boot Test and Spring Security Test dependencies are in `build.gradle`, so more tests can be added under `src/test/java`.

## Docker

A multi-stage `Dockerfile` is included (Gradle build stage → `eclipse-temurin:21-jre-alpine` runtime), exposing port `8041`:

```bash
docker build -t e-recruitment-backend .
docker run -p 8041:8041 --env-file .env e-recruitment-backend
```

Provide the [required environment variables](#configuration) via `--env-file` or `-e` flags.

## Deployment

A GitHub Actions pipeline (`.github/workflows/deploy.yml`) runs on every push to `master`: run tests → build the Docker image and scan it for critical vulnerabilities (Trivy) → push it to GitHub Container Registry → trigger a deploy on the hosting provider via its deploy-hook webhook (stored as the `RENDER_DEPLOY_HOOK_URL` repo secret) → poll `/actuator/health` (via the `HEALTHCHECK_URL` repo secret) until the new deploy is confirmed healthy. The provider pulls the image directly from the registry rather than being connected to this Git repo. Set the environment variables from [Configuration](#configuration) (plus `SPRING_PROFILES_ACTIVE=prod` and PostgreSQL credentials) once in the hosting provider's dashboard — they aren't managed through this pipeline.
