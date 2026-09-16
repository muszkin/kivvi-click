/**
 * Privacy policy copy (PIO-70, decision D3).
 *
 * A legal document, not marketing copy. The administrator's identifying details, the contact
 * address, the absence of a data protection officer and the hosting arrangement were supplied by
 * the owner on 2026-09-15; do not edit them without him.
 *
 * PIO-71 named the e-mail provider, which this page had promised to do before the first message
 * went out. That promise is now a statement of fact about a subprocessor: the section must be
 * edited in the same change as any switch away from Brevo, or the policy starts describing a
 * company that no longer touches the data.
 *
 * Two statements here are pinned to things the code actually does, and both break silently if the
 * code changes: the "no cookies on the public pages" section is only true while no controller
 * calls request.getSession(true) on a public path (today only LoginService and
 * ImportUploadController do, both behind the panel), and the Google Fonts disclosure is only
 * needed while frontend/index.html still loads them from Google's CDN. Self-hosting the fonts
 * removes a transfer to the United States and lets that section shrink to one sentence.
 *
 * The contact address is written `piotr{'@'}kivvi.click`, not with a bare `@`: vue-i18n reads an
 * unescaped `@` as the start of its linked-message syntax and refuses to compile the string at all
 * ("Invalid linked format"). `{'@'}` is its literal escape and renders as a plain `@`. This is the
 * first catalogue in the project to carry an e-mail address, so there was no precedent to copy.
 *
 * Kept under the `privacyPage` top-level key rather than `privacy`, which PublicLayout.vue
 * already uses for the footer link's label — the i18n merge in src/i18n/index.ts assigns
 * top-level keys, so a collision would silently replace the other catalogue's entry.
 */
