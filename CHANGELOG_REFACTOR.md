# 🔄 PRZevolut — Refaktoryzacja: Architektura, Testy, Dostępność, AR Overlay

> **Autor:** @maul2  
> **Data:** 2026-06-12  
> **Branch:** `feature/clean-arch-ar-overlay`  
> **Dotyczy:** Android (`android/`) + Backend (`backend/`)

---

## 📋 TL;DR

Cztery duże bloki zmian w jednym branchu:

1. **Clean Architecture** — warstwa Repository + UseCases + Mapper + DI module
2. **Testy** — 24 nowe testy (unit + instrumented + backend)
3. **Dostępność (a11y)** — TalkBack, contentDescription, live regions
4. **AR Overlay** — nakładka AR rysująca przeliczone ceny na podglądzie kamery

**Żaden istniejący kod nie został usunięty.** Wszystkie zmiany są backward-compatible.

---

## 📁 Nowe pliki — Android

### Warstwa Domain

| Plik | Co robi |
|------|---------|
| `domain/repository/RateRepository.kt` | Interfejs repozytorium kursów walut. Warstwa domain nie zna implementacji (Room/Retrofit). |
| `domain/usecase/ConvertCurrencyUseCase.kt` | Use case przeliczania kwoty z waluty obcej na PLN. Waliduje argumenty (`amount >= 0`, `rate > 0`). |
| `domain/usecase/GetRatesUseCase.kt` | Use case zwracający `Flow<List<ExchangeRate>>` z repozytorium. |
| `domain/usecase/RefreshRatesUseCase.kt` | Use case odświeżania kursów z serwera. Zwraca `Result<Unit>`. |
| `domain/model/DetectedPrice.kt` | Data class ceny wykrytej przez OCR — zawiera `amount`, `currency`, `boundingBox` (pozycja na obrazie kamery). |

### Warstwa Data

| Plik | Co robi |
|------|---------|
| `data/repository/RateRepositoryImpl.kt` | Implementacja offline-first: Room = single source of truth, Retrofit = remote source. Mapuje DTO → Entity automatycznie. |
| `data/mapper/RateMapper.kt` | Extension functions: `RateEntity.toDomain()` i `RateResponse.toEntity(fetchedAt)`. Centralne miejsce mapowania. |

### Warstwa DI

| Plik | Co robi |
|------|---------|
| `di/RepositoryModule.kt` | Hilt `@Module` z `@Binds` — wiąże `RateRepository` (interfejs) z `RateRepositoryImpl`. Singleton. |

### Worker

| Plik | Co robi |
|------|---------|
| `worker/RateSyncWorker.kt` | `@HiltWorker` + `CoroutineWorker`. Synchronizuje kursy co **1 godzinę** gdy jest sieć. Max 3 retry z exponential backoff. Companion `enqueuePeriodicSync()` do łatwego uruchomienia. |

### AR Overlay

| Plik | Co robi |
|------|---------|
| `utils/PriceDetector.kt` | Analizuje wynik ML Kit (`Text`) i wyodrębnia ceny z bounding boxami. Obsługuje symbole (€$£Fr Kč) i kody ISO (EUR, USD, GBP itd.). Zwraca `List<DetectedPrice>`. |
| `ui/scanner/ArOverlayView.kt` | Custom `View` rysujący nakładkę AR na Canvas. Dla każdej wykrytej ceny rysuje: (1) pulsującą ramkę z narożnikami, (2) etykietę z ceną w PLN, (3) trójkątną strzałkę łączącą etykietę z ceną. Używa `ValueAnimator` do animacji pulsowania. |

### Testy Android

| Plik | Typ | Ilość testów | Co testuje |
|------|-----|:---:|---------|
| `test/.../ConvertCurrencyUseCaseTest.kt` | Unit | 9 | Przeliczanie walut, walidacja ujemnych kwot, zerowy kurs, duże kwoty, precyzja |
| `androidTest/.../RateDaoTest.kt` | Instrumented | 6 | Insert/retrieve, getLatestRate, deleteOldRates, empty DB, getLatestRates (1 per currency) |

---

## 📁 Nowe pliki — Backend

