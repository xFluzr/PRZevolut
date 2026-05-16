# PRZevolut — Walutowy Skaner AR

PRZevolut to mobilna aplikacja do skanowania i przeliczania cen za granicą w czasie rzeczywistym, działająca w trybie **offline-first**. Zaprojektowana z myślą o studentach, turystach i podróżnikach.

## Architektura i Technologie
*   **Aplikacja Mobilna**: Android, Kotlin, Jetpack Compose, Hilt, Room, CameraX, ML Kit, WorkManager.
*   **Backend**: Python, FastAPI, SQLAlchemy, PostgreSQL, APScheduler, Firebase Admin.

## Struktura Repozytorium
*   `android/` - kod źródłowy aplikacji mobilnej
*   `backend/` - kod źródłowy serwera
*   `docs/` - dokumentacja projektowa, analityczna, oraz pliki dla Google Play

## Uruchomienie lokalnie (Backend)
1. `cd backend`
2. `pip install -r requirements.txt`
3. Skonfiguruj `.env` na podstawie `.env.example`
4. `alembic upgrade head`
5. `uvicorn app.main:app --reload`

## Uruchomienie lokalnie (Android)
1. Otwórz folder `android/` w Android Studio.
2. Zsynchronizuj projekt z plikami Gradle.
3. Utwórz plik `local.properties` na podstawie `.env.example` i podaj `BASE_URL`.
4. Skompiluj i uruchom na urządzeniu (wymagany dostęp do aparatu).
