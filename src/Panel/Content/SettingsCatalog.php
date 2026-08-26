<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * The eight settings tabs and the data each one renders.
 *
 * Every tab is a URL (/settings/{tab}), so a support link can point straight at
 * "e-mail providers" and the browser's back button behaves.
 */
final readonly class SettingsCatalog
{
    public const DEFAULT_TAB = 'account';

    private const TABS = [
        ['id' => 'account', 'icon' => 'user', 'label' => 'Konto'],
        ['id' => 'sites', 'icon' => 'globe', 'label' => 'Śledzone strony'],
        ['id' => 'team', 'icon' => 'users', 'label' => 'Zespół'],
        ['id' => 'providers', 'icon' => 'mail', 'label' => 'Dostawcy email'],
        ['id' => 'api', 'icon' => 'code', 'label' => 'Webhooks i API'],
        ['id' => 'notifications', 'icon' => 'bell', 'label' => 'Powiadomienia'],
        ['id' => 'billing', 'icon' => 'money', 'label' => 'Plan i płatności'],
        ['id' => 'gdpr', 'icon' => 'info', 'label' => 'RODO / DPA'],
    ];

    private const SUBTITLES = [
        'account' => 'Dane firmy, faktury i preferencje właściciela konta.',
        'sites' => 'Domeny objęte trackingiem oraz instalacja skryptu.',
        'team' => 'Osoby z dostępem do panelu, ich role i zaproszenia.',
        'providers' => 'Skąd wychodzą Twoje e-maile i jak radzą sobie z dostarczalnością.',
        'api' => 'Klucze API, webhooks i logi wywołań.',
        'notifications' => 'Kiedy Kivvi ma Cię powiadomić i którym kanałem.',
        'billing' => 'Plan, wykorzystanie limitów, metoda płatności i faktury.',
        'gdpr' => 'Retencja danych, umowa powierzenia i obsługa żądań podmiotów.',
    ];

    public const TRACKER_SNIPPET = <<<'SNIPPET'
        <!-- Kivvi-click tracker · ~2KB, no deps -->
        <script async
          src="https://cdn.kivvi-click.io/k.js"
          data-site="aureashop.pl"
          data-key="pk_live_8a4f2c..."></script>

        // then anywhere in your shop code:
        window.kvi('purchase', {
          order_id: 'AR-1287',
          value:     412.00,
          currency:  'PLN',
          items: [
            { sku: 'TEA-SENCHA-100', qty: 1, price: 38.90 },
            { sku: 'CUP-NORA',       qty: 2, price: 50.00 },
          ],
        });
        SNIPPET;

    /**
     * @return list<array{id: string, icon: string, label: string}>
     */
    public function tabs(): array
    {
        return self::TABS;
    }

    public function isKnownTab(string $tab): bool
    {
        return \array_key_exists($tab, self::SUBTITLES);
    }

    public function subtitle(string $tab): string
    {
        return self::SUBTITLES[$tab] ?? '';
    }

    /**
     * @return list<array{name: string, color: string, events: string, key: string}>
     */
    public function trackedSites(): array
    {
        return [
            ['name' => 'aureashop.pl', 'color' => '#7a8763', 'events' => Format::number(28410), 'key' => 'pk_live_8a4f2c…'],
            ['name' => 'mlot-narzedzia.pl', 'color' => '#a3825b', 'events' => Format::number(14820), 'key' => 'pk_live_3c91b7…'],
            ['name' => 'polna-bistro.pl', 'color' => '#8b6f53', 'events' => Format::number(12240), 'key' => 'pk_live_be22a0…'],
        ];
    }

    /**
     * @return list<string>
     */
    public function automaticEvents(): array
    {
        return ['pageview', 'login', 'signup', 'search', 'add_to_cart', 'remove_from_cart', 'wishlist', 'cart_abandon'];
    }

    /**
     * @return list<array{name: string, email: string, role: string, roleTone: string, last: string, invited: bool, mfa: bool}>
     */
    public function team(): array
    {
        return [
            ['name' => 'Maciej Kowalczyk', 'email' => 'maciej@aureashop.pl', 'role' => 'Właściciel', 'roleTone' => 'accent', 'last' => 'teraz', 'invited' => false, 'mfa' => true],
            ['name' => 'Anna Bartosz', 'email' => 'anna@aureashop.pl', 'role' => 'Administrator', 'roleTone' => 'brown', 'last' => '2 godz. temu', 'invited' => false, 'mfa' => true],
            ['name' => 'Piotr Sobczak', 'email' => 'piotr@aureashop.pl', 'role' => 'Marketer', 'roleTone' => 'neutral', 'last' => 'wczoraj', 'invited' => false, 'mfa' => false],
            ['name' => 'Iga Nowak', 'email' => 'iga@agencja-lumo.pl', 'role' => 'Marketer', 'roleTone' => 'neutral', 'last' => '4 dni temu', 'invited' => false, 'mfa' => true],
            ['name' => '—', 'email' => 'kamil@aureashop.pl', 'role' => 'Analityk', 'roleTone' => 'neutral', 'last' => '—', 'invited' => true, 'mfa' => false],
        ];
    }

    /**
     * @return list<array{name: string, description: string, count: int}>
     */
    public function roles(): array
    {
        return [
            ['name' => 'Właściciel', 'description' => 'Pełny dostęp, rozliczenia, usuwanie konta.', 'count' => 1],
            ['name' => 'Administrator', 'description' => 'Wszystko poza rozliczeniami i usuwaniem konta.', 'count' => 1],
            ['name' => 'Marketer', 'description' => 'Automatyzacje, kampanie, popupy, segmenty. Bez ustawień.', 'count' => 2],
            ['name' => 'Analityk', 'description' => 'Tylko podglądanie danych i eksport raportów.', 'count' => 1],
        ];
    }

    /**
     * @return list<array{name: string, region: string, statusTone: string, statusLabel: string, sent: string, bounce: string, bounceWarn: bool, complaint: string, verified: bool}>
     */
    public function emailProviders(): array
    {
        return [
            ['name' => 'Amazon SES', 'region' => 'eu-central-1', 'statusTone' => 'good', 'statusLabel' => 'Główny', 'sent' => Format::number(118420), 'bounce' => '0,24%', 'bounceWarn' => false, 'complaint' => '0,01%', 'verified' => true],
            ['name' => 'SendGrid', 'region' => 'EU', 'statusTone' => 'info', 'statusLabel' => 'Zapasowy', 'sent' => Format::number(23990), 'bounce' => '0,41%', 'bounceWarn' => true, 'complaint' => '0,03%', 'verified' => true],
            ['name' => 'SMTP własny (poczta.aureashop.pl)', 'region' => '—', 'statusTone' => 'neutral', 'statusLabel' => 'Wyłączony', 'sent' => '0', 'bounce' => '0%', 'bounceWarn' => false, 'complaint' => '0%', 'verified' => false],
        ];
    }

    /**
     * @return list<array{record: string, value: string, ok: bool}>
     */
    public function dnsRecords(): array
    {
        return [
            ['record' => 'SPF', 'value' => 'v=spf1 include:amazonses.com ~all', 'ok' => true],
            ['record' => 'DKIM', 'value' => 'kivvi1._domainkey.aureashop.pl', 'ok' => true],
            ['record' => 'DMARC', 'value' => 'v=DMARC1; p=quarantine; rua=mailto:dmarc@aureashop.pl', 'ok' => true],
            ['record' => 'BIMI', 'value' => 'nie skonfigurowane — logo marki nie pojawi się w Gmailu', 'ok' => false],
        ];
    }

    /**
     * @return list<array{name: string, prefix: string, created: string, last: string, scopes: list<string>}>
     */
    public function apiKeys(): array
    {
        return [
            ['name' => 'Produkcja — backend', 'prefix' => 'sk_live_8a4f2c', 'created' => '14 sty 2024', 'last' => '3 min temu', 'scopes' => ['events:write', 'customers:read']],
            ['name' => 'Tracker publiczny', 'prefix' => 'pk_live_8a4f2c', 'created' => '14 sty 2024', 'last' => 'teraz', 'scopes' => ['events:write']],
            ['name' => 'Staging', 'prefix' => 'sk_test_1b09de', 'created' => '02 mar 2026', 'last' => 'wczoraj', 'scopes' => ['*']],
        ];
    }

    /**
     * @return list<array{url: string, events: list<string>, code: int, last: string}>
     */
    public function webhooks(): array
    {
        return [
            ['url' => 'https://aureashop.pl/hooks/kivvi', 'events' => ['purchase', 'cart_abandon'], 'code' => 200, 'last' => '2 min temu'],
            ['url' => 'https://erp.aureashop.pl/api/kivvi', 'events' => ['customer.created'], 'code' => 200, 'last' => '11 min temu'],
            ['url' => 'https://hooks.slack.com/services/T0…', 'events' => ['automation.failed'], 'code' => 410, 'last' => '3 godz. temu'],
        ];
    }

    /**
     * @return list<array{label: string, pct: float, value: string, tone: string}>
     */
    public function apiLimits(): array
    {
        return [
            ['label' => 'Ingest zdarzeń (na sek.)', 'pct' => 42.0, 'value' => '42 / 100', 'tone' => 'accent'],
            ['label' => 'REST API (na min.)', 'pct' => 18.0, 'value' => '216 / 1200', 'tone' => 'accent'],
            ['label' => 'Eksporty (na godz.)', 'pct' => 60.0, 'value' => '6 / 10', 'tone' => 'brown'],
        ];
    }

    /**
     * @return list<array{label: string, email: bool, slack: bool, sms: bool}>
     */
    public function notificationMatrix(): array
    {
        return [
            ['label' => 'Automatyzacja przestała działać', 'email' => true, 'slack' => true, 'sms' => true],
            ['label' => 'Feed produktów zwrócił błąd', 'email' => true, 'slack' => true, 'sms' => false],
            ['label' => 'Bounce rate przekroczył próg', 'email' => true, 'slack' => true, 'sms' => true],
            ['label' => 'Limit wysyłek na wyczerpaniu (80%)', 'email' => true, 'slack' => false, 'sms' => false],
            ['label' => 'Import klientów zakończony', 'email' => true, 'slack' => false, 'sms' => false],
            ['label' => 'Nowa osoba dołączyła do zespołu', 'email' => true, 'slack' => false, 'sms' => false],
            ['label' => 'Tygodniowe podsumowanie wyników', 'email' => true, 'slack' => false, 'sms' => false],
            ['label' => 'Tracker przestał odbierać zdarzenia', 'email' => true, 'slack' => true, 'sms' => true],
        ];
    }

    /**
     * @return list<array{label: string, pct: float, value: string, tone: string}>
     */
    public function planUsage(): array
    {
        return [
            ['label' => 'Zdarzenia', 'pct' => 34.0, 'value' => '1,42 mln / bez limitu', 'tone' => 'accent'],
            ['label' => 'Wysłane e-maile', 'pct' => 58.0, 'value' => '142 410 / bez limitu', 'tone' => 'accent'],
            ['label' => 'Śledzone strony', 'pct' => 60.0, 'value' => '3 / 5', 'tone' => 'brown'],
            ['label' => 'Członkowie zespołu', 'pct' => 40.0, 'value' => '4 / 10', 'tone' => 'brown'],
            ['label' => 'Rekomendacje ML', 'pct' => 100.0, 'value' => 'włączone', 'tone' => 'accent'],
        ];
    }

    /**
     * @return list<array{number: string, date: string, amount: string, status: string}>
     */
    public function invoices(): array
    {
        return [
            ['number' => 'FV/2026/08/0142', 'date' => '01 sie 2026', 'amount' => '149,00 zł', 'status' => 'zapłacona'],
            ['number' => 'FV/2026/07/0139', 'date' => '01 lip 2026', 'amount' => '149,00 zł', 'status' => 'zapłacona'],
            ['number' => 'FV/2026/06/0131', 'date' => '01 cze 2026', 'amount' => '149,00 zł', 'status' => 'zapłacona'],
            ['number' => 'FV/2026/05/0127', 'date' => '01 maj 2026', 'amount' => '198,00 zł', 'status' => 'zapłacona'],
        ];
    }

    /**
     * @return list<array{id: string, person: string, type: string, status: string, done: bool, due: string}>
     */
    public function dataSubjectRequests(): array
    {
        return [
            ['id' => 'DSR-0142', 'person' => 'hania.k@aurea.pl', 'type' => 'Dostęp do danych', 'status' => 'zakończone', 'done' => true, 'due' => '22 sie 2026'],
            ['id' => 'DSR-0141', 'person' => 'marek.p@example.com', 'type' => 'Usunięcie danych', 'status' => 'w toku', 'done' => false, 'due' => '25 sie 2026'],
            ['id' => 'DSR-0139', 'person' => 'iga.n@example.pl', 'type' => 'Sprzeciw wobec profilowania', 'status' => 'zakończone', 'done' => true, 'due' => '18 sie 2026'],
        ];
    }

    /**
     * @return list<array{label: string, options: list<string>, selected: string}>
     */
    public function retentionPolicies(): array
    {
        return [
            ['label' => 'Surowe zdarzenia', 'options' => ['90 dni', '13 miesięcy', '24 miesiące'], 'selected' => '13 miesięcy'],
            ['label' => 'Profile klientów bez aktywności', 'options' => ['24 miesiące', '36 miesięcy', 'bez limitu'], 'selected' => '24 miesiące'],
            ['label' => 'Logi wysyłek e-mail', 'options' => ['12 miesięcy', '24 miesiące'], 'selected' => '12 miesięcy'],
            ['label' => 'Logi webhooków', 'options' => ['30 dni', '90 dni'], 'selected' => '30 dni'],
        ];
    }
}
