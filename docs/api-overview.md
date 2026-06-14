# API Overview — PRZevolut Backend

Wersja API: **v1**  
Data aktualizacji: **2026-06-14**  
Base URL (produkcja): `https://przevolut.onrender.com`  
Base URL (lokalnie): `http://10.0.2.2:8000` (Android Emulator) | `http://localhost:8000`  
Dokumentacja interaktywna: `GET /docs` (Swagger UI) | `GET /redoc` (ReDoc)

---

## Autentykacja

API używa **JWT Bearer Token** (OAuth2PasswordBearer).

```
Authorization: Bearer <access_token>
```

- **access_token** — ważny 15 minut (HS256)
- **refresh_token** — ważny 30 dni (cookie lub body)
- Odświeżanie: `POST /auth/refresh` z `refresh_token` w body

---

## Endpointy

### Auth — `/auth`

| Metoda | Ścieżka | Auth | Opis |
|--------|---------|------|------|
| POST | `/auth/register` | — | Rejestracja (email + hasło ≥ 8 znaków) → para tokenów JWT |
| POST | `/auth/login` | — | Logowanie → access_token + refresh_token |
| POST | `/auth/refresh` | refresh_token | Rotacja refresh tokena → nowy access_token |
| POST | `/auth/logout` | JWT | Unieważnienie refresh tokena |
| GET | `/auth/me` | JWT | Profil zalogowanego użytkownika |
| PATCH | `/auth/password` | JWT | Zmiana hasła (wymaga podania aktualnego hasła) |

**Przykład rejestracji:**
```bash
curl -X POST https://przevolut.onrender.com/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "SecurePass1"}'
```

**Przykład logowania:**
```bash
curl -X POST https://przevolut.onrender.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "SecurePass1"}'
# Zwraca: {"access_token": "...", "refresh_token": "...", "token_type": "bearer"}
```

---

### Kursy — `/rates`

| Metoda | Ścieżka | Auth | Opis |
|--------|---------|------|------|
| GET | `/rates` | JWT | Aktualne kursy (PLN base) — ostatni snapshot NBP |
| GET | `/rates/history` | JWT | Historia kursu: `?code=EUR&days=14` (1–365 dni roboczych) |
| GET | `/rates/{currency}` | JWT | Aktualny kurs pojedynczej waluty (np. `/rates/EUR`) |

**Parametry `/rates/history`:**
- `code` (wymagany) — kod ISO 4217, np. `EUR`, `USD`, `GBP`
- `days` (opcjonalny, domyślnie: 14, max: 365) — liczba dni roboczych historii

**Przykład odpowiedzi `/rates`:**
```json
{
  "fetched_at": "2024-01-15T09:00:00Z",
  "base": "PLN",
  "rates": [
    {"code": "EUR", "rate": 4.312, "name": "euro"},
    {"code": "USD", "rate": 3.987, "name": "dolar amerykański"},
    {"code": "GBP", "rate": 5.021, "name": "funt szterling"}
  ]
}
```

---

### Alerty — `/alerts`

| Metoda | Ścieżka | Auth | Opis |
|--------|---------|------|------|
| GET | `/alerts` | JWT | Lista alertów zalogowanego użytkownika |
| POST | `/alerts` | JWT | Utwórz alert |
| PATCH | `/alerts/{id}` | JWT | Edytuj alert |
| DELETE | `/alerts/{id}` | JWT | Usuń alert |

**Body POST /alerts:**
```json
{
  "currency_code": "EUR",
  "direction": "above",
  "threshold": 4.40
}
```

**Kierunek:** `"above"` (kurs powyżej progu) | `"below"` (kurs poniżej progu)

---

### Urządzenia — `/devices`

| Metoda | Ścieżka | Auth | Opis |
|--------|---------|------|------|
| POST | `/devices/register` | JWT | Rejestracja/aktualizacja tokena FCM urządzenia |

> **Uwaga:** Jeśli token FCM już istnieje w bazie, endpoint aktualizuje powiązanego użytkownika — nie tworzy duplikatu.

**Body:**
```json
{
  "fcm_token": "dGVzdF90b2tlbl8xMjM...",
  "platform": "android"
}
```

**Odpowiedź (201 Created):**
```json
{
  "id": 1,
  "user_id": 42,
  "fcm_token": "dGVzdF90b2tlbl8xMjM...",
  "platform": "android",
  "created_at": "2026-06-14T10:00:00Z"
}
```

---

### Health — `/health`

| Metoda | Ścieżka | Auth | Opis |
|--------|---------|------|------|
| GET | `/health` | — | Healthcheck dla Render.com |

**Odpowiedź:**
```json
{"status": "ok", "version": "1.0.0", "db": "connected"}
```

---

## Kody błędów

| Kod | Opis | Przykład |
|-----|------|----------|
| 400 | Błędne dane wejściowe | Brakujące pole w body |
| 401 | Brak lub nieważny token | Wygasły access_token |
| 403 | Brak uprawnień | Edycja cudzego alertu |
| 404 | Zasób nie istnieje | Alert o podanym ID nie istnieje |
| 409 | Konflikt | E-mail już zarejestrowany |
| 422 | Błąd walidacji Pydantic | Hasło krótsze niż 8 znaków |
| 429 | Rate limit przekroczony | Zbyt wiele żądań do /auth/* |
| 500 | Błąd serwera | Błąd bazy danych |

---

## Interaktywna dokumentacja

Po uruchomieniu backendu lokalnie:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc
- OpenAPI JSON: http://localhost:8000/openapi.json
