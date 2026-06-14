# Polityka Prywatności — PRZevolut

**Data ostatniej aktualizacji:** 2026-06-14  
**Aplikacja:** PRZevolut — Walutowy Skaner AR  
**Deweloper:** Zespół PRZevolut (projekt akademicki)  
**Kontakt:** privacy@przevolut.app

---

## 1. Wstęp

Niniejsza Polityka Prywatności opisuje, w jaki sposób aplikacja PRZevolut zbiera, używa i chroni Twoje dane osobowe. Cenimy Twoją prywatność i zobowiązujemy się do jej ochrony.

---

## 2. Jakie dane zbieramy

### Dane zbierane:
- **Adres e-mail** — używany jako identyfikator konta i do odzyskiwania hasła
- **Hasło** — przechowywane wyłącznie jako hash bcrypt (cost 12); nigdy nie przechowujemy hasła w postaci jawnej
- **Token FCM (Firebase Cloud Messaging)** — unikalny identyfikator urządzenia do dostarczania powiadomień push o alertach walutowych
- **Ustawienia alertów walutowych** — waluta, kierunek (powyżej/poniżej), próg kursu, status aktywności

### Danych których NIE zbieramy:
- ❌ **Zdjęcia ani kadry z kamery** — OCR (rozpoznawanie cen) odbywa się **w 100% lokalnie na Twoim urządzeniu** przy użyciu ML Kit; żadne obrazy nie są wysyłane na nasze serwery ani do żadnych usług zewnętrznych
- ❌ Dane lokalizacji GPS
- ❌ Lista kontaktów
- ❌ Historia przeglądania
- ❌ Dane finansowe ani numery kart płatniczych
- ❌ Dane biometryczne — weryfikacja biometryczna odbywa się wyłącznie przez system operacyjny Android; aplikacja nie ma dostępu do odcisków palców ani danych twarzy

---

## 3. W jaki sposób używamy danych

| Dane | Cel |
|------|-----|
| E-mail | Uwierzytelnienie, odzyskiwanie konta |
| Hash hasła | Weryfikacja tożsamości przy logowaniu |
| Token FCM | Wysyłanie powiadomień push o alertach walutowych |
| Ustawienia alertów | Monitorowanie kursów walut i wyzwalanie powiadomień |

---

## 4. Udostępnianie danych

**Nie sprzedajemy ani nie udostępniamy Twoich danych osobowych podmiotom trzecim** za wyjątkiem:

- **Firebase (Google LLC)** — używany do dostarczania powiadomień push (FCM). Google może przetwarzać tokeny FCM zgodnie ze swoją polityką prywatności: https://policies.google.com/privacy
- **Organy prawne** — wyłącznie gdy jesteśmy do tego prawnie zobowiązani

---

## 5. Przechowywanie i bezpieczeństwo danych

- Dane konta przechowywane są na serwerach hostowanych na platformie Render.com (UE)
- Połączenia szyfrowane protokołem TLS 1.2+
- Hasła hashowane algorytmem bcrypt z parametrem cost 12
- Tokeny JWT z krótkim czasem ważności (access: 15 minut, refresh: 30 dni)
- Lokalne dane aplikacji (kursy walut, ustawienia) przechowywane w zaszyfrowanej bazie Room i EncryptedSharedPreferences

---

## 6. Twoje prawa (RODO)

Zgodnie z Rozporządzeniem RODO przysługuje Ci prawo do:
- **Dostępu** do swoich danych — `GET /auth/me` (imię, e-mail, data rejestracji)
- **Sprostowania** danych
- **Usunięcia** konta i wszystkich powiązanych danych (alerty, tokeny FCM, refresh tokeny)
- **Przenoszenia** danych
- **Wycofania zgody** w dowolnym momencie

Aby skorzystać z tych praw, skontaktuj się z nami pod adresem: privacy@przevolut.app

---

## 7. Pliki cookie i dane lokalne

Aplikacja mobilna nie używa plików cookie. Na urządzeniu przechowywane są:
- Bufor kursów walut (Room Database) — dane publiczne z NBP, brak danych osobowych
- Tokeny sesji (EncryptedSharedPreferences) — szyfrowane kluczem Android Keystore
- Ustawienia aplikacji (DataStore) — preferencje użytkownika

---

## 8. Dzieci

Aplikacja nie jest skierowana do dzieci poniżej 13 roku życia. Nie zbieramy świadomie danych od dzieci.

---

## 9. Zmiany w polityce prywatności

O istotnych zmianach w polityce prywatności poinformujemy Cię przez powiadomienie w aplikacji. Dalsze korzystanie z aplikacji po wprowadzeniu zmian oznacza akceptację nowej polityki.

---

## 10. Kontakt

W sprawach dotyczących prywatności skontaktuj się z nami:  
📧 privacy@przevolut.app  
🌐 https://przevolut.app/privacy
