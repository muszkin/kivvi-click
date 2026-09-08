# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: events.spec.ts >> event stream >> a collected event reaches the stream as a rendered row
- Location: specs/events.spec.ts:35:9

# Error details

```
Error: expect(locator).toHaveCount(expected) failed

Locator:  locator('#event-stream').locator('.event-row')
Expected: 30
Received: 31
Timeout:  5000ms

Call log:
  - Expect "toHaveCount" with timeout 5000ms
  - waiting for locator('#event-stream').locator('.event-row')
    14 × locator resolved to 31 elements
       - unexpected value "31"

```

# Page snapshot

```yaml
- generic [ref=e3]:
  - complementary [ref=e4]:
    - generic [ref=e5]: kivviclick
    - button "AS aureashop.pl Plan Pro · 3 strony" [ref=e18] [cursor=pointer]:
      - generic [ref=e19]: AS
      - generic [ref=e20]: aureashop.pl Plan Pro · 3 strony
    - navigation "Nawigacja główna" [ref=e23]:
      - generic [ref=e24]:
        - generic [ref=e25]: Główne
        - link "Pulpit" [ref=e26] [cursor=pointer]:
          - /url: /pl/dashboard
        - link "Strumień zdarzeń ·" [ref=e33] [cursor=pointer]:
          - /url: /pl/events
          - generic [ref=e36]: Strumień zdarzeń
          - generic [ref=e37]: ·
        - link "Klienci" [ref=e38] [cursor=pointer]:
          - /url: /pl/customers
      - generic [ref=e45]:
        - generic [ref=e46]: Automatyzacja
        - link "Reguły" [ref=e47] [cursor=pointer]:
          - /url: /pl/automations
        - link "Kampanie email" [ref=e51] [cursor=pointer]:
          - /url: /pl/campaigns
        - link "Popupy i widgety" [ref=e56] [cursor=pointer]:
          - /url: /pl/popups
      - generic [ref=e60]:
        - generic [ref=e61]: Dane
        - link "Feedy produktów" [ref=e62] [cursor=pointer]:
          - /url: /pl/feeds
        - link "Import klientów" [ref=e68] [cursor=pointer]:
          - /url: /pl/import
      - generic [ref=e73]:
        - generic [ref=e74]: Konfiguracja
        - link "Ustawienia" [ref=e75] [cursor=pointer]:
          - /url: /pl/settings
    - generic [ref=e80]:
      - generic [ref=e81]: MK
      - generic [ref=e82]:
        - generic [ref=e83]: Maciej Kowalczyk
        - generic [ref=e84]: maciej@aureashop.pl
  - main [ref=e85]:
    - generic [ref=e86]:
      - button "Zwiń panel boczny" [ref=e87] [cursor=pointer]
      - navigation "Ścieżka" [ref=e90]:
        - generic [ref=e91]: aureashop.pl
        - generic [ref=e92]: /
        - generic [ref=e93]: Strumień zdarzeń
      - button "Szukaj" [ref=e94] [cursor=pointer]:
        - generic [ref=e98]: Szukaj zdarzeń, klientów, reguł…
        - generic [ref=e99]: ⌘K
      - link "PL" [ref=e100] [cursor=pointer]:
        - /url: /en/events
      - button "Motyw" [ref=e102] [cursor=pointer]
      - button "Powiadomienia" [ref=e105] [cursor=pointer]
    - generic [ref=e110]:
      - generic [ref=e111]:
        - generic [ref=e112]:
          - heading "Strumień zdarzeń" [level=1] [ref=e113]
          - paragraph [ref=e114]: Wszystkie zdarzenia odebrane ze skryptu trackingowego, z możliwością filtrowania.
        - generic [ref=e115]:
          - generic [ref=e116]: na żywo
          - button "Pauza" [ref=e119] [cursor=pointer]
          - button "Eksport (CSV)" [ref=e124] [cursor=pointer]
          - link "Webhook" [ref=e129] [cursor=pointer]:
            - /url: /pl/settings/api
      - generic [ref=e134]:
        - generic [ref=e135]: "Typ:"
        - button "Wszystkie" [ref=e136] [cursor=pointer]
        - button "Wyświetlenie strony" [ref=e137] [cursor=pointer]
        - button "Dodanie do koszyka" [ref=e141] [cursor=pointer]
        - button "Zakup" [ref=e146] [cursor=pointer]
        - button "Zalogowanie" [ref=e149] [cursor=pointer]
        - button "Rejestracja" [ref=e153] [cursor=pointer]
        - button "Wyszukiwanie" [ref=e157] [cursor=pointer]
        - button "Porzucony koszyk" [ref=e161] [cursor=pointer]
        - button "Lista życzeń" [ref=e166] [cursor=pointer]
      - generic [ref=e169]:
        - generic [ref=e170]: "Strona:"
        - button "Wszystkie" [ref=e171] [cursor=pointer]
        - button "aureashop.pl" [ref=e175] [cursor=pointer]
        - button "mlot-narzedzia.pl" [ref=e177] [cursor=pointer]
        - button "polna-bistro.pl" [ref=e179] [cursor=pointer]
        - generic [ref=e181]: "Okres:"
        - tablist [ref=e182]:
          - tab "5 min" [ref=e183] [cursor=pointer]
          - tab "1 godz." [selected] [ref=e184] [cursor=pointer]
          - tab "24 godz." [ref=e185] [cursor=pointer]
          - tab "7 dni" [ref=e186] [cursor=pointer]
      - generic [ref=e187]:
        - generic [ref=e188]:
          - heading "Log zdarzeń" [level=3] [ref=e189]
          - generic [ref=e194]: 9 360 zdarzeń w wybranym oknie
          - button "Szukaj w logu" [ref=e196] [cursor=pointer]
        - generic [ref=e201]:
          - generic [ref=e202] [cursor=pointer]:
            - generic [ref=e203]: 22:02:02
            - generic [ref=e208]: Rejestracja
            - generic [ref=e209]: iga-1788904922729@example.com
            - generic [ref=e210]:
              - generic [ref=e211]: IP
              - generic [ref=e212]: Iga Pszczółkowska
            - generic [ref=e213]: aureashop.pl
          - generic [ref=e216] [cursor=pointer]:
            - generic [ref=e217]: 22:02:02
            - generic [ref=e223]: Dodanie do koszyka
            - generic [ref=e224]: Zielona herbata Sencha 100g
            - generic [ref=e225]:
              - generic [ref=e226]: AK
              - generic [ref=e227]: Anna K.
            - generic [ref=e228]: aureashop.pl
          - generic [ref=e231] [cursor=pointer]:
            - generic [ref=e232]: 22:01:50
            - generic [ref=e237]: Rejestracja
            - generic [ref=e238]: lukasz.n@example.com
            - generic [ref=e239]:
              - generic [ref=e240]: ŁN
              - generic [ref=e241]: Łukasz N.
            - generic [ref=e242]: polna-bistro.pl
          - generic [ref=e245] [cursor=pointer]:
            - generic [ref=e246]: 22:01:38
            - generic [ref=e250]: Lista życzeń
            - generic [ref=e251]: Świeca sojowa „Figa”
            - generic [ref=e252]:
              - generic [ref=e253]: HC
              - generic [ref=e254]: Hania C.
            - generic [ref=e255]: mlot-narzedzia.pl
          - generic [ref=e258] [cursor=pointer]:
            - generic [ref=e259]: 22:01:26
            - generic [ref=e263]: Zakup
            - generic [ref=e264]: 230,00 PLN · 4 produkty
            - generic [ref=e265]:
              - generic [ref=e266]: AP
              - generic [ref=e267]: Anna P.
            - generic [ref=e268]: aureashop.pl
          - generic [ref=e271] [cursor=pointer]:
            - generic [ref=e272]: 22:01:14
            - generic [ref=e277]: Wyszukiwanie
            - generic [ref=e278]: „świeca”
            - generic [ref=e279]:
              - generic [ref=e280]: ŁK
              - generic [ref=e281]: Łukasz K.
            - generic [ref=e282]: polna-bistro.pl
          - generic [ref=e285] [cursor=pointer]:
            - generic [ref=e286]: 22:01:02
            - generic [ref=e291]: Wyświetlenie strony
            - generic [ref=e292]: /produkt/zielona-herbata-sencha
            - generic [ref=e293]:
              - generic [ref=e294]: KN
              - generic [ref=e295]: Kasia N.
            - generic [ref=e296]: mlot-narzedzia.pl
          - generic [ref=e299] [cursor=pointer]:
            - generic [ref=e300]: 22:00:50
            - generic [ref=e305]: Zalogowanie
            - generic [ref=e306]: olek.c@example.com
            - generic [ref=e307]:
              - generic [ref=e308]: OC
              - generic [ref=e309]: Olek C.
            - generic [ref=e310]: aureashop.pl
          - generic [ref=e313] [cursor=pointer]:
            - generic [ref=e314]: 22:00:38
            - generic [ref=e320]: Porzucony koszyk
            - generic [ref=e321]: Po 8:42 minut
            - generic [ref=e322]:
              - generic [ref=e323]: WP
              - generic [ref=e324]: Wojtek P.
            - generic [ref=e325]: polna-bistro.pl
          - generic [ref=e328] [cursor=pointer]:
            - generic [ref=e329]: 22:00:26
            - generic [ref=e335]: Dodanie do koszyka
            - generic [ref=e336]: Plecak canvas Olive
            - generic [ref=e337]:
              - generic [ref=e338]: KK
              - generic [ref=e339]: Kasia K.
            - generic [ref=e340]: mlot-narzedzia.pl
          - generic [ref=e343] [cursor=pointer]:
            - generic [ref=e344]: 22:00:14
            - generic [ref=e349]: Rejestracja
            - generic [ref=e350]: olek.n@example.com
            - generic [ref=e351]:
              - generic [ref=e352]: "ON"
              - generic [ref=e353]: Olek N.
            - generic [ref=e354]: aureashop.pl
          - generic [ref=e357] [cursor=pointer]:
            - generic [ref=e358]: 22:00:02
            - generic [ref=e362]: Lista życzeń
            - generic [ref=e363]: Zielona herbata Sencha 100g
            - generic [ref=e364]:
              - generic [ref=e365]: MC
              - generic [ref=e366]: Marta C.
            - generic [ref=e367]: polna-bistro.pl
          - generic [ref=e370] [cursor=pointer]:
            - generic [ref=e371]: 21:59:50
            - generic [ref=e375]: Zakup
            - generic [ref=e376]: 256,00 PLN · 4 produkty
            - generic [ref=e377]:
              - generic [ref=e378]: MP
              - generic [ref=e379]: Magda P.
            - generic [ref=e380]: mlot-narzedzia.pl
          - generic [ref=e383] [cursor=pointer]:
            - generic [ref=e384]: 21:59:38
            - generic [ref=e389]: Wyszukiwanie
            - generic [ref=e390]: „pasta”
            - generic [ref=e391]:
              - generic [ref=e392]: JK
              - generic [ref=e393]: Justyna K.
            - generic [ref=e394]: aureashop.pl
          - generic [ref=e397] [cursor=pointer]:
            - generic [ref=e398]: 21:59:26
            - generic [ref=e403]: Wyświetlenie strony
            - generic [ref=e404]: /produkt/pasta-pomidorowa
            - generic [ref=e405]:
              - generic [ref=e406]: MN
              - generic [ref=e407]: Marta N.
            - generic [ref=e408]: polna-bistro.pl
          - generic [ref=e411] [cursor=pointer]:
            - generic [ref=e412]: 21:59:14
            - generic [ref=e417]: Zalogowanie
            - generic [ref=e418]: magda.c@example.com
            - generic [ref=e419]:
              - generic [ref=e420]: MC
              - generic [ref=e421]: Magda C.
            - generic [ref=e422]: mlot-narzedzia.pl
          - generic [ref=e425] [cursor=pointer]:
            - generic [ref=e426]: 21:59:02
            - generic [ref=e432]: Porzucony koszyk
            - generic [ref=e433]: Po 3:12 minut
            - generic [ref=e434]:
              - generic [ref=e435]: TP
              - generic [ref=e436]: Tomek P.
            - generic [ref=e437]: aureashop.pl
          - generic [ref=e440] [cursor=pointer]:
            - generic [ref=e441]: 21:58:50
            - generic [ref=e447]: Dodanie do koszyka
            - generic [ref=e448]: Filiżanka porcelanowa Nora
            - generic [ref=e449]:
              - generic [ref=e450]: BK
              - generic [ref=e451]: Bartek K.
            - generic [ref=e452]: polna-bistro.pl
          - generic [ref=e455] [cursor=pointer]:
            - generic [ref=e456]: 21:58:38
            - generic [ref=e461]: Rejestracja
            - generic [ref=e462]: karol.n@example.com
            - generic [ref=e463]:
              - generic [ref=e464]: KN
              - generic [ref=e465]: Karol N.
            - generic [ref=e466]: mlot-narzedzia.pl
          - generic [ref=e469] [cursor=pointer]:
            - generic [ref=e470]: 21:58:26
            - generic [ref=e474]: Lista życzeń
            - generic [ref=e475]: Plecak canvas Olive
            - generic [ref=e476]:
              - generic [ref=e477]: TC
              - generic [ref=e478]: Tomek C.
            - generic [ref=e479]: aureashop.pl
          - generic [ref=e482] [cursor=pointer]:
            - generic [ref=e483]: 21:58:14
            - generic [ref=e487]: Zakup
            - generic [ref=e488]: 282,00 PLN · 4 produkty
            - generic [ref=e489]:
              - generic [ref=e490]: BP
              - generic [ref=e491]: Bartek P.
            - generic [ref=e492]: polna-bistro.pl
          - generic [ref=e495] [cursor=pointer]:
            - generic [ref=e496]: 21:58:02
            - generic [ref=e501]: Wyszukiwanie
            - generic [ref=e502]: „olej kokosowy”
            - generic [ref=e503]:
              - generic [ref=e504]: PK
              - generic [ref=e505]: Piotr K.
            - generic [ref=e506]: mlot-narzedzia.pl
          - generic [ref=e509] [cursor=pointer]:
            - generic [ref=e510]: 21:57:50
            - generic [ref=e515]: Wyświetlenie strony
            - generic [ref=e516]: /produkt/filizanka-porcelana
            - generic [ref=e517]:
              - generic [ref=e518]: IN
              - generic [ref=e519]: Iga N.
            - generic [ref=e520]: aureashop.pl
          - generic [ref=e523] [cursor=pointer]:
            - generic [ref=e524]: 21:57:38
            - generic [ref=e529]: Zalogowanie
            - generic [ref=e530]: sandra.c@example.com
            - generic [ref=e531]:
              - generic [ref=e532]: SC
              - generic [ref=e533]: Sandra C.
            - generic [ref=e534]: polna-bistro.pl
          - generic [ref=e537] [cursor=pointer]:
            - generic [ref=e538]: 21:57:26
            - generic [ref=e544]: Porzucony koszyk
            - generic [ref=e545]: Po 14:09 minut
            - generic [ref=e546]:
              - generic [ref=e547]: PP
              - generic [ref=e548]: Piotr P.
            - generic [ref=e549]: mlot-narzedzia.pl
          - generic [ref=e552] [cursor=pointer]:
            - generic [ref=e553]: 21:57:14
            - generic [ref=e559]: Dodanie do koszyka
            - generic [ref=e560]: Pasta z pomidorów
            - generic [ref=e561]:
              - generic [ref=e562]: AK
              - generic [ref=e563]: Anna K.
            - generic [ref=e564]: aureashop.pl
          - generic [ref=e567] [cursor=pointer]:
            - generic [ref=e568]: 21:57:02
            - generic [ref=e573]: Rejestracja
            - generic [ref=e574]: lukasz.n@example.com
            - generic [ref=e575]:
              - generic [ref=e576]: ŁN
              - generic [ref=e577]: Łukasz N.
            - generic [ref=e578]: polna-bistro.pl
          - generic [ref=e581] [cursor=pointer]:
            - generic [ref=e582]: 21:56:50
            - generic [ref=e586]: Lista życzeń
            - generic [ref=e587]: Filiżanka porcelanowa Nora
            - generic [ref=e588]:
              - generic [ref=e589]: HC
              - generic [ref=e590]: Hania C.
            - generic [ref=e591]: mlot-narzedzia.pl
          - generic [ref=e594] [cursor=pointer]:
            - generic [ref=e595]: 21:56:38
            - generic [ref=e599]: Zakup
            - generic [ref=e600]: 308,00 PLN · 4 produkty
            - generic [ref=e601]:
              - generic [ref=e602]: AP
              - generic [ref=e603]: Anna P.
            - generic [ref=e604]: aureashop.pl
          - generic [ref=e607] [cursor=pointer]:
            - generic [ref=e608]: 21:56:26
            - generic [ref=e613]: Wyszukiwanie
            - generic [ref=e614]: „herbata”
            - generic [ref=e615]:
              - generic [ref=e616]: ŁK
              - generic [ref=e617]: Łukasz K.
            - generic [ref=e618]: polna-bistro.pl
          - generic [ref=e621] [cursor=pointer]:
            - generic [ref=e622]: 21:56:14
            - generic [ref=e627]: Wyświetlenie strony
            - generic [ref=e628]: /produkt/plecak-canvas
            - generic [ref=e629]:
              - generic [ref=e630]: KN
              - generic [ref=e631]: Kasia N.
            - generic [ref=e632]: mlot-narzedzia.pl
        - generic [ref=e635]:
          - generic [ref=e636]: Pokazuję ostatnie 30 z 9 360
          - button "Załaduj więcej" [ref=e638] [cursor=pointer]
```

