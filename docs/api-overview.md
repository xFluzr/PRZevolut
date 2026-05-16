# API Overview — PRZevolut Backend

Wersja API: **v1**  
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
| POST | `/auth/register` | — | Rejestracja (email + hasło ≥ 8 znaków) |
| POST | `/auth/login` | — | Logowanie → access_token + refresh_token |
| POST | `/auth/refresh` | refresh_token | Odświeżenie access tokena |
| POST | `/auth/logout` | JWT | Unieważnienie refresh tokena |

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
| GET | `/rates` | JWT | Aktualne kursy (PLN base) ze wszystkich walut |
| GET | `/rates/history` | JWT | Historia kursu: `?code=EUR&days=30` |

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
| POST | `/devices/register` | JWT | Rejestracja tokena FCM urządzenia |

**Body:**
```json
{
  "fcm_token": "dGVzdF90b2tlbl8xMjM...",
  "platform": "android"
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

| Kod | Opis |
|-----|------|
| 400 | Błędne dane wejściowe (walidacja Pydantic) |
| 401 | Brak lub nieważny token |
| 403 | Brak uprawnień (np. edycja cudzego alertu) |
| 404 | Zasób nie istnieje |
| 409 | Konflikt (np. email już zarejestrowany) |
| 422 | Błąd walidacji (szczegóły w body) |
| 429 | Rate limit przekroczony (dotyczy /auth/*) |
| 500 | Błąd serwera |

---

## Interaktywna dokumentacja

Po uruchomieniu backendu lokalnie:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc
- OpenAPI JSON: http://localhost:8000/openapi.json
