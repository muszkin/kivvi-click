# Narracja open source: koniec z waitlistą i cennikiem

**Status:** spec + plan gotowe do wykonania (2026-09-16). Decyzje właściciela podjęte przed spisaniem.
**Poprzednik:** PIO-120 (licencja MIT + utwardzenie CI) — musi być zmergowane wcześniej, bo blokuje upublicznienie repozytorium.
**Nie zmienia:** nazewnictwa w kodzie i bazie. `waitlist_subscriber`, `/waitlist/confirm`, klasy `Waitlist*` zostają.

## Wynik

Strona przestaje obiecywać start platformy i okres próbny. Mówi to, co jest prawdą: oprogramowanie
jest otwarte na licencji MIT, można je postawić u siebie, a kto woli tego nie robić sam — zostawia
adres i rozmawiamy o wdrożeniu. Cennik znika, bo nie ma czego wyceniać.

## Decyzje (właściciel, 2026-09-16)

| # | Decyzja | Konsekwencja |
| --- | --- | --- |
| D1 | Narracja: **wdrożenie i wsparcie komercyjne**. Software darmowy i otwarty; formularz zbiera kontakt do rozmowy o wdrożeniu. | Zmienia się **cel przetwarzania** w polityce prywatności i treść klauzuli zgody. |
| D2 | **Cennik usuwamy w całości.** | Dotyka dwunastu miejsc, w tym fixture'ów backendu, DTO i asercji w `public.spec.ts`. |
| D3 | Nazewnictwo w kodzie i bazie **zostaje** `waitlist`. | Zero migracji, zero zmian tras, stare linki potwierdzające dalej działają. |
| D4 | Obraz na GHCR **zostaje prywatny**. | Poza zakresem tego zadania; odnotowane, żeby nikt nie „poprawił" tego przy okazji. |

## Dlaczego to nie jest sama zmiana tekstów

**Treść klauzuli zgody jest zapisywana w bazie jako dowód.** Kolumna `consent_text` w
`waitlist_subscriber` przechowuje dokładnie to zdanie, które widział użytkownik w chwili zapisu.
Zmiana copy oznacza, że nowe wiersze niosą nową klauzulę, a stare zostają ze starą — i tak ma być,
bo dowód zgody ma odzwierciedlać to, na co ktoś się faktycznie zgodził.

**Nie wolno przepisać cudzej zgody na nowy cel.** Osoba, która zapisała się „po powiadomienie
o starcie", nie zgodziła się na kontakt handlowy w sprawie wdrożenia. Dziś na produkcji jest jeden
wiersz — testowy adres właściciela — więc problem jest teoretyczny. Gdyby pojawiły się prawdziwe
zapisy przed wdrożeniem tej zmiany, trzeba by je albo zapytać ponownie, albo usunąć. **Sprawdzić
zawartość tabeli przed merge'em.**

**Polityka prywatności jest przywiązana do starej narracji** w trzech sekcjach: „W jakim celu"
(jedno powiadomienie o starcie), „Jak długo przechowujemy dane" (12 miesięcy po powiadomieniu)
i „Czy podanie danych jest obowiązkowe" (nie trafisz na listę oczekujących). Wszystkie trzy do
przepisania, w PL i EN.

## Zakres

### Robimy

- Nowa narracja na landingu: kicker, lead, punkty zaufania, jedna karta funkcji, kroki.
- Usunięcie sekcji cennika wraz z komponentem, danymi z backendu, DTO, tłumaczeniami i asercją E2E.
- Nowa sekcja w miejscu cennika: „Postaw sam albo z nami" — MIT, self-hosting, wsparcie.
- Stopka: „Symfony, PHP" → Java i Vue 3, plus link do repozytorium.
- Copy formularza: etykieta przycisku, mikro-copy i **klauzula zgody**.
- Copy maila potwierdzającego oraz stron potwierdzenia i wypisu.
- Polityka prywatności: cel, retencja, dobrowolność — PL i EN.
- `README.md` pod kątem osoby z zewnątrz: czym to jest, licencja, jak postawić u siebie.

### Nie robimy

