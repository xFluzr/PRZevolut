# Polityka Prywatności — PRZevolut

*Ostatnia aktualizacja: Maj 2026*

---

## 1. Informacje Ogólne

Niniejsza Polityka Prywatności opisuje zasady gromadzenia, przechowywania i przetwarzania danych osobowych przez aplikację mobilną **PRZevolut — Walutowy Skaner AR** (dalej: „Aplikacja").

Administratorem danych osobowych jest zespół projektowy PRZevolut (dalej: „My", „Twórcy").

---

## 2. Jakie Dane Zbieramy

### 2.1 Dane konta użytkownika
- **Adres e-mail** — wymagany do rejestracji i logowania
- **Hasło** — przechowywane wyłącznie w formie zahashowanej (bcrypt), nigdy w postaci jawnej

### 2.2 Dane aplikacji
- **Alerty walutowe** — wartości kursów i waluty ustawione przez użytkownika
- **Historia przeliczeń** — przechowywana wyłącznie lokalnie na urządzeniu (Room DB), nie jest wysyłana na serwer
- **Ustawienia aplikacji** — preferencje użytkownika (domyślna waluta, biometria ON/OFF)

### 2.3 Dane techniczne
- **Token JWT** — służy do autoryzacji żądań API, przechowywany lokalnie i wygasa po określonym czasie
- **Logi błędów** — anonimowe logi techniczne służące do diagnozy błędów

---

## 3. Czego NIE Zbieramy

- **Nie przechowujemy** zdjęć ani klatek wideo z aparatu — przetwarzanie OCR odbywa się wyłącznie lokalnie na urządzeniu (Google ML Kit on-device)
- **Nie sprzedajemy** żadnych danych osobowych stronom trzecim
- **Nie zbieramy** danych lokalizacyjnych
- **Nie używamy** ciasteczek (cookies) ani śledzenia reklamowego

---

## 4. Uprawnienia Aplikacji

| Uprawnienie | Cel |
|-------------|-----|
| `CAMERA` | Skanowanie cen za pomocą aparatu (OCR) |
| `INTERNET` | Pobieranie aktualnych kursów walut z serwera |
| `USE_BIOMETRIC` | Opcjonalne logowanie odciskiem palca / twarzą |
| `RECEIVE_BOOT_COMPLETED` | Przywrócenie zaplanowanych powiadomień po restarcie |
| `POST_NOTIFICATIONS` | Wysyłanie alertów walutowych |

---

## 5. Przechowywanie i Bezpieczeństwo Danych

- Dane konta są przechowywane na serwerze zabezpieczonym protokołem HTTPS (TLS 1.3)
- Hasła są hashowane algorytmem bcrypt z odpowiednią liczbą iteracji
- Tokeny JWT mają ograniczony czas ważności (access token: 60 min, refresh token: 30 dni)
- Dane lokalne na urządzeniu są chronione przez Android Keystore

---

## 6. Udostępnianie Danych Stronom Trzecim

Aplikacja korzysta z następujących usług zewnętrznych:

| Usługa | Cel | Polityka prywatności |
|--------|-----|---------------------|
| **NBP Open API** | Pobieranie kursów walut | Dane publiczne, brak danych osobowych |
| **Google ML Kit** | OCR tekstu z aparatu | Przetwarzanie on-device, brak wysyłania do Google |

---

## 7. Prawa Użytkownika

Masz prawo do:
- **Dostępu** do swoich danych — skontaktuj się z nami
- **Poprawiania** danych — poprzez ustawienia konta w aplikacji
- **Usunięcia** konta i wszystkich danych — opcja dostępna w Ustawieniach → "Usuń konto"
- **Przenoszenia** danych — na żądanie dostarczymy dane w formacie JSON

---

## 8. Dzieci

Aplikacja nie jest przeznaczona dla dzieci poniżej 13. roku życia. Nie gromadzimy świadomie danych od dzieci.

---

## 9. Zmiany w Polityce Prywatności

O wszelkich istotnych zmianach w niniejszej Polityce poinformujemy przez powiadomienie w aplikacji lub e-mail. Dalsze korzystanie z aplikacji po zmianach oznacza ich akceptację.

---

## 10. Kontakt

W razie pytań dotyczących prywatności skontaktuj się z nami:
- **E-mail**: przevolut@student.edu.pl
- **Repozytorium**: https://github.com/YOUR_ORG/PRZevolut
