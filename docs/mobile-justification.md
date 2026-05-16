# Uzasadnienie wyboru platformy mobilnej — PRZevolut

## Dlaczego PRZevolut musi być aplikacją mobilną?

Aplikacja PRZevolut rozwiązuje problem realny, który pojawia się w ruchu: **turysta stoi przed półką w zagranicznym sklepie i chce wiedzieć, ile kosztuje produkt w jego walucie**. Żadna platforma desktopowa ani webowa nie jest w stanie zaspokoić tej potrzeby równie skutecznie co smartfon.

---

## 1. Aparat — Skaner AR w czasie rzeczywistym

**Dlaczego tylko smartfon?** Każdy współczesny smartfon ma wbudowany aparat wystarczający do OCR tekstu. Komputer wymaga zewnętrznej kamery; tablet jest zbyt duży do trzymania nad półką sklepową.

**Scenariusz:** Maria jest na wakacjach w Barcelonie. Stoi w drogerii i trzyma krem za 12,99 EUR. Zamiast otwierać kalkulator, kieruje aparat na etykietę — aplikacja natychmiast pokazuje **"≈ 55,72 PLN"** nałożone na kadr kamery.

**Technicznie:** CameraX API → ML Kit Text Recognition V2 (lokalnie, zero przesyłu zdjęć) → Canvas Compose (nakładka AR).

---

## 2. Powiadomienia Push w tle (FCM)

**Dlaczego tylko smartfon?** Przeglądarka webowa wymaga otwartej karty. Smartfon dzięki FCM może dostarczyć powiadomienie nawet gdy aplikacja jest zamknięta.

**Scenariusz:** Piotr leci za tydzień do Berlina i ustawia alert: EUR > 4,35 PLN. W środę rano telefon wibruje: *"EUR przekroczył próg 4,35 → aktualny kurs: 4,37"*. Piotr idzie wymienić walutę.

**Technicznie:** FCM token rejestrowany przy logowaniu → APScheduler co godzinę sprawdza kursy → push_sender wysyła powiadomienie.

---

## 3. Biometria — Face Unlock / Fingerprint

**Dlaczego tylko smartfon?** Czytniki linii papilarnych i Face ID są integralną częścią smartfonów. Przeglądarki nie mają dostępu do biometrii systemowej.

**Scenariusz:** Anna udostępnia telefon córce do gry. Włącza biometrię w PRZevolut. Przy każdym uruchomieniu pojawia się BiometricPrompt — córka nie może wejść do aplikacji.

**Technicznie:** BiometricPrompt API (AndroidX Biometric) z fallbackiem do PIN urządzenia.

---

## 4. Działanie offline w roamingu zagranicznym

**Dlaczego tylko smartfon?** W roamingu internet jest drogi lub niedostępny. Smartfon jest zawsze pod ręką i dzięki Room działa bez połączenia.

**Scenariusz:** Tomek jedzie pociągiem przez Alpy bez zasięgu. Otwiera PRZevolut — baner "Tryb offline — kursy z dnia 2024-01-15" pojawia się na górze, ale skaner i przelicznik działają normalnie z zabuforowanymi kursami CHF.

**Technicznie:** Room `cached_rates` → WorkManager codziennie odświeża gdy sieć dostępna → NetworkMonitor wykrywa brak sieci → baner offline.

---

## 5. Podsumowanie porównawcze

| Funkcja | Smartfon | Desktop | Web |
|---------|----------|---------|-----|
| Skaner AR | ✅ | ❌ | ❌ |
| Push offline | ✅ | ❌ | ⚠️ PWA |
| Biometria | ✅ | ⚠️ | ❌ |
| Offline w roamingu | ✅ | ❌ | ❌ |
| Zawsze pod ręką | ✅ | ❌ | ❌ |

**Wniosek:** Kombinacja aparatu, powiadomień, biometrii i offline tworzy zestaw funkcji możliwy wyłącznie na platformie mobilnej, rozwiązujący realny problem realnego użytkownika w kontekście zakupów zagranicznych.
