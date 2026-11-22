# FinTrack

FinTrack keeps the Android client and its backend API in one repository:

```text
api/        ASP.NET Core API, tests, and Docker Compose configuration
interface/  Android app built with Kotlin, Jetpack Compose, and Gradle
```

## Android app

Open `interface/` in Android Studio, or build and run the checks from a terminal:

```sh
cd interface
./gradlew build
```

## Backend API

Use the .NET SDK and follow [`api/README.md`](api/README.md) for local setup,
Docker Compose, database configuration, and tests. The API integration tests
require a dedicated PostgreSQL test database:

```sh
cd api
dotnet build FinTrack.sln
dotnet test FinTrack.sln
```

Keep local `.env` files and secrets out of version control; `api/.env.example`
documents the required local configuration.
