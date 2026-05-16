# MVP vs Dodatki — PRZevolut

## Zakres MVP (Minimum Viable Product)

Funkcje, które **muszą działać** aby aplikacja spełniała swój cel i zaliczała checklistę prowadzącego.

### Android MVP

| Funkcja | Priorytet | Status |
|---------|-----------|--------|
| ScannerScreen z CameraX + ML Kit OCR | 🔴 KRYTYCZNE | MVP |
| Nakładka AR z przeliczoną ceną (Canvas) | 🔴 KRYTYCZNE | MVP |
| ManualConverterScreen | 🔴 KRYTYCZNE | MVP |
| Room: cached_rates (offline-first) | 🔴 KRYTYCZNE | MVP |
| NetworkMonitor + baner offline | 🔴 KRYTYCZNE | MVP |
| AuthScreen (login + rejestracja) | 🟠 WAŻNE | MVP |
| JWT + EncryptedSharedPreferences | 🟠 WAŻNE | MVP |
| AlertsListScreen + AlertEditScreen | 🟠 WAŻNE | MVP |
| SettingsScreen (waluta domyślna) | 🟠 WAŻNE | MVP |
| BiometricPrompt | 🟠 WAŻNE | MVP |
| WorkManager (codzienny refresh) | 🟡 POTRZEBNE | MVP |
| FCM powiadomienia push | 🟡 POTRZEBNE | MVP |

### Backend MVP

| Funkcja | Priorytet | Status |
|---------|-----------|--------|
| POST /auth/register, /auth/login | 🔴 KRYTYCZNE | MVP |
| GET /rates (z NBP) | 🔴 KRYTYCZNE | MVP |
| APScheduler — agregator NBP | 🔴 KRYTYCZNE | MVP |
| CRUD /alerts | 🟠 WAŻNE | MVP |
| Silnik alertów + FCM push | 🟡 POTRZEBNE | MVP |
| GET /rates/history | 🟡 POTRZEBNE | MVP |
| POST /auth/refresh (rotacja tokenów) | 🟡 POTRZEBNE | MVP |
| POST /devices/register | 🟡 POTRZEBNE | MVP |
| GET /health | 🟢 PROSTE | MVP |

---

## Dodatki / Faza 2 (poza MVP)

Funkcje wartościowe, ale **niebędące warunkiem działania MVP**.

### Android — Dodatki

| Funkcja | Opis |
|---------|------|
| Tryb AR z glasses (ARCore) | Nakładka na okulary AR zamiast ekranu telefonu |
| Wykres historii kursów | Interaktywny wykres (MPAndroidChart lub Compose Canvas) |
| Widget ekranu głównego | Glance API — miniprzelicznik na homescreen |
| Eksport kursów do CSV/PDF | Eksport historii alertów i kursów |
| Wiele profili walutowych | Szybkie przełączanie między zestawami walut |
| Tryb podróży | Autokonfiguracja waluty na podstawie lokalizacji GPS |
| OCR — tabele cen | Rozpoznawanie całych tablic cenowych |
| Udostępnianie przeliczonej ceny | Share intent — wyślij wynik do komunikatora |
| Dark/Light theme toggle | Manualne przełączanie motywu (domyślnie Material You) |
| Waluta kryptowalutowa | Integracja z CoinGecko API (BTC, ETH) |

### Backend — Dodatki

| Funkcja | Opis |
|---------|------|
| OAuth2 (Google Sign-In) | Logowanie przez Google zamiast email/hasło |
| WebSocket live rates | Stream kursów w czasie rzeczywistym |
| Rate limiting per-user | Fairuse quota na endpointy |
| Admin panel (FastAPI Admin) | Panel zarządzania użytkownikami i alertami |
| Eksport danych RODO | Endpoint GET /users/me/export |
| Multi-source aggregation | Łączenie NBP + ECB + inne źródła |
| A/B testing alerts | Testy różnych progów wyzwalania |

---

## Zasada upraszczania

> Jeśli w trakcie implementacji brakuje czasu — upraszczaj **alerty i push** (powiadomienie lokalne zamiast FCM, brak historii), ale **NIGDY nie upraszczaj skanera AR ani offline-first**. Te dwie funkcje są rdzeniem produktu i jedynym uzasadnieniem istnienia tej aplikacji jako aplikacji mobilnej.
