<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Format;

/**
 * The four-step customer import: file, column mapping, rules, preview.
 *
 * Every step is reachable by URL, so a half-configured import survives a reload and the
 * back button behaves like the stepper.
 */
final readonly class ImportWizard
{
    public const FIRST_STEP = 1;
    public const LAST_STEP = 4;

    private const COLUMNS = [
        ['name' => 'Adres e-mail', 'samples' => ['hania.k@aurea.pl', 'marta.w@example.com', 'tomek.s@gmail.com'], 'mapped' => 'email', 'confidence' => 99],
        ['name' => 'Imię', 'samples' => ['Hania', 'Marta', 'Tomek'], 'mapped' => 'first_name', 'confidence' => 98],
        ['name' => 'Nazwisko', 'samples' => ['Kowalska', 'Wiśniewska', 'Sobczak'], 'mapped' => 'last_name', 'confidence' => 97],
        ['name' => 'Telefon', 'samples' => ['+48 600 100 200', '+48 502 311 882', '—'], 'mapped' => 'phone', 'confidence' => 95],
        ['name' => 'Data rejestracji', 'samples' => ['2024-01-14', '2024-08-22', '2025-02-03'], 'mapped' => 'created_at', 'confidence' => 92],
        ['name' => 'Status zgody mkt.', 'samples' => ['Tak', 'Tak', 'Nie'], 'mapped' => 'opt_in', 'confidence' => 88],
        ['name' => 'Liczba zamówień', 'samples' => ['7', '2', '0'], 'mapped' => 'orders_count', 'confidence' => 90],
        ['name' => 'Łączna wartość PLN', 'samples' => ['1248.00', '384.00', '0'], 'mapped' => 'lifetime_value', 'confidence' => 85],
        ['name' => 'Język', 'samples' => ['pl', 'pl', 'en'], 'mapped' => 'locale', 'confidence' => 80],
        ['name' => 'Tag CRM', 'samples' => ['vip,herbata', 'newsletter', '—'], 'mapped' => '__custom', 'confidence' => 42],
        ['name' => 'Identyfikator Shoper', 'samples' => ['83128', '83410', '84001'], 'mapped' => '__skip', 'confidence' => 0],
    ];

    private const TARGETS = [
        ['value' => '', 'label' => '— wybierz pole —'],
        ['value' => 'email', 'label' => 'Email (klucz dedup.)'],
        ['value' => 'first_name', 'label' => 'Imię'],
        ['value' => 'last_name', 'label' => 'Nazwisko'],
        ['value' => 'phone', 'label' => 'Telefon'],
        ['value' => 'created_at', 'label' => 'Data utworzenia'],
        ['value' => 'opt_in', 'label' => 'Zgoda marketingowa'],
        ['value' => 'orders_count', 'label' => 'Liczba zamówień'],
        ['value' => 'lifetime_value', 'label' => 'Wartość życiowa (LTV)'],
        ['value' => 'locale', 'label' => 'Język'],
        ['value' => 'segments', 'label' => 'Segmenty (lista)'],
        ['value' => 'tags', 'label' => 'Tagi (lista)'],
        ['value' => '__custom', 'label' => 'Atrybut własny…'],
        ['value' => '__skip', 'label' => 'Pomiń kolumnę'],
    ];

    private const PREVIEW_ROWS = [
        ['email' => 'hania.k@aurea.pl', 'name' => 'Hania Kowalska', 'optIn' => 'Tak', 'orders' => '7', 'ltv' => 1248, 'status' => 'new', 'segments' => ['Newsletter', 'VIP', 'Import — marzec 2026']],
        ['email' => 'marta.w@example.com', 'name' => 'Marta Wiśniewska', 'optIn' => 'Tak', 'orders' => '2', 'ltv' => 384, 'status' => 'update', 'segments' => ['Newsletter', 'Import — marzec 2026']],
        ['email' => 'tomek.s@gmail.com', 'name' => 'Tomek Sobczak', 'optIn' => 'Nie', 'orders' => '0', 'ltv' => 0, 'status' => 'new', 'segments' => ['Newsletter', 'Do reaktywacji', 'Import — marzec 2026']],
        ['email' => 'olek.b@example.pl', 'name' => 'Olek Borowski', 'optIn' => 'Tak', 'orders' => '14', 'ltv' => 3490, 'status' => 'update', 'segments' => ['Newsletter', 'VIP', 'Import — marzec 2026']],
        ['email' => 'invalid@@@bad', 'name' => '(nieznane)', 'optIn' => '?', 'orders' => '?', 'ltv' => null, 'status' => 'error', 'segments' => [], 'error' => 'Email nie przechodzi walidacji'],
        ['email' => 'iga.p@example.com', 'name' => 'Iga Pszczółkowska', 'optIn' => 'Tak', 'orders' => '0', 'ltv' => 0, 'status' => 'new', 'segments' => ['Newsletter', 'Do reaktywacji', 'Import — marzec 2026']],
    ];

    private const STATUS_CHIP = [
        'new' => ['tone' => 'good', 'label' => 'Nowy'],
        'update' => ['tone' => 'info', 'label' => 'Aktualizacja'],
        'error' => ['tone' => 'bad', 'label' => 'Błąd'],
    ];

    /**
     * @return list<array{n: int, label: string}>
     */
    public function steps(): array
    {
        return [
            ['n' => 1, 'label' => 'Plik'],
            ['n' => 2, 'label' => 'Mapowanie kolumn'],
            ['n' => 3, 'label' => 'Reguły i segmenty'],
            ['n' => 4, 'label' => 'Podgląd i start'],
        ];
    }

    /**
     * @return array{name: string, meta: string}
     */
    public function file(?string $uploadedName): array
    {
        return [
            'name' => $uploadedName ?? 'klienci.csv',
            'meta' => '8 420 wierszy · CSV UTF-8 · ; jako separator',
        ];
    }

    /**
     * map-row.html.twig params, one per detected column.
     *
     * @return list<array{letter: string, name: string, samples: list<string>, targets: list<array{value: string, label: string}>, mapped: string, confidence: int, skipped: bool}>
     */
    public function columns(): array
    {
        $rows = [];
        foreach (self::COLUMNS as $index => $column) {
            $rows[] = [
                'letter' => \chr(65 + $index),
                'name' => $column['name'],
                'samples' => $column['samples'],
                'targets' => self::TARGETS,
                'mapped' => $column['mapped'],
                'confidence' => $column['confidence'],
                'skipped' => '__skip' === $column['mapped'],
            ];
        }

        return $rows;
    }

    /**
     * @return list<array{value: string, label: string}>
     */
    public function targets(): array
    {
        return self::TARGETS;
    }

    /**
     * @return array{recognised: int, total: int, sure: int, unsure: int, skipped: int}
     */
    public function detection(): array
    {
        $sure = 0;
        $unsure = 0;
        $skipped = 0;
        foreach (self::COLUMNS as $column) {
            match (true) {
                0 === $column['confidence'] => ++$skipped,
                $column['confidence'] >= 70 => ++$sure,
                default => ++$unsure,
            };
        }

        return [
            'recognised' => $sure + $unsure,
            'total' => \count(self::COLUMNS),
            'sure' => $sure,
            'unsure' => $unsure,
            'skipped' => $skipped,
        ];
    }

    /**
     * @return list<array{label: string, tone: string}>
     */
    public function validations(): array
    {
        return [
            ['label' => 'Format email (RFC 5322)', 'tone' => 'ok'],
            ['label' => 'Format daty (auto-wykrycie: <span class="mono">YYYY-MM-DD</span>)', 'tone' => 'ok'],
            ['label' => 'Numer telefonu (E.164 + heurystyka PL)', 'tone' => 'ok'],
            ['label' => 'Konwersja zgody marketingowej (Tak/Nie → bool)', 'tone' => 'ok'],
            ['label' => 'Konwersja waluty (kropka/przecinek → liczba)', 'tone' => 'ok'],
            ['label' => 'Wykryte 4 duplikaty po email — patrz krok 3', 'tone' => 'info'],
        ];
    }

    /**
     * @return list<array{value: string, label: string, checked: bool}>
     */
    public function dedupStrategies(): array
    {
        return [
            ['value' => 'merge', 'label' => 'Nadpisz polami z pliku, gdy nie są puste (zalecane)', 'checked' => true],
            ['value' => 'fill', 'label' => 'Wypełnij tylko brakujące pola — nie nadpisuj istniejących', 'checked' => false],
            ['value' => 'skip', 'label' => 'Pomiń duplikat — nie zmieniaj nic', 'checked' => false],
            ['value' => 'replace', 'label' => 'Zastąp wszystkie pola pełną zawartością z pliku', 'checked' => false],
        ];
    }

    /**
     * @return list<array<string, mixed>>
     */
    public function summary(): array
    {
        return [
            ['label' => 'Wierszy łącznie', 'value' => Format::number(8420), 'delta' => 'plik wczytany poprawnie', 'dir' => 'up', 'deltaIcon' => 'check'],
            ['label' => 'Nowi klienci', 'value' => Format::number(7124), 'delta' => '~84,6% wszystkich', 'dir' => 'up'],
            ['label' => 'Aktualizacje istniejących', 'value' => Format::number(1252), 'delta' => 'nadpisanie wg reguł z kroku 3', 'dir' => 'flat', 'deltaIcon' => 'check'],
            ['label' => 'Z błędem walidacji', 'value' => '44', 'delta' => 'zostaną pominięte', 'dir' => 'down', 'deltaIcon' => 'info'],
        ];
    }

    /**
     * @return list<array{status: string, statusTone: string, statusLabel: string, email: string, name: string, optIn: string, orders: string, ltv: string, segments: list<string>, error: string}>
     */
    public function preview(): array
    {
        return array_map(static function (array $row): array {
            $chip = self::STATUS_CHIP[$row['status']];

            return [
                'status' => $row['status'],
                'statusTone' => $chip['tone'],
                'statusLabel' => $chip['label'],
                'email' => $row['email'],
                'name' => $row['name'],
                'optIn' => $row['optIn'],
                'orders' => $row['orders'],
                'ltv' => null === $row['ltv'] ? '?' : Format::money($row['ltv']),
                'segments' => $row['segments'],
                'error' => $row['error'] ?? '',
            ];
        }, self::PREVIEW_ROWS);
    }

    public function rowCount(): int
    {
        return 8420;
    }

    public function errorCount(): int
    {
        return 44;
    }

    /**
     * @return list<array{file: string, rows: string, date: string, who: string, ok: bool, note: string}>
     */
    public function recentImports(): array
    {
        return [
            ['file' => 'newsletter-2026-feb.csv', 'rows' => Format::number(4280), 'date' => '14 lut 2026', 'who' => 'Maciej K.', 'ok' => true, 'note' => 'OK'],
            ['file' => 'klienci-shoper.xml', 'rows' => Format::number(1284), 'date' => '02 sty 2026', 'who' => 'Anna B.', 'ok' => true, 'note' => 'OK'],
            ['file' => 'mailerlite-archiwum.csv', 'rows' => Format::number(8120), 'date' => '18 gru 2025', 'who' => 'Maciej K.', 'ok' => false, 'note' => '12 błędów'],
        ];
    }
}
