# PRZevolut — Walutowy Skaner AR

PRZevolut to mobilna aplikacja do skanowania i przeliczania cen za granicą w czasie rzeczywistym, działająca w trybie **offline-first**. Zaprojektowana z myślą o studentach, turystach i podróżnikach.

## Architektura

| Warstwa | Technologie |
|---------|-------------|
| Android | Kotlin, Jetpack Compose, Hilt, Room, CameraX, ML Kit, WorkManager |
| Backend | Python, FastAPI, SQLAlchemy, PostgreSQL, APScheduler, Firebase Admin |

## Struktura repozytorium

```
PRZevolut/
├── android/          # aplikacja mobilna
├── backend/          # API REST (FastAPI)
├── docs/             # dokumentacja projektowa
└── docker-compose.yml
```

## Uruchomienie backendu (Docker)

Wymagania: [Docker](https://docs.docker.com/get-docker/) i Docker Compose v2 (daemon musi być uruchomiony).

### 1. Przygotuj konfigurację

```bash
cd /ścieżka/do/PRZevolut
cp .env.example .env
```

Opcjonalnie wygeneruj bezpieczny klucz JWT:

```bash
openssl rand -hex 32
# Wklej wynik do SECRET_KEY= w pliku .env
```

### 2. Uruchom backend + PostgreSQL

```bash
docker compose up --build
```

Pierwsze uruchomienie zbuduje obraz backendu i wykona migracje Alembic.

### 3. Sprawdź, czy działa

| Co | URL |
|----|-----|
| API | http://localhost:8000 |
| Dokumentacja Swagger | http://localhost:8000/docs |
| Health check | http://localhost:8000/health |

```bash
curl http://localhost:8000/
curl http://localhost:8000/health
```

### 4. Przydatne komendy

```bash
# Uruchom w tle
docker compose up -d --build

# Logi backendu
docker compose logs -f backend

# Zatrzymaj
docker compose down

# Zatrzymaj i usuń dane bazy
docker compose down -v
```

### 5. Podłączenie aplikacji Android

W `android/local.properties`:

```properties
BASE_URL="http://10.0.2.2:8000"
```

`10.0.2.2` to localhost hosta widziany z emulatora Android. Na fizycznym telefonie użyj IP komputera w sieci LAN, np. `http://192.168.1.10:8000`.

## Uruchomienie lokalne (Backend)

```bash
cd backend
pip install -r requirements.txt -r requirements-dev.txt
cp .env.example .env
alembic upgrade head
uvicorn app.main:app --reload
```

## Uruchomienie lokalne (Android)

1. Otwórz folder `android/` w Android Studio.
2. Zsynchronizuj projekt z plikami Gradle.
3. Ustaw `BASE_URL` w `local.properties` (np. `http://10.0.2.2:8000` dla emulatora).
4. Skompiluj i uruchom na urządzeniu z dostępem do aparatu.

## Testy

```bash
# Backend
cd backend && pytest -v

# Android (unit)
cd android && ./gradlew :app:testDebugUnitTest
```
