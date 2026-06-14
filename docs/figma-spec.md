# Specyfikacja Ekranów Figma — PRZevolut

## Wytyczne projektowe

- **Motyw:** Material 3 z dynamic color (Android 12+), fallback granatowo-złoty
- **Kolory podstawowe:**
  - Primary: `#1565C0` (niebieski — zaufanie, finanse)
  - Secondary: `#F9A825` (złoty — waluta)
  - Error: `#D32F2F`
  - Background (dark): `#0D1117`
  - Surface (dark): `#161B22`
- **Typografia:** Roboto (system Android), nagłówki: Roboto Medium
- **Corner radius:** 12dp (karty), 50dp (FAB, przyciski pill)
- **Elevation:** Material 3 tonal elevation

---

## Ekran 1: ScannerScreen

**Opis:** Pełnoekranowy podgląd kamery z nakładką AR

### Elementy UI (od góry do dołu):
1. **TopAppBar (półprzezroczysta):**
   - Tytuł: "PRZevolut" (logo)
   - Chip wyboru waluty: `[🇩🇪 EUR ▼]`
   - Ikona ustawień (prawy bok)

2. **Podgląd CameraX (pełnoekranowy, za TopAppBar)**

3. **Nakładka AR (Canvas nad kamerą):**
   - Bounding box: zaokrąglony prostokąt (stroke 2dp, kolor Secondary #F9A825)
   - Chip z wynikiem: `[≈ 18,49 PLN]` (kolor Surface + Primary tekst, shadow)
   - Pozycja: nad wykrytym tekstem

4. **BottomPanel (półprzezroczysty):**
   - Status: "Skanowanie..." / "Wykryto: 4,30 €" / "Brak ceny w kadrze"
   - Baner offline: `[⚠ Tryb offline — kursy z dnia 2024-01-15]` (kolor Warning)

### Stany:
- **Scanning:** animacja pulse na ikonie aparatu
- **Detected:** żółty bounding box + chip z wynikiem
- **Offline:** baner na dole, bounding box działa
- **Permission denied:** fullscreen ekran z przyciskiem "Udziel dostępu do aparatu"

---

## Ekran 2: ManualConverterScreen

**Opis:** Klasyczny kalkulator walutowy

### Elementy UI:
1. **TopAppBar:** "Przelicznik"

2. **Karta konwertera (padding 24dp):**
   ```
   ┌─────────────────────────────────┐
   │  [Kwota]          [EUR ▼]       │
   │  ─────────────────────────────  │
   │  [Wynik]          [PLN ▼]       │
   └─────────────────────────────────┘
   ```
   - Przycisk zamiany walut `⇅` w środku karty (FAB mini)
   - Kwota: `OutlinedTextField` z klawiaturą numeryczną
   - Waluta: `ExposedDropdownMenu` z flagą + kodem ISO

3. **Wynik:** duży tekst `4 312,00 PLN` (kolor Primary)

4. **Timestamp:** `Kurs z: 2024-01-15 09:00` (kolor OnSurfaceVariant, small)

5. **Baner offline** (gdy brak sieci)

### Stany: Loading | Success | Error | Offline

---

## Ekran 3: AlertsListScreen

**Opis:** Lista alertów walutowych użytkownika

### Elementy UI:
1. **TopAppBar:** "Alerty"

2. **Lista alertów (LazyColumn):**
   Każdy element:
   ```
   ┌────────────────────────────────────────┐
   │ 🇪🇺 EUR  >  4,40 PLN      [Aktywny ●] │
   │ Ostatnio: nigdy                   [>]  │
   └────────────────────────────────────────┘
   ```
   - Swipe-to-delete (gestura)
   - Klik → AlertEditScreen

3. **Empty state:** ilustracja + "Nie masz jeszcze alertów\nDotknij + aby dodać"

4. **FAB:** `[+ Nowy alert]` (Extended FAB, kolor Primary)

---

## Ekran 4: AlertEditScreen

**Opis:** Tworzenie/edycja alertu (Bottom Sheet lub osobny ekran)

### Elementy UI:
1. **TopAppBar:** "Nowy alert" / "Edytuj alert"

2. **Formularz:**
   - `ExposedDropdownMenu` — waluta (EUR, USD, GBP...)
   - `SegmentedButton` — kierunek: `[Powyżej | Poniżej]`
   - `OutlinedTextField` — próg (np. 4.40)
   - `Switch` — Aktywny/Nieaktywny

3. **Przyciski:** `[Zapisz]` (Primary) | `[Usuń alert]` (Destructive, tylko przy edycji)

---

## Ekran 5: AuthScreen

**Opis:** Logowanie i rejestracja (TabRow)

### Elementy UI:
1. **Logo + tagline** (centered, top 80dp)

2. **TabRow:** `[Logowanie | Rejestracja]`

3. **Formularz (Logowanie):**
   - Email: `OutlinedTextField` (keyboard email)
   - Hasło: `OutlinedTextField` (keyboard password, toggle visibility)
   - `[Zaloguj się]` (FilledButton, full width)

4. **Formularz (Rejestracja):**
   - Email + Hasło + Powtórz hasło
   - Checkbox: `Akceptuję Politykę Prywatności [link]`
   - `[Zarejestruj się]` (FilledButton, full width)

5. **Loading overlay:** CircularProgressIndicator

### Stany: Idle | Loading | Error (inline pod polem)

---

## Ekran 6: SettingsScreen

**Opis:** Ustawienia aplikacji

### Elementy UI:
1. **TopAppBar:** "Ustawienia"

2. **Sekcja "Przelicznik":**
   - `ListItem` "Waluta domyślna" → `ExposedDropdownMenu` (inline)
   - `ListItem` "Odśwież kursy teraz" → przycisk Outlined z ikoną ↻

3. **Sekcja "Bezpieczeństwo":**
   - `ListItem` "Biometria" + `Switch`
   - `ListItem` "Zmień hasło" → dialog z polami: aktualne hasło, nowe hasło, powtórz

4. **Sekcja "Konto":**
   - `ListItem` "Zalogowany jako: user@example.com"
   - `ListItem` "Wyloguj się" (kolor Error)

5. **Sekcja "Informacje":**
   - Wersja aplikacji: "PRZevolut v1.0.0"
   - Link: "Polityka prywatności"

---

## Ekran 7: HistoryScreen

**Opis:** Historia kursu wybranej waluty (dane z `GET /rates/history`)

### Elementy UI:
1. **TopAppBar:** "Historia kursów"

2. **Chip wyboru waluty** (EUR, USD, GBP, CHF, CZK)

3. **Chip wyboru okresu:** `[7 dni | 14 dni | 30 dni]` (SegmentedButton)

4. **Wykres liniowy (Compose Canvas lub MPAndroidChart):**
   - Oś X: daty, oś Y: kurs PLN
   - Punkt dotknięcia: tooltip z datą i wartością
   - Kolor linii: Primary `#1565C0`

5. **Lista poniżej wykresu (LazyColumn):**
   ```
   ┌────────────────────────────────────────┐
   │ 2026-06-14 (pon)         4,312 PLN │
   │ 2026-06-13 (ndz)         4,295 PLN │
   └────────────────────────────────────────┘
   ```

### Stany: Loading | Success | Error | Empty

---

## Nawigacja (Navigation Graph)

```
AuthScreen
    │ (po zalogowaniu)
    ▼
BottomNavigation:
    ├── ScannerScreen (ikona: kamera)
    ├── ManualConverterScreen (ikona: ⇌)
    ├── AlertsListScreen (ikona: dzwonek)
    ├── HistoryScreen (ikona: wykres) → [nowy ekran]
    └── SettingsScreen (ikona: koła)
                │
                ▼
        AlertEditScreen (overlay/sheet)
```

---

## Komponenty wspólne

- **LoadingScreen:** `CircularProgressIndicator` na środku z `"Ładowanie..."`
- **ErrorScreen:** ikona błędu + wiadomość + przycisk "Spróbuj ponownie"
- **OfflineBanner:** żółty pasek na górze ekranu z ikoną wifi_off
- **EmptyState:** ilustracja + opis + opcjonalny CTA button
