<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * Customer profiles shown in the index, the dashboard "recently seen" card and the
 * 360 profile.
 *
 * The set is generated from a fixed seed, so a row keeps the same name, revenue and
 * order count between requests and between test runs.
 */
final readonly class CustomerDirectory
{
    private const FIRST_NAMES = ['Anna', 'Kasia', 'Marta', 'Tomek', 'Piotr', 'Łukasz', 'Olek', 'Magda', 'Bartek', 'Iga', 'Hania', 'Wojtek', 'Justyna', 'Karol', 'Sandra'];
    private const LAST_INITIALS = ['K.', 'W.', 'S.', 'N.', 'B.', 'M.', 'C.', 'Z.', 'R.', 'P.', 'D.', 'L.'];
    private const SEGMENTS = [
        ['label' => 'VIP', 'tone' => 'accent'],
        ['label' => 'Powracający', 'tone' => 'good'],
        ['label' => 'Nowy', 'tone' => 'brown'],
        ['label' => 'Ryzyko odejścia', 'tone' => 'warn'],
    ];
    private const TOTAL = 24;

    /**
     * @return list<array{id: string, name: string, email: string, initials: string, orders: int, revenue: int, lastSeenMinutes: int, segment: array{label: string, tone: string}}>
     */
    public function all(): array
    {
        return array_map($this->profile(...), range(0, self::TOTAL - 1));
    }

    /**
     * @return array{id: string, name: string, email: string, initials: string, orders: int, revenue: int, lastSeenMinutes: int, segment: array{label: string, tone: string}}
     */
    public function byId(string $id): array
    {
        foreach ($this->all() as $customer) {
            if ($customer['id'] === $id) {
                return $customer;
            }
        }

        throw new \InvalidArgumentException(sprintf('Unknown customer "%s".', $id));
    }

    /**
     * @return list<array{id: string, name: string, email: string, initials: string, orders: int, revenue: int, lastSeenMinutes: int, segment: array{label: string, tone: string}}>
     */
    public function recent(int $limit): array
    {
        return \array_slice($this->all(), 0, $limit);
    }

    public function total(): int
    {
        return 4_218;
    }

    /**
     * @return array{id: string, name: string, email: string, initials: string, orders: int, revenue: int, lastSeenMinutes: int, segment: array{label: string, tone: string}}
     */
    private function profile(int $seed): array
    {
        $firstName = self::FIRST_NAMES[$seed % \count(self::FIRST_NAMES)];
        $lastInitial = self::LAST_INITIALS[($seed * 3) % \count(self::LAST_INITIALS)];
        $name = $firstName.' '.$lastInitial;

        return [
            'id' => 'c_'.(1000 + $seed),
            'name' => $name,
            'email' => mb_strtolower($this->asciiFold($firstName)).'.'.mb_strtolower(rtrim($lastInitial, '.')).'@example.com',
            'initials' => Format::initials($name),
            'orders' => $seed % 7,
            'revenue' => (($seed * 137) % 1900) + 49,
            'lastSeenMinutes' => $seed % 12,
            'segment' => self::SEGMENTS[$seed % \count(self::SEGMENTS)],
        ];
    }

    private function asciiFold(string $value): string
    {
        return strtr($value, ['ą' => 'a', 'ć' => 'c', 'ę' => 'e', 'ł' => 'l', 'ń' => 'n', 'ó' => 'o', 'ś' => 's', 'ź' => 'z', 'ż' => 'z', 'Ł' => 'L']);
    }
}
