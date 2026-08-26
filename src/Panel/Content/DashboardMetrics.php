<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * Dashboard KPIs, sparkline series and the cardiogram legend.
 *
 * Series are generated deterministically: the same page render always draws the same
 * sparkline, which keeps visual snapshots stable.
 */
final readonly class DashboardMetrics
{
    private const SERIES_LENGTH = 40;

    /**
     * @return list<array<string, mixed>>
     */
    public function kpis(): array
    {
        return [
            ['label' => 'Zdarzeń ostatnia minuta', 'value' => '847', 'delta' => '+12,4% vs śr.', 'dir' => 'up', 'series' => $this->series(60, 0.4)],
            ['label' => 'Aktywne sesje', 'value' => '312', 'delta' => '+4,1%', 'dir' => 'up', 'series' => $this->series(30, 0.3)],
            ['label' => 'Maile dostarczone (24h)', 'value' => Format::number(8410), 'delta' => '−2,0%', 'dir' => 'down', 'series' => $this->series(90, 0.25)],
            ['label' => 'Przypisany przychód (24h)', 'value' => Format::money(94200), 'delta' => '+22,4%', 'dir' => 'up', 'series' => $this->series(100, 0.5)],
        ];
    }

    /**
     * @return list<array{label: string, value: string}>
     */
    public function cardiogramLegend(): array
    {
        return [
            ['label' => 'Średnia 5 min:', 'value' => '14,2 ev/s'],
            ['label' => 'Pik:', 'value' => '28 ev/s · 12:42:18'],
            ['label' => 'Aktualizacja:', 'value' => 'co 1 s'],
        ];
    }

    /**
     * @return list<float>
     */
    private function series(float $base, float $variance): array
    {
        $values = [];
        for ($i = 0; $i < self::SERIES_LENGTH; ++$i) {
            $wave = sin($i / 3.1 + $base) + 0.5 * sin($i / 1.7 + $variance * 10);
            $values[] = round($base * (1 + $variance * $wave / 2), 2);
        }

        return $values;
    }
}
