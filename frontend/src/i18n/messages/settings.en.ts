/**
 * Settings tabs' page chrome — English side of settings.pl.ts. Keys mirror settings.pl.ts
 * exactly. Values come from translations/messages.en.yaml's flat literal-Polish-text-as-key
 * dictionary where an entry exists for that Polish string; where it does NOT (most of this page:
 * the old dictionary only ever grew entries for strings some other page happened to need first),
 * the Polish text is reproduced unchanged — exactly what Symfony's translator returns for an id
 * with no translation resource, in ANY locale. That gap is real product behaviour today, not a
 * placeholder this port invents.
 */
export default {
    settings: {
        title: "Settings", // en.yaml: settings: { title: 'Settings' }
        account: {
            cardTitle: "Account data", // en.yaml: 'Dane konta'
            cancel: "Cancel", // en.yaml: 'Anuluj' (common.cancel key; not reused here — see packet note)
            save: "Save changes", // en.yaml: 'Zapisz zmiany'
            companyName: "Nazwa firmy", // no en.yaml entry
            vatId: "NIP", // no en.yaml entry
            street: "Adres", // no en.yaml entry
            cityAndPostcode: "Miasto i kod", // no en.yaml entry
            country: "Kraj", // no en.yaml entry
            countryPoland: "Polska", // no en.yaml entry
            countryGermany: "Niemcy", // no en.yaml entry
            countryCzechia: "Czechy", // no en.yaml entry
            currency: "Waluta raportowania", // no en.yaml entry
            ownerCardTitle: "Account owner", // en.yaml: 'Właściciel konta'
            transferOwnership: "Przenieś własność", // no en.yaml entry
            require2fa: "Wymagaj 2FA dla wszystkich członków zespołu", // no en.yaml entry
            sessionExpiry: "Wyloguj sesje po 14 dniach nieaktywności", // no en.yaml entry
            dangerZoneTitle: "Irreversible zone", // en.yaml: 'Strefa nieodwracalna'
            deleteAccountTitle: "Usuń konto i wszystkie dane", // no en.yaml entry
            deleteAccountBody:
                "Zdarzenia, klienci, automatyzacje i szablony zostaną usunięte po 30 dniach.", // no en.yaml entry
            deleteAccount: "Delete account", // en.yaml: 'Usuń konto'
        },
        sites: {
            columnDomain: "Domena", // no en.yaml entry
            columnTrackingStatus: "Status śledzenia", // no en.yaml entry
            columnEvents24h: "Zdarzeń (24h)", // no en.yaml entry
            columnScript: "Skrypt", // no en.yaml entry
            active: "Aktywne", // no en.yaml entry
            cardTitle: "Tracked sites", // en.yaml: 'Śledzone strony'
            cardSub: "3 z 5 wykorzystane w planie Pro", // no en.yaml entry
            addSite: "Add a site", // en.yaml: 'Dodaj stronę'
            scriptCardTitle: "Script installation", // en.yaml: 'Instalacja skryptu'
            copySnippet: "Copy snippet", // en.yaml: 'Kopiuj snippet'
            snippetHelpBefore: "Wklej ten kod tuż przed", // no en.yaml entry
            snippetHelpAfter:
                "na wszystkich stronach. Skrypt waży ~2KB, ładuje się asynchronicznie, nie blokuje renderu.", // no en.yaml entry
            scriptDetected: "Skrypt wykryty na 14 z 14 stron", // no en.yaml entry
            firstEvent: "Pierwsze zdarzenie: 14 stycznia 2024", // no en.yaml entry
            automaticEventsTitle: "Automatic events", // en.yaml: 'Zdarzenia automatyczne'
            automaticEventsSub: "detected without configuration", // en.yaml: 'wykrywane bez konfiguracji'
        },
        team: {
            columnPerson: "Osoba", // no en.yaml entry
            columnRole: "Rola", // no en.yaml entry
            columnLastActivity: "Last activity", // en.yaml: 'Ostatnia aktywność'
            columnActions: "Actions", // en.yaml: 'Akcje'
            invited: "zaproszony", // no en.yaml entry
            mfaOn: "włączone", // no en.yaml entry
            mfaOff: "brak", // no en.yaml entry
            cardTitle: "Team members", // en.yaml: 'Członkowie zespołu'
            cardSub:
                "4 aktywnych · 1 zaproszenie oczekuje · limit planu Pro: 10", // no en.yaml entry
            invite: "Invite someone", // en.yaml: 'Zaproś osobę'
            rolesCardTitle: "Roles and permissions", // en.yaml: 'Role i uprawnienia'
            createRole: "Rola własna", // no en.yaml entry
            personSingular: "osoba", // no en.yaml entry
            personPlural: "osoby", // no en.yaml entry
        },
        providers: {
            cardTitle: "Sending providers", // en.yaml: 'Dostawcy wysyłki'
            cardSub: "failover przy błędzie głównego dostawcy jest włączony", // no en.yaml entry
            addProvider: "Dodaj dostawcę", // no en.yaml entry
            verified: "zweryfikowany", // no en.yaml entry
            unverified: "niezweryfikowany", // no en.yaml entry
            sent30d: "Wysłane 30d", // no en.yaml entry
            complaints: "Skargi", // no en.yaml entry
            test: "Test", // no en.yaml entry
            dnsCardTitle: "Domain authentication", // en.yaml: 'Uwierzytelnianie domeny'
            limitsCardTitle: "Sender addresses and limits", // en.yaml: 'Adresy nadawcy i limity'
            defaultSender: "Domyślny nadawca", // no en.yaml entry
            senderName: "Nazwa nadawcy", // no en.yaml entry
            dailyLimit: "Limit dzienny", // no en.yaml entry
            unsubscribeOnHardBounce:
                "Automatycznie wypisuj adresy po hard bounce", // no en.yaml entry
            pauseOnBounceRate:
                "Wstrzymaj kampanię, gdy bounce rate przekroczy 2%", // no en.yaml entry
        },
        api: {
            columnName: "Nazwa", // no en.yaml entry
            columnKey: "Klucz", // no en.yaml entry
            columnScopes: "Zakresy", // no en.yaml entry
            columnCreated: "Utworzony", // no en.yaml entry
            columnLastUsed: "Ostatnie użycie", // no en.yaml entry
            keysCardTitle: "API keys", // en.yaml: 'Klucze API'
            newKey: "New key", // en.yaml: 'Nowy klucz'
            webhooksCardTitle: "Webhooks",
            webhooksCardSub: "retry z backoffem: 5 prób w ciągu 6 godzin", // no en.yaml entry
            addWebhook: "Add a webhook", // en.yaml: 'Dodaj webhook'
            webhookWarning:
                "Webhook Slacka zwraca 410 od 3 godzin. Po 5 nieudanych próbach zostanie automatycznie wstrzymany.", // no en.yaml entry
            showLogs: "Pokaż logi", // no en.yaml entry
            limitsCardTitle: "API limits", // en.yaml: 'Limity API'
        },
        notifications: {
            columnEvent: "Zdarzenie", // no en.yaml entry
            cardTitle: "Notification channels", // en.yaml: 'Kanały powiadomień'
            cardSub: "ustawienia dotyczą Twojego konta, nie całego zespołu", // no en.yaml entry
            channelsCardTitle: "Channel integrations", // en.yaml: 'Integracje kanałów'
            active: "aktywny", // no en.yaml entry
            disconnect: "Rozłącz", // no en.yaml entry
            smsOnly: "Tylko krytyczne alerty · dodatkowo płatne", // no en.yaml entry
            connect: "Podłącz", // no en.yaml entry
            quietHoursCardTitle: "Quiet hours", // en.yaml: 'Cisza nocna'
            quietFrom: "Nie powiadamiaj od", // no en.yaml entry
            quietTo: "do", // no en.yaml entry
            criticalBypass: "Alerty krytyczne omijają ciszę nocną", // no en.yaml entry
        },
        gdpr: {
            dpaCardTitle: "Data processing agreement (DPA)", // en.yaml: 'Umowa powierzenia (DPA)'
            dpaSigned: "Podpisana 14 stycznia 2024", // no en.yaml entry
            dpaVersion: "wersja 3.1", // no en.yaml entry
            dpaProcessor: "podmiot przetwarzający: Kivvi sp. z o.o.", // no en.yaml entry
            downloadPdf: "Pobierz PDF", // no en.yaml entry
            signNewVersion: "Podpisz nową wersję", // no en.yaml entry
            dpaRegionBefore: "Dane przechowywane są w regionie", // no en.yaml entry
            dpaRegionAfter:
                "Podprzetwarzający: Amazon Web Services, SendGrid (EU). Lista podprzetwarzających jest częścią DPA.", // no en.yaml entry
            retentionCardTitle: "Data retention", // en.yaml: 'Retencja danych'
            retentionCardSub:
                "po upływie okresu dane są nieodwracalnie usuwane", // no en.yaml entry
            anonymizeIp: "Anonimizuj adresy IP w zdarzeniach (ostatni oktet)", // no en.yaml entry
            consentGate:
                "Nie zapisuj zdarzeń, dopóki nie ma zgody na cookies analityczne", // no en.yaml entry
            mlConsent:
                "Wyłącz profilowanie ML dla klientów z UE bez wyraźnej zgody", // no en.yaml entry
            columnPerson: "Osoba", // no en.yaml entry
            columnType: "Typ", // no en.yaml entry
            columnStatus: "Status", // no en.yaml entry
            columnDue: "Termin", // no en.yaml entry
            dsrCardTitle: "Data subject requests", // en.yaml: 'Żądania podmiotów danych'
            dsrCardSub: "termin ustawowy: 30 dni", // no en.yaml entry
            newRequest: "Nowe żądanie", // no en.yaml entry
            exportCardTitle: "Export and erasure", // en.yaml: 'Eksport i usuwanie'
            exportAllTitle: "Eksport wszystkich danych konta", // no en.yaml entry
            exportAllSub: "JSON + CSV, przygotowanie do 24 godzin", // no en.yaml entry
            orderExport: "Zamów eksport", // no en.yaml entry
            eraseCustomerTitle: "Usuń dane pojedynczego klienta", // no en.yaml entry
            eraseCustomerSub:
                "Po e-mailu lub customer_id — natychmiastowe i nieodwracalne", // no en.yaml entry
            eraseCustomer: "Usuń klienta", // no en.yaml entry
        },
    },
};
