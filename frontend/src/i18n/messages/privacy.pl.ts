/**
 * Privacy policy copy (PIO-70, decision D3).
 *
 * DRAFT — this is a legal document written to accompany the waitlist form, not marketing copy.
 * It needs the owner's reading and approval before it goes live, and the administrator's
 * identifying details below are placeholders until he supplies the real ones.
 *
 * Kept under the `privacyPage` top-level key rather than `privacy`, which PublicLayout.vue
 * already uses for the footer link's label — the i18n merge in src/i18n/index.ts assigns
 * top-level keys, so a collision would silently replace the other catalogue's entry.
 */
export default {
    privacyPage: {
        title: "Polityka prywatności",
        updated: "Ostatnia aktualizacja: 15 września 2026",
        intro: "Ta polityka opisuje, co dzieje się z danymi, które zostawiasz, zapisując się na listę oczekujących kivvi·click. Dotyczy wyłącznie tego zapisu — panel i konta użytkowników nie są jeszcze dostępne publicznie.",
        sections: [
            {
                heading: "Kto jest administratorem",
                body: "Administratorem Twoich danych jest właściciel serwisu kivvi·click, dostępnego pod adresem kivvi.click. W sprawach dotyczących danych osobowych pisz na adres podany niżej, w sekcji „Kontakt”.",
            },
            {
                heading: "Jakie dane zbieramy",
                body: "Przy zapisie na listę oczekujących zapisujemy: podany adres e-mail, datę i godzinę wyrażenia zgody, adres IP, z którego wysłano formularz, identyfikator przeglądarki (user-agent) oraz dokładną treść klauzuli zgody, którą widziałeś w chwili zapisu. Trzy ostatnie służą wyłącznie temu, żeby móc wykazać, na co i kiedy wyraziłeś zgodę.",
            },
            {
                heading: "W jakim celu",
                body: "Wyłącznie po to, żeby wysłać Ci jedno powiadomienie, kiedy kivvi·click ruszy. Nie wysyłamy newslettera, nie profilujemy Cię i nie przekazujemy Twojego adresu nikomu w celach marketingowych.",
            },
            {
                heading: "Na jakiej podstawie",
                body: "Na podstawie Twojej zgody — art. 6 ust. 1 lit. a RODO. Zgoda jest dobrowolna; bez niej po prostu nie zapisujemy adresu.",
            },
            {
                heading: "Jak długo przechowujemy dane",
                body: "Do czasu wysłania powiadomienia o starcie i przez maksymalnie 12 miesięcy po nim, albo do chwili wycofania zgody — zależnie od tego, co nastąpi wcześniej. Potem usuwamy adres wraz z dowodem zgody.",
            },
            {
                heading: "Jak wycofać zgodę",
                body: "Napisz na adres podany w sekcji „Kontakt”, a usuniemy Twój adres z listy. Wycofanie zgody nie wpływa na zgodność z prawem tego, co zrobiliśmy przed jej wycofaniem.",
            },
            {
                heading: "Twoje prawa",
                body: "Masz prawo dostępu do swoich danych, ich sprostowania, usunięcia, ograniczenia przetwarzania oraz przeniesienia. Masz też prawo wnieść skargę do Prezesa Urzędu Ochrony Danych Osobowych, jeśli uznasz, że przetwarzamy Twoje dane niezgodnie z prawem.",
            },
            {
                heading: "Kontakt",
                // DO UZUPEŁNIENIA PRZED PUBLIKACJĄ. To jedyna wskazana w tym dokumencie droga
                // wycofania zgody, więc dopóki nie ma tu prawdziwego adresu, polityka opisuje
                // mechanizm, którego nie da się użyć.
                body: "[adres e-mail administratora — do uzupełnienia przed publikacją]",
            },
            {
                heading: "Komu powierzamy dane",
                body: "Dane przechowujemy na serwerze obsługującym serwis kivvi·click. Korzystamy z dostawcy infrastruktury działającego na terenie Unii Europejskiej. Nie przekazujemy danych poza Europejski Obszar Gospodarczy.",
            },
        ],
    },
};
