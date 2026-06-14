# Changelog — PRZevolut

Wszystkie istotne zmiany w projekcie są dokumentowane w tym pliku.

Format oparty na [Keep a Changelog](https://keepachangelog.com/pl/1.0.0/).
Projekt stosuje [Semantic Versioning](https://semver.org/lang/pl/).

---

## [Unreleased]

### Dodano
- Endpoint `GET /auth/me` — profil zalogowanego użytkownika
- Endpoint `PATCH /auth/password` — zmiana hasła po walidacji aktualnego
- Endpoint `GET /rates/{currency}` — aktualny kurs pojedynczej waluty
- Rotacja refresh tokenów przy każdym użyciu `POST /auth/refresh`
- Domyślna wartość `days=14` w `/rates/history` (zakres 1–365)

### Planowane
- Rozpoznawanie paragonów (multi-line OCR)
- Widget ekranu głównego z aktualnym kursem EUR/PLN (Glance API)
- Tryb ciemny / jasny (manualne przełączanie motywu)
- Eksport historii kursów do CSV
- WebSocket — live rates stream

---

## [1.0.0] — 2026-06-14 — MVP Release 🚀

### Dodano
- Ekran skanera AR z nakładką przeliczonej ceny (CameraX + ML Kit OCR)
- Obsługa walut: EUR, USD, GBP, CHF, CZK → PLN
- Ekran Dashboard z aktualną tabelą kursów NBP
- System alertów walutowych (ustawianie targetów + powiadomienia push)
- Ekran Ustawień (domyślna waluta, biometria, o aplikacji)
- Rejestracja i logowanie przez e-mail + JWT
- Logowanie biometryczne (odcisk palca / twarz)
- Tryb offline — kursy buforowane lokalnie (Room DB)
- Baner offline z datą ostatniej aktualizacji kursów
- Historia ostatnich 20 przeliczeń
- Backend REST API na Render.com (5 endpointów)
- Cykliczne pobieranie kursów z NBP API (co 1 godzinę w dni robocze)

### Bezpieczeństwo
- Hasła hashowane bcrypt
- Tokeny JWT z czasem wygaśnięcia
- EncryptedSharedPreferences dla wrażliwych danych lokalnych

---

## [0.2.0] — 2026-05-28 — Sprint 2

### Dodano
- Alerty walutowe (UI + backend)
- Powiadomienia push o osiągnięciu targetu
- Logowanie biometryczne
- Historia przeliczeń
- Testy jednostkowe Android (10 testów)
- Testy integracyjne API (5 testów)

### Poprawiono
- Poprawa dokładności OCR przy słabym oświetleniu
- Optymalizacja zużycia baterii przez CameraX

---

## [0.1.0] — 2026-05-14 — Sprint 1 / MVP Core

### Dodano
- Inicjalizacja projektu (monorepo: android/ + backend/)
- Ekran skanera z podstawowym OCR
- Ekran Dashboard z kursami walut
- Rejestracja i logowanie (e-mail + hasło)
- Backend FastAPI z endpointami: /auth/register, /auth/login, /rates
- Integracja z NBP Open API
- Room DB — lokalny cache kursów
- Dockerfile + CI/CD (GitHub Actions)
