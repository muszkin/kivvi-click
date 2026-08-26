<?php

declare(strict_types=1);

namespace App\Panel;

/**
 * Polish-locale number, money and relative-time formatting.
 *
 * Values reach Twig pre-formatted: templates are markup, not a place to decide
 * how many decimals a conversion rate has.
 */
final class Format
{
    private const THOUSANDS_SEPARATOR = "\u{202f}";

    public static function number(int|float $value): string
    {
        return number_format((float) $value, 0, ',', self::THOUSANDS_SEPARATOR);
    }

    public static function money(int|float $value): string
    {
        return self::number($value).' zł';
    }

    public static function percent(float $value, int $decimals = 1): string
    {
        return number_format($value, $decimals, ',', self::THOUSANDS_SEPARATOR).'%';
    }

    public static function initials(string $name): string
    {
        $parts = preg_split('/\s+/', trim($name)) ?: [];
        $initials = '';
        foreach (\array_slice($parts, 0, 2) as $part) {
            $initials .= mb_strtoupper(mb_substr($part, 0, 1));
        }

        return $initials;
    }

    public static function timeAgo(\DateTimeImmutable $moment, \DateTimeImmutable $now): string
    {
        $seconds = max(0, $now->getTimestamp() - $moment->getTimestamp());

        return match (true) {
            $seconds < 60 => 'teraz',
            $seconds < 3600 => \intdiv($seconds, 60).' min temu',
            $seconds < 86400 => \intdiv($seconds, 3600).' godz. temu',
            $seconds < 172800 => 'wczoraj',
            default => \intdiv($seconds, 86400).' dni temu',
        };
    }
}
