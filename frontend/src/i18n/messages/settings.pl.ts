/**
 * Settings tabs' page chrome — ported verbatim from the page-level `|trans` calls across
 * `pages/settings.html.twig` and its 27 tab partials. Only strings the old templates actually ran
 * through `|trans` live here; every other piece of copy — tab labels/subtitles, tracked-site
 * names, team members, provider stats, DNS values, API keys, webhook URLs, invoices, DSR rows,
 * retention policy labels/options, and every UI word the old partials left un-translated (e.g.
 * "2FA", "E-mail"/"Slack"/"SMS", "PLN"/"EUR", "ON", "Bounce", "Reply-to", "Wersja k.js: 2.4.1",
 * "aureashop.pl") — is either hard-coded Polish fixture text (SettingsFixtures/
 * SettingsViewService, rendered verbatim by the settings/* components) or literal markup those
 * same components reproduce directly, exactly like the Twig partials never ran it through
 * `|trans` either.
 *
 * Where a string had NO entry in translations/messages.en.yaml (the old stack's flat
 * literal-Polish-text-as-key dictionary), settings.en.ts carries the same Polish text: that is
 * exactly what Symfony's translator does for an untranslated id — it returns the id itself,
 * regardless of locale. Each such case is called out in settings.en.ts's own comment.
 */
