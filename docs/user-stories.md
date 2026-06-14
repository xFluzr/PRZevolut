# User Stories — PRZevolut

Wersja: 1.0.0 | Data: 2026-06-14

---

## US-01: Skanowanie ceny aparatem

**Jako** turysta w niemieckim supermarkecie  
**Chcę** skierować aparat na cenę  
**Aby** od razu zobaczyć ile to w PLN

**Kryteria akceptacji (Gherkin):**
```gherkin
Given mam włączony aparat i udzielone pozwolenie na jego użycie
  And wybrana waluta źródłowa to EUR
  And kursy są dostępne (online lub z cache Room)
When cena "4,30 €" jest widoczna w kadrze przez 500 ms
Then na ekranie pojawi się nakładka AR z tekstem "≈ 18,49 PLN"
  And nakładka jest wyrównana do bounding boxa wykrytego tekstu
  And wynik jest stabilny (ta sama wartość 3 razy z rzędu)
```

---

## US-02: Ręczny przelicznik walut

**Jako** użytkownik bez dostępu do aparatu  
**Chcę** wpisać kwotę ręcznie i wybrać waluty z listy  
**Aby** szybko przeliczyć dowolną kwotę

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem na ekranie ManualConverter
  And kursy są załadowane
When wpisuję kwotę "100" i wybieram EUR → PLN
Then wyświetlona zostaje przeliczona kwota oparta na aktualnym kursie
  And widoczny jest timestamp "Kurs z: [data]"
```

---

## US-03: Praca w trybie offline (za granicą bez internetu)

**Jako** turysta bez dostępu do internetu w roamingu  
**Chcę** korzystać z przelicznika i skanera  
**Aby** aplikacja działała bez połączenia

**Kryteria akceptacji (Gherkin):**
```gherkin
Given aplikacja była uruchomiona z internetem wcześniej
  And kursy zostały zabuforowane lokalnie w Room
When wyłączam internet i otwieram aplikację
Then aplikacja wyświetla baner "Tryb offline — kursy z dnia [data]"
  And skaner AR i ręczny przelicznik działają poprawnie z buforowanymi kursami
  And brak informacji o błędzie sieci blokującej działanie
```

---

## US-04: Tworzenie alertu walutowego

**Jako** inwestor śledzący kurs EUR  
**Chcę** ustawić alert gdy EUR/PLN przekroczy 4,40  
**Aby** zostać powiadomionym o korzystnym kursie wymiany

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem zalogowany i na ekranie AlertsListScreen
When wybieram "Nowy alert" i ustawiam EUR, kierunek "above", próg 4.40
  And zatwierdzam formularz
Then alert pojawia się na liście ze statusem "Aktywny"
  And serwer zapisuje alert z moim user_id
```

---

## US-05: Edycja istniejącego alertu

**Jako** użytkownik z aktywnym alertem  
**Chcę** zmienić próg cenowy alertu  
**Aby** dostosować go do bieżącej sytuacji rynkowej

**Kryteria akceptacji (Gherkin):**
```gherkin
Given mam alert EUR "above" 4.40 na liście
When klikam "Edytuj" i zmieniam próg na 4.35
  And zapisuję zmiany
Then alert wyświetla zaktualizowany próg 4.35
  And serwer potwierdza zapis (PATCH /alerts/{id} → 200)
```

---

## US-06: Usuwanie alertu

**Jako** użytkownik który nie chce śledzić danej waluty  
**Chcę** usunąć alert z listy  
**Aby** nie otrzymywać niechcianych powiadomień

**Kryteria akceptacji (Gherkin):**
```gherkin
Given mam alert EUR na liście
When przesuwam alert w lewo i wybieram "Usuń"
  Or wchodzę w edycję i wybieram "Usuń alert"
Then alert znika z listy
  And serwer zwraca 204 na DELETE /alerts/{id}
```

---

## US-07: Powiadomienie push po przekroczeniu progu

**Jako** użytkownik z aktywnym alertem EUR > 4.40  
**Chcę** otrzymać powiadomienie push gdy kurs przekroczy próg  
**Aby** nie musieć otwierać aplikacji

**Kryteria akceptacji (Gherkin):**
```gherkin
Given mam aktywny alert EUR "above" 4.40
  And kurs EUR wcześniej wynosił 4.38
When agregator pobierze nowe kursy i EUR = 4.42
Then otrzymuję powiadomienie push z tytułem "Alert PRZevolut"
  And treść: "EUR przekroczył próg 4,40 → aktualny kurs: 4,42"
  And pole last_triggered_at alertu jest zaktualizowane
```

