# 📋 User Stories — PRZevolut (Walutowy Skaner AR)

> Format: "Jako [rola], chcę [cel], aby [korzyść]"
> Kryteria akceptacji w formacie Given / When / Then

---

## 🔐 Uwierzytelnianie (Epik: AUTH)

### US-01: Rejestracja konta
**Jako** nowy użytkownik,  
**chcę** założyć konto podając email i hasło,  
**aby** móc korzystać z synchronizacji alertów między urządzeniami.

**Kryteria akceptacji:**
- **Given** jestem na ekranie rejestracji
- **When** wpisuję prawidłowy email i hasło (min. 8 znaków) i klikam "Zarejestruj"
- **Then** konto zostaje utworzone i jestem przekierowany do ekranu głównego
- **And** hasło jest przechowywane zahashowane (bcrypt) po stronie serwera
- **And** przy próbie rejestracji z już zajętym emailem widzę czytelny komunikat błędu

---

### US-02: Logowanie biometryczne
**Jako** zarejestrowany użytkownik,  
**chcę** logować się odciskiem palca lub twarzą,  
**aby** szybko uzyskać dostęp do aplikacji bez wpisywania hasła.

**Kryteria akceptacji:**
- **Given** jestem na ekranie logowania i mam skonfigurowaną biometrię w telefonie
- **When** klikam "Zaloguj odciskiem" i przykładam palec
- **Then** jestem zalogowany i widzę ekran główny w czasie < 2s
- **And** jeśli biometria zawiedzie 3 razy, pojawia się opcja logowania hasłem

---

### US-03: Wylogowanie
**Jako** zalogowany użytkownik,  
**chcę** się wylogować,  
**aby** zabezpieczyć moje dane gdy przekazuję telefon innej osobie.

**Kryteria akceptacji:**
- **Given** jestem zalogowany
- **When** przechodzę do Ustawień i klikam "Wyloguj"
- **Then** token JWT zostaje unieważniony, jestem przekierowany do ekranu logowania
- **And** dane wrażliwe są czyszczone z pamięci podręcznej

---

## 📷 Skaner AR (Epik: SCANNER)

### US-04: Skanowanie ceny aparatem
**Jako** turysta w zagranicznym sklepie,  
**chcę** skierować aparat na cenę na etykiecie,  
**aby** natychmiast zobaczyć przeliczoną wartość w złotówkach.

**Kryteria akceptacji:**
- **Given** aplikacja ma uprawnienie do kamery i otwarty jest ekran skanera
- **When** nakieruję aparat na widoczną cenę (np. "€ 12,99")
- **Then** na ekranie pojawia się nakładka AR z wartością w PLN w czasie < 1,5s
- **And** przeliczenie używa kursu z lokalnej bazy (aktualnego lub ostatnio pobranego)
- **And** jeśli OCR nie rozpozna ceny, na ekranie pojawia się subtelna animacja szukania

---

### US-05: Przeliczanie wielu walut
**Jako** użytkownik podróżujący po Europie,  
**chcę** aby aplikacja automatycznie rozpoznawała walutę ze zdjęcia (€, £, $, CHF, CZK),  
**aby** nie musieć ręcznie wybierać waluty za każdym razem.

**Kryteria akceptacji:**
- **Given** jestem na ekranie skanera
- **When** skanuję cenę z symbolem waluty
- **Then** aplikacja rozpoznaje walutę i przelicza na PLN bez konieczności manualnego wyboru
- **And** obsługiwane są: EUR, USD, GBP, CHF, CZK

---

### US-06: Ręczne wpisanie kwoty
**Jako** użytkownik przy słabym oświetleniu,  
**chcę** ręcznie wpisać kwotę do przeliczenia,  
**aby** korzystać z przelicznika gdy OCR nie działa prawidłowo.

**Kryteria akceptacji:**
- **Given** jestem na ekranie skanera
- **When** klikam ikonę kalkulatora i wpisuję kwotę
- **Then** natychmiast widzę przeliczoną wartość w PLN
- **And** mogę wybrać walutę źródłową z listy rozwijanej

---

## 💱 Kursy Walut (Epik: RATES)

### US-07: Przeglądanie aktualnych kursów
**Jako** użytkownik,  
**chcę** widzieć aktualne kursy walut na ekranie głównym,  
**aby** orientować się w bieżącej sytuacji rynkowej.

**Kryteria akceptacji:**
- **Given** jestem na ekranie głównym (Dashboard)
- **Then** widzę tabelę kursów: EUR, USD, GBP, CHF, CZK do PLN
- **And** każdy kurs pokazuje zmianę procentową względem poprzedniego dnia
- **And** widoczna jest data i godzina ostatniej aktualizacji

---

### US-08: Działanie offline
**Jako** turysta za granicą bez internetu,  
**chcę** aby aplikacja działała bez połączenia z siecią,  
**aby** móc przeliczać ceny nawet w sklepach bez Wi-Fi.

**Kryteria akceptacji:**
- **Given** urządzenie nie ma połączenia z internetem
- **When** otwieram aplikację
- **Then** aplikacja ładuje kursy z lokalnej bazy Room DB
- **And** wyświetla baner "Kursy z [data ostatniej aktualizacji] — brak połączenia"
- **And** skaner AR działa normalnie z kursami offline

