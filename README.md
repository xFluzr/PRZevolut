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

## Uruchomienie (Docker Compose)

Najszybszy sposób na postawienie backendu z PostgreSQL:

```bash
cp .env.example .env
docker compose up --build
```

API będzie dostępne pod adresem `http://localhost:8000` (dokumentacja: `/docs`).

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
