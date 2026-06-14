# Changelog — PRZevolut

Wszystkie istotne zmiany w projekcie są dokumentowane w tym pliku.

Format oparty na [Keep a Changelog](https://keepachangelog.com/pl/1.0.0/).
Projekt stosuje [Semantic Versioning](https://semver.org/lang/pl/).

---

## [Unreleased]

### Dodano
- Docker Compose (PostgreSQL + backend)
- Clean Architecture w Android (Repository, UseCases, AR overlay)
- Testy jednostkowe i integracyjne (Android + backend)

### Zmieniono
- Ujednolicono strukturę backendu (usunięto legacy warstwę sync API)
- Scalono dokumentację i workflow CI

### Planowane
- Rozpoznawanie paragonów (multi-line OCR)
- Widget ekranu głównego z aktualnym kursem EUR/PLN
- Tryb ciemny / jasny

---

## [1.0.0] — 2026-06-XX — MVP Release 🚀

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

## [0.2.0] — 2026-05-XX — Sprint 2

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

## [0.1.0] — 2026-05-XX — Sprint 1 / MVP Core

### Dodano
- Inicjalizacja projektu (monorepo: android/ + backend/)
- Ekran skanera z podstawowym OCR
- Ekran Dashboard z kursami walut
- Rejestracja i logowanie (e-mail + hasło)
- Backend FastAPI z endpointami: /auth/register, /auth/login, /rates
- Integracja z NBP Open API
- Room DB — lokalny cache kursów
- Dockerfile + CI/CD (GitHub Actions)