| Plik | Typ | Ilość testów | Co testuje |
|------|-----|:---:|---------|
| `backend/app/tests/__init__.py` | Config | — | Moduł Pythona dla pytest |
| `backend/app/tests/test_nbp_service.py` | pytest | 9 | **NBP Client:** sukces, HTTP error, timeout → graceful degradation. **Alert Engine:** above/below threshold, cooldown 6h, expired cooldown, invalid direction. |

---

## ✏️ Zmodyfikowane pliki

### `PRZevolutApp.kt`
- Implementuje `Configuration.Provider` (WorkManager + Hilt)
- Wstrzykuje `HiltWorkerFactory`
- Wywołuje `RateSyncWorker.enqueuePeriodicSync(this)` w `onCreate()`
- **Efekt:** kursy walut synchronizują się w tle co 1h, nawet gdy aplikacja jest zamknięta

### `SettingsViewModel.kt`
> ⚠️ **BUG FIX**

**Przed:** `logout()` czyścił klucze `jwt_token` i `refresh_token` w SharedPreferences `przevolut_prefs`.  
**Problem:** Token jest faktycznie zapisywany jako `access_token` w SharedPreferences `auth_prefs` przez `TokenManager`.  
**Po:** `logout()` wywołuje `tokenManager.clearToken()` — czyści właściwe dane.

```kotlin
// BYŁO (niepoprawne):
prefs.edit()
    .remove("jwt_token")       // ← ten klucz nie istnieje
    .remove("refresh_token")   // ← ten klucz nie istnieje
    .apply()

// JEST (poprawne):
tokenManager.clearToken()      // → auth_prefs / access_token
```

### `ScannerViewModel.kt`
- Dodano `ratesMap: StateFlow<Map<String, Double>>` — mapa `currency → mid` z Room Flow
- Używane przez `ArOverlayView` do konwersji cen w czasie rzeczywistym
- `WhileSubscribed(5000)` — nie trzyma Flow gdy UI jest w tle

### `ScannerFragment.kt`
- Integracja z `PriceDetector` — wyciąga ceny z bounding boxami z wyniku ML Kit
- Przekazuje wykryte ceny do `binding.arOverlay.updatePrices(prices, rates)`
- **a11y:** `announceForAccessibility()` — TalkBack ogłasza wykrytą cenę głosowo
- **a11y:** `contentDescription` na polu ręcznego wpisywania ceny

### `fragment_scanner.xml`
- Dodano `<ArOverlayView>` — nakładka AR na podglądzie kamery
- Dodano `<ChipGroup>` z chipami EUR/USD/GBP/CHF do wyboru waluty
- **a11y:** `accessibilityLiveRegion="polite"` na wyniku przeliczenia
- **a11y:** `contentDescription` na PreviewView i przycisku

### `item_rate.xml`
- `focusable="true"` na `MaterialCardView` — karta jest focusowalna przez TalkBack
- `importantForAccessibility="no"` na dzieciach (`tv_currency_code`, `tv_rate`, `tv_effective_date`) — TalkBack czyta jedną zagregowaną informację z karty zamiast trzech osobnych

### `RatesAdapter.kt`
- Dodano `contentDescription` na karcie:
  ```
  "EUR: kurs 4.3245 PLN, data 2026-06-12"
  ```
- TalkBack czyta pełny kontekst jednym zdaniem

### `DashboardFragment.kt`
- **a11y:** `announceForAccessibility("Tryb offline. Kursy z ...")` — TalkBack ogłasza zmianę stanu sieci

### `build.gradle.kts`
```diff
+ androidTestImplementation(libs.androidx.test.core)
+ androidTestImplementation(libs.room.testing)
+ androidTestImplementation(libs.coroutines.test)
```

### `gradle/libs.versions.toml`
```diff
+ room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
+ androidx-test-core = { group = "androidx.test", name = "core", version = "1.5.0" }
```

### `AndroidManifest.xml`
- Dodano `tools:node="remove"` do `WorkManagerInitializer` — **krytyczne!**
- Bez tego WorkManager inicjalizowałby się dwukrotnie (automatycznie + przez `Configuration.Provider`), co powoduje crash na starcie aplikacji