export default {
    settings: {
        title: "Ustawienia",
        account: {
            cardTitle: "Dane konta",
            cancel: "Anuluj",
            save: "Zapisz zmiany",
            companyName: "Nazwa firmy",
            vatId: "NIP",
            street: "Adres",
            cityAndPostcode: "Miasto i kod",
            country: "Kraj",
            countryPoland: "Polska",
            countryGermany: "Niemcy",
            countryCzechia: "Czechy",
            currency: "Waluta raportowania",
            ownerCardTitle: "Właściciel konta",
            transferOwnership: "Przenieś własność",
            require2fa: "Wymagaj 2FA dla wszystkich członków zespołu",
            sessionExpiry: "Wyloguj sesje po 14 dniach nieaktywności",
            dangerZoneTitle: "Strefa nieodwracalna",
            deleteAccountTitle: "Usuń konto i wszystkie dane",
            deleteAccountBody:
                "Zdarzenia, klienci, automatyzacje i szablony zostaną usunięte po 30 dniach.",
            deleteAccount: "Usuń konto",
        },
        sites: {
            columnDomain: "Domena",
            columnTrackingStatus: "Status śledzenia",
            columnEvents24h: "Zdarzeń (24h)",
            columnScript: "Skrypt",
            active: "Aktywne",
            cardTitle: "Śledzone strony",
            cardSub: "3 z 5 wykorzystane w planie Pro",
            addSite: "Dodaj stronę",
            scriptCardTitle: "Instalacja skryptu",
            copySnippet: "Kopiuj snippet",
            snippetHelpBefore: "Wklej ten kod tuż przed",
            snippetHelpAfter:
                "na wszystkich stronach. Skrypt waży ~2KB, ładuje się asynchronicznie, nie blokuje renderu.",
            scriptDetected: "Skrypt wykryty na 14 z 14 stron",
            firstEvent: "Pierwsze zdarzenie: 14 stycznia 2024",
            automaticEventsTitle: "Zdarzenia automatyczne",
            automaticEventsSub: "wykrywane bez konfiguracji",
        },
        team: {
            columnPerson: "Osoba",
            columnRole: "Rola",
            columnLastActivity: "Ostatnia aktywność",
            columnActions: "Akcje",
            invited: "zaproszony",
            mfaOn: "włączone",
            mfaOff: "brak",
            cardTitle: "Członkowie zespołu",
            cardSub:
                "4 aktywnych · 1 zaproszenie oczekuje · limit planu Pro: 10",
            invite: "Zaproś osobę",
            rolesCardTitle: "Role i uprawnienia",
            createRole: "Rola własna",
            personSingular: "osoba",
            personPlural: "osoby",
        },
        providers: {
            cardTitle: "Dostawcy wysyłki",
            cardSub: "failover przy błędzie głównego dostawcy jest włączony",
            addProvider: "Dodaj dostawcę",
            verified: "zweryfikowany",
            unverified: "niezweryfikowany",
            sent30d: "Wysłane 30d",
            complaints: "Skargi",
            test: "Test",
            dnsCardTitle: "Uwierzytelnianie domeny",
            limitsCardTitle: "Adresy nadawcy i limity",
            defaultSender: "Domyślny nadawca",
            senderName: "Nazwa nadawcy",
            dailyLimit: "Limit dzienny",
            unsubscribeOnHardBounce:
                "Automatycznie wypisuj adresy po hard bounce",
            pauseOnBounceRate:
                "Wstrzymaj kampanię, gdy bounce rate przekroczy 2%",
        },
        api: {
            columnName: "Nazwa",
            columnKey: "Klucz",
            columnScopes: "Zakresy",
            columnCreated: "Utworzony",
            columnLastUsed: "Ostatnie użycie",
            keysCardTitle: "Klucze API",
            newKey: "Nowy klucz",
            webhooksCardTitle: "Webhooks",
            webhooksCardSub: "retry z backoffem: 5 prób w ciągu 6 godzin",
            addWebhook: "Dodaj webhook",
            webhookWarning:
                "Webhook Slacka zwraca 410 od 3 godzin. Po 5 nieudanych próbach zostanie automatycznie wstrzymany.",
            showLogs: "Pokaż logi",
            limitsCardTitle: "Limity API",
        },
        notifications: {
            columnEvent: "Zdarzenie",
            cardTitle: "Kanały powiadomień",
            cardSub: "ustawienia dotyczą Twojego konta, nie całego zespołu",
            channelsCardTitle: "Integracje kanałów",
            active: "aktywny",
            disconnect: "Rozłącz",
            smsOnly: "Tylko krytyczne alerty · dodatkowo płatne",
            connect: "Podłącz",
            quietHoursCardTitle: "Cisza nocna",
            quietFrom: "Nie powiadamiaj od",
            quietTo: "do",
            criticalBypass: "Alerty krytyczne omijają ciszę nocną",
        },
        billing: {
            limitsCardTitle: "Wykorzystanie limitów",
            limitsCardSub: "okres 1–26 sierpnia 2026",
            paymentMethodCardTitle: "Metoda płatności",
            change: "Zmień",
            cardExpiry: "wygasa 04/2029 · Maciej Kowalczyk",
            defaultPaymentMethod: "domyślna",
            // "@" is vue-i18n message-syntax reserved (linked-message references, "@:key"), so
            // a literal "@" in the address must be escaped as {'@'} — quoted-literal escaping,
            // the same mechanism vue-i18n reserves "{", "}" and "|" for.
            invoiceRecipient:
                "Wysyłaj faktury na <span class=\"mono\">ksiegowosc{'@'}aureashop.pl</span>",
            invoicesCardTitle: "Faktury",
            downloadAll: "Pobierz wszystkie",
            columnNumber: "Numer",
            columnDate: "Data",
            columnAmount: "Kwota netto",
            columnStatus: "Status",
            yourPlan: "Twój plan",
            monthlyBilling: "rozliczenie miesięczne",
            nextRenewal:
                "Następne odnowienie 1 września 2026 · 149,00 zł netto",
            switchToYearly: "Zmień na roczny (−20%)",
            changePlan: "Zmień plan",
        },
        gdpr: {
            dpaCardTitle: "Umowa powierzenia (DPA)",
            dpaSigned: "Podpisana 14 stycznia 2024",
            dpaVersion: "wersja 3.1",
            dpaProcessor: "podmiot przetwarzający: Kivvi sp. z o.o.",
            downloadPdf: "Pobierz PDF",
            signNewVersion: "Podpisz nową wersję",
            dpaRegionBefore: "Dane przechowywane są w regionie",
            dpaRegionAfter:
                "Podprzetwarzający: Amazon Web Services, SendGrid (EU). Lista podprzetwarzających jest częścią DPA.",
            retentionCardTitle: "Retencja danych",
            retentionCardSub:
                "po upływie okresu dane są nieodwracalnie usuwane",
            anonymizeIp: "Anonimizuj adresy IP w zdarzeniach (ostatni oktet)",
            consentGate:
                "Nie zapisuj zdarzeń, dopóki nie ma zgody na cookies analityczne",
            mlConsent:
                "Wyłącz profilowanie ML dla klientów z UE bez wyraźnej zgody",
            columnPerson: "Osoba",
            columnType: "Typ",
            columnStatus: "Status",
            columnDue: "Termin",
            dsrCardTitle: "Żądania podmiotów danych",
            dsrCardSub: "termin ustawowy: 30 dni",
            newRequest: "Nowe żądanie",
            exportCardTitle: "Eksport i usuwanie",
            exportAllTitle: "Eksport wszystkich danych konta",
            exportAllSub: "JSON + CSV, przygotowanie do 24 godzin",
            orderExport: "Zamów eksport",
            eraseCustomerTitle: "Usuń dane pojedynczego klienta",
            eraseCustomerSub:
                "Po e-mailu lub customer_id — natychmiastowe i nieodwracalne",
            eraseCustomer: "Usuń klienta",
        },
    },
};
