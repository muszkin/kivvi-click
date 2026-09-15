# Research spike — dostawca wysyłki transakcyjnej

**Status:** zdecydowane (właściciel, 2026-09-15) — **Brevo**.
**Data:** 2026-09-15
**Pytanie:** czym wysyłać maile transakcyjne z kivvi.click, skoro domena nie ma dziś skrzynki nadawczej, a stack nie ma żadnej infrastruktury mailowej?
**Wywołane przez:** PIO-71 (double opt-in). Wybór wiąże też PIO-76 (reset hasła) i przyszły moduł kampanii e-mail.
**Docelowe miejsce w repo:** `context/research/2026-09-15-transactional-email-provider.md`

## Kryteria

1. **RODO / rezydencja danych** — polski SaaS przetwarzający adresy klientów europejskich sklepów. Procesor w EU bez standardowych klauzul umownych upraszcza całą historię przy pierwszym audycie.
2. **Koszt na starcie** — waitlista i resety hasła to setki maili miesięcznie, nie miliony. Free tier musi to unieść.
3. **Relay SMTP** — implementacja idzie przez `spring.mail.*`, więc dostawca musi dawać zwykły SMTP, nie tylko własne API.
4. **Droga wzrostu** — kampanie e-mail są rdzeniem produktu, nie dodatkiem. Ta sama szyna powinna je udźwignąć.

## Rozpatrzone opcje

| Dostawca | Rezydencja | Free | Dalej | Werdykt |
| --- | --- | --- | --- | --- |
| **Brevo** (FR) | całość przetwarzania w EU, jurysdykcja CNIL, DPA bez SCC | 300/dzień ≈ 9 000/mies. | od ~$9 za 5 000/mies. | **wybrane** |
| Scaleway TEM (FR) | DC w EU | 300/mies. | €0,25 / 1 000, bez opłaty stałej | najtańszy przy skali, ale free tier nie pokrywa nawet startu, brak części marketingowej, mało dowodów na dostarczalność przy zimnej domenie |
| Resend (US) | wysyłka może iść z Irlandii, ale konto, logi i metadane zostają w USA | 3 000/mies. | od $20 | najlepsze DX, odrzucone ze względu na rezydencję |
| Postmark (US) | USA | 100/mies. (tylko testy) | od $15 | najlepsza reputacja dostarczalności, ale płacimy od pierwszego dnia i dane w USA |

## Decyzja

**Brevo.** Jedyna opcja, która spełnia wszystkie cztery kryteria naraz: przetwarzanie w całości w EU
z DPA bez SCC, free tier z zapasem na waitlistę i resety hasła, relay SMTP, oraz ta sama platforma
obsługująca wysyłkę marketingową, kiedy dojdziemy do kampanii.

Scaleway TEM byłby tańszy przy dużym wolumenie i równie europejski — zostaje jako realna ścieżka
odwrotu, gdyby koszt Brevo zaczął uwierać. Migracja jest tania, bo integrujemy się przez standardowy
SMTP, nie przez SDK dostawcy: zmiana to podmiana czterech zmiennych środowiskowych.

## Ustalenia techniczne

| Rzecz | Wartość |
| --- | --- |
| Host SMTP | `smtp-relay.brevo.com` |
| Port | `587` (STARTTLS); alternatywnie `465` (SSL/TLS) lub `2525` |
| Użytkownik | adres logowania SMTP z panelu Brevo |
| Hasło | **klucz SMTP**, generowany w SMTP & API → SMTP. **Nie** hasło do konta i **nie** klucz API v3 |
| Limit free | 300 maili/dzień |

Artefakty zweryfikowane w Maven Central pod wersję Boota z projektu (4.1.1):

| Artefakt | Wersja | Uwaga |
| --- | --- | --- |
| `org.springframework.boot:spring-boot-starter-mail` | 4.1.1 | istnieje; wersję bierzemy z BOM-u rodzica, nie pinujemy |
| `org.springframework.boot:spring-boot-starter-thymeleaf` | 4.1.1 | j.w.; Boot 4 wydzielił autokonfigurację do modułu `spring-boot-thymeleaf` |
| `com.icegreen:greenmail-junit5` | 2.1.3 | serwer SMTP w JVM do testów, bez Dockera |

## Do zrobienia po stronie właściciela (poza kodem)

1. Założyć konto Brevo i zweryfikować domenę `kivvi.click`.
2. Ustawić rekordy **SPF** i **DKIM** wskazane przez Brevo, oraz **DMARC** na `p=none` na start.
   Bez tego potwierdzenia waitlisty wylądują w spamie, a reputacja zimnej domeny ucierpi przy
   pierwszej większej wysyłce.
3. Wygenerować klucz SMTP i wstawić poświadczenia do `.env.prod.docker` (plik jest ignorowany przez
   Git i Dockera — żadne poświadczenie nie trafia do repozytorium).
4. Podpisać DPA udostępniane przez Brevo i dopisać ich do listy podprocesorów w polityce prywatności
   tworzonej w PIO-70.

## Źródła

- [Brevo — Send transactional emails using Brevo SMTP](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP)
- [Brevo — SMTP relay integration (dokumentacja API)](https://developers.brevo.com/docs/smtp-integration)
- [Brevo — GDPR compliance](https://www.brevo.com/company/gdpr/)
- [Scaleway TEM — European Alternatives](https://european-alternatives.eu/product/scaleway-transactional-email)
- [Nuntly — Resend pricing 2026 (rezydencja danych)](https://nuntly.com/alternatives/resend-eu)
- [Spring — Modularizing Spring Boot](https://spring.io/blog/2025/10/28/modularizing-spring-boot/)