```diff
 <meta-data
     android:name="androidx.work.WorkManagerInitializer"
-    android:value="androidx.startup" />
+    android:value="androidx.startup"
+    tools:node="remove" />
```

---

## 🧱 Nowa struktura pakietów

```
com.przevolut/
├── data/
│   ├── local/          (bez zmian: AppDatabase, RateDao, RateEntity, TokenManager)
│   ├── mapper/         ← NOWY: RateMapper.kt
│   ├── remote/         (bez zmian: ApiService, AuthInterceptor, ApiModels)
│   └── repository/     ← NOWY: RateRepositoryImpl.kt
├── di/
│   ├── AppModule.kt    (bez zmian)
│   └── RepositoryModule.kt  ← NOWY
├── domain/
│   ├── model/
│   │   ├── Models.kt         (bez zmian)
│   │   └── DetectedPrice.kt  ← NOWY
│   ├── repository/
│   │   └── RateRepository.kt ← NOWY
│   └── usecase/               ← NOWY CAŁY PAKIET
│       ├── ConvertCurrencyUseCase.kt
│       ├── GetRatesUseCase.kt
│       └── RefreshRatesUseCase.kt
├── ui/
│   ├── dashboard/      (zmodyfikowane: RatesAdapter, DashboardFragment)
│   ├── scanner/
│   │   ├── ArOverlayView.kt   ← NOWY
│   │   ├── ScannerFragment.kt (zmodyfikowany)
│   │   └── ScannerViewModel.kt(zmodyfikowany)
│   └── settings/       (zmodyfikowany: SettingsViewModel)
├── utils/
│   ├── CurrencyParser.kt      (bez zmian)
│   └── PriceDetector.kt       ← NOWY
├── worker/                     ← NOWY CAŁY PAKIET
│   └── RateSyncWorker.kt
└── PRZevolutApp.kt            (zmodyfikowany)
```

---

## 🧪 Jak uruchomić testy

### Android — testy jednostkowe
```bash
cd android
./gradlew :app:testDebugUnitTest
```
Uruchomi m.in. `ConvertCurrencyUseCaseTest` (+ istniejące `CurrencyParserTest`, `ScannerViewModelTest`).

### Android — testy instrumentalne (wymaga emulatora/urządzenia)
```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```
Uruchomi `RateDaoTest` — testy Room DAO na in-memory bazie danych.

### Backend — pytest
```bash
cd backend
pip install pytest pytest-asyncio httpx
pytest app/tests/ -v
```

---

## ⚠️ Na co zwrócić uwagę przy review

1. **`RepositoryModule.kt`** — `@Binds` wymaga, żeby `RateRepositoryImpl` miał `@Inject constructor`. Jest ✅
2. **`RateSyncWorker`** — używa `@AssistedInject` (standard dla Hilt Workers). Wymaga `hilt-work` i `androidx-hilt-compiler` w dependencies — obydwa już były w projekcie ✅
3. **`PRZevolutApp`** — implementuje `Configuration.Provider`. Oznacza to, że WorkManager **nie inicjalizuje się automatycznie** przez `ContentProvider` — zamiast tego app dostarcza konfigurację ręcznie. Jeśli w `AndroidManifest.xml` jest domyślny `WorkManagerInitializer`, trzeba go usunąć:
   ```xml
   <provider
       android:name="androidx.startup.InitializationProvider"
       android:authorities="${applicationId}.androidx-startup"
       tools:node="merge">
       <meta-data
           android:name="androidx.work.WorkManagerInitializer"
           android:value="androidx.startup"
           tools:node="remove" />
   </provider>
   ```
4. **AR Overlay** — `ArOverlayView` rysuje na Canvas, więc nie wymaga OpenGL ani ARCore. Działa na każdym urządzeniu z kamerą.
5. **Testy backendowe** — mockują `httpx.AsyncClient`, nie wymagają działającej bazy danych ani serwera NBP.

---

## 📊 Statystyki

| Metryka | Wartość |
|---------|---------|
| Nowe pliki Kotlin | 12 |
| Nowe pliki Python | 2 |
| Zmodyfikowane pliki | 11 |
| Nowe testy | 24 |
| Naprawione bugi | 1 |
| Usunięte pliki | 0 |
| Breaking changes | 0 |
