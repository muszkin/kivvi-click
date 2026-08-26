<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * Copy for the public marketing page: feature grid, the three onboarding steps,
 * and the Free / Pro pricing pair.
 */
final readonly class LandingContent
{
    /**
     * @return list<array{icon: string, title: string, body: string}>
     */
    public function features(): array
    {
        return [
            ['icon' => 'activity', 'title' => 'Strumień zdarzeń na żywo', 'body' => 'Widzisz każde kliknięcie, dodanie do koszyka i zakup — z dokładnością do milisekundy. Mercure-driven, bez polling-u.'],
            ['icon' => 'bolt', 'title' => 'Reguły bez kodowania', 'body' => 'KIEDY → JEŚLI → WTEDY. Skomponuj automatyzację z bloków lub przeciągnij węzły na płótnie. Test reguły na danych historycznych zanim wystartujesz.'],
            ['icon' => 'mail', 'title' => 'E-maile z prawdziwego zdarzenia', 'body' => 'WYSIWYG z blokami i zmiennymi. Wysyłka przez Twojego dostawcę (SMTP / SES / SendGrid). Tracking otwarć i kliknięć w panelu.'],
            ['icon' => 'layout', 'title' => 'Popupy i web layery', 'body' => 'Modale, slide-iny, paski. Pełna kontrola nad triggerami: exit intent, czas na stronie, scroll, segment klienta.'],
            ['icon' => 'coupon', 'title' => 'Kupony w punkcie konwersji', 'body' => 'Generuj unikalne kody, pokazuj wtedy gdy mają znaczenie — przy porzuceniu koszyka, dla VIP-ów, po N zakupach.'],
            ['icon' => 'target', 'title' => 'Rekomendacje ML', 'body' => 'Collaborative filtering, „kupili też”, „podobne”, „trending”. Plan Pro odblokowuje pełen silnik personalizacji.'],
        ];
    }

    /**
     * @return list<array{number: string, title: string, body: string}>
     */
    public function steps(): array
    {
        return [
            ['number' => '01', 'title' => 'Wklej snippet', 'body' => 'Jeden tag <script> w <head>. Po 30 sekundach zaczynasz widzieć zdarzenia w panelu.'],
            ['number' => '02', 'title' => 'Wybierz szablon', 'body' => 'Powitanie, porzucony koszyk, win-back, rekomendacje — startuj z gotowca i dopasuj do Twoich tonacji.'],
            ['number' => '03', 'title' => 'Publikuj', 'body' => 'Najpierw test na danych historycznych. Potem przycisk „Opublikuj” — i automatyzacja działa.'],
        ];
    }

    /**
     * @return list<array{tier: string, price: string, unit: string, items: list<string>, cta: string, featured?: bool, badge?: string}>
     */
    public function plans(): array
    {
        return [
            [
                'tier' => 'Free',
                'price' => '0',
                'unit' => ' zł / mies.',
                'items' => [
                    '1 strona, do 50 000 zdarzeń / mies.',
                    '1 000 maili / mies.',
                    'Reguły, popupy, kupony',
                    'Wsparcie społeczności',
                ],
                'cta' => 'Zacznij za darmo',
            ],
            [
                'tier' => 'Pro',
                'price' => '149',
                'unit' => ' zł / mies.',
                'featured' => true,
                'badge' => 'popularne',
                'items' => [
                    '5 stron, bez limitu zdarzeń',
                    'Bez limitu maili (Twój dostawca)',
                    'Pełny silnik rekomendacji ML',
                    'Webhooks, API, RODO/DPA',
                    'Wsparcie e-mail w 24h',
                ],
                'cta' => 'Zacznij 14-dniowy trial →',
            ],
        ];
    }

    /**
     * @return list<string>
     */
    public function trustPoints(): array
    {
        return ['14 dni Pro za darmo', 'Bez karty', 'Skrypt 2 KB'];
    }

    /**
     * Traffic shape drawn inside the framed product preview.
     *
     * The panel's cardiogram is a live canvas; the marketing page has no live account
     * behind it, so the same shape is server-rendered as a sparkline instead of showing
     * a visitor an empty chart.
     *
     * @return list<float>
     */
    public function previewSeries(): array
    {
        $values = [];
        for ($i = 0; $i < 60; ++$i) {
            $pulse = sin($i / 2.2) + 0.6 * sin($i / 0.9) + 0.35 * sin($i / 5.5);
            $values[] = round(12 + 5 * $pulse + ($i > 44 ? ($i - 44) * 0.9 : 0), 2);
        }

        return $values;
    }

    /**
     * KPI tiles inside the framed product preview.
     *
     * @return list<array{label: string, value: string, unit?: string}>
     */
    public function previewTiles(): array
    {
        return [
            ['label' => 'Zdarzeń / min', 'value' => '847'],
            ['label' => 'Aktywne sesje', 'value' => '312'],
            ['label' => 'Maile (24h)', 'value' => Format::number(8410)],
            ['label' => 'Przychód (24h)', 'value' => Format::number(94200), 'unit' => 'zł'],
        ];
    }
}