---

## US-08: Logowanie do aplikacji

**Jako** zarejestrowany użytkownik  
**Chcę** zalogować się swoim e-mailem i hasłem  
**Aby** uzyskać dostęp do alertów i historii kursów

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem na ekranie logowania
When wpisuję poprawny e-mail i hasło
  And klikam "Zaloguj się"
Then otrzymuję access_token i refresh_token
  And jestem przekierowany na główny ekran aplikacji
  And token jest zapisany w EncryptedSharedPreferences
```

---

## US-09: Rejestracja nowego konta

**Jako** nowy użytkownik  
**Chcę** założyć konto podając e-mail i hasło  
**Aby** móc korzystać z alertów i synchronizacji

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem na ekranie rejestracji
When wpisuję unikalny e-mail i hasło min. 8 znaków
  And klikam "Zarejestruj się"
Then konto jest tworzone (POST /auth/register → 201)
  And jestem automatycznie zalogowany
  And hasło jest przechowywane jako bcrypt hash (cost 12)
```

---

## US-10: Weryfikacja biometryczna przy starcie

**Jako** użytkownik dbający o bezpieczeństwo  
**Chcę** odblokować aplikację odciskiem palca lub Face Unlock  
**Aby** nikt bez mojej biometrii nie mógł zobaczyć moich alertów

**Kryteria akceptacji (Gherkin):**
```gherkin
Given opcja biometrii jest włączona w ustawieniach
  And urządzenie ma zarejestrowaną biometrię
When uruchamiam aplikację lub wracam z tła po 5 minutach
Then wyświetla się BiometricPrompt z komunikatem "Odblokuj PRZevolut"
When autoryzacja się powiedzie
Then aplikacja staje się dostępna
When autoryzacja się nie powiedzie 3 razy
Then wyświetlany jest fallback ekran logowania hasłem
```

---

## US-11: Zmiana domyślnej waluty

**Jako** użytkownik często podróżujący do Niemiec  
**Chcę** ustawić EUR jako domyślną walutę skanera  
**Aby** nie musieć jej wybierać przy każdym uruchomieniu

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem na ekranie Ustawień
When wybieram "Waluta domyślna" i wybieram EUR
  And zapisuję
Then przy następnym otwarciu skanera EUR jest już wybrany
  And ustawienie przetrwa restart aplikacji (DataStore)
```

---

## US-12: Przeglądanie historii kursów

**Jako** użytkownik analizujący trendy  
**Chcę** zobaczyć historię kursu EUR/PLN z ostatnich 30 dni  
**Aby** podjąć decyzję o wymianie walut

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem zalogowany
When otwieram sekcję historii dla EUR
  And serwer zwraca dane z GET /rates/history?code=EUR&days=30
Then widzę wykres lub listę kursów z datami
  And dane są posortowane od najnowszego do najstarszego
```

---

## US-13: Obsługa błędu sieci z wyraźnym komunikatem

**Jako** użytkownik z niestabilnym połączeniem  
**Chcę** zobaczyć czytelny komunikat o błędzie sieci  
**Aby** wiedzieć że problem leży po stronie połączenia, nie aplikacji

**Kryteria akceptacji (Gherkin):**
```gherkin
Given brak dostępu do internetu i brak buforowanych kursów
When otwieram ScannerScreen lub ManualConverterScreen
Then wyświetla się ekran stanu "Error" z komunikatem "Brak połączenia"
  And widoczny jest przycisk "Spróbuj ponownie"
When klikam "Spróbuj ponownie" i jest już połączenie
Then dane zostają pobrane i ekran przechodzi do stanu "Success"
```

---

## US-14: Odświeżenie tokena dostępu (refresh)

**Jako** zalogowany użytkownik  
**Chcę** aby moja sesja była automatycznie odświeżana  
**Aby** nie musieć logować się co 15 minut

**Kryteria akceptacji (Gherkin):**
```gherkin
Given mam ważny refresh_token (30 dni)
  And access_token wygasł
When aplikacja wykonuje chronione żądanie API
Then AuthInterceptor wykrywa 401
  And automatycznie wywołuje POST /auth/refresh
  And powtarza oryginalne żądanie z nowym access_token
  And użytkownik nie widzi ekranu logowania
```

---

