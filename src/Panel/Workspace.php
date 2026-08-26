<?php

declare(strict_types=1);

namespace App\Panel;

/**
 * The tenant the panel is scoped to: its display name, plan summary and tracked sites.
 *
 * Site colours drive the dots in the event stream and the site filter rail, so they live
 * with the site rather than being picked at render time.
 */
final readonly class Workspace
{
    public const NAME = 'aureashop.pl';

    private const SITES = [
        ['id' => 'aurea', 'name' => 'aureashop.pl', 'color' => '#7a8763'],
        ['id' => 'mlot', 'name' => 'mlot-narzedzia.pl', 'color' => '#a3825b'],
        ['id' => 'pol', 'name' => 'polna-bistro.pl', 'color' => '#8b6f53'],
    ];

    /**
     * @return array{name: string, meta: string, mark: string}
     */
    public function card(): array
    {
        return [
            'name' => self::NAME,
            'meta' => 'Plan Pro · 3 strony',
            'mark' => 'AS',
        ];
    }

    /**
     * @return list<array{id: string, name: string, color: string}>
     */
    public function sites(): array
    {
        return self::SITES;
    }

    /**
     * @return array{id: string, name: string, color: string}
     */
    public function site(int $index): array
    {
        return self::SITES[$index % \count(self::SITES)];
    }
}
