<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * On-site widgets: popups, banners, slide-ins, toasts and full-screen takeovers.
 *
 * Widget copy is kept apart from the panel's own tokens — the widget renders on the
 * customer's storefront, so its colours are literals inside the template.
 */
final readonly class WidgetCatalog
{
    private const WIDGETS = [
        ['id' => 'p1', 'name' => 'Exit intent — 10% rabatu', 'status' => 'active', 'type' => 'modal', 'impressions' => 48210, 'conversion' => 7.4],
        ['id' => 'p2', 'name' => 'Pasek darmowej dostawy od 199 zł', 'status' => 'active', 'type' => 'banner', 'impressions' => 182400, 'conversion' => 2.1],
        ['id' => 'p3', 'name' => 'Slide-in: zapisz się do newslettera', 'status' => 'active', 'type' => 'slide-in', 'impressions' => 62100, 'conversion' => 4.8],
        ['id' => 'p4', 'name' => 'Social proof — „ktoś właśnie kupił”', 'status' => 'paused', 'type' => 'toast', 'impressions' => 0, 'conversion' => 0.0],
        ['id' => 'p5', 'name' => 'Promo wakacyjne — full screen', 'status' => 'draft', 'type' => 'fullscreen', 'impressions' => 0, 'conversion' => 0.0],
    ];

    private const TYPE_LABEL = [
        'modal' => 'Modal',
        'slide-in' => 'Slide-in',
        'banner' => 'Pasek banner',
        'fullscreen' => 'Pełny ekran',
        'toast' => 'Toast',
    ];

    private const STATUS_CHIP = [
        'active' => ['tone' => 'good', 'label' => 'Aktywny'],
        'paused' => ['tone' => 'warn', 'label' => 'Wstrzymany'],
        'draft' => ['tone' => 'neutral', 'label' => 'Szkic'],
    ];

    /**
     * @return list<array<string, mixed>>
     */
    public function cards(): array
    {
        return array_map(static function (array $widget): array {
            $chip = self::STATUS_CHIP[$widget['status']];

            return [
                'title' => $widget['name'],
                'chips' => [
                    ['label' => $chip['label'], 'tone' => $chip['tone']],
                    ['label' => self::TYPE_LABEL[$widget['type']], 'tone' => 'brown'],
                    ['label' => 'na wszystkich stronach'],
                    ['label' => 'exit intent'],
                ],
                'metrics' => [
                    ['value' => $widget['impressions'] > 0 ? Format::number($widget['impressions']) : '—', 'label' => 'wyświetleń', 'width' => 90],
                    ['value' => $widget['conversion'] > 0 ? Format::percent($widget['conversion']) : '—', 'label' => 'konwersja', 'width' => 80, 'color' => $widget['conversion'] > 0 ? 'var(--good)' : 'var(--fg-muted)'],
                ],
                'action' => 'go-popup',
                'payload' => $widget['id'],
            ];
        }, self::WIDGETS);
    }

    /**
     * @return array{id: string, name: string, type: string, meta: string, widget: array<string, string>}
     */
    public function selected(string $id): array
    {
        foreach (self::WIDGETS as $widget) {
            if ($widget['id'] === $id) {
                return [
                    'id' => $widget['id'],
                    'name' => $widget['name'],
                    'type' => $widget['type'],
                    'meta' => sprintf(
                        '%s na aureashop.pl · %s wyświetleń · %s konwersji',
                        self::STATUS_CHIP[$widget['status']]['label'],
                        Format::number($widget['impressions']),
                        Format::percent($widget['conversion']),
                    ),
                    'widget' => $this->content($widget['type']),
                ];
            }
        }

        return [
            'id' => $id,
            'name' => 'Nowy widget',
            'type' => 'modal',
            'meta' => 'Szkic · nieopublikowany',
            'widget' => $this->content('modal'),
        ];
    }

    public function firstId(): string
    {
        return self::WIDGETS[0]['id'];
    }

    /**
     * popup-widget.html.twig params for one widget shape.
     *
     * @return array<string, string>
     */
    public function content(string $type): array
    {
        return match ($type) {
            'banner' => [
                'type' => 'banner',
                'kicker' => 'DARMOWA DOSTAWA',
                'title' => 'Od 199 zł wysyłamy na nasz koszt',
                'cta' => 'Do zakupów →',
            ],
            'toast' => [
                'type' => 'toast',
                'title' => 'Ktoś właśnie kupił',
                'body' => 'Zielona herbata Sencha 100g · Kraków, 4 min temu',
                'cta' => 'Zobacz',
            ],
            default => [
                'type' => $type,
                'kicker' => 'CZEKAJ —',
                'title' => 'Zostań na 10% taniej',
                'body' => 'Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie. Trwa to 30 sekund.',
                'placeholder' => 'twoj@email.pl',
                'cta' => 'Wyślij mi kupon →',
                'fine' => 'Bez spamu. Wypisujesz się w 1 kliknięciu.',
            ],
        };
    }

    /**
     * @return list<array{id: string, label: string, icon: string}>
     */
    public function types(): array
    {
        return [
            ['id' => 'modal', 'label' => 'Modal', 'icon' => 'layout'],
            ['id' => 'slide-in', 'label' => 'Slide-in', 'icon' => 'arrow_right'],
            ['id' => 'banner', 'label' => 'Pasek', 'icon' => 'minus'],
            ['id' => 'fullscreen', 'label' => 'Pełny ekran', 'icon' => 'grid'],
            ['id' => 'toast', 'label' => 'Toast', 'icon' => 'bell'],
        ];
    }

    /**
     * @return list<array{icon: string, label: string, type: string}>
     */
    public function blocks(): array
    {
        return [
            ['icon' => 'list', 'label' => 'Nagłówek', 'type' => 'heading'],
            ['icon' => 'list', 'label' => 'Tekst', 'type' => 'text'],
            ['icon' => 'eye', 'label' => 'Obraz', 'type' => 'image'],
            ['icon' => 'mail', 'label' => 'Pole e-mail', 'type' => 'email-field'],
            ['icon' => 'user', 'label' => 'Pole tekstowe', 'type' => 'text-field'],
            ['icon' => 'play', 'label' => 'Przycisk CTA', 'type' => 'cta'],
            ['icon' => 'coupon', 'label' => 'Kod kuponu', 'type' => 'coupon'],
            ['icon' => 'cart', 'label' => 'Karuzela produktów', 'type' => 'carousel'],
            ['icon' => 'check', 'label' => 'Checkbox zgody', 'type' => 'consent'],
            ['icon' => 'minus', 'label' => 'Licznik czasu', 'type' => 'countdown'],
        ];
    }

    /**
     * @return list<string>
     */
    public function variables(): array
    {
        return ['{{customer.first_name}}', '{{cart.value}}', '{{coupon.code}}', '{{product.last_viewed}}'];
    }

    /**
     * @return list<array{label: string, tone?: string, value?: string, unit?: string, note?: string}>
     */
    public function triggers(): array
    {
        return [
            ['label' => 'exit intent', 'tone' => 'accent', 'note' => 'kursor opuszcza okno'],
            ['label' => 'czas na stronie', 'value' => '20', 'unit' => 'sek.'],
            ['label' => 'scroll', 'value' => '60', 'unit' => '% strony'],
        ];
    }

    /**
     * @return list<array{label: string, checked: bool}>
     */
    public function audience(): array
    {
        return [
            ['label' => 'Tylko niezalogowani', 'checked' => true],
            ['label' => 'Nie widzieli w ostatnich <span class="mono">14 dniach</span>', 'checked' => true],
            ['label' => 'Tylko ruch z kampanii płatnych', 'checked' => false],
            ['label' => 'Pomiń, jeśli koszyk jest pusty', 'checked' => true],
        ];
    }

    /**
     * @return list<string>
     */
    public function accentColors(): array
    {
        return ['oklch(0.42 0.06 150)', 'oklch(0.55 0.07 55)', 'oklch(0.52 0.12 32)', 'oklch(0.22 0.02 150)'];
    }
}
