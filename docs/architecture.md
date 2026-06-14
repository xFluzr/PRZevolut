# Architektura — PRZevolut

## Diagram warstw

```
┌─────────────────────────────────────────────────────────────┐
│                    ANDROID APPLICATION                       │
│                                                             │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────┐   │
│  │  UI Layer │  │Domain Layer  │  │   Data Layer       │   │
│  │(Compose) │  │(Use Cases)   │  │(Room + Retrofit)   │   │
│  │          │  │              │  │                    │   │
│  │Scanner   │→ │ConvertPrice  │→ │RatesRepository     │   │
│  │Converter │  │GetRates      │  │  ├─ Room DB         │   │
│  │Alerts    │  │ManageAlerts  │  │  │  (cached_rates)  │   │
│  │Auth      │  │Authenticate  │  │  └─ Retrofit API   │   │
│  │Settings  │  │              │  │AlertsRepository    │   │
│  └──────────┘  └──────────────┘  └────────────────────┘   │
│        ↕                                    ↕              │
│  ┌──────────┐                  ┌────────────────────────┐  │
│  │ CameraX  │                  │  NetworkMonitor        │  │
│  │ ML Kit   │                  │  WorkManager           │  │
│  │ OCR      │                  │  DataStore             │  │
│  └──────────┘                  └────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           │ HTTPS + JWT
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    FASTAPI BACKEND                          │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    API Routes                        │  │
│  │  /auth    /rates    /alerts    /devices    /health   │  │
│  │  (/me, /password)  (/history, /{currency})           │  │
│  └──────────────────────────────────────────────────────┘  │
│                           │                                │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Services   │  │  SQLAlchemy  │  │   APScheduler    │  │
│  │             │  │  (async)     │  │                  │  │
│  │ nbp_client  │→ │  PostgreSQL  │  │ NBP aggregator   │  │
│  │ alert_engine│  │              │  │ (every 60 min)   │  │
│  │ push_sender │  │  Alembic     │  │                  │  │
│  └─────────────┘  └──────────────┘  └──────────────────┘  │
│                                              │              │
│                                      ┌──────────────┐      │
│                                      │  Firebase    │      │
│                                      │  FCM Push    │      │
│                                      └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                           │
                    ┌──────────────┐
                    │  api.nbp.pl  │
                    │  (publiczne  │
                    │   darmowe)   │
                    └──────────────┘
```

## Opis warstw — Android

### UI Layer (Jetpack Compose)
- Single-Activity z Navigation Compose
- Każdy ekran to Composable + ViewModel (MVVM)
- UiState<T> sealed class: Loading | Success | Error | Empty | Offline
- Material 3 theming z dynamic color

### Domain Layer (Clean Architecture)
- Use cases jako klasy Kotlin z operatorem `invoke`
- Modele biznesowe niezależne od Android/Room/Retrofit
- Interfejsy repozytoriów (dependency inversion)

### Data Layer
- **Room**: `AppDatabase` z `RateDao`, `AlertDao`, `UserPreferencesDao`
- **Retrofit**: `ApiService` ze spójnymi nazwami endpointów jak backend
- **Repository pattern**: `RatesRepository` zwraca `Flow` z Room, odświeża z API w tle
- **DataStore**: ustawienia użytkownika (waluta domyślna, biometria enabled)

### OCR Layer
- `PriceOcrAnalyzer`: `ImageAnalysis.Analyzer` z CameraX
- ML Kit Text Recognition V2 — **lokalnie na urządzeniu**
- Regex na ceny (wieloformatowy)
- Stabilizacja wyniku: debounce 500ms, wyświetl po 3 powtórzeniach

### Offline-first
- Room jako jedyne źródło prawdy (single source of truth)
- WorkManager: `RefreshRatesWorker` codziennie gdy wifi + charging
- `NetworkMonitor`: `ConnectivityManager.NetworkCallback` → `StateFlow<Boolean>`

---

## Opis warstw — Backend

### API Routes (FastAPI)
- OpenAPI auto-docs (`/docs`, `/redoc`)
- Dependency injection: `get_db`, `get_current_user`
- Rate limiting na `/auth/*` (slowapi)
- CORS konfigurowany z ENV

### Services
- **nbp_client**: `httpx` async, pobiera tabelę A z api.nbp.pl + historia dziennych notowań
- **rate_aggregator**: APScheduler scheduler, trigger co 60 min (konfigurowalne przez `RATE_REFRESH_INTERVAL_MINUTES`)
- **alert_engine**: iteruje aktywne alerty, sprawdza przekroczenie progu, wysyła FCM
- **push_sender**: `firebase-admin` → FCM API

### Database
- PostgreSQL (produkcja: Render.com managed DB)
- SQLAlchemy 2.0 async (`asyncpg` driver)
- Alembic dla migracji schematu
- Modele: `User`, `Alert`, `Rate`, `RefreshToken`, `DeviceToken`
- Rotacja refresh tokenów — przy każdym użyciu `/auth/refresh` stary token jest unieważzniany

---

## Przepływ danych — Skaner AR

```
Camera Frame
    │
    ▼
ML Kit Text Recognition (lokalnie)
    │
    ▼
PriceOcrAnalyzer.analyze()
    │ regex: r"[\d\s]+[,.][\d]{2}"
    ▼
CurrencyParser.detectCurrency()
    │ symbol/kod ISO lub fallback do wybranej waluty
    ▼
ConvertPriceUseCase.execute(amount, fromCurrency, "PLN")
    │ kurs z Room (cached_rates)
    ▼
ScannerViewModel.uiState (StateFlow)
    │
    ▼
ScannerScreen (Composable Canvas overlay)
    → wyświetl nakładkę na bounding boxie wykrytego tekstu
```

---

## Technologie

| Warstwa | Technologia |
|---------|-------------|
| Android | Kotlin 2.0, Jetpack Compose, Material 3 |
| DI | Hilt |
| Database (mobile) | Room 2.6 |
| Sieć (mobile) | Retrofit 2 + OkHttp + kotlinx.serialization |
| OCR | ML Kit Text Recognition V2 |
| Kamera | CameraX |
| Async | Coroutines + Flow |
| Powiadomienia | FCM (Firebase Cloud Messaging) |
| Ustawienia | DataStore Preferences |
| Tło | WorkManager |
| Bezpieczeństwo (mobile) | BiometricPrompt, EncryptedSharedPreferences, Android Keystore |
| Backend | FastAPI + Python 3.12 |
| ORM | SQLAlchemy 2.0 async |
| DB | PostgreSQL (produkcja) / SQLite (testy) |
| Scheduler | APScheduler |
| Push | firebase-admin (FCM) |
| Kursy | api.nbp.pl (darmowe, publiczne, Tabela A) |
| Rate limiting | slowapi |
| CI/CD | GitHub Actions |
| Hosting | Render.com |
| Konteneryzacja | Docker + Docker Compose |
