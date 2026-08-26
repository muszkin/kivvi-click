<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * E-mail campaigns: index KPIs, table rows and the composer model behind the
 * e-mail editor (envelope, blocks, document sections, variables).
 */
final readonly class CampaignCatalog
{
    private const CAMPAIGNS = [
        ['id' => 'k1', 'name' => 'Powrót do koszyka — wariant A', 'status' => 'active', 'type' => 'trigger', 'sent' => 1287, 'open' => 62.4, 'click' => 18.4, 'revenue' => 24800],
        ['id' => 'k2', 'name' => 'Witamy w Kivvi', 'status' => 'active', 'type' => 'trigger', 'sent' => 412, 'open' => 78.1, 'click' => 41.2, 'revenue' => 8930],
        ['id' => 'k3', 'name' => 'Newsletter — Tydzień smaków #18', 'status' => 'sent', 'type' => 'broadcast', 'sent' => 8420, 'open' => 31.8, 'click' => 6.2, 'revenue' => 14200],
        ['id' => 'k4', 'name' => 'Black weekend — VIP', 'status' => 'scheduled', 'type' => 'broadcast', 'sent' => 0, 'open' => 0.0, 'click' => 0.0, 'revenue' => 0],
        ['id' => 'k5', 'name' => 'Reaktywacja po 60 dniach', 'status' => 'paused', 'type' => 'trigger', 'sent' => 0, 'open' => 0.0, 'click' => 0.0, 'revenue' => 0],
    ];

    private const STATUS_CHIP = [
        'active' => ['tone' => 'good', 'label' => 'Aktywna'],
        'paused' => ['tone' => 'warn', 'label' => 'Wstrzymana'],
        'sent' => ['tone' => 'neutral', 'label' => 'Wysłana'],
        'scheduled' => ['tone' => 'info', 'label' => 'Zaplanowana'],
        'draft' => ['tone' => 'neutral', 'label' => 'Szkic'],
    ];

    /**
     * @return list<array<string, mixed>>
     */
    public function kpis(): array
    {
        return [
            ['label' => 'Wysłane (30 dni)', 'value' => Format::number(142410), 'delta' => '+18% vs poprzedni okres', 'dir' => 'up'],
            ['label' => 'Średni open rate', 'value' => '38,4', 'unit' => '%', 'delta' => '+2,1pp', 'dir' => 'up'],
            ['label' => 'Średni CTR', 'value' => '7,8', 'unit' => '%', 'delta' => '−0,4pp', 'dir' => 'down'],
            ['label' => 'Przychód z kampanii', 'value' => Format::money(184230), 'delta' => '+24%', 'dir' => 'up'],
        ];
    }

    /**
     * @return list<array{label: string, align?: string}>
     */
    public function columns(): array
    {
        return [
            ['label' => 'Kampania'],
            ['label' => 'Typ'],
            ['label' => 'Status'],
            ['label' => 'Wysłane', 'align' => 'right'],
            ['label' => 'Otwarcia', 'align' => 'right'],
            ['label' => 'Kliknięcia', 'align' => 'right'],
            ['label' => 'Przychód', 'align' => 'right'],
            ['label' => ''],
        ];
    }

    /**
     * @return list<array{id: string, name: string, status: string, statusTone: string, statusLabel: string, typeLabel: string, typeTone: string, sent: string, open: string, click: string, revenue: string}>
     */
    public function rows(): array
    {
        return array_map(static function (array $campaign): array {
            $chip = self::STATUS_CHIP[$campaign['status']];

            return [
                'id' => $campaign['id'],
                'name' => $campaign['name'],
                'status' => $campaign['status'],
                'statusTone' => $chip['tone'],
                'statusLabel' => $chip['label'],
                'typeLabel' => 'trigger' === $campaign['type'] ? 'Wyzwalana' : 'Masowa',
                'typeTone' => 'trigger' === $campaign['type'] ? 'accent' : 'brown',
                'sent' => $campaign['sent'] > 0 ? Format::number($campaign['sent']) : '—',
                'open' => $campaign['open'] > 0 ? Format::percent($campaign['open']) : '—',
                'click' => $campaign['click'] > 0 ? Format::percent($campaign['click']) : '—',
                'revenue' => $campaign['revenue'] > 0 ? Format::money($campaign['revenue']) : '—',
            ];
        }, self::CAMPAIGNS);
    }

    /**
     * @return list<array{label: string, count?: int, active: bool, action: string, payload: string}>
     */
    public function filters(string $active = 'all'): array
    {
        return [
            ['label' => 'Wszystkie', 'count' => 18, 'active' => 'all' === $active, 'action' => 'set-campaign-filter', 'payload' => 'all'],
            ['label' => 'Wyzwalane', 'count' => 12, 'active' => 'trigger' === $active, 'action' => 'set-campaign-filter', 'payload' => 'trigger'],
            ['label' => 'Jednorazowe', 'count' => 6, 'active' => 'broadcast' === $active, 'action' => 'set-campaign-filter', 'payload' => 'broadcast'],
            ['label' => 'Wstrzymane', 'count' => 2, 'active' => 'paused' === $active, 'action' => 'set-campaign-filter', 'payload' => 'paused'],
        ];
    }

    /**
     * @return array{id: string, name: string, meta: string, subject: string, sender: string}
     */
    public function template(string $id): array
    {
        foreach (self::CAMPAIGNS as $campaign) {
            if ($campaign['id'] === $id) {
                return [
                    'id' => $campaign['id'],
                    'name' => $campaign['name'],
                    'meta' => 'Szablon wyzwalany · 3 produkty placeholders · 412 wysyłek (7d)',
                    'subject' => 'Hania, Twój koszyk czeka — wróć i odbierz −10%',
                    'sender' => 'sklep@aureashop.pl',
                ];
            }
        }

        return [
            'id' => $id,
            'name' => 'Nowy szablon email',
            'meta' => 'Szkic · nigdy nie wysłany',
            'subject' => 'Temat wiadomości',
            'sender' => 'sklep@aureashop.pl',
        ];
    }

    /**
     * @return list<array{icon: string, label: string, type: string}>
     */
    public function blocks(): array
    {
        return [
            ['icon' => 'layout', 'label' => 'Nagłówek', 'type' => 'hero'],
            ['icon' => 'list', 'label' => 'Tekst', 'type' => 'text'],
            ['icon' => 'eye', 'label' => 'Obraz', 'type' => 'image'],
            ['icon' => 'cart', 'label' => 'Produkty', 'type' => 'products'],
            ['icon' => 'coupon', 'label' => 'Kupon', 'type' => 'coupon'],
            ['icon' => 'play', 'label' => 'Przycisk CTA', 'type' => 'cta'],
            ['icon' => 'users', 'label' => 'Recenzje', 'type' => 'reviews'],
            ['icon' => 'minus', 'label' => 'Separator', 'type' => 'divider'],
            ['icon' => 'mail', 'label' => 'Stopka', 'type' => 'footer'],
            ['icon' => 'code', 'label' => 'HTML własny', 'type' => 'html'],
        ];
    }

    /**
     * @return list<array{token: string, description: string}>
     */
    public function variables(): array
    {
        return [
            ['token' => '{{customer.first_name}}', 'description' => 'Imię klienta'],
            ['token' => '{{cart.value}}', 'description' => 'Wartość koszyka'],
            ['token' => '{{cart.items}}', 'description' => 'Produkty w koszyku'],
            ['token' => '{{coupon.code}}', 'description' => 'Kod kuponu'],
            ['token' => '{{site.name}}', 'description' => 'Nazwa sklepu'],
        ];
    }

    /**
     * Sections of the 600px e-mail document.
     *
     * @return list<array<string, mixed>>
     */
    public function sections(): array
    {
        return [
            [
                'type' => 'hero',
                'kicker' => 'AUREASHOP · ZIELONE HERBATY',
                'title' => 'Hania, Twój koszyk czeka.',
                'body' => 'Zostawiłaś u nas <strong>2 produkty</strong> warte <strong>88,90 zł</strong>. Wróć w 48h, dostajesz <strong>−10%</strong>.',
            ],
            [
                'type' => 'coupon',
                'code' => 'WROCMY-A8F2',
                'note' => 'Ważny do 14 maja 2026, 23:59',
                'cta' => 'Wróć do koszyka →',
                'href' => '#',
            ],
            [
                'type' => 'products',
                'kicker' => 'W TWOIM KOSZYKU',
                'items' => [
                    ['name' => 'Zielona herbata Sencha 100g', 'price' => '38,90 zł'],
                    ['name' => 'Filiżanka porcelanowa Nora', 'price' => '50,00 zł'],
                ],
            ],
            [
                'type' => 'products',
                'kicker' => 'REKOMENDACJE DLA CIEBIE',
                'items' => [
                    ['name' => 'Czajnik żeliwny', 'price' => '189,00 zł'],
                    ['name' => 'Świeca sojowa „Figa”', 'price' => '49,00 zł'],
                ],
            ],
            [
                'type' => 'footer',
                'body' => 'Dostajesz tę wiadomość, bo subskrybujesz aureashop.pl. <a href="#" style="color: oklch(0.42 0.06 150);">Zarządzaj subskrypcjami</a> · <a href="#" style="color: oklch(0.42 0.06 150);">Wypisz się</a>',
            ],
        ];
    }

    /**
     * Inspector state for the block selected in the canvas.
     *
     * @return array{blockName: string, blockId: string, title: string, placeholders: list<string>, backgrounds: list<string>, alignment: string, padding: string, visibility: string}
     */
    public function selectedBlock(): array
    {
        return [
            'blockName' => 'Hero — tytuł + opis',
            'blockId' => 'hero_1',
            'title' => 'Hania, Twój koszyk czeka.',
            'placeholders' => ['{{customer.first_name}}, Twój koszyk czeka.', 'Twój koszyk czeka.'],
            'backgrounds' => ['oklch(0.94 0.02 85)', 'oklch(0.90 0.04 150)', 'oklch(0.88 0.035 60)', 'oklch(1 0 0)'],
            'alignment' => 'center',
            'padding' => '36px 32px',
            'visibility' => 'customer.has_orders > 0',
        ];
    }
}
