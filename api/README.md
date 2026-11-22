# FinTrack backend

Phase 1 provides ASP.NET Core Identity registration, JWT login, a profile endpoint,
and PostgreSQL storage. It targets .NET 10; Docker Compose uses PostgreSQL 16.
No finance features are included yet.

## Prerequisites

- .NET 10 SDK for local development and tests
- Docker with Compose for the container setup
- OpenSSL to generate secrets

## Start with Docker Compose

From this directory:

```sh
cp .env.example .env
```

Edit `.env`: set `POSTGRES_PASSWORD` to a unique strong random password and
`JWT_SIGNING_KEY` to a unique random signing key. For example, generate each
with `openssl rand -hex 32`. Never share or commit `.env`. The examples below
assume shell-compatible values without quotes, spaces, semicolons, or `$`.
Set `ASPNETCORE_ENVIRONMENT=Production` outside local development.

```sh
docker compose up --build -d
docker compose ps
curl -f http://localhost:8080/health
```

Compose waits for PostgreSQL, runs the committed EF migrations in the `migrate`
service, and starts the API only after migration succeeds. To run migrations
explicitly before starting the API:

```sh
docker compose up -d db
docker compose run --rm --build migrate
docker compose up -d --build api
```

Check `docker compose logs migrate` and `docker compose logs api` if startup
fails. The API connects to the `db` service, not `localhost`. The named
`postgres_data` volume persists database files across container restarts and
ordinary `docker compose down`; `docker compose down -v` **deletes all data**.
PostgreSQL's initial username and password are set only when the volume is
first initialized; changing `.env` does not change existing database credentials.

The Compose ports bind to localhost. Compose uses HTTP for local development;
do not expose it directly on a network. For production, terminate HTTPS at a
trusted TLS reverse proxy, keep the API and PostgreSQL private, and supply
secrets using your deployment's secret management. Do not send credentials or
tokens over HTTP on an untrusted network. No CORS origins are enabled by default.
The authentication limiter is in-memory and per remote IP per API instance
(10 registration/login requests combined per 15 minutes). Behind a proxy it
sees the proxy IP unless trusted forwarding is explicitly configured; use a
trusted edge/distributed limiter for multi-instance deployments.

## Run locally with HTTPS

With the Compose database running and `.env` configured as above:

```sh
set -a; . ./.env; set +a
export Database__Host=localhost Database__Port="$POSTGRES_PORT"
export Database__Name=fintrack Database__Username="$POSTGRES_USER" Database__Password="$POSTGRES_PASSWORD"
export Jwt__Issuer="$JWT_ISSUER" Jwt__Audience="$JWT_AUDIENCE" Jwt__SigningKey="$JWT_SIGNING_KEY"
dotnet restore FinTrack.sln
dotnet build FinTrack.sln
dotnet run --no-launch-profile --project FinTrack.Api -- --migrate
dotnet dev-certs https --trust
dotnet run --project FinTrack.Api --launch-profile https
```

The local API listens on `https://localhost:7043` and `http://localhost:5010`.
Use HTTPS for credentials; trust the development certificate only on your own
development machine. Android emulators/devices need an appropriate trusted
certificate and reachable host address; `localhost` refers to the device.
OpenAPI JSON is available only in Development at
`https://localhost:7043/openapi/v1.json`; Swagger UI is at
`https://localhost:7043/swagger`. With Compose in Development, open
`http://localhost:8080/swagger` (or use your configured `API_PORT`).
To test `/api/auth/me`, log in through Swagger UI, copy `accessToken`, and
paste it into the **Authorize** dialog.

The application also supports `ConnectionStrings__DefaultConnection` instead of
the individual `Database__*` values. Configuration and secret validation happen
at startup. JWTs use HS256 and expire after 15 minutes. The key must contain at
least 32 UTF-8 bytes and must be generated randomly. Five failed password checks
lock the account for 15 minutes. Passwords require at least 12 characters,
uppercase, lowercase, a digit, and a non-alphanumeric character.

## API examples

For the local Compose setup:

```sh
curl -i http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"person@example.com","password":"Strong-Password123!","displayName":"Person"}'

curl -i http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"person@example.com","password":"Strong-Password123!"}'

# Substitute the accessToken from the login response.
curl -i http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <accessToken>"

curl -i http://localhost:8080/health
```

Registration returns 201, invalid input 400, duplicate email 409, invalid
credentials 401, and rate-limited requests 429. `/me` requires a valid token.
Responses contain no password hashes. `/health` checks PostgreSQL connectivity
and returns 200 or 503; it does not apply or validate migrations.

## Tests and persistence

The integration tests require a **dedicated** PostgreSQL database; they apply
migrations and create test users but do not delete the database. For example,
with the Compose database running:

```sh
set -a; . ./.env; set +a
docker compose exec -T db createdb -U "$POSTGRES_USER" fintrack_test
export FINTRACK_TEST_CONNECTION="Host=localhost;Port=$POSTGRES_PORT;Database=fintrack_test;Username=$POSTGRES_USER;Password=$POSTGRES_PASSWORD"
dotnet restore FinTrack.sln
dotnet build FinTrack.sln --no-restore
dotnet test FinTrack.sln --no-build --no-restore
```

If `fintrack_test` already exists, omit the `createdb` command. Do not point
the tests at a production database. To check the Docker volume, register a
user as above, restart PostgreSQL, and log in again with the same credentials:

```sh
docker compose restart db
docker compose ps
curl -i http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"person@example.com","password":"Strong-Password123!"}'
```

Wait for the database and API to become healthy if necessary.

## Creating future migrations

```sh
dotnet tool restore
dotnet tool run dotnet-ef migrations add MigrationName --project FinTrack.Api --startup-project FinTrack.Api --output-dir Migrations
```

Review and commit the generated migration, then apply it using the migration
command above before starting the updated API. `EnsureCreated` is not used.
