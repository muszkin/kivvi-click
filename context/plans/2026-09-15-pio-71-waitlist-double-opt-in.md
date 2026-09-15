# PIO-71 — Waitlista: potwierdzenie adresu (double opt-in)

**Status:** spec + plan gotowe do wykonania (2026-09-15). Decyzje właściciela podjęte przed spisaniem.
**Uzgodnione z kodem:** 2026-09-15, po merge'u PIO-70 (`327049d`) — patrz „Korekty po merge'u PIO-70”.
**Ticket:** [PIO-71](https://linear.app/piotr-mucha/issue/PIO-71/waitlista-potwierdzenie-adresu-double-opt-in) · P0 · `growth`
**Blokowane przez:** PIO-70 — startuje dopiero po jego merge'u. Dotyka tych samych plików i tej samej tabeli.
**Gałąź:** `muszkin/pio-71-waitlista-potwierdzenie-adresu-double-opt-in`
**Research:** `context/research/2026-09-15-transactional-email-provider.md` (wybór dostawcy: Brevo)
**Docelowe miejsce w repo:** `context/plans/2026-09-15-pio-71-waitlist-double-opt-in.md`

## Wynik

Adres z listy oczekujących jest potwierdzony przez właściciela skrzynki. Po zapisie wychodzi mail
z linkiem ważnym 7 dni; kliknięcie przełącza status na `confirmed` i zapisuje dowód: czas, IP
i user-agent potwierdzenia. Zużyty albo przeterminowany link pokazuje stronę z akcją wysłania
nowego. Powtórny zapis niepotwierdzonego adresu wysyła świeży link zamiast tworzyć drugi wpis.
Każdy mail niesie działający link „wypisz się", który nie wymaga logowania.

## Decyzje (właściciel, 2026-09-15)

| # | Decyzja | Konsekwencja |
| --- | --- | --- |
| D1 | HTML maila renderuje **Thymeleaf** (`spring-boot-starter-thymeleaf`, wersja z BOM-u Boota 4.1.1). | Pierwszy silnik szablonów w tym backendzie. Uzasadnienie: PIO-76 i moduł kampanii i tak będą go potrzebowały — lepiej raz postawić szynę niż składać HTML w stringach. |
| D2 | Wysyłka przez **outbox w Postgresie** obsługiwany przez istniejący scheduler ShedLock. | Nowa tabela `mail_outbox`. POST tylko wstawia wiersz i wraca. Trwałe po restarcie, retry z backoffem za darmo, ta sama szyna dla PIO-76 i kampanii. Koszt: opóźnienie rzędu interwału odpytywania (30 s). |
| D3 | Dostawca: **Brevo**, przez zwykły relay SMTP (`smtp-relay.brevo.com:587`, STARTTLS). | Przetwarzanie w całości w EU, DPA bez SCC, free 300/dzień. Integracja przez `spring.mail.*`, nie przez SDK — zmiana dostawcy to podmiana czterech zmiennych środowiskowych. |
| D4 | Testy SMTP na **GreenMail 2.1.3** (`com.icegreen:greenmail-junit5`). | Serwer SMTP w JVM, bez Dockera i bez nowej usługi w compose. |
| D5 | W profilu `dev` maile **lądują na dysku jako pliki `.eml`**, nie w SMTP. | Pozwala suicie E2E odczytać link potwierdzający i domknąć pełną pętlę double opt-in bez dokładania kontenera Mailpit do stacku, który świadomie ma tylko Postgresa. Szczegóły w „Przechwytywanie maili w dev". |

## Ustalenia z repozytorium (discover)

| Ustalenie | Dowód | Wpływ |
| --- | --- | --- |
| **Zero infrastruktury mailowej.** Brak `spring-boot-starter-mail`, brak sekcji `spring.mail`, brak usługi SMTP w compose. | `backend/pom.xml`, `application.yml`, `compose.yaml` | Wszystko od zera. Ticket odsyła do „sekcji `spring.mail` (do utworzenia)" — zgadza się. |
| **`EmailDocument.vue` nie może być szablonem maila.** To komponent Vue renderowany w przeglądarce, stylowany klasami z `04-patterns.css`, których klient pocztowy nie pobierze. | `components/organisms/EmailDocument.vue` | To jest **podgląd w edytorze kampanii**, nie szablon wysyłkowy. Mail dostaje własny szablon Thymeleaf z pełnym inline'owaniem stylów. Ticket mówi „szablon oparty o EmailDocument" — do przemapowania na „wizualnie zgodny z", nie „ten sam plik". |
| **`EmailDocument.vue` używa `oklch()`.** | tamże, plus `.ee-doc` w `04-patterns.css` | `oklch()` to CSS Color 4 — klienci pocztowi w większości go nie obsługują. Szablon maila musi używać **hexów**, nie oklch. Kolory dobieramy tak, by wyglądały jak te z podglądu. |
| **Brak Messengera i jakiejkolwiek kolejki.** | `backend/pom.xml`, `CLAUDE.md` („no message-queue/outbox layer exists") | Ticket mówi „puszczamy przez Messenger (async), worker już działa" — nieprawda w tym stacku. Stąd D2. |
| **Scheduler ma pulę jednowątkową.** `heartbeatTaskScheduler` to `ThreadPoolTaskScheduler` z `poolSize = 1`, a heartbeat jedzie na własnym triggerze. | `infrastructure/config/SchedulingConfig.java` | Dorzucenie wysyłki do tej samej puli oznacza, że timeout SMTP zablokuje heartbeat. Sender dostaje **własną pulę jednowątkową**, nie współdzieli tej. |
| **Sprzątanie wygasłych wierszy jest już uogólnione.** PIO-70 przemianował `EventDedupCleanupJob` na `ExpiredRowsCleanupJob`, przeniósł go do `infrastructure/scheduling`, ustawił klucz konfiguracji na `kivvi.cleanup.interval` i osłonił `@ConditionalOnProperty("kivvi.cleanup.enabled")`. Nazwa blokady została **stara** — `LOCK_NAME = "event-dedup-cleanup"`. | `infrastructure/scheduling/ExpiredRowsCleanupJob.java` na `main` | Sprzątanie wysłanych wierszy outboxu dokładamy **do tego zadania**, nie tworzymy trzeciego, i nie ruszamy nazwy blokady — produkcyjny wiersz w `shedlock` zostałby osierocony. |
| Kolumny tokenu istnieją już w `waitlist_subscriber` (nullable), założone przez PIO-70: `confirmation_token_hash CHAR(64)`, `confirmation_token_expires_at TIMESTAMPTZ`, `confirmed_at TIMESTAMPTZ`. | zmergowany `V2__waitlist.sql` | Potwierdzone. Migracja PIO-71 tylko je wykorzystuje i dokłada to, czego brakuje — nie przebudowuje tabeli. |
| **Wyniki operacji mieszkają w `application.waitlist`, nie w `domain.waitlist`.** PIO-70 umieścił tam `SignupOutcome` (sealed interface) razem z `SignupRequest`. | `application/waitlist/SignupOutcome.java` | `ConfirmationOutcome` i `UnsubscribeOutcome` idą obok niego, a nie do `domain`. `ArchitectureTest` dopuszcza oba miejsca; spójność z sąsiadem rozstrzyga. |
| **Jedyny `TaskScheduler` jest wpięty w rejestrator zadań.** `SchedulingConfig.configureTasks` woła `registrar.setTaskScheduler(heartbeatTaskScheduler())`, więc **każde** `@Scheduled` w aplikacji — łącznie z `ExpiredRowsCleanupJob` — jedzie po tym jednym wątku heartbeatu. | `infrastructure/config/SchedulingConfig.java` | Sender **nie może** być `@Scheduled`: to by go wsadziło dokładnie do puli, której R4 zabrania. Rejestrujemy go ręcznie na własnym `ThreadPoolTaskScheduler`. |
| **`WaitlistSubscriberStore.save` zwraca `boolean`** (`INSERT ... ON CONFLICT (email) DO NOTHING`) i nie oddaje identyfikatora wiersza. | `infrastructure/waitlist/WaitlistSubscriberStore.java` | Nie zmieniamy tego kontraktu. Po zapisie serwis odczytuje wiersz po adresie — dzięki temu nowy zapis i powtórny zapis niepotwierdzonego adresu idą **jedną** ścieżką. |

> **Uwaga o kolejności.** Ten spec opisuje stan po merge'u PIO-70. Zanim zacznie się implementacja,
> trzeba przeczytać faktycznie zmergowaną `V2__waitlist.sql` i nazwy klas — jeżeli PIO-70 odjechało
> od swojego planu, ten dokument koryguje się przed pierwszym krokiem, nie w trakcie.

## Zakres

### Robimy

- Migracja `V3__waitlist_confirmation.sql`: tabela `mail_outbox`, kolumny wypisu i dowodu
  potwierdzenia w `waitlist_subscriber`.
- Wysyłka maila potwierdzającego po zapisie na waitlistę (token jednorazowy, ważny 7 dni, w bazie
  wyłącznie hash).
- Strony potwierdzenia i wypisu wraz ze stanami: potwierdzono, już potwierdzone, link wygasł
  (z akcją wysłania nowego), token nieznany.
- Ponowna wysyłka linku przy powtórnym zapisie niepotwierdzonego adresu — bez drugiego wpisu.
- Link „wypisz się" działający bez logowania, ustawiający status `unsubscribed`.
- Szablon maila w Thymeleaf, PL i EN, z inline'owanymi stylami i hexami.
- Outbox z ponawianiem, backoffem i limitem prób; sprzątanie wysłanych wierszy.
- Konfiguracja SMTP z env, profil `dev` zapisujący maile na dysk.

### Nie robimy

- Obsługi bounce'ów i webhooków zwrotnych od dostawcy — zadanie P2.
- Panelu podglądu wysłanych maili — PIO-72 pokaże listę subskrybentów, nie skrzynkę.
- Wysyłki marketingowej i kampanii — osobny moduł produktu.
- Kolejki ogólnego przeznaczenia. `mail_outbox` jest outboxem **maili**, nie brokerem zdarzeń.

## Model danych — `V3__waitlist_confirmation.sql`

```sql
ALTER TABLE waitlist_subscriber
    ADD COLUMN unsubscribe_token_hash      CHAR(64),
    ADD COLUMN unsubscribed_at             TIMESTAMPTZ,
    ADD COLUMN confirmed_ip                VARCHAR(45),
    ADD COLUMN confirmed_user_agent        VARCHAR(512);

CREATE UNIQUE INDEX waitlist_subscriber_confirmation_token_idx
    ON waitlist_subscriber (confirmation_token_hash)
    WHERE confirmation_token_hash IS NOT NULL;

CREATE UNIQUE INDEX waitlist_subscriber_unsubscribe_token_idx
    ON waitlist_subscriber (unsubscribe_token_hash)
    WHERE unsubscribe_token_hash IS NOT NULL;

CREATE TABLE mail_outbox (
    id              BIGSERIAL PRIMARY KEY,
    dedup_key       VARCHAR(160) UNIQUE,
    recipient       VARCHAR(320) NOT NULL,
    subject         VARCHAR(512) NOT NULL,
    html_body       TEXT         NOT NULL,
    text_body       TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ
);

CREATE INDEX mail_outbox_due_idx ON mail_outbox (status, next_attempt_at);
```

- **Token nigdy nie leży w bazie jawnie.** Generujemy 32 losowe bajty z `SecureRandom`, wysyłamy
  w linku jako hex, zapisujemy SHA-256 — dokładnie jak `EventDedupStore` hashuje swoje klucze.
- **Dwa osobne tokeny, celowo.** Potwierdzający wygasa po 7 dniach i jest jednorazowy (kasowany po
  użyciu). Wypisujący **nie wygasa nigdy** — wędruje w stopce każdego maila i musi działać rok
  później, inaczej jedyną drogą wypisu zostaje zgłoszenie do obsługi.
- **`dedup_key`** chroni przed podwójną wysyłką tego samego maila przy równoległym żądaniu:
  `waitlist-confirm:<hash tokenu potwierdzającego>`. **Korekta:** pierwotnie miał to być
  `<id subskrybenta>:<numer wydania tokenu>`, co wymagałoby dodatkowej kolumny-licznika. Hash już
  jest w bazie, jest unikalny dla każdego wydania tokenu i zmienia się przy każdym ponowieniu —
  robi dokładnie to samo za darmo.
- **`text_body` jest obowiązkowe.** Mail bez części tekstowej dostaje gorszą ocenę
  antyspamową i jest nieczytelny w klientach tekstowych.

## Kontrakt

| Ścieżka | Metoda | Zachowanie |
| --- | --- | --- |
| `/{locale}/waitlist/confirm/{token}` | GET | Poprawny, żywy token → status `confirmed`, zapis czasu/IP/UA, token skasowany, dokument SPA z `data-waitlist-confirm="ok"`. Token już zużyty, ale adres potwierdzony → `data-waitlist-confirm="already"`. Token wygasły → `data-waitlist-confirm="expired"` + `data-waitlist-token` do ponowienia. Token nieznany → `data-waitlist-confirm="unknown"`. |
| `/{locale}/waitlist/confirm/resend` | POST | Przyjmuje wygasły token, wydaje nowy, wstawia maila do outboxu, `302` na `/{locale}/waitlist/confirm/sent`. Nieznany token zachowuje się identycznie — nie zdradzamy, czy adres istnieje. Ten sam limit zapytań co zapis (PIO-70). |
| `/{locale}/waitlist/unsubscribe/{token}` | GET | Poprawny token → status `unsubscribed`, `data-waitlist-unsubscribe="ok"`. Nieznany → `"unknown"`. Ponowne kliknięcie → `"ok"` (idempotentne). |

Wszystkie odpowiedzi to dokument SPA z `Content-Type: text/html; charset=UTF-8` i atrybutami
`data-*` na `<html>` — ten sam mechanizm, który PIO-70 uogólnił na mapę atrybutów.

Trasy trafiają do `RouteTable` (layout `PUBLIC`), inaczej bezpośrednie wejście z maila albo
odświeżenie strony skończy się czterysta czwórką.

Potwierdzenie mutuje stan na `GET`, bo tak działa link w mailu. To świadome odstępstwo od czystości
HTTP, wymuszone medium — odnotowane, nie przypadkowe.

## Architektura i pliki

```
domain/
  Sha256.java                   wspólne hashowanie hex (wyjęte z WaitlistSignupService)
domain/waitlist/
  OpaqueToken.java              32 bajty z SecureRandom, hex, SHA-256, kontrola kształtu
  ConfirmationToken.java        token + termin ważności (7 dni)
domain/mail/
  OutboundMail.java             record: odbiorca, temat, html, tekst, klucz deduplikacji
application/waitlist/
  ConfirmationOutcome.java      sealed: Confirmed | AlreadyConfirmed | Expired | Unknown
  UnsubscribeOutcome.java       sealed: Unsubscribed | Unknown
  WaitlistConfirmationService.java   rejestracja (transakcyjnie: wiersz + mail), potwierdzenie,
                                     ponowienie, wypis
  WaitlistMailComposer.java          składa OutboundMail z szablonu i locale subskrybenta
application/mail/
  MailQueue.java                     wstawia do outboxu (używane też przez PIO-76)
infrastructure/waitlist/
  WaitlistSubscriberStore.java       rozszerzone o operacje na tokenach (z PIO-70)
infrastructure/mail/
  MailOutboxStore.java               JdbcTemplate: wstaw, pobierz należne, oznacz wysłane/nieudane
  MailOutboxSenderJob.java           @SchedulerLock, uruchamiany z własnej puli
  MailTransport.java                 seam wysyłkowy
  SmtpMailTransport.java             @Profile("!dev"): JavaMailSender → SMTP
  FilesystemMailTransport.java       @Profile("dev"): zapis .eml na dysk zamiast SMTP (D5)
  MimeMailComposer.java              jeden MimeMessage dla obu transportów
  MailConfigurationGuard.java        @Profile("!dev"): brak spring.mail.host przerywa start (R5)
infrastructure/config/
  MailSchedulingConfig.java          własna pula jednowątkowa + rejestracja sendera co 30 s
web/
  WaitlistConfirmationController.java
resources/templates/email/
  waitlist-confirmation.html         Thymeleaf, inline style, hexy, 600 px, układ tabelowy
```

Warstwy: kontroler w `web`, orkiestracja w `application`, SQL i SMTP w `infrastructure`, reguły
tokenu w `domain`. `ArchitectureTest` to weryfikuje.

Frontend:

```
views/WaitlistConfirmView.vue        cztery stany: ok / already / expired+ponów / unknown
views/WaitlistUnsubscribeView.vue    dwa stany: ok / unknown
router/routes.ts                     trzy nowe trasy publiczne
i18n/messages/waitlist.{pl,en}.ts    komunikaty stron
```

## Wysyłka

**Outbox.** `MailQueue.enqueue(OutboundMail)` wstawia wiersz ze statusem `pending`
i `next_attempt_at = now()`. Wstawienie jest w tej samej transakcji co zmiana statusu subskrybenta,
więc albo mamy oba, albo żadnego — to jest cały sens outboxu.

**Sender.** Zadanie co 30 sekund, pod blokadą ShedLock, na własnej jednowątkowej puli
(nie na tej od heartbeatu — SMTP potrafi wisieć). **Korekta:** rejestrujemy je ręcznie na własnym
`ThreadPoolTaskScheduler`, a nie adnotacją `@Scheduled` — `SchedulingConfig` wpina pulę heartbeatu
jako scheduler całego rejestratora zadań, więc `@Scheduled` trafiłoby dokładnie tam, gdzie nie wolno. Pobiera do 20 należnych wierszy jednym
`UPDATE ... RETURNING` ze statusem `sending`, więc dwa procesy nigdy nie wyślą tego samego maila.
Przejęcie wiersza jest **dzierżawą**: ustawia `next_attempt_at = teraz + 10 min`, a warunek wyboru
bierze `status IN ('pending','sending') AND next_attempt_at <= teraz`. Dzięki temu wiersz porzucony
przez proces, który padł w trakcie wysyłki, wraca do obiegu sam, bez osobnego zadania odzyskującego.
Sukces → `sent` + `sent_at`. Błąd → `attempts + 1`, `last_error`, `next_attempt_at` przesunięte
wykładniczo (1 min, 5 min, 25 min, 2 h, 10 h); po piątej próbie status `failed` i wpis
w logu na poziomie `ERROR` — to jedyny przypadek, w którym wolno zalogować coś głośniej niż INFO,
bo nieodebrane potwierdzenie to realna utrata subskrybenta.

**Sprzątanie.** Wiersze `sent` starsze niż 30 dni usuwa istniejące zadanie `ExpiredRowsCleanupJob`.
Wiersze `failed` zostają — są dowodem, że coś nie doszło.

## Przechwytywanie maili w dev (D5)

Suita E2E musi odczytać link z maila, żeby domknąć pętlę double opt-in. Dokładanie kontenera
Mailpit zaprzeczyłoby zasadzie „Postgres jest jedyną usługą wspierającą", więc zamiast tego:

- w profilu `dev` bean `FilesystemMailSender` zapisuje każdą wiadomość jako `.eml`
  w `${kivvi.mail.dev-directory:./var/mail}` zamiast łączyć się z SMTP;
- `compose.yaml` ustawia `SPRING_PROFILES_ACTIVE` (domyślnie `dev`; overlay produkcyjny nadpisuje go
  na `prod`) i podmontowuje `./var/mail` do kontenera — **korekta**: bez tego pliki `.eml` powstają
  wyłącznie wewnątrz kontenera, a suita Playwright działa na hoście i nie ma ich jak przeczytać;
- test E2E zapisuje adres, czeka na plik i wyciąga z niego link potwierdzający;
- w produkcji bean w ogóle nie powstaje (`@Profile("dev")`), więc nie ma ryzyka, że maile cicho
  wylądują na dysku zamiast u odbiorcy. Brak skonfigurowanego `spring.mail.host` poza profilem
  `dev` **przerywa start aplikacji** — lepiej nie wstać niż udawać, że wysyłamy.

## Plan wykonania

### Krok 1 — schemat i outbox

1. `V3__waitlist_confirmation.sql`.
2. `MailOutboxStoreIT` (Testcontainers): wstaw, pobierz należne, deduplikacja po `dedup_key`,
   oznaczenie wysłanym, oznaczenie nieudanym z backoffem, dwa równoległe pobrania nie dostają tego
   samego wiersza.
3. `MailOutboxStore` na `JdbcTemplate`.

**Bramka:** pełna bramka walidacji z `.ai/agentic.config.json`.

### Krok 2 — token potwierdzający

1. `ConfirmationTokenTest`: token ma 64 znaki hex, dwa wywołania nie dają tej samej wartości,
   hash jest stabilny, ważność liczona na 7 dni, token po terminie jest wygasły.
2. `ConfirmationToken` w `domain.waitlist`.

**Bramka:** pełna bramka.

### Krok 3 — szablon i skład maila

1. Dodanie `spring-boot-starter-thymeleaf` i `spring-boot-starter-mail` (wersje z BOM-u).
2. `WaitlistMailComposerTest`: szablon renderuje się w PL i EN, zawiera link potwierdzający i link
   wypisu, **nie zawiera ciągu `oklch(`**, część tekstowa nie jest pusta, żaden placeholder nie
   został nierozwinięty.
3. `templates/email/waitlist-confirmation.html` + klucze w `messages_pl/en.properties`.
4. `WaitlistMailComposer`.

**Bramka:** pełna bramka.

### Krok 4 — sender

1. `MailOutboxSenderJobTest` na atrapie `JavaMailSender`: wysyła należne, oznacza wysłane, przy
   wyjątku ponawia z backoffem, po piątej próbie oznacza `failed`.
2. `MailOutboxSenderIT` na **GreenMail**: wiadomość naprawdę dociera na SMTP, ma oba warianty treści
   i poprawnego nadawcę.
3. `MailOutboxSenderJob` + własna pula w nowym `MailSchedulingConfig` + `FilesystemMailTransport`
   dla profilu `dev` i `MailConfigurationGuard` poza nim.
4. Sekcja `spring.mail.*` w `application.yml`, wszystko z env, zero poświadczeń w repo, oraz
   `backend/src/test/resources/application.yml` z atrapą hosta — **korekta**: każdy `*IT` startuje
   pełny kontekst w profilu domyślnym, więc bez tego strażnik z R5 wywróciłby całą suitę.

**Bramka:** pełna bramka.

### Krok 5 — potwierdzenie, ponowienie, wypis

1. `WaitlistConfirmationServiceTest`: potwierdzenie żywym tokenem, token zużyty, token wygasły,
   token nieznany, wypis, wypis powtórzony, ponowny zapis niepotwierdzonego adresu wydaje nowy token
   i nie tworzy drugiego wiersza.
2. `WaitlistConfirmationControllerTest`: cztery stany potwierdzenia, dwa wypisu, `302` po ponowieniu,
   nieznany token przy ponowieniu zachowuje się jak znany.
3. `WaitlistConfirmationApiIT`: pełna ścieżka na prawdziwej bazie — zapis, odczyt maila z outboxu,
   potwierdzenie, ponowne kliknięcie.
4. Implementacja serwisu i kontrolera, wpięcie wysyłki w ścieżkę zapisu z PIO-70.
5. Wpisy w `RouteTable` + `RouteTableTest`.

**Bramka:** pełna bramka.

### Krok 6 — strony

1. `frontend/test/integration/WaitlistConfirmView.spec.ts` i `WaitlistUnsubscribeView.spec.ts`:
   każdy stan renderuje właściwy komunikat, stan „wygasł" pokazuje przycisk ponowienia.
2. Widoki, trasy, katalogi i18n PL i EN.
3. **Dodane w trakcie:** treść „dzięki, jesteś na liście” na landingu mówi dziś nieprawdę — wiersz
   jest dopiero `pending`. Zmieniamy ją w PL i EN na komunikat o wysłanym linku i poprawiamy testy,
   które ją cytują (`WaitlistForm.spec.ts`, `waitlist.spec.ts`).
4. **Dodane w trakcie:** polityka prywatności z PIO-70 obiecuje w sekcji „Komu powierzamy dane”, że
   dostawca poczty **zostanie nazwany, zanim wyjdzie pierwsza wiadomość**. Ten PR włącza wysyłkę,
   więc nazywa Brevo w obu językach (`privacy.pl.ts`, `privacy.en.ts`) — patrz też research, punkt
   4 listy zadań właściciela. Adres kontaktowy w tych katalogach pisze się `piotr{'@'}kivvi.click`:
   vue-i18n czyta goły `@` jako składnię linked-message i odmawia kompilacji.

**Bramka:** pełna bramka.

### Krok 7 — E2E i domknięcie

1. Rozszerzenie `tests/e2e/specs/waitlist.spec.ts`: zapis → odczyt `.eml` z `var/mail` → wejście
   w link → komunikat potwierdzenia → kliknięcie „wypisz się" → komunikat wypisu.
2. Pełna suita headless przeciwko stackowi `-p kivvi-dev`.

**Bramka:** pełna bramka + E2E.

## Kryteria akceptacji → dowód

| Kryterium z ticketa | Dowód |
| --- | --- |
| Po zapisie przychodzi mail z linkiem potwierdzającym | `WaitlistConfirmationApiIT`, `MailOutboxSenderIT` (GreenMail), `waitlist.spec.ts` |
| Kliknięcie linku ustawia `confirmed` i pokazuje stronę potwierdzenia | `WaitlistConfirmationControllerTest`, `WaitlistConfirmView.spec.ts`, E2E |
| Zużyty lub wygasły token pokazuje „link wygasł" z akcją wysłania nowego | `WaitlistConfirmationServiceTest`, `WaitlistConfirmationControllerTest`, `WaitlistConfirmView.spec.ts` |
| Powtórny zapis niepotwierdzonego adresu wysyła nowy link, bez drugiego wpisu | `WaitlistConfirmationServiceTest`, `WaitlistConfirmationApiIT` |
| Link „wypisz się" działa bez logowania i ustawia `unsubscribed` | `WaitlistConfirmationControllerTest`, E2E |
| W bazie widać czas i IP potwierdzenia zgody | `WaitlistConfirmationApiIT` |
| Nieudana wysyłka jest ponawiana, a po wyczerpaniu prób zostaje jako `failed` | `MailOutboxSenderJobTest`, `MailOutboxStoreIT` |

## Ryzyka

| # | Ryzyko | Postępowanie |
| --- | --- | --- |
| R1 | Bez SPF/DKIM/DMARC potwierdzenia trafią do spamu, a zimna domena straci reputację przy pierwszej większej wysyłce. | Krok poza kodem, po stronie właściciela — wypisany w dokumencie research. PR nie jest gotowy do produkcji, dopóki rekordy nie stoją. |
| R2 | `GET` mutujący stan: prefetch linków przez klienta pocztowego albo skaner antywirusowy może potwierdzić adres bez udziału człowieka. | Znane ograniczenie double opt-in w całej branży. Łagodzimy zapisem IP i user-agenta potwierdzenia — skan widać po rozjeździe z IP zapisu. Nie zamieniamy na POST, bo to zabija konwersję. |
| R3 | Token w URL-u trafia do logów serwera i nagłówka `Referer`. | Token jest jednorazowy i kasowany przy użyciu; ważność 7 dni. Strona potwierdzenia nie ładuje żadnego zasobu z obcej domeny, więc `Referer` nie wycieka na zewnątrz. |
| R4 | Wysyłka blokuje scheduler i zatrzymuje heartbeat. | Sender ma własną pulę, nigdy nie współdzieli tej od heartbeatu. Test to sprawdza. |
| R5 | Bean przechwytujący maile w dev trafia przypadkiem na produkcję i wszystko cicho ląduje na dysku. | `@Profile("dev")` + twarde przerwanie startu przy braku `spring.mail.host` poza profilem dev. Osobny test tego pilnuje. |
| R6 | Spec pisany przed merge'em PIO-70 może rozminąć się z tym, co faktycznie wylądowało. | **Zamknięte 2026-09-15**: dokument uzgodniony z `main` na `327049d` przed pierwszą linijką kodu. Rozbieżności spisane niżej. |

## Korekty po merge'u PIO-70

Wykonane 2026-09-15 na `main` w stanie `327049d`, przed pierwszą zmianą w kodzie (R6, „Uwaga
o kolejności”). Każda pozycja jest naniesiona w treści dokumentu powyżej; ta lista mówi, co się
zmieniło i dlaczego.

| # | Spec mówił | Kod mówi | Korekta |
| --- | --- | --- | --- |
| K1 | Kolumny tokenu „istnieją, nullable” — bez nazw. | `V2__waitlist.sql` daje `confirmation_token_hash CHAR(64)`, `confirmation_token_expires_at`, `confirmed_at`. | Potwierdzone bez zmian; nazwy wpisane wprost, żeby `V3` nie zgadywał. |
| K2 | `ConfirmationOutcome` i `UnsubscribeOutcome` w `domain.waitlist`. | PIO-70 trzyma `SignupOutcome` w `application.waitlist`. | Oba wyniki lądują w `application.waitlist`, obok sąsiada. |
| K3 | `dedup_key = waitlist-confirm:<id>:<numer wydania>`. | Numeru wydania nigdzie nie ma i wymagałby kolumny-licznika. | `dedup_key = waitlist-confirm:<hash tokenu>` — hash już jest w bazie i zmienia się przy każdym ponowieniu. |
| K4 | Sender jako `@Scheduled` „na własnej puli”. | `SchedulingConfig.configureTasks` wpina pulę heartbeatu (`poolSize = 1`) jako scheduler **całego** rejestratora, więc każde `@Scheduled` jedzie po niej. | Sender rejestrowany ręcznie w nowym `MailSchedulingConfig`; adnotacja `@Scheduled` byłaby dokładnie tym, czego zabrania R4. |
| K5 | Sprzątanie „dokładamy do `ExpiredRowsCleanupJob`”. | Zadanie ma `LOCK_NAME = "event-dedup-cleanup"` — celowo starą nazwę, bo wiersz istnieje w produkcyjnym `shedlock`. | Dokładamy trzeci sweep, **nie ruszając** nazwy blokady ani `@ConditionalOnProperty`. |
| K6 | `WaitlistSubscriberStore` „rozszerzone o operacje na tokenach”. | `save()` zwraca `boolean` i nie oddaje `id`. | Kontrakt `save()` zostaje; serwis po zapisie odczytuje wiersz po adresie, więc nowy i powtórny zapis idą jedną ścieżką. |
| K7 | Profil `dev` zapisuje `.eml` „na dysk”. | `compose.yaml` nie ustawia `SPRING_PROFILES_ACTIVE` i nie montuje niczego z hosta. | Compose ustawia profil i montuje `./var/mail`; bez tego Playwright (host) nie zobaczy plików z kontenera. |
| K8 | „Brak `spring.mail.host` poza profilem `dev` przerywa start”. | Każdy `*IT` startuje pełny kontekst w profilu domyślnym. | `backend/src/test/resources/application.yml` podaje atrapę hosta i wyłącza sendera; inaczej strażnik wywraca całą suitę. |
| K9 | (brak) | Landing mówi „jesteś na liście” w chwili zapisu, a wiersz jest dopiero `pending`. | Treść w PL i EN zmieniona na komunikat o wysłanym linku; testy cytujące ją poprawione. |
| K10 | (brak) | Polityka prywatności obiecuje nazwać dostawcę poczty **zanim wyjdzie pierwsza wiadomość**. | Ten PR włącza wysyłkę, więc nazywa Brevo w obu katalogach polityki. |
| K11 | (brak) | Ticket ma siódme kryterium akceptacji (ponawianie i `failed`), którego tabela dowodów nie obejmowała. | Dopisane do tabeli „Kryteria akceptacji → dowód”. |
