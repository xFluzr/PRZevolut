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

## Wdrożenie na Render.com

Projekt zawiera plik [`backend/render.yaml`](file:///c:/Users/Admin/Documents/Github/apkav2/PRZevolut/backend/render.yaml) z konfiguracją Blueprint — Render automatycznie tworzy serwis webowy i bazę danych PostgreSQL.

### 1. Wymagania wstępne

- Konto na [render.com](https://render.com)
- Repozytorium na GitHubie (publiczne lub połączone z Render)
- Plik `firebase-credentials.json` (klucz serwisowy Firebase Admin SDK)

### 2. Wdróż przez Blueprint

1. Zaloguj się na Render → **New → Blueprint**
2. Wskaż repozytorium i folder `backend/` jako root
3. Render wykryje `render.yaml` i zaproponuje utworzenie:
   - **Web Service** `przevolut-api` (Docker, plan Free)
   - **PostgreSQL** `przevolut-db` (plan Free)
4. Kliknij **Apply** — Render zbuduje obraz i wykona migracje Alembic automatycznie

### 3. Zmienne środowiskowe

`render.yaml` konfiguruje większość zmiennych automatycznie. Zmienną, którą **musisz dodać ręcznie** po deploymencie:

| Zmienna | Skąd wziąć | Uwagi |
|---------|-----------|-------|
| `DATABASE_URL` | Generowana automatycznie z `przevolut-db` | Ustawiana przez Blueprint |
| `SECRET_KEY` | Generowana automatycznie (`generateValue: true`) | Losowy 32-bajtowy klucz |
| `FIREBASE_CREDENTIALS_PATH` | ⚠️ Musisz dodać ręcznie | Ścieżka do pliku lub zawartość JSON |
| `CORS_ORIGINS` | `render.yaml` ustawia `przevolut.app` | Zmień jeśli masz inną domenę |

> [!IMPORTANT]
> Firebase credentials **nie mogą być commitowane** do repozytorium. Wgraj je jako Secret File na Render:
> Dashboard → Web Service → **Environment → Secret Files** → dodaj plik jako `firebase-credentials.json`.

### 4. Weryfikacja wdrożenia

Po zakończeniu buildu sprawdź:

```bash
# Health check
curl https://przevolut.onrender.com/health
# Oczekiwana odpowiedź: {"status":"ok","version":"1.0.0","db":"connected"}

# Swagger UI
open https://przevolut.onrender.com/docs
```

### 5. Ograniczenia planu Free

| Ograniczenie | Wartość |
|---|---|
| Uśpienie po nieaktywności | ~15 minut (cold start ~30s) |
| RAM Web Service | 512 MB |
| Baza PostgreSQL — wygaśnięcie | 90 dni bez aktywności |
| Bandwidth | 100 GB / miesiąc |

> [!TIP]
> Aby zapobiec uśpieniu serwisu, skonfiguruj zewnętrzny monitoring (np. UptimeRobot pingujący `/health` co 10 minut).

---

## Testy

```bash
# Backend
cd backend && pytest -v

# Android (unit)
cd android && ./gradlew :app:testDebugUnitTest
```