- Zmiany nazw w kodzie i bazie (D3).
- Zmiany widoczności obrazu na GHCR (D4).
- Usunięcia funkcji produktu z panelu — zmieniamy opowieść o produkcie, nie produkt.
- Tłumaczenia panelu; angielskie braki w `LandingFixtures` obejmuje PIO-117.

## Copy do wprowadzenia

Polski jest wersją źródłową, angielski tłumaczeniem. Poniższe brzmienia są ustalone — implementacja
ma ich użyć, a nie wymyślać własnych.

### Landing

| Miejsce | Było | Ma być |
| --- | --- | --- |
| Punkty zaufania | „14 dni Pro za darmo", „Bez karty", „Skrypt 2 KB" | „Licencja MIT", „Postawisz u siebie", „Skrypt 2 KB" |
| Karta „Rekomendacje ML" | „…Plan Pro odblokowuje pełen silnik personalizacji." | bez odwołania do planu: „…Collaborative filtering, «kupili też», «podobne», «trending»." |
| Krok 01 | „Wklej snippet" | bez zmian — dotyczy wdrożonej instancji |
| Nawigacja | `#pricing` „Cennik" | `#open-source` „Open source" |
| Stopka | „Zbudowane w Polsce z Symfony, PHP i sporą ilością herbaty" | „Zbudowane w Polsce na Javie i Vue 3. Kod na licencji MIT." + link do repozytorium |

**Lead w hero** — dopisać jedno zdanie o otwartości, nie przepisywać całości:
> „Kivvi-click łączy śledzenie zachowania odwiedzających z automatyzacjami: popupami, e-mailami,
> kuponami i rekomendacjami — wysyłanymi w odpowiednim momencie. Cały kod jest otwarty na licencji
> MIT: możesz postawić go u siebie."

**Nowa sekcja `#open-source`** w miejscu cennika, trzy punkty:
> **Postaw sam albo z nami.** Kod jest na licencji MIT — klonujesz, budujesz, uruchamiasz na swojej
> infrastrukturze i nikomu nic nie płacisz. Jeśli wolisz nie robić tego sam, pomożemy wdrożyć,
> zintegrować z Twoim sklepem i utrzymać.

### Formularz

| Element | Ma być |
| --- | --- |
| Etykieta przycisku | „Porozmawiajmy o wdrożeniu →" |
| Klauzula zgody | „Zgadzam się na kontakt w sprawie wdrożenia kivvi·click na podany adres e-mail." |
| Mikro-copy pod formularzem | „Odezwiemy się w sprawie wdrożenia. Żadnego newslettera, wypisujesz się jednym kliknięciem." |
| Stan po zapisie | „Wysłaliśmy link potwierdzający. Potwierdź adres, a odezwiemy się w sprawie wdrożenia." |

### Mail potwierdzający

| Element | Ma być |
| --- | --- |
| Temat | „Potwierdź swój adres — kivvi·click" |
| Nagłówek | „Potwierdź, że to Twój adres" |
| Treść | „Ktoś — mamy nadzieję, że Ty — zostawił ten adres, żeby porozmawiać o wdrożeniu kivvi·click. Potwierdź, a odezwiemy się." |
| Stopka | bez zmian poza słowem „lista oczekujących" → „kontakt" |

### Polityka prywatności

| Sekcja | Ma być |
| --- | --- |
| W jakim celu | „Żeby skontaktować się z Tobą w sprawie wdrożenia kivvi·click: odpowiedzieć na pytania, umówić rozmowę i przedstawić zakres wsparcia, o który poprosisz. Nie wysyłamy newslettera, nie profilujemy Cię i nie przekazujemy Twojego adresu nikomu w celach marketingowych." |
| Czy podanie danych jest obowiązkowe | skutek niepodania: „nie skontaktujemy się z Tobą" zamiast „nie trafisz na listę oczekujących" |
| Jak długo przechowujemy dane | „Do zakończenia rozmowy o wdrożeniu i przez maksymalnie 12 miesięcy po ostatnim kontakcie, albo do chwili wycofania zgody — zależnie od tego, co nastąpi wcześniej." |