---

### US-09: Automatyczna aktualizacja kursów
**Jako** użytkownik,  
**chcę** aby kursy aktualizowały się automatycznie,  
**aby** przeliczenia były zawsze zgodne z aktualnym rynkiem.

**Kryteria akceptacji:**
- **Given** urządzenie ma połączenie z internetem
- **When** otwieram aplikację po raz pierwszy lub po 1 godzinie nieaktywności
- **Then** aplikacja pobiera świeże kursy z serwera (NBP API)
- **And** wyświetla animację ładowania podczas pobierania
- **And** kursy są zapisywane do lokalnej bazy Room DB

---

## 🔔 Alerty Walutowe (Epik: ALERTS)

### US-10: Ustawianie alertu walutowego
**Jako** użytkownik planujący wymianę walut,  
**chcę** ustawić alert gdy EUR/PLN spadnie poniżej wybranej kwoty,  
**aby** kupić walutę w najlepszym momencie.

**Kryteria akceptacji:**
- **Given** jestem na ekranie Alertów
- **When** klikam "Dodaj alert" i wybieram walutę EUR, kierunek "poniżej" i kwotę 4.20 PLN
- **Then** alert zostaje zapisany na serwerze i widoczny jest na liście alertów
- **And** serwer sprawdza warunek przy każdej aktualizacji kursów

---

### US-11: Powiadomienie push o alercie
**Jako** użytkownik z ustawionym alertem,  
**chcę** otrzymać powiadomienie push gdy kurs osiągnie mój target,  
**aby** nie musieć co chwilę sprawdzać aplikacji.

**Kryteria akceptacji:**
- **Given** mam ustawiony alert EUR < 4.20 PLN i kurs właśnie spadł poniżej tej wartości
- **When** serwer wykonuje cykliczne sprawdzanie
- **Then** otrzymuję powiadomienie push w czasie < 5 minut od zmiany kursu
- **And** powiadomienie zawiera: nazwę waluty, aktualny kurs i ustawiony target

---

### US-12: Zarządzanie alertami
**Jako** użytkownik,  
**chcę** przeglądać, edytować i usuwać swoje alerty,  
**aby** utrzymać porządek w swoich ustawieniach.

**Kryteria akceptacji:**
- **Given** jestem na ekranie Alertów
- **Then** widzę listę wszystkich moich alertów z oznaczeniem aktywny/wyzwolony
- **When** przesuwam alert w lewo
- **Then** pojawia się przycisk usunięcia
- **When** potwierdzam usunięcie
- **Then** alert jest usuwany z serwera i znika z listy

---

## ⚙️ Ustawienia (Epik: SETTINGS)

### US-13: Wybór waluty domyślnej do przeliczania
**Jako** użytkownik podróżujący głównie po strefie euro,  
**chcę** ustawić EUR jako domyślną walutę skanera,  
**aby** aplikacja od razu przeliczała EUR bez pytania.

**Kryteria akceptacji:**
- **Given** jestem w Ustawieniach
- **When** wybieram "Domyślna waluta skanera" → EUR
- **Then** przy następnym skanowaniu aplikacja domyślnie używa EUR → PLN
- **And** ustawienie jest zapisane lokalnie i persystuje po restarcie aplikacji

---

### US-14: Włączenie/wyłączenie biometrii
**Jako** użytkownik dbający o bezpieczeństwo,  
**chcę** włączyć logowanie biometryczne w ustawieniach,  
**aby** szybciej się logować nie rezygnując z ochrony konta.

**Kryteria akceptacji:**
- **Given** jestem zalogowany i w Ustawieniach
- **When** przełączam opcję "Logowanie biometryczne" na ON
- **Then** przy następnym uruchomieniu aplikacji pojawia się dialog biometryczny
- **And** ustawienie jest zapisane bezpiecznie w EncryptedSharedPreferences

---

### US-15: Historia przeliczeń
**Jako** użytkownik,  
**chcę** widzieć historię ostatnich 20 przeliczeń,  
**aby** móc wrócić do kwot które skanowałem wcześniej.

**Kryteria akceptacji:**
- **Given** dokonałem co najmniej jednego przeliczenia
- **When** wchodzę w zakładkę Historia na ekranie głównym
- **Then** widzę listę ostatnich 20 przeliczeń z: kwotą źródłową, walutą, kwotą PLN i godziną
- **And** historia jest przechowywana lokalnie w Room DB
- **And** mogę wyczyścić historię jednym przyciskiem

---

### US-16 (Bonus): Udostępnianie przeliczonej ceny
**Jako** użytkownik robiący zakupy z rodziną,  
**chcę** udostępnić przeliczoną cenę przez komunikator,  
**aby** szybko pokazać innym jaki jest koszt w złotówkach.

**Kryteria akceptacji:**
- **Given** na ekranie wyświetla się przeliczona cena
- **When** przytrzymuję wynik przeliczenia
- **Then** pojawia się Android Share Sheet z opcją udostępnienia tekstu
- **And** tekst zawiera: oryginalną kwotę, walutę, przeliczoną kwotę PLN i datę kursu
