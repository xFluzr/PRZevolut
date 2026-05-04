# 💱 PRZevolut — Walutowy Skaner AR

> Mobilna aplikacja Android do skanowania i przeliczania cen za granicą w czasie rzeczywistym.

[![Backend CI](https://github.com/YOUR_ORG/PRZevolut/actions/workflows/backend_ci.yml/badge.svg)](https://github.com/YOUR_ORG/PRZevolut/actions)

---

## 📱 O Projekcie

**PRZevolut** to aplikacja na Androida, która umożliwia podróżującym błyskawiczne przeliczanie cen w sklepach za granicą. Wystarczy skierować aparat na cenę w sklepie, a aplikacja:

- **Odczyta cenę** za pomocą OCR (Google ML Kit)
- **Przeliczy ją** na złotówki (PLN) po aktualnym kursie
- **Wyświetli wynik** bezpośrednio na ekranie (nakładka AR)
- **Działa offline** — kursy są buforowane lokalnie w Room DB
- **Powiadomi Cię**, gdy kurs waluty osiągnie Twój target

---

## 🏗️ Architektura Projektu

```
PRZevolut/
├── android/          # Aplikacja Android (Kotlin, MVVM, Jetpack)
├── backend/          # REST API (Python, FastAPI, PostgreSQL)
├── docs/             # Dokumentacja, User Stories, Play Store
└── .github/          # CI/CD, szablony Issues
```

### Tech Stack

| Warstwa | Technologia |
|---------|-------------|
| Android | Kotlin, Jetpack (Room, CameraX, Navigation, Hilt) |
| OCR | Google ML Kit Text Recognition |
| API Client | Retrofit 2 + OkHttp |
| Backend | Python 3.12, FastAPI |
| Baza danych (serwer) | PostgreSQL + SQLAlchemy + Alembic |
| Autoryzacja | JWT (python-jose) + bcrypt |
| Kursy walut | NBP Open API |
| CI/CD | GitHub Actions |
| Hosting | Render.com |

---

## 🚀 Szybki Start

### Backend

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Konfiguracja
cp .env.example .env
# Edytuj .env (SECRET_KEY, DATABASE_URL)

# Migracje
alembic upgrade head

# Start
uvicorn app.main:app --reload
```

Dokumentacja API dostępna pod: **http://localhost:8000/docs**

### Android

1. Otwórz folder `android/` w **Android Studio Hedgehog** (lub nowszy)
2. Zsynchronizuj Gradle (`File → Sync Project with Gradle Files`)
3. Uzupełnij `android/app/src/main/res/values/api_config.xml` (URL backendu)
4. Uruchom na emulatorze lub urządzeniu fizycznym (wymagany Android 8.0+)

---

## 👥 Zespół i Podział Ról

| Rola | Zakres |
|------|--------|
| 👤 **Product Lead & UX** | User Stories, Figma prototypy, dokumentacja Play Store, zarządzanie Issues |
| 👤 **Frontend Developer** | Aplikacja Android (Kotlin), CameraX, ML Kit, Room, UI |
| 👤 **Backend & DevOps** | FastAPI, PostgreSQL, JWT, CI/CD, hosting, testy API |

---

## 📋 Zarządzanie Projektem

- **Tablica zadań**: GitHub Issues (zakładka Projects)
- **Sprinty**: 2 Milestone'y — Sprint 1 (MVP) i Sprint 2 (Funkcje dodatkowe)
- **Pull Requesty**: Każda zmiana przez PR, code review przez pozostałych członków zespołu

---

## 📄 Licencja

Projekt akademicki — Politechnika / Wydział Informatyki.
