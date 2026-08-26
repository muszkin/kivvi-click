<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * Automations: the index rows and the two interchangeable editor representations
 * (linear pipeline and node graph) of the same rule model.
 */
final readonly class AutomationCatalog
{
    private const AUTOMATIONS = [
        ['id' => 'a1', 'name' => 'Powrót do porzuconego koszyka', 'status' => 'active', 'trigger' => 'cart_abandon', 'channels' => ['email', 'popup'], 'runs' => 1287, 'conversion' => 18.4, 'revenue' => 24800],
        ['id' => 'a2', 'name' => 'Powitanie po rejestracji', 'status' => 'active', 'trigger' => 'signup', 'channels' => ['email'], 'runs' => 412, 'conversion' => 41.2, 'revenue' => 8930],
        ['id' => 'a3', 'name' => 'Rekomendacje „podobne produkty”', 'status' => 'active', 'trigger' => 'pageview', 'channels' => ['widget'], 'runs' => 18420, 'conversion' => 7.9, 'revenue' => 41200],
        ['id' => 'a4', 'name' => 'Kupon dla VIP po 5 zamówieniach', 'status' => 'active', 'trigger' => 'purchase', 'channels' => ['coupon', 'email'], 'runs' => 89, 'conversion' => 62.1, 'revenue' => 12400],
        ['id' => 'a5', 'name' => 'Win-back po 60 dniach nieaktywności', 'status' => 'paused', 'trigger' => 'inactivity', 'channels' => ['email'], 'runs' => 0, 'conversion' => 0.0, 'revenue' => 0],
        ['id' => 'a6', 'name' => 'Powiadomienie o powrocie produktu', 'status' => 'draft', 'trigger' => 'product_back_in_stock', 'channels' => ['email'], 'runs' => 0, 'conversion' => 0.0, 'revenue' => 0],
    ];

    private const STATUS_CHIP = [
        'active' => ['tone' => 'good', 'label' => 'Aktywna'],
        'paused' => ['tone' => 'warn', 'label' => 'Wstrzymana'],
        'draft' => ['tone' => 'neutral', 'label' => 'Szkic'],
    ];

    private const TRIGGER_LABEL = [
        'cart_abandon' => 'Porzucenie koszyka',
        'signup' => 'Rejestracja',
        'pageview' => 'Wyświetlenie produktu',
        'purchase' => 'Zakup',
        'inactivity' => 'Brak aktywności',
        'product_back_in_stock' => 'Powrót produktu',
    ];

    /**
     * List-card param hashes for the index.
     *
     * @return list<array<string, mixed>>
     */
    public function cards(): array
    {
        return array_map(function (array $automation): array {
            $chip = self::STATUS_CHIP[$automation['status']];

            $chips = [
                ['label' => $chip['label'], 'tone' => $chip['tone']],
                ['label' => self::TRIGGER_LABEL[$automation['trigger']], 'tone' => 'accent'],
            ];
            foreach ($automation['channels'] as $channel) {
                $chips[] = ['label' => $channel, 'tone' => 'brown'];
            }

            return [
                'title' => $automation['name'],
                'chips' => $chips,
                'metrics' => [
                    ['value' => Format::number($automation['runs']), 'label' => 'uruchomień (7d)', 'width' => 90],
                    ['value' => $automation['conversion'] > 0 ? Format::percent($automation['conversion']) : '—', 'label' => 'konwersja', 'width' => 80, 'color' => $automation['conversion'] > 0 ? 'var(--good)' : 'var(--fg-muted)'],
                    ['value' => $automation['revenue'] > 0 ? Format::money($automation['revenue']) : '—', 'label' => 'przychód (7d)', 'width' => 110],
                ],
                'action' => 'go-automation',
                'payload' => $automation['id'],
                'href' => $automation['id'],
            ];
        }, self::AUTOMATIONS);
    }

    /**
     * @return list<array{label: string, icon?: string, count?: int, active: bool, action: string, payload: string}>
     */
    public function statusFilters(string $active = 'all'): array
    {
        $counts = ['active' => 0, 'paused' => 0, 'draft' => 0];
        foreach (self::AUTOMATIONS as $automation) {
            ++$counts[$automation['status']];
        }

        return [
            ['label' => 'Wszystkie', 'icon' => 'grid', 'count' => \count(self::AUTOMATIONS), 'active' => 'all' === $active, 'action' => 'set-automation-status', 'payload' => 'all'],
            ['label' => 'Aktywne', 'count' => $counts['active'], 'active' => 'active' === $active, 'action' => 'set-automation-status', 'payload' => 'active'],
            ['label' => 'Wstrzymane', 'count' => $counts['paused'], 'active' => 'paused' === $active, 'action' => 'set-automation-status', 'payload' => 'paused'],
            ['label' => 'Szkice', 'count' => $counts['draft'], 'active' => 'draft' === $active, 'action' => 'set-automation-status', 'payload' => 'draft'],
        ];
    }

    /**
     * @return list<array{id: string, name: string, status: string, trigger: string, channels: list<string>, runs: int, conversion: float, revenue: int}>
     */
    public function topEarning(int $limit): array
    {
        $active = array_filter(self::AUTOMATIONS, static fn (array $a): bool => 'active' === $a['status']);
        usort($active, static fn (array $a, array $b): int => $b['revenue'] <=> $a['revenue']);

        return \array_slice($active, 0, $limit);
    }

    /**
     * @return array{id: string, name: string, statusLabel: string, status: string}
     */
    public function header(string $id): array
    {
        foreach (self::AUTOMATIONS as $automation) {
            if ($automation['id'] === $id) {
                return [
                    'id' => $automation['id'],
                    'name' => $automation['name'],
                    'status' => $automation['status'],
                    'statusLabel' => self::STATUS_CHIP[$automation['status']]['label'].' · Edytuj logikę uruchamiania, warunki i akcje',
                ];
            }
        }

        return ['id' => $id, 'name' => 'Nowa automatyzacja', 'status' => 'draft', 'statusLabel' => 'Wersja robocza · Edytuj logikę uruchamiania, warunki i akcje'];
    }

    /**
     * @return list<array{label: string, active: bool}>
     */
    public function editorTabs(): array
    {
        return [
            ['label' => 'Budowniczy', 'active' => true],
            ['label' => 'Statystyki', 'active' => false],
            ['label' => 'Wykonania (1 287)', 'active' => false],
            ['label' => 'Historia zmian', 'active' => false],
        ];
    }

    /**
     * Rule-pipeline steps: KIEDY → JEŚLI → WTEDY.
     *
     * @return list<array{n: int, kicker: string, title: string, addLabel: string, blocks: list<array{icon: string, title: string, body: string}>}>
     */
    public function pipelineSteps(): array
    {
        return [
            [
                'n' => 1,
                'kicker' => 'KIEDY · trigger',
                'title' => 'Klient porzuca koszyk',
                'addLabel' => 'Dodaj warunek wyzwalacza',
                'blocks' => [
                    ['icon' => 'cart', 'title' => 'Zdarzenie', 'body' => '<span class="pill">cart_abandon</span> wyzwolone po <span class="pill">8 min</span> nieaktywności'],
                    ['icon' => 'globe', 'title' => 'Strony', 'body' => 'Reguła działa na: <span class="pill">aureashop.pl</span> <span class="pill">mlot-narzedzia.pl</span>'],
                    ['icon' => 'user', 'title' => 'Segment klienta', 'body' => 'Klient z koszykiem o wartości <span class="pill">≥ 150 PLN</span>, niebędący w segmencie <span class="pill">VIP</span>'],
                ],
            ],
            [
                'n' => 2,
                'kicker' => 'JEŚLI · warunki',
                'title' => 'Wszystkie muszą się zgadzać',
                'addLabel' => 'Dodaj warunek',
                'blocks' => [
                    ['icon' => 'eye', 'title' => 'Częstotliwość', 'body' => 'Klient <strong>nie otrzymał</strong> tej automatyzacji w ciągu <span class="pill">7 dni</span>'],
                    ['icon' => 'book', 'title' => 'Historia zakupów', 'body' => 'Liczba zamówień <span class="pill">≥ 1</span> w ciągu ostatnich <span class="pill">180 dni</span>'],
                    ['icon' => 'mail', 'title' => 'Subskrypcja', 'body' => 'Klient ma zgodę marketingową <span class="pill">tak</span> i adres email <span class="pill">zweryfikowany</span>'],
                ],
            ],
            [
                'n' => 3,
                'kicker' => 'WTEDY · akcje',
                'title' => 'Sekwencja krok po kroku',
                'addLabel' => 'Dodaj krok',
                'blocks' => [
                    ['icon' => 'mail', 'title' => 'Wyślij email — od razu', 'body' => 'Szablon <span class="pill">„Wróć po Twój koszyk”</span> · od <span class="pill">sklep@aureashop.pl</span>'],
                    ['icon' => 'coupon', 'title' => 'Po 24h — wyślij kupon', 'body' => 'Kod jednorazowy <span class="pill">WROCMY-{id}</span> · <span class="pill">−10%</span> na cały koszyk · ważny 48h'],
                    ['icon' => 'layout', 'title' => 'Pokaż popup przy powrocie', 'body' => 'Slide-in z napisem „Twój koszyk czeka” — tylko jeśli klient wraca w ciągu <span class="pill">72h</span>'],
                ],
            ],
        ];
    }

    /**
     * @return list<array{x: int, y: int, kind: string, kicker: string, title: string, sub: string, icon: string}>
     */
    public function flowNodes(): array
    {
        return [
            ['x' => 30, 'y' => 40, 'kind' => 'trigger', 'kicker' => 'KIEDY', 'title' => 'Porzucenie koszyka', 'sub' => 'cart_abandon · po 8 min', 'icon' => 'cart'],
            ['x' => 320, 'y' => 40, 'kind' => 'cond', 'kicker' => 'JEŚLI', 'title' => 'Wartość ≥ 150 PLN', 'sub' => 'cart.value >= 150', 'icon' => 'filter'],
            ['x' => 320, 'y' => 200, 'kind' => 'cond', 'kicker' => 'JEŚLI', 'title' => 'Nie był w segmencie VIP', 'sub' => 'customer.segment != "vip"', 'icon' => 'filter'],
            ['x' => 610, 'y' => 40, 'kind' => 'action', 'kicker' => 'WTEDY · 1', 'title' => 'Email — szablon A', 'sub' => '„Wróć po koszyk”', 'icon' => 'mail'],
            ['x' => 610, 'y' => 200, 'kind' => 'action', 'kicker' => 'WTEDY · 2', 'title' => 'Czekaj 24h', 'sub' => 'delay 24h', 'icon' => 'pause'],
            ['x' => 610, 'y' => 360, 'kind' => 'action', 'kicker' => 'WTEDY · 3', 'title' => 'Kupon −10%', 'sub' => 'coupon: WROCMY-{id}', 'icon' => 'coupon'],
        ];
    }

    /**
     * @return list<array{0: int, 1: int}>
     */
    public function flowEdges(): array
    {
        return [[0, 1], [1, 2], [1, 3], [3, 4], [4, 5]];
    }

    /**
     * @return list<array{label: string, value: string, note: string, color?: string}>
     */
    public function simulation(): array
    {
        return [
            ['label' => 'Zdarzeń pasujących', 'value' => Format::number(2412), 'note' => 'w ostatnich 7 dniach'],
            ['label' => 'Spełniających warunki', 'value' => Format::number(1287).' <span class="muted" style="font-size:14px;">(53,4%)</span>', 'note' => 'zostałoby uruchomione'],
            ['label' => 'Estymowany przychód', 'value' => '~ '.Format::money(24800), 'note' => 'przy 18,4% konwersji', 'color' => 'var(--good)'],
        ];
    }

    /**
     * @return list<array{name: string}>
     */
    public function activeForCustomer(): array
    {
        return [
            ['name' => 'Powitanie po rejestracji'],
            ['name' => 'Rekomendacje „podobne produkty”'],
            ['name' => 'Newsletter — Tydzień smaków'],
        ];
    }
}