Reszta polityki — administrator, IOD, zakres danych, prawa, podprocesorzy, Google Fonts, ciasteczka,
art. 22, data wejścia w życie — **bez zmian**. Zaktualizować datę „ostatniej aktualizacji"
i „obowiązuje od" na 16 września 2026.

## Powierzchnie do zmiany

```
frontend/src/views/LandingView.vue            sekcja cennika out, sekcja #open-source in
frontend/src/components/molecules/PriceCard.vue   do usunięcia
frontend/src/layouts/PublicLayout.vue         nawigacja + stopka
frontend/src/i18n/messages/landing.{pl,en}.ts klucze cennika out, nowe klucze
frontend/src/i18n/messages/privacy.{pl,en}.ts trzy sekcje
frontend/src/i18n/{pl,en}.ts                  klucze nawigacji
frontend/src/styles/04-patterns.css           reguły .price-card do usunięcia
backend/.../fixtures/LandingFixtures.java     PLANS i rekord Plan out, TRUST_POINTS, jedna karta
backend/.../application/LandingView.java      pole plans out
backend/.../application/LandingViewService.java
backend/.../resources/messages_{pl,en}.properties  copy formularza i maila
backend/.../resources/templates/email/*        copy maila
tests/e2e/specs/public.spec.ts                asercja .price-card do usunięcia
README.md                                      pod kątem osoby z zewnątrz
```

## Plan wykonania

### Krok 1 — sprawdzenie stanu bazy przed zmianą

Odczytać `select email, status, consent_text from waitlist_subscriber` na produkcji. Jeżeli są tam
wiersze inne niż testowy adres właściciela, **zatrzymać się i zapytać** — zmiana celu przetwarzania
nie może po cichu objąć cudzej zgody.

### Krok 2 — backend: dane landingu bez cennika

1. Test: `LandingFixturesTest` i `LandingViewServiceTest` — brak planów w odpowiedzi, nowe punkty
   zaufania, karta funkcji bez odwołania do planu.
2. Usunięcie `PLANS`, rekordu `Plan` i pola `plans` z `LandingView`; aktualizacja `TRUST_POINTS`.
3. Copy formularza i maila w `messages_{pl,en}.properties`.

**Bramka:** pełna bramka z `.ai/agentic.config.json`.

### Krok 3 — frontend: sekcja cennika out, open source in

1. Testy: `landing.spec.ts` (integration) — brak `.price-card`, obecna sekcja `#open-source`,
   nowe punkty zaufania; `WaitlistForm.spec.ts` — nowa etykieta i klauzula.
2. `LandingView.vue`, usunięcie `PriceCard.vue`, reguł `.price-card` z `04-patterns.css`,
   kluczy cennika z katalogów i18n.
3. `PublicLayout.vue`: nawigacja i stopka, link do repozytorium.

**Bramka:** pełna bramka.

### Krok 4 — polityka prywatności

1. `PrivacyView.spec.ts`: asercje na nowy cel i retencję; istniejące asercje na tożsamość
   administratora, adres kontaktowy i brak placeholdera zostają.
2. Trzy sekcje w `privacy.{pl,en}.ts`, nowe daty.

**Bramka:** pełna bramka.

### Krok 5 — README i E2E

1. `README.md` napisany dla kogoś z zewnątrz: czym to jest, licencja, jak postawić u siebie,
   jak uruchomić testy.
2. `public.spec.ts`: asercja `.price-card` out, asercja sekcji open source in.
3. Pełna suita headless na stacku `-p kivvi-dev`, porty 8544/8543.

**Bramka:** pełna bramka + E2E.

## Poza kodem

- **Brevo**: lista id 3 „Waitlist kivvi.click" → nazwa opisująca kontakt w sprawie wdrożenia;
  szkic kampanii id 1 „Waitlist — powiadomienie o starcie" traci sens — do przepisania lub usunięcia.
- **Zmiana widoczności repozytorium na publiczne** — dopiero po zmergowaniu PIO-120.

## Ryzyka