export default {
    privacyPage: {
        title: "Polityka prywatności",
        updated: "Ostatnia aktualizacja: 16 września 2026",
        intro: "Ta polityka opisuje, co dzieje się z danymi, które zostawiasz, zapisując się na listę oczekujących kivvi·click. Dotyczy wyłącznie tego zapisu — panel i konta użytkowników nie są jeszcze dostępne publicznie.",
        sections: [
            {
                heading: "Kto jest administratorem",
                body: "Administratorem Twoich danych jest Fairydeck Piotr Mucha, jednoosobowa działalność gospodarcza z siedzibą przy ul. Topolowej 16U, 32-005 Niepołomice, NIP 7321962870. Kontakt w sprawach danych osobowych: piotr{'@'}kivvi.click.",
            },
            {
                heading: "Inspektor ochrony danych",
                body: "Nie powołaliśmy inspektora ochrony danych — nie mamy takiego obowiązku przy tej skali i rodzaju przetwarzania. Wszystkie sprawy dotyczące Twoich danych kieruj bezpośrednio na piotr{'@'}kivvi.click.",
            },
            {
                heading: "Jakie dane zbieramy",
                body: "Przy zapisie na listę oczekujących zapisujemy: podany adres e-mail, datę i godzinę wyrażenia zgody, adres IP, z którego wysłano formularz, identyfikator przeglądarki (user-agent) oraz dokładną treść klauzuli zgody, którą widziałeś w chwili zapisu. Trzy ostatnie służą wyłącznie temu, żeby móc wykazać, na co i kiedy wyraziłeś zgodę.",
            },
            {
                heading: "W jakim celu",
                body: "Żeby skontaktować się z Tobą w sprawie wdrożenia kivvi·click: odpowiedzieć na pytania, umówić rozmowę i przedstawić zakres wsparcia, o który poprosisz. Nie wysyłamy newslettera, nie profilujemy Cię i nie przekazujemy Twojego adresu nikomu w celach marketingowych.",
            },
            {
                heading: "Na jakiej podstawie",
                body: "Na podstawie Twojej zgody — art. 6 ust. 1 lit. a RODO.",
            },
            {
                heading: "Czy podanie danych jest obowiązkowe",
                body: "Nie. Podanie adresu e-mail i wyrażenie zgody są całkowicie dobrowolne. Jedynym skutkiem niepodania danych jest to, że nie skontaktujemy się z Tobą. Nie tracisz przez to niczego innego — serwis pozostaje dostępny tak samo.",
            },
            {
                heading: "Jak długo przechowujemy dane",
                body: "Do zakończenia rozmowy o wdrożeniu i przez maksymalnie 12 miesięcy po ostatnim kontakcie, albo do chwili wycofania zgody — zależnie od tego, co nastąpi wcześniej. Potem usuwamy adres wraz z dowodem zgody.",
            },
            {
                heading: "Jak wycofać zgodę",
                body: "Masz prawo wycofać zgodę w dowolnym momencie — art. 7 ust. 3 RODO. Napisz na piotr{'@'}kivvi.click, a usuniemy Twój adres z listy. Wycofanie zgody jest równie łatwe jak jej wyrażenie i nie wpływa na zgodność z prawem tego, co zrobiliśmy przed jej wycofaniem.",
            },
            {
                heading: "Twoje prawa",
                body: "Masz prawo dostępu do swoich danych, ich sprostowania, usunięcia, ograniczenia przetwarzania, przeniesienia oraz wycofania zgody w dowolnym momencie. Żeby z nich skorzystać, napisz na piotr{'@'}kivvi.click. Masz też prawo wnieść skargę do Prezesa Urzędu Ochrony Danych Osobowych (ul. Stawki 2, 00-193 Warszawa), jeśli uznasz, że przetwarzamy Twoje dane niezgodnie z prawem.",
            },
            {
                heading: "Komu powierzamy dane",
                body: "Twój adres e-mail i dowód zgody przechowujemy wyłącznie na serwerze administratora, stojącym pod adresem siedziby podanym wyżej. Nie korzystamy z zewnętrznego dostawcy hostingu. Wiadomości — potwierdzenie zapisu i powiadomienie o starcie — wysyła w naszym imieniu Brevo (Sendinblue SAS, 106 boulevard Haussmann, 75008 Paryż, Francja), z którym zawarliśmy umowę powierzenia przetwarzania danych. Brevo przetwarza dane w całości na terenie Unii Europejskiej i dostaje wyłącznie to, co jest potrzebne do doręczenia wiadomości: Twój adres e-mail i jej treść. Żadnemu innemu podmiotowi nie przekazujemy tych danych.",
            },
            {
                heading:
                    "Przekazywanie danych poza Europejski Obszar Gospodarczy",
                body: "Strony tego serwisu ładują kroje pisma z Google Fonts (Google Ireland Limited, z infrastrukturą także poza EOG). Oznacza to, że przy wyświetlaniu strony Twoja przeglądarka łączy się z serwerami Google i przekazuje im Twój adres IP — dzieje się to niezależnie od tego, czy zapiszesz się na listę. Poza tym jednym przypadkiem nie przekazujemy danych poza Europejski Obszar Gospodarczy. Pracujemy nad tym, żeby kroje pisma serwować z własnego serwera i usunąć to przekazanie.",
            },
            {
                heading: "Ciasteczka i analityka",
                body: "Strona główna i ta polityka nie zapisują żadnych ciasteczek i nie korzystają z narzędzi analitycznych ani śledzących. Ciasteczko sesyjne powstaje dopiero po zalogowaniu do panelu, który nie jest jeszcze publicznie dostępny.",
            },
            {
                heading: "Automatyczne decyzje i profilowanie",
                body: "Nie podejmujemy wobec Ciebie decyzji w sposób zautomatyzowany i nie profilujemy Cię w rozumieniu art. 22 RODO. Twój adres trafia na listę i czeka tam na jedno powiadomienie — nic poza tym się z nim nie dzieje.",
            },
            {
                heading: "Kontakt",
                body: "We wszystkich sprawach dotyczących danych osobowych: piotr{'@'}kivvi.click. Odpowiadamy najpóźniej w ciągu miesiąca od otrzymania wiadomości.",
            },
            {
                heading: "Zmiany tej polityki",
                body: "Ta wersja obowiązuje od 16 września 2026 i opisuje nowy cel przetwarzania: kontakt w sprawie wdrożenia. Jeśli zmienimy zakres przetwarzania — na przykład zmienimy dostawcę poczty albo uruchomimy konta użytkowników — zaktualizujemy tę stronę i zmienimy datę powyżej.",
            },
        ],
    },
};
