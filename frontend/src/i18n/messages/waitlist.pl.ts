/**
 * The pages a waitlist confirmation mail leads to (PIO-71).
 *
 * Kept under `waitlistPage`, not `waitlist`: `landingPage.waitlist` already holds the form's copy,
 * and the i18n merge in src/i18n/index.ts assigns top-level keys — a collision would silently
 * replace another catalogue's entry (see the note in landing.pl.ts).
 *
 * Each state says what happened and what, if anything, to do next. "Unknown" deliberately never
 * says whether the address exists: the pages are reachable by anyone with a URL, and a different
 * answer for a real address would turn them into a way of testing who is on the list.
 */
export default {
    waitlistPage: {
        confirm: {
            ok: {
                title: "Gotowe — jesteś na liście",
                body: "Potwierdziliśmy Twój adres. Odezwiemy się dokładnie raz: kiedy kivvi·click ruszy.",
            },
            already: {
                title: "Ten adres jest już potwierdzony",
                body: "Nie musisz nic więcej robić — jesteś na liście i czekasz na jedno powiadomienie o starcie.",
            },
            expired: {
                title: "Ten link już wygasł",
                body: "Link potwierdzający jest ważny 7 dni. Wyślemy Ci nowy na ten sam adres — wystarczy kliknąć poniżej.",
                action: "Wyślij nowy link",
            },
            unknown: {
                title: "Nie znamy tego linku",
                body: "Link mógł zostać skrócony albo przycięty przez program pocztowy. Skopiuj go z wiadomości w całości albo zapisz się ponownie na stronie głównej.",
            },
            sent: {
                title: "Sprawdź skrzynkę",
                body: "Jeśli ten adres czeka na potwierdzenie, właśnie poszedł do niego nowy link. Zajrzyj też do spamu — pierwsza wiadomość z nowej domeny lubi tam trafiać.",
            },
        },
        unsubscribe: {
            ok: {
                title: "Wypisaliśmy Cię",
                body: "Twój adres nie jest już na liście oczekujących i nie dostaniesz od nas żadnej wiadomości. Jeśli zmienisz zdanie, możesz zapisać się ponownie na stronie głównej.",
            },
            unknown: {
                title: "Nie znamy tego linku",
                body: "Link mógł zostać przycięty przez program pocztowy. Skopiuj go z wiadomości w całości, a jeśli to nie pomoże — napisz na piotr{'@'}kivvi.click, a usuniemy adres ręcznie.",
            },
        },
        backHome: "Wróć na stronę główną",
    },
};
