# PIO-70 — Landing: zapis na listę oczekujących

**Status:** spec + plan gotowe do wykonania (2026-09-15). Decyzje właściciela podjęte przed spisaniem (sekcja „Decyzje").
**Ticket:** [PIO-70](https://linear.app/piotr-mucha/issue/PIO-70/landing-zapis-na-liste-oczekujacych) · P0 · `growth` · blokuje PIO-71, PIO-72
**Gałąź:** `muszkin/pio-70-landing-zapis-na-liste-oczekujacych`
**Stack po migracji:** Spring Boot 4.1 (Java 25) + Vue 3.5 SPA, Postgres 18 jako jedyny backing service.

## Wynik

Odwiedzający stronę główną zostawia adres e-mail w formularzu w sekcji hero, świadomie zaznaczając
zgodę, i widzi potwierdzenie zapisu. Adres ląduje w Postgresie ze statusem `pending`, źródłem
`landing` i zapisanym dowodem zgody (czas, IP, user-agent, treść klauzuli). Powtórny zapis tego
samego adresu nie tworzy duplikatu i nie wygląda na błąd. Boty i zalew żądań odbijają się od
honeypota i limitu zapytań, zanim cokolwiek trafi do bazy.

## Decyzje (właściciel, 2026-09-15)

| # | Decyzja | Konsekwencja |
| --- | --- | --- |
| D1 | Formularz **zastępuje** primary CTA w hero („Załóż darmowe konto →"). „Zobacz panel demo" zostaje. | `public.spec.ts` asertuje dziś `.hero-cta a` → 2; po zmianie zostaje 1 link + formularz. Test do aktualizacji w tym samym PR. |
| D2 | Rate limiting: **własny licznik w Postgresie**, bez nowej zależności. | Nowa tabela `waitlist_throttle`, atomowy upsert wzorowany na `EventDedupStore`. Trwały po restarcie `api`, zgodny z „Postgres backs everything". |
| D3 | Zgoda RODO: **wymagany checkbox + realna strona `/{locale}/privacy`**. | Zakres PIO-70 rośnie o publiczną trasę, widok, treść PL/EN i wpis w `RouteTable`. Brak zgody = błąd walidacji, nie zapis. |
| D4 | Panel zostaje otwarty (decyzja z tej samej sesji, PIO-75 odłożone). | CTA „Zobacz panel demo" i linki nawigacji publicznej działają bez zmian. |

## Ustalenia z repozytorium (discover)

Te fakty unieważniają część wskazówek technicznych z ticketa, pisanych jeszcze pod Symfony.

| Ustalenie | Dowód | Wpływ |
| --- | --- | --- |
| Formularze to natywny POST dokumentu, nie fetch. Sukces → 302; błąd → ponowny render dokumentu SPA z `data-*` na `<html>`. | `backend/.../web/LoginController.java`, `frontend/src/views/LoginView.vue`, `domain/SpaDocument.java` | Wskazówka „zwykły POST bez JS-owego fetcha" jest **poprawna** dla tego stacku. Zachowujemy wzorzec 1:1. |
| Brak JPA. `pom.xml` ma `starter-session-jdbc` + Flyway, persystencja przez `JdbcTemplate`. | `backend/pom.xml`, `infrastructure/tracking/EventDedupStore.java` | „Encja `Subscriber`" → **record w `domain` + store w `infrastructure`** na `JdbcTemplate`. Żadnego Hibernate'a. |
| Brak `spring-boot-starter-validation` i brak jakiegokolwiek rate-limitera. | `backend/pom.xml` | Walidacja ręczna w `domain` (jest już precedens), limit własny (D2). |
| `EmailValidation` już istnieje, z polskimi komunikatami. | `domain/EmailValidation.java` | Reużywamy zamiast pisać drugi walidator. |
| `Field.vue` nie ma `v-model` — prop `value`, zero emitów. | `components/atoms/Field.vue` | Pasuje do natywnego POST-a. Nie przerabiamy go na kontrolkę kontrolowaną. |
| ArchUnit pilnuje warstw: `infrastructure` dostępne wyłącznie z `application`, brak cykli, `domain` nie zna `web`. | `test/.../architecture/ArchitectureTest.java` | Store i throttle w `infrastructure.waitlist`, serwis w `application.waitlist`, kontroler w `web`. |
| Testy padają na logach `WARN`. | `testsupport/FailOnWarnLogExtension.java` | Odrzucony zapis (błąd walidacji, limit, honeypot) loguje na `INFO`/`DEBUG`, nigdy `WARN`. |
| Stopka publiczna ma już link „polityka prywatności" z `href="#"`. | `layouts/PublicLayout.vue` | D3 wpina się w istniejący link, nie dokłada nowego elementu nawigacji. |
| Stopka publiczna głosi „Zbudowane w Polsce z Symfony, PHP…". | `layouts/PublicLayout.vue` | Nieaktualne po migracji. **Poza zakresem** — osobny drobiazg, zgłoszony w „Obserwacje poboczne". |
| CSS design systemu jest śledzony przez Git i formatowany hookiem `PostToolUse`. | `.claude/settings.json`, `git ls-files frontend/src/styles` | Nowe klasy dopisujemy do `03-components.css`, na istniejących tokenach. Hook formatuje tylko edycje przez Write/Edit — po edycji z Basha trzeba ręcznie `npm run format` / `./mvnw spotless:apply`. |

## Zakres

### Robimy

- Tabela `waitlist_subscriber` + `waitlist_throttle` w nowej migracji Flyway `V2__waitlist.sql`.
- Model domenowy zapisu: normalizacja adresu, walidacja, statusy, źródła.
- Endpoint `POST /{locale}/waitlist` z obsługą: sukcesu (PRG), błędu walidacji, braku zgody,
  duplikatu, honeypota i przekroczenia limitu.
- Formularz w hero landingu na miejscu primary CTA: pole e-mail, checkbox zgody, honeypot,
  mikro-copy, stan „dziękujemy", komunikat błędu przy polu bez utraty wpisanej treści.
- Publiczna strona `/{locale}/privacy` z treścią polityki prywatności PL/EN, wpięta w stopkę.
- Tłumaczenia PL (domyślne) + EN dla wszystkich nowych tekstów.
- Testy: jednostkowe i integracyjne po stronie backendu, unit + integration po stronie frontendu,
  aktualizacja i rozszerzenie suite'u Playwright.

### Nie robimy

- Double opt-in i wysyłki maila potwierdzającego — **PIO-71**. Kolumny tokenu powstają już teraz
  (nullable), żeby PIO-71 nie musiało zmieniać kształtu tabeli.
- Ekranu listy oczekujących i eksportu CSV — **PIO-72**.
- Rejestracji konta i uwierzytelniania — **PIO-73/74**.
- Zamykania panelu i autoryzacji Mercure — **PIO-75** (świadomie odłożone, D4).
- Regulaminu, DPA i strony statusu (pozostałe `href="#"` w stopce).
- Ochrony CSRF — w repo nie ma dziś Spring Security ani żadnego tokenu CSRF, `POST /{locale}/login`
  i `POST /collect` też jej nie mają. Wprowadzenie jej globalnie należy do PIO-74; ten PR nie
  wprowadza mechanizmu, który zaraz trzeba by przepisać. Zapisany jako ryzyko R3.

## Kontrakt

### `POST /{locale:pl|en}/waitlist`

Body: `application/x-www-form-urlencoded`.

| Pole | Znaczenie |
| --- | --- |
| `email` | Adres, normalizowany `trim` + `toLowerCase(Locale.ROOT)`. |
| `consent` | Checkbox zgody. Wymagany; brak = błąd walidacji. |
| `website` | Honeypot. Musi być puste. |

| Sytuacja | Odpowiedź | Efekt w bazie |
| --- | --- | --- |
| Poprawny, nowy adres | `302` → `/{locale}?waitlist=ok` | wiersz `pending`, źródło `landing` |
| Poprawny, już zapisany | `302` → `/{locale}?waitlist=ok` | bez zmian (`ON CONFLICT DO NOTHING`) |
| Honeypot wypełniony | `302` → `/{locale}?waitlist=ok` | brak zapisu |
| Pusty lub błędny adres | `200 text/html` — dokument SPA z `data-waitlist-error`, `data-waitlist-email`, `data-waitlist-consent` | brak zapisu |
| Brak zgody | `200 text/html` jak wyżej, komunikat o zgodzie | brak zapisu |
| Limit przekroczony | `429 text/html` — ten sam dokument z komunikatem o limicie | brak zapisu |

Odpowiedź błędu to dokument SPA, nie JSON — dokładnie tak jak nieudany `POST /{locale}/login`.
`Content-Type` musi być `text/html; charset=UTF-8` (inaczej servlet domyśla się ISO-8859-1 i psuje
polskie znaki — patrz komentarz w `LoginController`).

### Limit zapytań (D2)

Dwa niezależne kubełki, oba w oknie przesuwnym 1 godziny:

- `ip:<adres>` — 10 zapisów na godzinę,
- `email:<znormalizowany adres>` — 3 zapisy na godzinę.

Przekroczenie któregokolwiek kończy się `429`. Adres klienta bierzemy z `HttpServletRequest#getRemoteAddr()`
— `server.forward-headers-strategy: native` sprawia, że Tomcat podstawia tu prawdziwe IP z
`X-Forwarded-For` wyłącznie dla zaufanych proxy, więc nie trzeba czytać nagłówka ręcznie.

## Model danych — `V2__waitlist.sql`

```sql
CREATE TABLE waitlist_subscriber (
    id                            BIGSERIAL PRIMARY KEY,
    email                         VARCHAR(320) NOT NULL UNIQUE,
    status                        VARCHAR(16)  NOT NULL,
    source                        VARCHAR(32)  NOT NULL,
    locale                        VARCHAR(2)   NOT NULL,
    signed_up_at                  TIMESTAMPTZ  NOT NULL,
    consent_at                    TIMESTAMPTZ  NOT NULL,
    consent_ip                    VARCHAR(45),
    consent_user_agent            VARCHAR(512),
    consent_text                  TEXT         NOT NULL,
    confirmation_token_hash       CHAR(64),
    confirmation_token_expires_at TIMESTAMPTZ,
    confirmed_at                  TIMESTAMPTZ
);

CREATE INDEX waitlist_subscriber_status_idx ON waitlist_subscriber (status);

CREATE TABLE waitlist_throttle (
    bucket_key        VARCHAR(160) PRIMARY KEY,
    window_started_at TIMESTAMPTZ  NOT NULL,
    hits              INT          NOT NULL
);
```

- `email` jest kluczem unikalnym i trzymamy go znormalizowanego — to on rozstrzyga duplikat.
- `consent_text` zapisuje **dokładną treść klauzuli**, którą widział użytkownik. Bez tego dowód
  zgody jest bezwartościowy po pierwszej zmianie copy.
- Kolumny tokenu są `NULL` w tym zadaniu; wypełni je PIO-71.
- `waitlist_throttle` czyści się przy okazji istniejącego sweepera (patrz Krok 3).
- `V1__baseline.sql` zostaje nietknięty — to baseline cutoveru.

## Architektura i pliki

```
domain/waitlist/
  SubscriberStatus.java        enum pending | confirmed | unsubscribed
  SignupSource.java            enum landing (na razie jedno źródło)
  WaitlistSignup.java          record: znormalizowany e-mail, locale, źródło, dowód zgody
  WaitlistSignupError.java     enum błędów walidacji + klucz komunikatu
application/waitlist/
  WaitlistSignupService.java   honeypot → limit → walidacja → zapis; zwraca wynik, nie wyjątek
  SignupOutcome.java           sealed: Accepted | Rejected(error) | ThrottleExceeded
infrastructure/waitlist/
  WaitlistSubscriberStore.java JdbcTemplate, INSERT ... ON CONFLICT (email) DO NOTHING
  SignupThrottle.java          interfejs (wzór: EventDedupLedger)
  JdbcSignupThrottle.java      atomowy upsert okna, zwraca czy mieścimy się w limicie
web/
  WaitlistController.java      POST /{locale}/waitlist
```

`SpaDocument.Attributes` rośnie dziś o trzy pola formularza waitlisty, a ma już dwa loginowe.
Zamiast piątego, szóstego i siódmego parametru pozycyjnego zamieniamy ogon rekordu na
`Map<String, String> dataAttributes` (nazwa atrybutu → wartość, kolejność zachowana przez
`LinkedHashMap`). `LoginController` przekazuje `{"login-error", "last-username"}`,
`WaitlistController` — `{"waitlist-error", "waitlist-email", "waitlist-consent"}`. Refaktor dotyka
`domain/SpaDocument.java`, `application/SpaDocumentService.java`, `web/LoginController.java` i
`test/domain/SpaDocumentTest.java`; nie zmienia ani jednego bajtu wysyłanego HTML-a dla istniejących
ścieżek i to jest asercja w teście.

Frontend:

```
views/LandingView.vue          formularz w hero zamiast primary CTA, stan sukcesu, błąd przy polu
views/PrivacyView.vue          nowy widok polityki prywatności
router/routes.ts               trasa privacy
layouts/PublicLayout.vue       stopka: href="#" → /{locale}/privacy
i18n/messages/landing.{pl,en}.ts   klucze waitlist.*
i18n/messages/privacy.{pl,en}.ts   treść polityki
i18n/{pl,en}.ts                rejestracja nowego katalogu
styles/03-components.css       .waitlist-form, .waitlist-consent, .visually-hidden
```

`RouteTable.LOCALE_PREFIXED` dostaje wpis `new Route("privacy", "/privacy", Layout.PUBLIC)` —
bez tego backend odda 404 na bezpośrednie wejście i odświeżenie strony.

## Plan wykonania

Każdy krok kończy się działającym, przetestowanym kawałkiem — kolejność jest taka, żeby czerwony
test poprzedzał implementację (TDD), a bramka dało się odpalić po każdym kroku.

### Krok 1 — schemat i store

1. `V2__waitlist.sql` z obiema tabelami.
2. `WaitlistSubscriberStoreIT` (Testcontainers, wzór `EventDedupStoreIT`): zapis nowego adresu,
   idempotencja duplikatu (`ON CONFLICT DO NOTHING` → 0 wierszy, brak wyjątku), odczyt statusu.
3. `WaitlistSubscriberStore` na `JdbcTemplate`.

**Bramka:** `cd backend && ./mvnw -q verify`

### Krok 2 — limit zapytań

1. `JdbcSignupThrottleIT`: pierwsze trafienie przechodzi, N-te przechodzi, N+1 odpada, okno wygasa
   i licznik startuje od nowa, dwa różne klucze nie mieszają się.
2. `SignupThrottle` + `JdbcSignupThrottle` — jeden atomowy upsert:
   `INSERT ... ON CONFLICT (bucket_key) DO UPDATE SET hits = CASE WHEN window wygasło THEN 1 ELSE hits + 1 END, window_started_at = CASE ... END RETURNING hits, window_started_at`.
   Zwracany `hits` rozstrzyga limit — bez odczytu przed zapisem, więc bez wyścigu.

**Bramka:** `./mvnw -q verify`

### Krok 3 — sprzątanie wygasłych okien

1. `WaitlistThrottleCleanupJobTest` — wzór `EventDedupCleanupJobTest`.
2. Rozszerzenie istniejącego godzinnego zadania (`@Scheduled` + ShedLock) o `DELETE FROM
   waitlist_throttle WHERE window_started_at < ?`. Bez nowego locka i bez nowego harmonogramu —
   dokładamy do istniejącego joba, żeby nie mnożyć infrastruktury.

**Bramka:** `./mvnw -q verify`

### Krok 4 — domena i serwis

1. `WaitlistSignupTest`: normalizacja (`  Ala@SKLEP.PL ` → `ala@sklep.pl`), odrzucenie pustego
   i błędnego adresu (przez `EmailValidation`), odrzucenie braku zgody.
2. `WaitlistSignupServiceTest` na atrapach store'a i throttle'a: honeypot wypełniony → `Accepted`
   bez dotknięcia store'a; limit przekroczony → `ThrottleExceeded` bez dotknięcia store'a;
   duplikat → `Accepted`; kolejność sprawdzeń (honeypot przed limitem, limit przed walidacją —
   tak, żeby bot nie mógł odpytywać limitu ani zgadywać poprawności adresów).
3. `WaitlistSignup`, `SubscriberStatus`, `SignupSource`, `WaitlistSignupError`, `SignupOutcome`,
   `WaitlistSignupService`.

**Bramka:** `./mvnw -q verify`

### Krok 5 — kontroler i dokument SPA

1. Refaktor `SpaDocument.Attributes` na mapę atrybutów; `SpaDocumentTest` dostaje asercję, że
   render bez dodatkowych atrybutów i render loginowy dają **identyczny** HTML jak przed zmianą.
2. `WaitlistControllerTest` (`@WebMvcTest`): 302 na sukces, 200 + `data-waitlist-*` na błędzie,
   429 przy limicie, 302 przy honeypocie, `Content-Type: text/html;charset=UTF-8`.
3. `WaitlistApiIT`: pełna ścieżka na prawdziwej bazie — zapis, duplikat, limit.
4. `WaitlistController`.
5. Komunikaty do `messages_pl.properties` / `messages_en.properties`
   (`waitlist.error.email.empty`, `.email.malformed`, `.consent.required`, `.throttled`).

**Bramka:** `./mvnw -q verify`

### Krok 6 — formularz na landingu

1. `frontend/test/unit/WaitlistForm.spec.ts`: renderuje pole, checkbox i honeypot; honeypot jest
   poza kolejnością tabulacji i ukryty przed czytnikiem ekranu; błąd z `data-waitlist-error` ląduje
   przy polu i zachowuje wpisany adres oraz stan checkboxa.
2. Aktualizacja `frontend/test/integration/landing.spec.ts`: hero ma jeden link CTA i formularz;
   `?waitlist=ok` pokazuje stan „dziękujemy" zamiast formularza.
3. `LandingView.vue` — formularz na miejscu primary CTA, `method="post"`,
   `:action="/${locale}/waitlist"`, `Field` dla e-maila (`type="email"`, `required`), checkbox zgody,
   honeypot `name="website"`, mikro-copy z linkiem do `/{locale}/privacy`.
4. Klucze `waitlist.*` w `landing.pl.ts` / `landing.en.ts` (PL najpierw, EN jako tłumaczenie).
5. `.waitlist-form`, `.waitlist-consent`, `.visually-hidden` w `03-components.css` — wyłącznie na
   istniejących tokenach z `01-tokens.css`, żadnych nowych kolorów.

**Bramka:** `cd frontend && npm run test -- --run && npm run test:integration -- --run && npm run lint && npm run typecheck && npm run format:check && npm run build`

### Krok 7 — strona polityki prywatności (D3)

1. `RouteTable` + `RouteTableTest`: `/pl/privacy` i `/en/privacy` mapują się na layout `PUBLIC`,
   `/de/privacy` dalej daje 404.
2. `frontend/test/integration/PrivacyView.spec.ts`: strona renderuje się w obu językach, stopka
   prowadzi do niej z landingu.
3. `PrivacyView.vue`, trasa w `routes.ts`, `i18n/messages/privacy.{pl,en}.ts`, podpięcie w
   `i18n/{pl,en}.ts`, stopka `PublicLayout.vue`.
4. Treść polityki: kto jest administratorem, jakie dane (adres e-mail, IP, user-agent, czas zgody),
   w jakim celu (powiadomienie o starcie), na jakiej podstawie (zgoda), jak długo, jak wycofać zgodę,
   prawa osoby. **Wymaga akceptacji właściciela przed merge'em — to treść prawna, nie copy
   marketingowe.** Zapisane jako ryzyko R1.

**Bramka:** pełna sekwencja frontendowa jak w kroku 6 + `./mvnw -q verify`

### Krok 8 — E2E i domknięcie

1. Aktualizacja `tests/e2e/specs/public.spec.ts`: `.hero-cta a` → 1, obecność formularza.
2. Nowy `tests/e2e/specs/waitlist.spec.ts`: zapis adresu → potwierdzenie na ekranie; drugi zapis
   tego samego adresu → to samo potwierdzenie; wysłanie bez zgody → komunikat i zachowany adres;
   wejście na `/pl/privacy` ze stopki.
3. Uruchomienie całej suite headless przeciwko stackowi dev.

**Bramka:**
```
HTTP_PORT=8080 HTTPS_PORT=8443 HTTP3_PORT=8443 docker compose -p kivvi-dev up -d --build --wait
cd tests/e2e && npm install && E2E_BASE_URL=https://localhost:8443 npx playwright test --workers=1
```

## Kryteria akceptacji → dowód

| Kryterium z ticketa | Dowód |
| --- | --- |
| Pole e-mail z CTA w hero; po wysłaniu potwierdzenie zapisu | `waitlist.spec.ts` (E2E), `landing.spec.ts` (integration) |
| Adres w tabeli ze statusem `pending` i źródłem `landing` | `WaitlistApiIT`, `WaitlistSubscriberStoreIT` |
| Powtórny zapis bez duplikatu i bez błędu | `WaitlistSubscriberStoreIT`, `WaitlistSignupServiceTest`, `waitlist.spec.ts` |
| Błędny adres wraca z komunikatem przy polu, bez utraty treści | `WaitlistControllerTest`, `WaitlistForm.spec.ts`, `waitlist.spec.ts` |
| Przekroczenie limitu → 429, bez zapisu | `WaitlistControllerTest`, `WaitlistApiIT`, `JdbcSignupThrottleIT` |
| Wypełniony honeypot udaje sukces i nic nie zapisuje | `WaitlistControllerTest`, `WaitlistSignupServiceTest` |
| **(D3)** Brak zgody blokuje zapis, strona polityki istnieje i jest podlinkowana | `WaitlistControllerTest`, `PrivacyView.spec.ts`, `RouteTableTest`, `waitlist.spec.ts` |

## Ryzyka

| # | Ryzyko | Postępowanie |
| --- | --- | --- |
| R1 | Treść polityki prywatności to dokument prawny, a pisze ją agent. | Draft oznaczony w PR jako wymagający akceptacji właściciela. Merge dopiero po jego przeczytaniu. |
| R2 | Refaktor `SpaDocument.Attributes` dotyka ścieżki logowania, którą pokrywa oracle parity. | Asercja „identyczny HTML jak przed zmianą" w `SpaDocumentTest`; `public.spec.ts` (logowanie) zostaje zielony bez modyfikacji. |
| R3 | Brak CSRF na publicznym POST — ktoś może wysłać formularz z obcej strony. | Świadomie poza zakresem: dziś żaden POST w repo go nie ma, mechanizm wchodzi całościowo w PIO-74. Szkoda ogranicza się do zapisu cudzego adresu na listę, a przed tym broni double opt-in (PIO-71). |
| R4 | Limit po IP uderzy w użytkowników za jednym NAT-em. | 10/h po IP to próg, którego normalny ruch nie dotyka; limit po adresie e-mail (3/h) łapie realne nadużycie. |
| R5 | Prod stoi pod `kivvi-click`; dev odpalony bez `-p kivvi-dev` zabije produkcję. | Każde polecenie compose w tym planie ma jawne `-p kivvi-dev`. |

## Obserwacje poboczne (nie w tym PR)

- `PublicLayout.vue` w stopce: „Zbudowane w Polsce z Symfony, PHP i sporą ilością herbaty" —
  nieaktualne po migracji na Javę. Osobny drobiazg do zgłoszenia.
- Stopka ma jeszcze trzy `href="#"`: regulamin, RODO/DPA, status. Poza zakresem.
- `LandingView.vue` renderuje `headline` przez `v-html` na tekście z katalogu i18n — bezpieczne,
  bo źródłem jest kod, nie dane użytkownika. Odnotowane, żeby nikt nie podpiął tam treści z bazy.