## US-15: Wylogowanie z aplikacji

**Jako** użytkownik kończący sesję  
**Chcę** wylogować się z aplikacji  
**Aby** moje dane były bezpieczne na współdzielonym urządzeniu

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem zalogowany i jestem w Ustawieniach
When wybieram "Wyloguj się" i potwierdzam
Then tokeny JWT są usuwane z EncryptedSharedPreferences
  And token FCM jest wyrejestrowywany z serwera
  And jestem przekierowany na ekran logowania
  And nie mogę wykonać żadnego chronionego żądania API
```

---

## US-16: Polityka prywatności i zgoda na przetwarzanie danych

**Jako** nowy użytkownik  
**Chcę** zobaczyć politykę prywatności przed rejestracją  
**Aby** wiedzieć jakie dane są zbierane i w jakim celu

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem na ekranie rejestracji
When klikam link "Polityka prywatności"
Then wyświetla się ekran/webview z pełną polityką
  And mogę ją przewinąć i wrócić
When zaznaczam checkbox zgody
  And klikam "Zarejestruj się"
Then rejestracja jest możliwa
  And bez zaznaczenia zgody przycisk pozostaje nieaktywny
```

---

## US-17: Force-refresh kursów z ustawień

**Jako** użytkownik chcący mieć najbardziej aktualne kursy  
**Chcę** wymusić pobranie kursów z serwera  
**Aby** pominąć buforowane dane

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem w Ustawieniach
  And dostępna jest sieć
When klikam "Odśwież kursy teraz"
Then aplikacja wywołuje GET /rates z nagłówkiem Cache-Control: no-cache
  And Room jest aktualizowany
  And widzę komunikat "Kursy zaktualizowane: [timestamp]"
```

---

## US-18: Zmiana hasła z poziomu aplikacji

**Jako** zalogowany użytkownik  
**Chcę** zmienić swoje hasło podając aktualne i nowe  
**Aby** zachować bezpieczeństwo konta w razie podejrzenia kradzieży danych

**Kryteria akceptacji (Gherkin):**
```gherkin
Given jestem zalogowany i jestem w Ustawieniach
When wybieram "Zmień hasło"
  And wpisuję aktualne hasło i nowe hasło (min. 8 znaków)
  And klikam "Zapisz"
Then aplikacja wywołuje PATCH /auth/password → 204
  And hasło jest zaktualizowane w bazie jako nowy hash bcrypt
When wpisuję niepoprawne aktualne hasło
Then wyświetla się komunikat "Aktualne hasło jest nieprawidłowe"
  And hasło nie zostaje zmienione
```

---

## Mapowanie US na sprinty

### Sprint 1 (tygodnie 1-2)

| # | Zadanie | US |
|---|---------|-----|
| T-01 | Setup projektu Android (Gradle, Hilt, Navigation) | — |
| T-02 | Setup projektu FastAPI (struktura, database, config) | — |
| T-03 | Modele Room: cached_rates, alerts | US-03 |
| T-04 | Implementacja OCR: PriceOcrAnalyzer + regex cen | US-01 |
| T-05 | ScannerScreen + CameraX + nakładka AR | US-01 |
| T-06 | ManualConverterScreen + ViewModel | US-02 |
| T-07 | Endpointy /auth/register i /auth/login | US-08, US-09 |
| T-08 | AuthScreen (login + rejestracja) w Compose | US-08, US-09 |
| T-09 | NBP client + agregator APScheduler | — |
| T-10 | WorkManager — codzienny refresh kursów | US-03 |

### Sprint 2 (tygodnie 3-4)

| # | Zadanie | US |
|---|---------|-----|
| T-11 | AlertsListScreen + AlertEditScreen + VM | US-04, US-05, US-06 |
| T-12 | Endpointy /alerts CRUD + /devices/register | US-04, US-05, US-06 |
| T-13 | Silnik alertów (alert_engine) + push FCM | US-07 |
| T-14 | SettingsScreen + DataStore waluta domyślna | US-11, US-17 |
| T-15 | BiometricPromptManager + integracja | US-10 |
| T-16 | NetworkMonitor + Offline banner + UiState | US-03, US-13 |
| T-17 | AuthInterceptor + refresh token flow | US-14 |
| T-18 | Testy jednostkowe Android (min. 10) | — |
| T-19 | Testy integracyjne backend (min. 5) | — |
| T-20 | Dokumentacja, CI/CD, Dockerfile, render.yaml | — |