# Test source

```ts
  1  | import { expect, test } from "@playwright/test";
  2  | 
  3  | /**
  4  |  * The event log: 30 server-rendered rows, filter rails, and a live row arriving
  5  |  * over Mercure as ready-made HTML.
  6  |  */
  7  | test.describe("event stream", () => {
  8  |     test("renders thirty rows with type, detail and site", async ({ page }) => {
  9  |         await page.goto("/pl/events");
  10 | 
  11 |         const rows = page.locator("#event-stream .event-row");
  12 |         await expect(rows).toHaveCount(30);
  13 | 
  14 |         const first = rows.first();
  15 |         await expect(first.locator(".event-row__type")).not.toBeEmpty();
  16 |         await expect(first.locator(".event-row__site")).not.toBeEmpty();
  17 |     });
  18 | 
  19 |     test("offers every event type and every tracked site as a filter", async ({
  20 |         page,
  21 |     }) => {
  22 |         await page.goto("/pl/events");
  23 | 
  24 |         await expect(
  25 |             page.locator(".events-toolbar").first().locator(".filter-chip"),
  26 |         ).toHaveCount(9);
  27 |         await expect(
  28 |             page.locator(".events-toolbar").nth(1).locator(".filter-chip"),
  29 |         ).toHaveCount(4);
  30 |         await expect(
  31 |             page.locator('.seg [data-action="set-range"]'),
  32 |         ).toHaveCount(4);
  33 |     });
  34 | 
  35 |     test("a collected event reaches the stream as a rendered row", async ({
  36 |         page,
  37 |         request,
  38 |     }) => {
  39 |         await page.goto("/pl/events");
  40 |         const stream = page.locator("#event-stream");
> 41 |         await expect(stream.locator(".event-row")).toHaveCount(30);
     |                                                    ^ Error: expect(locator).toHaveCount(expected) failed
  42 |         // Mercure has no replay: publish only once the subscription is open.
  43 |         await expect(stream).toHaveAttribute("data-stream-state", "live");
  44 | 
  45 |         // Other specs publish to the same topic, so recognise this event by its own detail.
  46 |         const marker = `412,00 PLN · zamówienie ${Date.now()}`;
  47 |         const response = await request.post("/collect", {
  48 |             data: {
  49 |                 idempotency_id: `e2e-${Date.now()}`,
  50 |                 type: "purchase",
  51 |                 detail: marker,
  52 |                 customer_id: "c_1001",
  53 |                 customer_name: "Hania Kowalska",
  54 |                 site: "aureashop.pl",
  55 |             },
  56 |         });
  57 |         expect(response.status()).toBe(202);
  58 | 
  59 |         const arrived = stream.locator(".event-row", { hasText: marker });
  60 |         await expect(arrived).toHaveCount(1, { timeout: 10_000 });
  61 |         await expect(arrived).toHaveClass(/new/);
  62 |         await expect(arrived.locator(".event-row__type")).toHaveText("Zakup");
  63 |         await expect(arrived.locator(".event-row__customer")).toContainText(
  64 |             "Hania Kowalska",
  65 |         );
  66 |     });
  67 | 
  68 |     test("a replayed event is not shown twice", async ({ page, request }) => {
  69 |         await page.goto("/pl/events");
  70 |         const stream = page.locator("#event-stream");
  71 |         await expect(stream).toHaveAttribute("data-stream-state", "live");
  72 | 
  73 |         // The topic is shared, so identify this test's own event by its detail line
  74 |         // rather than by counting every row that flashed in.
  75 |         const marker = `iga-${Date.now()}@example.com`;
  76 |         const payload = {
  77 |             idempotency_id: `e2e-dedup-${Date.now()}`,
  78 |             type: "signup",
  79 |             detail: marker,
  80 |             customer_id: "c_1002",
  81 |             customer_name: "Iga Pszczółkowska",
  82 |             site: "aureashop.pl",
  83 |         };
  84 |         const mine = stream.locator(".event-row", { hasText: marker });
  85 | 
  86 |         expect(
  87 |             (await request.post("/collect", { data: payload })).status(),
  88 |         ).toBe(202);
  89 |         await expect(mine).toHaveCount(1, { timeout: 10_000 });
  90 | 
  91 |         expect(
  92 |             (await request.post("/collect", { data: payload })).status(),
  93 |         ).toBe(200);
  94 |         await page.waitForTimeout(1000);
  95 |         await expect(mine).toHaveCount(1);
  96 |     });
  97 | });
  98 | 
```