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
            sampleCompanyName: "Aurea Shop Ltd",
            sampleVatId: "GB 527 2891 04",
            sampleStreet: "18 Lever Street",
            sampleCityAndPostcode: "M1 1BY Manchester",
            companyName: "Company name", // no en.yaml entry
            vatId: "VAT id", // no en.yaml entry
            street: "Address", // no en.yaml entry
            cityAndPostcode: "Town and postcode", // no en.yaml entry
            country: "Country", // no en.yaml entry
            countryPoland: "Poland", // no en.yaml entry
            countryGermany: "Germany", // no en.yaml entry
            countryCzechia: "Czechia", // no en.yaml entry
            currency: "Reporting currency", // no en.yaml entry
            ownerCardTitle: "Account owner", // en.yaml: 'Właściciel konta'
            transferOwnership: "Transfer ownership", // no en.yaml entry
            require2fa: "Require 2FA from everyone on the team", // no en.yaml entry
            sessionExpiry: "Sign sessions out after 14 days of inactivity", // no en.yaml entry
            dangerZoneTitle: "Irreversible zone", // en.yaml: 'Strefa nieodwracalna'
            deleteAccountTitle: "Delete the account and all its data", // no en.yaml entry
            deleteAccountBody:
                "Events, customers, automations and templates are removed after 30 days.", // no en.yaml entry
            deleteAccount: "Delete account", // en.yaml: 'Usuń konto'
        },
        sites: {
            columnDomain: "Domain", // no en.yaml entry
            columnTrackingStatus: "Tracking status", // no en.yaml entry
            columnEvents24h: "Events (24h)", // no en.yaml entry
            columnScript: "Script", // no en.yaml entry
            active: "Active", // no en.yaml entry
            cardTitle: "Tracked sites", // en.yaml: 'Śledzone strony'
            cardSub: "3 sites tracked", // no en.yaml entry
            addSite: "Add a site", // en.yaml: 'Dodaj stronę'
            scriptCardTitle: "Script installation", // en.yaml: 'Instalacja skryptu'
            copySnippet: "Copy snippet", // en.yaml: 'Kopiuj snippet'
            snippetHelpBefore: "Paste this code just before", // no en.yaml entry
            snippetHelpAfter:
                "on every page. The script weighs about 2KB, loads asynchronously and never blocks rendering.", // no en.yaml entry
            scriptDetected: "Script found on 14 of 14 pages", // no en.yaml entry
            firstEvent: "First event: 14 January 2024", // no en.yaml entry
            automaticEventsTitle: "Automatic events", // en.yaml: 'Zdarzenia automatyczne'
            automaticEventsSub: "detected without configuration", // en.yaml: 'wykrywane bez konfiguracji'
        },
        team: {
            columnPerson: "Person", // no en.yaml entry
            columnRole: "Role", // no en.yaml entry
            columnLastActivity: "Last activity", // en.yaml: 'Ostatnia aktywność'
            columnActions: "Actions", // en.yaml: 'Akcje'
            invited: "invited", // no en.yaml entry
            mfaOn: "on", // no en.yaml entry
            mfaOff: "none", // no en.yaml entry
            cardTitle: "Team members", // en.yaml: 'Członkowie zespołu'
            cardSub: "4 active · 1 invitation pending", // no en.yaml entry
            invite: "Invite someone", // en.yaml: 'Zaproś osobę'
            rolesCardTitle: "Roles and permissions", // en.yaml: 'Role i uprawnienia'
            createRole: "Custom role", // no en.yaml entry
            personSingular: "person", // no en.yaml entry
            personPlural: "people", // no en.yaml entry
        },
        providers: {
            cardTitle: "Sending providers", // en.yaml: 'Dostawcy wysyłki'
            cardSub: "failover to the backup provider is switched on", // no en.yaml entry
            addProvider: "Add a provider", // no en.yaml entry
            verified: "verified", // no en.yaml entry
            unverified: "unverified", // no en.yaml entry
            sent30d: "Sent 30d", // no en.yaml entry
            complaints: "Complaints", // no en.yaml entry
            test: "Test", // no en.yaml entry
            dnsCardTitle: "Domain authentication", // en.yaml: 'Uwierzytelnianie domeny'
            limitsCardTitle: "Sender addresses and limits", // en.yaml: 'Adresy nadawcy i limity'
            defaultSender: "Default sender", // no en.yaml entry
            senderName: "Sender name", // no en.yaml entry
            dailyLimit: "Daily limit", // no en.yaml entry
            unsubscribeOnHardBounce:
                "Unsubscribe an address automatically after a hard bounce", // no en.yaml entry
            pauseOnBounceRate:
                "Pause the campaign when the bounce rate goes over 2%", // no en.yaml entry
        },
        api: {
            columnName: "Name", // no en.yaml entry
            columnKey: "Key", // no en.yaml entry
            columnScopes: "Scopes", // no en.yaml entry
            columnCreated: "Created", // no en.yaml entry
            columnLastUsed: "Last used", // no en.yaml entry
            keysCardTitle: "API keys", // en.yaml: 'Klucze API'
            newKey: "New key", // en.yaml: 'Nowy klucz'
            webhooksCardTitle: "Webhooks",
            webhooksCardSub: "retried with backoff: 5 attempts over 6 hours", // no en.yaml entry
            addWebhook: "Add a webhook", // en.yaml: 'Dodaj webhook'
            webhookWarning:
                "The Slack webhook has been returning 410 for 3 hours. After 5 failed attempts it is paused automatically.", // no en.yaml entry
            showLogs: "Show the logs", // no en.yaml entry
            limitsCardTitle: "API limits", // en.yaml: 'Limity API'
        },
        notifications: {
            columnEvent: "Event", // no en.yaml entry
            cardTitle: "Notification channels", // en.yaml: 'Kanały powiadomień'
            cardSub: "these settings are yours, not the whole team's", // no en.yaml entry
            channelsCardTitle: "Channel integrations", // en.yaml: 'Integracje kanałów'
            active: "active", // no en.yaml entry
            disconnect: "Disconnect", // no en.yaml entry
            smsOnly: "Critical alerts only · charged separately", // no en.yaml entry
            connect: "Connect", // no en.yaml entry
            quietHoursCardTitle: "Quiet hours", // en.yaml: 'Cisza nocna'
            quietFrom: "Do not notify from", // no en.yaml entry
            quietTo: "until", // no en.yaml entry
            criticalBypass: "Critical alerts ignore the quiet hours", // no en.yaml entry
        },
        gdpr: {
            dpaCardTitle: "Data processing agreement (DPA)", // en.yaml: 'Umowa powierzenia (DPA)'
            dpaSigned: "Signed 14 January 2024", // no en.yaml entry
            dpaVersion: "version 3.1", // no en.yaml entry
            dpaProcessor: "processor: Kivvi sp. z o.o.", // no en.yaml entry
            downloadPdf: "Download the PDF", // no en.yaml entry
            signNewVersion: "Sign the new version", // no en.yaml entry
            dpaRegionBefore: "The data is stored in the region", // no en.yaml entry
            dpaRegionAfter:
                "Sub-processors: Amazon Web Services, SendGrid (EU). The list of sub-processors is part of the DPA.", // no en.yaml entry
            retentionCardTitle: "Data retention", // en.yaml: 'Retencja danych'
            retentionCardSub:
                "once the period is over, the data is deleted irreversibly", // no en.yaml entry
            anonymizeIp:
                "Anonymise IP addresses in events (drop the last octet)", // no en.yaml entry
            consentGate:
                "Record no events until there is consent for analytics cookies", // no en.yaml entry
            mlConsent:
                "Switch off ML profiling for EU customers without explicit consent", // no en.yaml entry
            columnPerson: "Person", // no en.yaml entry
            columnType: "Type", // no en.yaml entry
            columnStatus: "Status", // no en.yaml entry
            columnDue: "Due", // no en.yaml entry
            dsrCardTitle: "Data subject requests", // en.yaml: 'Żądania podmiotów danych'
            dsrCardSub: "the statutory deadline: 30 days", // no en.yaml entry
            newRequest: "New request", // no en.yaml entry
            exportCardTitle: "Export and erasure", // en.yaml: 'Eksport i usuwanie'
            exportAllTitle: "Export every piece of account data", // no en.yaml entry
            exportAllSub: "JSON + CSV, ready within 24 hours", // no en.yaml entry
            orderExport: "Order the export", // no en.yaml entry
            eraseCustomerTitle: "Erase a single customer's data", // no en.yaml entry
            eraseCustomerSub:
                "By e-mail address or customer_id — immediate and irreversible", // no en.yaml entry
            eraseCustomer: "Erase the customer", // no en.yaml entry
        },
    },
};
