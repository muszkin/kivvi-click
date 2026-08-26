<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * Product feeds and the matching diagnostic.
 *
 * Without a catalogue an `add_to_cart` event is just an identifier — this is where
 * prices, availability and images come from, and where mismatches are explained.
 */
final readonly class ProductFeedCatalog
{
    private const FEEDS = [
        ['id' => 'f1', 'name' => 'aureashop.pl — Google Merchant', 'source' => 'google', 'url' => 'https://aureashop.pl/feeds/google.xml', 'products' => 1284, 'mapped' => 1284, 'mismatched' => 0, 'status' => 'synced', 'lastSyncMinutes' => 12, 'schedule' => 'co 6 godzin'],
        ['id' => 'f2', 'name' => 'aureashop.pl — Facebook Catalog', 'source' => 'facebook', 'url' => 'https://aureashop.pl/feeds/facebook.xml', 'products' => 1280, 'mapped' => 1278, 'mismatched' => 2, 'status' => 'syncing', 'lastSyncMinutes' => 1, 'schedule' => 'co 1 godzinę'],
        ['id' => 'f3', 'name' => 'mlot-narzedzia.pl — Google Merchant', 'source' => 'google', 'url' => 'https://mlot-narzedzia.pl/feed/google', 'products' => 0, 'mapped' => 0, 'mismatched' => 0, 'status' => 'error', 'lastSyncMinutes' => 180, 'schedule' => 'co 6 godzin', 'error' => 'HTTP 503 — Service Unavailable'],
        ['id' => 'f4', 'name' => 'polna-bistro.pl — XML własny', 'source' => 'xml', 'url' => 'https://polna-bistro.pl/produkty.xml', 'products' => 84, 'mapped' => 84, 'mismatched' => 0, 'status' => 'synced', 'lastSyncMinutes' => 28, 'schedule' => 'codziennie'],
    ];

    /**
     * @return list<array<string, mixed>>
     */
    public function kpis(): array
    {
        return [
            ['label' => 'Aktywne feedy', 'value' => '3', 'unit' => '/4', 'delta' => '1 z błędem', 'dir' => 'down'],
            ['label' => 'Produktów w katalogu', 'value' => Format::number(2648), 'delta' => '+42 w tym tyg.', 'dir' => 'up'],
            ['label' => 'Dopasowanie zdarzeń → produkty', 'value' => '94,8', 'unit' => '%', 'delta' => 'ostatnia doba', 'dir' => 'up', 'deltaIcon' => 'spark'],
            ['label' => 'Wartość koszyków (24h)', 'value' => Format::money(382140), 'delta' => 'z dopasowanymi cenami', 'dir' => 'up'],
        ];
    }

    /**
     * @return list<array{id: string, letter: string, color: string, bg: string, title: string, sub: string, recommended?: bool}>
     */
    public function sources(): array
    {
        return [
            ['id' => 'google', 'letter' => 'G', 'color' => 'oklch(0.62 0.16 28)', 'bg' => 'oklch(0.93 0.04 28)', 'title' => 'Google Merchant Center', 'sub' => 'OAuth · automatyczna synchronizacja co 6h', 'recommended' => true],
            ['id' => 'facebook', 'letter' => 'f', 'color' => 'oklch(0.55 0.13 250)', 'bg' => 'oklch(0.91 0.04 250)', 'title' => 'Facebook Catalog (Meta)', 'sub' => 'Catalog API · synchronizacja co 1h'],
            ['id' => 'xml', 'letter' => '×', 'color' => 'oklch(0.45 0.07 60)', 'bg' => 'oklch(0.91 0.04 60)', 'title' => 'XML / RSS własny', 'sub' => 'Dowolny URL — np. PrestaShop, Shoper, WooCommerce'],
            ['id' => 'csv', 'letter' => '↧', 'color' => 'oklch(0.42 0.06 150)', 'bg' => 'oklch(0.91 0.04 150)', 'title' => 'Plik CSV / arkusz Google', 'sub' => 'Upload pojedynczego pliku lub link do arkusza'],
        ];
    }

    /**
     * feed-card.html.twig params per connected feed.
     *
     * @return list<array<string, mixed>>
     */
    public function feeds(): array
    {
        return array_map(static function (array $feed): array {
            $card = [
                'name' => $feed['name'],
                'url' => $feed['url'],
                'source' => $feed['source'],
                'status' => $feed['status'],
                'products' => $feed['products'],
                'mapped' => $feed['mapped'],
                'mismatched' => $feed['mismatched'],
                'lastSync' => $feed['lastSyncMinutes'] < 60
                    ? $feed['lastSyncMinutes'].' min temu'
                    : \intdiv($feed['lastSyncMinutes'], 60).' godz. temu',
                'schedule' => $feed['schedule'],
            ];

            if (isset($feed['error'])) {
                $card['error'] = $feed['error'];
            }

            return $card;
        }, self::FEEDS);
    }

    /**
     * @return list<array{label: string, pct: float, value: string, tone: string}>
     */
    public function matchingCoverage(): array
    {
        return [
            ['label' => 'Po id', 'pct' => 78.0, 'value' => '78%', 'tone' => 'accent'],
            ['label' => 'Po sku (fallback)', 'pct' => 12.0, 'value' => '12%', 'tone' => 'brown'],
            ['label' => 'Po URL', 'pct' => 4.8, 'value' => '4,8%', 'tone' => 'brown'],
            ['label' => 'Niedopasowane', 'pct' => 5.2, 'value' => '5,2%', 'tone' => 'bad'],
        ];
    }

    /**
     * @return list<array{text: string, field: string}>
     */
    public function fallbackRules(): array
    {
        return [
            ['text' => 'Jeśli brak id → spróbuj', 'field' => 'sku'],
            ['text' => 'Jeśli brak sku → spróbuj', 'field' => 'gtin'],
            ['text' => 'Jeśli nadal brak → użyj', 'field' => 'URL produktu'],
        ];
    }

    public function mismatchedCount(): int
    {
        return 142;
    }
}