| # | Ryzyko | Postępowanie |
| --- | --- | --- |
| R1 | Zmiana celu przetwarzania obejmie zgody zebrane na stary cel. | Krok 1 sprawdza tabelę przed jakąkolwiek zmianą. |
| R2 | Usunięcie pola `plans` z DTO psuje zbudowany SPA u kogoś z cache'em. | Pole znika razem z kodem, który je czyta; deploy wymienia obie strony naraz. |
| R3 | Nowa sekcja `#open-source` to moja propozycja, nie polecenie właściciela. | Wprost oznaczone w PR jako decyzja do odrzucenia jednym słowem — alternatywą jest samo usunięcie sekcji i pozycji w nawigacji. |
| R4 | Angielska wersja landingu i tak jest częściowo polska (PIO-117). | Nie naprawiamy tu; nowe klucze piszemy od razu w obu językach, żeby nie powiększać długu. |

## Inwentarz uzupełniający (2026-09-16, po przeglądzie kodu)

Miejsca niosące starą narrację, których pierwsza wersja tej specyfikacji nie wymieniała. Znalezione
przy próbie implementacji; wpisane tutaj, żeby nie trzeba było ich szukać drugi raz.

### Objęte tym zadaniem

| Miejsce | Co niesie |
| --- | --- |
| `LandingFixtures.java:63-67` | karta „Rekomendacje ML" warunkuje funkcję planem Pro |
| `LandingFixtures.java:115-116` | punkty zaufania to obietnica triala — samo usunięcie cennika jej nie usuwa |
| `PublicLayout.vue:56` | zdanie o Symfony jest **hardkodowane, nie w i18n** — dlatego angielska wersja już dziś renderuje polski tekst; poprawka wymaga klucza w obu katalogach |
| `.claude/skills/product-spec/SKILL.md:60` | źródłowa kotwica podziału Free/Pro — nietknięta, narracja odrośnie przy następnym planowaniu |
| testy backendu | `LandingApiIT.java:50-52,75`, `LandingControllerTest.java:29-31`, `LandingFixturesTest.java:26-40`, `WaitlistSignupServiceTest.java:31,46`, `WaitlistMailComposerTest.java:47,64` |
| testy frontu i E2E | `frontend/test/unit/landing.spec.ts`, `tests/e2e/specs/public.spec.ts:25-26` |

Asercja E2E wymieniona w tickecie to **jedno z ośmiu miejsc**, w których testy przypinają starą treść.

Klauzula zgody jest zduplikowana w czterech plikach, które komentarze w kodzie każą trzymać co do
słowa identycznie: `landing.pl.ts:27`, `landing.en.ts:21`, `messages_pl.properties:19`,
`messages_en.properties:17`. Zmiana musi objąć wszystkie cztery naraz.

Sekcje polityki do zmiany: `privacy.pl.ts:48-50` (cel), `:56-58` (skutek niepodania), `:60-62`
(retencja), daty w `:32` i `:93-95`. Angielskie odpowiedniki: `:28-30`, `:36-38`, `:40-42`, `:12`,
`:72-74`.

### Poza tym zadaniem — wymaga decyzji właściciela

**Panel zaprzecza landingowi.** `BillingTab.vue:42` wraz z `settings.pl.ts:147-150` i
`settings.en.ts:138-141` pokazuje „Następne odnowienie 1 września 2026 · 149,00 zł netto",
`SettingsFixtures.java:264-269` zasila trzy faktury po 149,00 zł, a `ShellFixtures.java:20` nazywa
demo-workspace „Plan Pro · 3 strony". Strona będzie mówić „otwarte, postawisz u siebie", a panel
jedno kliknięcie dalej zachowuje się jak płatny SaaS.

To nie jest zmiana tekstu, tylko pytanie produktowe: czy panel ma nadal mieć zakładkę rozliczeń.
**Nie ruszamy tego bez decyzji** — do osobnego zadania.

**Dokumenty `context/foundation/*`** (`prd.md:24`, `shape-notes.md:67`, `roadmap.md:43`,
`health-check.md`, `stack-assessment.md`) nadal opisują stack jako PHP/Symfony. To datowane
artefakty sprzed migracji, oznaczone jako nieaktualne w `context/map/INDEX.md` — zostawiamy jako
zapis historyczny, ale w publicznym repozytorium mogą mylić. Do rozważenia osobno.
