<?php

declare(strict_types=1);

/**
 * Story registry — the storybook's single source of truth.
 *
 * Wire it up in config/services.yaml:
 *
 *   App\Controller\StorybookController:
 *       arguments:
 *           $stories: '%storybook.stories%'
 *
 * and in config/packages/storybook.yaml (dev only):
 *
 *   parameters:
 *       storybook.stories: !include '../storybook.php'
 *
 * or simply require this file from a compiler pass / bundle extension.
 *
 * A "variant" is literally the params hash you would pass to include().
 * Adding a variant here is how you document a state — never by writing a
 * one-off demo template.
 */

return [
    /* ---------------- ATOMS ---------------- */

    'button' => [
        'title' => 'Button',
        'group' => 'Atomy',
        'template' => 'components/atoms/button.html.twig',
        'doc' => 'Primary interactive control. Five variants x three sizes. Behaviour is always declared with data-action, never an inline handler.',
        'variants' => [
            'primary' => ['label' => 'Opublikuj', 'variant' => 'primary', 'icon' => 'play'],
            'default' => ['label' => 'Synchronizuj teraz', 'icon' => 'play'],
            'ghost' => ['label' => 'Anuluj', 'variant' => 'ghost'],
            'danger' => ['label' => 'Usuń konto', 'variant' => 'danger', 'icon' => 'trash'],
            'small' => ['label' => 'Filtry', 'size' => 'sm', 'icon' => 'filter'],
            'large' => ['label' => 'Zacznij za darmo', 'variant' => 'primary', 'size' => 'lg'],
            'icon-only' => ['icon' => 'settings', 'variant' => 'ghost', 'size' => 'sm'],
            'disabled' => ['label' => 'Poprzednia', 'size' => 'sm', 'disabled' => true],
        ],
    ],

    'chip' => [
        'title' => 'Chip',
        'group' => 'Atomy',
        'template' => 'components/atoms/chip.html.twig',
        'doc' => 'Status and metadata pill. The seven tones carry meaning — pick by semantics, not by looks. Never interactive; use filter-chip for that.',
        'variants' => [
            'neutral' => ['label' => 'na wszystkich stronach'],
            'good' => ['label' => 'Aktywna', 'tone' => 'good'],
            'good-live' => ['label' => 'Synchronizuje…', 'tone' => 'good', 'live' => true],
            'warn' => ['label' => 'Wstrzymana', 'tone' => 'warn'],
            'bad' => ['label' => 'Błąd', 'tone' => 'bad'],
            'info' => ['label' => 'Zaplanowana', 'tone' => 'info'],
            'accent' => ['label' => 'VIP', 'tone' => 'accent'],
            'brown' => ['label' => 'Wyzwalana', 'tone' => 'brown'],
            'mono' => ['label' => 'cart_abandon', 'mono' => true],
            'with-icon' => ['label' => 'zweryfikowany', 'tone' => 'good', 'icon' => 'check'],
        ],
    ],

    'dot' => [
        'title' => 'Dot',
        'group' => 'Atomy',
        'template' => 'components/atoms/dot.html.twig',
        'doc' => 'Status dot. The live variant needs the wrapper span so the pulse ring has a positioning context.',
        'variants' => [
            'static' => ['color' => 'var(--fg-muted)'],
            'site-colour' => ['color' => '#7a8763'],
            'live' => ['live' => true],
        ],
    ],

    'avatar' => [
        'title' => 'Avatar',
        'group' => 'Atomy',
        'template' => 'components/atoms/avatar.html.twig',
        'doc' => 'Initials on brown. There are no profile photos in this system — do not add them without a design decision.',
        'variants' => [
            'row (22px)' => ['name' => 'Hania Kowalska', 'size' => 22],
            'default (30px)' => ['name' => 'Maciej Kowalczyk'],
            'header (48px)' => ['name' => 'Anna Bartosz', 'size' => 48],
            'profile (72px)' => ['name' => 'Iga Nowak', 'size' => 72],
        ],
    ],

    'field' => [
        'title' => 'Field',
        'group' => 'Atomy',
        'template' => 'components/atoms/field.html.twig',
        'doc' => 'Labelled control. Focus is a 3px accent-soft ring plus an accent border — do not remove the outline without replacing it.',
        'variants' => [
            'text' => ['name' => 'company', 'label' => 'Nazwa firmy', 'value' => 'Aurea Shop sp. z o.o.'],
            'mono' => ['name' => 'sender', 'label' => 'Domyślny nadawca', 'value' => 'sklep@aureashop.pl', 'mono' => true],
            'password' => ['name' => 'pass', 'type' => 'password', 'label' => 'Hasło', 'value' => 'sekret123'],
            'select' => ['name' => 'retention', 'label' => 'Surowe zdarzenia', 'type' => 'select', 'options' => [
                ['value' => '90d', 'label' => '90 dni'],
                ['value' => '13m', 'label' => '13 miesięcy', 'selected' => true],
                ['value' => '24m', 'label' => '24 miesiące'],
            ]],
            'textarea' => ['name' => 'body', 'label' => 'Opis', 'type' => 'textarea', 'value' => 'Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie.'],
            'with-help' => ['name' => 'limit', 'label' => 'Limit dzienny', 'value' => '50 000', 'mono' => true, 'help' => 'Po przekroczeniu kampanie są kolejkowane do następnej doby.'],
            'error' => ['name' => 'email', 'label' => 'Email', 'value' => 'invalid@@@bad', 'error' => 'Adres nie przechodzi walidacji RFC 5322.'],
        ],
    ],

    'toggle-row' => [
        'title' => 'Toggle row',
        'group' => 'Atomy',
        'template' => 'components/atoms/toggle-row.html.twig',
        'doc' => 'Checkbox or radio with an inline label. The whole row is the hit target.',
        'variants' => [
            'checkbox-on' => ['name' => 'mfa', 'label' => 'Wymagaj 2FA dla wszystkich członków zespołu', 'checked' => true],
            'checkbox-off' => ['name' => 'ml', 'label' => 'Wyłącz profilowanie ML dla klientów z UE bez wyraźnej zgody'],
            'radio' => ['kind' => 'radio', 'name' => 'dedup', 'label' => 'Nadpisz polami z pliku, gdy nie są puste (zalecane)', 'checked' => true],
        ],
    ],

    'bar' => [
        'title' => 'Usage bar',
        'group' => 'Atomy',
        'template' => 'components/atoms/bar.html.twig',
        'doc' => 'Coverage / quota bar. Brown means informational, bad means a problem, accent means normal usage.',
        'variants' => [
            'accent' => ['label' => 'Po id', 'pct' => 78, 'value' => '78%'],
            'brown' => ['label' => 'Po sku (fallback)', 'pct' => 12, 'value' => '12%', 'tone' => 'brown'],
            'bad' => ['label' => 'Niedopasowane', 'pct' => 5.2, 'value' => '5.2%', 'tone' => 'bad'],
            'quota' => ['label' => 'Ingest zdarzeń (na sek.)', 'pct' => 42, 'value' => '42 / 100'],
        ],
    ],

    'sparkline' => [
        'title' => 'Sparkline',
        'group' => 'Atomy',
        'template' => 'components/atoms/sparkline.html.twig',
        'doc' => 'Server-rendered SVG trend. No JS, no library. Lives inside a KPI tile at 45% opacity.',
        'variants' => [
            'rising' => ['values' => [42, 48, 44, 51, 58, 55, 61, 68, 64, 72, 78, 74, 81, 88]],
            'volatile' => ['values' => [60, 22, 71, 34, 66, 28, 74, 41, 58, 30, 68, 44]],
        ],
    ],

    'code-block' => [
        'title' => 'Code block',
        'group' => 'Atomy',
        'template' => 'components/atoms/code-block.html.twig',
        'doc' => 'Dark snippet panel. Highlighting is a Twig filter so the markup is deterministic and printable.',
        'variants' => [
            'tracker-snippet' => ['code' => "<!-- Kivvi-click tracker -->\n<script async\n  src=\"https://cdn.kivvi-click.io/k.js\"\n  data-site=\"aureashop.pl\"\n  data-key=\"pk_live_8a4f2c...\"></script>"],
        ],
    ],

    'wordmark' => [
        'title' => 'Logo / wordmark',
        'group' => 'Atomy',
        'template' => 'components/atoms/wordmark.html.twig',
        'doc' => 'Kiwi bird: body in currentColor, beak in --brown, leaf in --accent. The dot in "kivvi·click" is a --brown square, not a period.',
        'variants' => [
            'sidebar' => [],
            'auth' => ['size' => 28, 'fontSize' => 24],
            'footer' => ['size' => 16, 'fontSize' => 14],
        ],
    ],

    /* ---------------- MOLECULES ---------------- */

    'card' => [
        'title' => 'Card',
        'group' => 'Molekuły',
        'template' => 'components/molecules/card.html.twig',
        'doc' => 'The default surface. Use the raw slot (not body) when a table fills the card, so it goes edge to edge.',
        'variants' => [
            'with-head' => ['title' => 'Webhooks', 'sub' => 'retry z backoffem: 5 prób w ciągu 6 godzin', 'body' => '<p class="muted" style="margin:0;">Treść karty.</p>'],
            'with-icon-and-live' => ['title' => 'Strumień na żywo', 'sub' => 'wszystkie strony', 'live' => true, 'body' => '<p class="muted" style="margin:0;">Treść karty.</p>'],
            'body-only' => ['body' => '<p style="margin:0;">Karta bez nagłówka — używana w podsumowaniach.</p>'],
            'with-foot' => ['title' => 'Log zdarzeń', 'body' => '<p class="muted" style="margin:0;">Treść.</p>', 'foot' => '<span class="muted" style="font-size:12px;">Pokazuję ostatnie 30 z 9 360</span>'],
            'danger-zone' => ['title' => 'Strefa nieodwracalna', 'class' => 'danger-zone', 'body' => '<p class="muted" style="margin:0;">Usunięcie konta jest nieodwracalne.</p>'],
        ],
    ],

    'kpi-tile' => [
        'title' => 'KPI tile',
        'group' => 'Molekuły',
        'template' => 'components/molecules/kpi-tile.html.twig',
        'doc' => 'One metric. Values arrive pre-formatted for pl-PL — number formatting is a server concern, never done in the template.',
        'variants' => [
            'up-with-sparkline' => ['label' => 'Zdarzeń ostatnia minuta', 'value' => '847', 'delta' => '+12.4% vs śr.', 'dir' => 'up', 'series' => [42, 48, 44, 51, 58, 55, 61, 68, 64, 72, 78, 74, 81, 88]],
            'down' => ['label' => 'Maile dostarczone (24h)', 'value' => '8 410', 'delta' => '−2.0%', 'dir' => 'down'],
            'with-unit' => ['label' => 'Średni open rate', 'value' => '38.4', 'unit' => '%', 'delta' => '+2.1pp', 'dir' => 'up'],
            'currency' => ['label' => 'Przypisany przychód (24h)', 'value' => '94 200 zł', 'delta' => '+22.4%', 'dir' => 'up'],
            'bare' => ['label' => 'Aktywne sesje', 'value' => '312'],
        ],
    ],

    'segmented' => [
        'title' => 'Segmented control',
        'group' => 'Molekuły',
        'template' => 'components/molecules/segmented.html.twig',
        'doc' => 'Two to four mutually exclusive options with short labels. Above four, use a select.',
        'variants' => [
            'theme' => ['action' => 'set-theme', 'options' => [
                ['value' => 'light', 'label' => 'Light', 'icon' => 'sun', 'active' => true],
                ['value' => 'dark', 'label' => 'Dark', 'icon' => 'moon'],
            ]],
            'rule-builder' => ['action' => 'set-rulebuilder', 'options' => [
                ['value' => 'list', 'label' => 'Lista', 'icon' => 'list', 'active' => true],
                ['value' => 'flow', 'label' => 'Diagram', 'icon' => 'flow'],
            ]],
            'range' => ['action' => 'set-range', 'options' => [
                ['value' => '5m', 'label' => '5 min'],
                ['value' => '1h', 'label' => '1 godz.', 'active' => true],
                ['value' => '24h', 'label' => '24 godz.'],
                ['value' => '7d', 'label' => '7 dni'],
            ]],
        ],
    ],

    'filter-chip' => [
        'title' => 'Filter chip',
        'group' => 'Molekuły',
        'template' => 'components/molecules/filter-chip.html.twig',
        'doc' => 'Interactive filter pill for list rails. Counts are formatted with a narrow space thousands separator.',
        'variants' => [
            'active' => ['label' => 'Wszyscy', 'icon' => 'users', 'count' => 4416, 'active' => true],
            'inactive' => ['label' => 'VIP', 'count' => 142],
            'with-dot' => ['label' => 'aureashop.pl', 'dotColor' => '#7a8763'],
            'plain' => ['label' => 'Porzucone koszyki', 'count' => 287],
        ],
    ],

    'nav-item' => [
        'title' => 'Nav item',
        'group' => 'Molekuły',
        'template' => 'components/molecules/nav-item.html.twig',
        'doc' => 'Sidebar row. The label must truncate with an ellipsis, never wrap to two lines.',
        'variants' => [
            'active' => ['label' => 'Pulpit', 'icon' => 'dashboard', 'route' => 'dashboard', 'href' => '/pl/dashboard', 'active' => true],
            'idle' => ['label' => 'Klienci', 'icon' => 'users', 'route' => 'customers', 'href' => '/pl/customers'],
            'with-badge' => ['label' => 'Strumień zdarzeń', 'icon' => 'activity', 'route' => 'events', 'href' => '/pl/events', 'badge' => '·'],
            'long-label' => ['label' => 'Bardzo długa nazwa pozycji nawigacji', 'icon' => 'settings', 'route' => 'settings', 'href' => '/pl/settings'],
        ],
    ],

    'event-row' => [
        'title' => 'Event row',
        'group' => 'Molekuły',
        'template' => 'components/molecules/event-row.html.twig',
        'doc' => 'The atom of the live feed. Six columns collapse to three under 900px. Set isNew for the 800ms flash-in used when Mercure prepends a row.',
        'variants' => [
            'purchase' => ['time' => '14:42:08', 'typeIcon' => 'money', 'tone' => 'good', 'type' => 'Zakup', 'detail' => '412,00 PLN · 4 produkty', 'customerName' => 'Hania Kowalska', 'customerId' => 'c_1001', 'siteName' => 'aureashop.pl', 'siteColor' => '#7a8763'],
            'add-to-cart' => ['time' => '14:41:52', 'typeIcon' => 'cart', 'tone' => 'accent', 'type' => 'Dodanie do koszyka', 'detail' => 'Zielona herbata Sencha 100g', 'customerName' => 'Marta Wiśniewska', 'customerId' => 'c_1002', 'siteName' => 'aureashop.pl', 'siteColor' => '#7a8763'],
            'cart-abandon' => ['time' => '14:38:11', 'typeIcon' => 'cart', 'tone' => 'warn', 'type' => 'Porzucony koszyk', 'detail' => 'Po 8:42 minut', 'customerName' => 'Tomek Sobczak', 'customerId' => 'c_1003', 'siteName' => 'mlot-narzedzia.pl', 'siteColor' => '#a3825b'],
            'pageview' => ['time' => '14:37:04', 'typeIcon' => 'eye', 'type' => 'Wyświetlenie strony', 'detail' => '/produkt/swieca-soja-figa', 'customerName' => 'Iga Nowak', 'customerId' => 'c_1004', 'siteName' => 'polna-bistro.pl', 'siteColor' => '#8b6f53'],
            'signup' => ['time' => '14:33:47', 'typeIcon' => 'user', 'tone' => 'brown', 'type' => 'Rejestracja', 'detail' => 'olek.b@example.pl', 'customerName' => 'Olek Borowski', 'customerId' => 'c_1005', 'siteName' => 'aureashop.pl', 'siteColor' => '#7a8763'],
            'just-arrived' => ['time' => '14:42:09', 'typeIcon' => 'search', 'type' => 'Wyszukiwanie', 'detail' => '"herbata zielona organiczna"', 'customerName' => 'Kasia Nowak', 'customerId' => 'c_1006', 'siteName' => 'aureashop.pl', 'siteColor' => '#7a8763', 'isNew' => true],
        ],
    ],

    'timeline-item' => [
        'title' => 'Timeline item',
        'group' => 'Molekuły',
        'template' => 'components/molecules/timeline-item.html.twig',
        'doc' => 'Node on the customer activity axis. Must be inside .timeline — the rail and the ring are drawn by that parent.',
        'variants' => [
            'pageview' => ['time' => 'Dziś · 14:42', 'title' => 'Wyświetlenie produktu', 'detail' => '/produkt/zielona-herbata-sencha', 'icon' => 'eye'],
            'purchase' => ['time' => '14 mar · 11:08', 'title' => 'Zakup', 'detail' => '4 produkty · 412,00 PLN · zamówienie #AR-1287', 'icon' => 'money'],
            'automation-fired' => ['time' => 'Wczoraj · 17:09', 'title' => 'Porzucenie koszyka', 'detail' => '2 produkty · 88,90 PLN · uruchomiona reguła "Powrót do koszyka"', 'icon' => 'cart'],
        ],
    ],

    'table' => [
        'title' => 'Table',
        'group' => 'Molekuły',
        'template' => 'components/molecules/table.html.twig',
        'doc' => 'Sticky-header data table. Numeric columns are right-aligned and monospaced. Wrap in a card raw slot.',
        'variants' => [
            'invoices' => [
                'columns' => [['label' => 'Numer'], ['label' => 'Data'], ['label' => 'Kwota netto', 'align' => 'right'], ['label' => 'Status']],
                'rows' => [
                    ['<span class="mono" style="font-size:12.5px;">FV/2026/08/0142</span>', '<span class="muted" style="font-size:12.5px;">01 sie 2026</span>', '<span class="mono">149,00 zł</span>', '<span class="chip good">zapłacona</span>'],
                    ['<span class="mono" style="font-size:12.5px;">FV/2026/07/0139</span>', '<span class="muted" style="font-size:12.5px;">01 lip 2026</span>', '<span class="mono">149,00 zł</span>', '<span class="chip good">zapłacona</span>'],
                ],
            ],
        ],
    ],

    'tabs' => [
        'title' => 'Tabs',
        'group' => 'Molekuły',
        'template' => 'components/molecules/tabs.html.twig',
        'doc' => 'Underline tab strip for detail pages. Counts belong in the label, in parentheses.',
        'variants' => [
            'automation-editor' => ['tabs' => [
                ['label' => 'Budowniczy', 'active' => true],
                ['label' => 'Statystyki'],
                ['label' => 'Wykonania (1 287)'],
                ['label' => 'Historia zmian'],
            ]],
            'customer' => ['tabs' => [
                ['label' => 'Aktywność', 'active' => true],
                ['label' => 'Zamówienia (7)'],
                ['label' => 'Wysłane maile (12)'],
                ['label' => 'Atrybuty'],
            ]],
        ],
    ],

    'stepper' => [
        'title' => 'Stepper',
        'group' => 'Molekuły',
        'template' => 'components/molecules/stepper.html.twig',
        'doc' => 'Wizard header. Every step is a jump target — users must be able to go back and change a decision.',
        'variants' => [
            'step-1' => ['current' => 1, 'steps' => [['n' => 1, 'label' => 'Plik'], ['n' => 2, 'label' => 'Mapowanie kolumn'], ['n' => 3, 'label' => 'Reguły i segmenty'], ['n' => 4, 'label' => 'Podgląd i start']]],
            'step-2' => ['current' => 2, 'steps' => [['n' => 1, 'label' => 'Plik'], ['n' => 2, 'label' => 'Mapowanie kolumn'], ['n' => 3, 'label' => 'Reguły i segmenty'], ['n' => 4, 'label' => 'Podgląd i start']]],
            'step-4' => ['current' => 4, 'steps' => [['n' => 1, 'label' => 'Plik'], ['n' => 2, 'label' => 'Mapowanie kolumn'], ['n' => 3, 'label' => 'Reguły i segmenty'], ['n' => 4, 'label' => 'Podgląd i start']]],
        ],
    ],

    'dropzone' => [
        'title' => 'Dropzone',
        'group' => 'Molekuły',
        'template' => 'components/molecules/dropzone.html.twig',
        'doc' => 'File drop target. Wire drag events in upload.ts; the dragging state is data-dragging on the zone.',
        'variants' => [
            'import' => ['title' => 'Przeciągnij plik tutaj lub kliknij aby wybrać', 'sub' => 'CSV (UTF-8 / ISO-8859-2), XML, XLSX · maks. 50 MB', 'hint' => 'np. newsletter-2026-mar.csv', 'accept' => '.csv,.xml,.xlsx'],
        ],
    ],

    'map-row' => [
        'title' => 'Column mapping row',
        'group' => 'Molekuły',
        'template' => 'components/molecules/map-row.html.twig',
        'doc' => 'One row of the import mapping table. Confidence colour-codes the auto-detection: >=90 good, >=70 amber, below that bad.',
        'variants' => [
            'high-confidence' => ['letter' => 'A', 'name' => 'Adres e-mail', 'samples' => ['hania.k@aurea.pl', 'marta.w@example.com'], 'mapped' => 'email', 'confidence' => 99, 'targets' => [['value' => 'email', 'label' => 'Email (klucz dedup.)'], ['value' => '__skip', 'label' => 'Pomiń kolumnę']]],
            'low-confidence' => ['letter' => 'J', 'name' => 'Tag CRM', 'samples' => ['vip,herbata', 'newsletter'], 'mapped' => '__custom', 'confidence' => 42, 'targets' => [['value' => '__custom', 'label' => 'Atrybut własny…'], ['value' => 'tags', 'label' => 'Tagi (lista)'], ['value' => '__skip', 'label' => 'Pomiń kolumnę']]],
            'skipped' => ['letter' => 'K', 'name' => 'Identyfikator Shoper', 'samples' => ['83128', '83410'], 'mapped' => '__skip', 'confidence' => 0, 'skipped' => true, 'targets' => [['value' => '__skip', 'label' => 'Pomiń kolumnę']]],
        ],
    ],

    'callout' => [
        'title' => 'Callout',
        'group' => 'Molekuły',
        'template' => 'components/molecules/callout.html.twig',
        'doc' => 'Inline advisory. Never use for form validation — errors belong on the field.',
        'variants' => [
            'info' => ['text' => 'Webhook Slacka zwraca 410 od 3 godzin. Po 5 nieudanych próbach zostanie automatycznie wstrzymany.'],
            'accent-autodetect' => ['tone' => 'accent', 'icon' => 'spark', 'text' => '<strong>Kivvi rozpoznał 9 z 11 kolumn automatycznie.</strong> Sprawdź żółte i czerwone — wymagają decyzji.'],
            'bad' => ['tone' => 'bad', 'text' => 'HTTP 503 — Service Unavailable'],
        ],
    ],

    'cond-rule' => [
        'title' => 'Conditional rule',
        'group' => 'Molekuły',
        'template' => 'components/molecules/cond-rule.html.twig',
        'doc' => 'Editable condition line. Wraps on narrow panels rather than scrolling horizontally.',
        'variants' => [
            'ltv' => ['field' => 'lifetime_value', 'operators' => ['≥', '>', '='], 'value' => '500', 'suffix' => 'PLN, dodaj do', 'targetLabel' => 'VIP'],
            'orders' => ['field' => 'orders_count', 'operators' => ['='], 'value' => '0', 'suffix' => ', dodaj do', 'targetLabel' => 'Do reaktywacji', 'targetTone' => 'brown'],
        ],
    ],

    'position-grid' => [
        'title' => 'Position grid',
        'group' => 'Molekuły',
        'template' => 'components/molecules/position-grid.html.twig',
        'doc' => '3x3 widget placement. Index is row-major, 0 to 8; 4 is centre.',
        'variants' => [
            'centre' => ['selected' => 4],
            'bottom-right' => ['selected' => 8],
        ],
    ],

    'dns-row' => [
        'title' => 'DNS record row',
        'group' => 'Molekuły',
        'template' => 'components/molecules/dns-row.html.twig',
        'doc' => 'Email domain authentication status.',
        'variants' => [
            'verified' => ['record' => 'SPF', 'value' => 'v=spf1 include:amazonses.com ~all', 'ok' => true],
            'missing' => ['record' => 'BIMI', 'value' => 'nie skonfigurowane — logo marki nie pojawi się w Gmailu', 'ok' => false],
        ],
    ],

    'hook-row' => [
        'title' => 'Webhook row',
        'group' => 'Molekuły',
        'template' => 'components/molecules/hook-row.html.twig',
        'doc' => 'Endpoint with last HTTP status. Non-2xx flips the code chip to the bad tone.',
        'variants' => [
            'healthy' => ['url' => 'https://aureashop.pl/hooks/kivvi', 'events' => ['purchase', 'cart_abandon'], 'code' => 200, 'last' => '2 min temu'],
            'failing' => ['url' => 'https://hooks.slack.com/services/T0…', 'events' => ['automation.failed'], 'code' => 410, 'last' => '3 godz. temu'],
        ],
    ],

    /* ---------------- ORGANISMS ---------------- */

    'kpi-grid' => [
        'title' => 'KPI grid',
        'group' => 'Organizmy',
        'template' => 'components/organisms/kpi-grid.html.twig',
        'doc' => 'Four metrics across, two under 1100px. Three columns is the only other allowed count (customer profile scores).',
        'variants' => [
            'dashboard' => ['tiles' => [
                ['label' => 'Zdarzeń ostatnia minuta', 'value' => '847', 'delta' => '+12.4%', 'dir' => 'up', 'series' => [42, 51, 58, 61, 68, 72, 78, 88]],
                ['label' => 'Aktywne sesje', 'value' => '312', 'delta' => '+4.1%', 'dir' => 'up'],
                ['label' => 'Maile dostarczone (24h)', 'value' => '8 410', 'delta' => '−2.0%', 'dir' => 'down'],
                ['label' => 'Przypisany przychód (24h)', 'value' => '94 200 zł', 'delta' => '+22.4%', 'dir' => 'up'],
            ]],
            'three-up' => ['columns' => 3, 'tiles' => [
                ['label' => 'Wskaźnik zaangażowania', 'value' => '82', 'unit' => '/100', 'delta' => '+12 vs miesiąc temu', 'dir' => 'up'],
                ['label' => 'Prawd. zakupu (30d)', 'value' => '68', 'unit' => '%', 'delta' => 'silny sygnał', 'dir' => 'up', 'deltaIcon' => 'spark'],
                ['label' => 'Open rate (90d)', 'value' => '74', 'unit' => '%', 'delta' => '+6.2pp', 'dir' => 'up'],
            ]],
        ],
    ],

    'page-head' => [
        'title' => 'Page head',
        'group' => 'Organizmy',
        'template' => 'components/organisms/page-head.html.twig',
        'doc' => 'The serif italic title REQUIRES line-height >= 1.15 and padding-bottom >= 0.08em or its descenders clip. This bit off once already.',
        'variants' => [
            'plain' => ['title' => 'Klienci', 'sub' => '4 416 zidentyfikowanych klientów · 7 632 anonimowych sesji'],
            'with-back' => ['title' => 'Hania Kowalska', 'sub' => 'hania.k@aurea.pl · klient od 14 stycznia 2024', 'back' => ['label' => 'Powrót do listy', 'route' => 'customers']],
        ],
    ],

    'event-stream' => [
        'title' => 'Event stream',
        'group' => 'Organizmy',
        'template' => 'components/organisms/event-stream.html.twig',
        'doc' => 'Server renders page one; the Mercure controller only prepends. Rows arrive as rendered HTML so event-row.html.twig stays the only place row markup exists.',
        'variants' => [
            'seeded' => ['events' => [
                ['time' => '14:42:08', 'typeIcon' => 'money', 'tone' => 'good', 'type' => 'Zakup', 'detail' => '412,00 PLN · 4 produkty', 'customerName' => 'Hania Kowalska', 'customerId' => 'c_1001', 'siteName' => 'aureashop.pl', 'siteColor' => '#7a8763'],
                ['time' => '14:41:52', 'typeIcon' => 'cart', 'tone' => 'accent', 'type' => 'Dodanie do koszyka', 'detail' => 'Zielona herbata Sencha 100g', 'customerName' => 'Marta Wiśniewska', 'customerId' => 'c_1002', 'siteName' => 'aureashop.pl', 'siteColor' => '#7a8763'],
                ['time' => '14:38:11', 'typeIcon' => 'cart', 'tone' => 'warn', 'type' => 'Porzucony koszyk', 'detail' => 'Po 8:42 minut', 'customerName' => 'Tomek Sobczak', 'customerId' => 'c_1003', 'siteName' => 'mlot-narzedzia.pl', 'siteColor' => '#a3825b'],
                ['time' => '14:37:04', 'typeIcon' => 'eye', 'type' => 'Wyświetlenie strony', 'detail' => '/produkt/swieca-soja-figa', 'customerName' => 'Iga Nowak', 'customerId' => 'c_1004', 'siteName' => 'polna-bistro.pl', 'siteColor' => '#8b6f53'],
            ]],
        ],
    ],

    'rule-pipeline' => [
        'title' => 'Rule pipeline (list view)',
        'group' => 'Organizmy',
        'template' => 'components/organisms/rule-pipeline.html.twig',
        'doc' => 'The linear rule builder. Arrows between steps are pure CSS and only appear above 1100px.',
        'variants' => [
            'cart-abandon' => ['steps' => [
                ['n' => 1, 'kicker' => 'KIEDY · trigger', 'title' => 'Klient porzuca koszyk', 'addLabel' => 'Dodaj warunek wyzwalacza', 'blocks' => [
                    ['icon' => 'cart', 'title' => 'Zdarzenie', 'body' => '<span class="pill">cart_abandon</span> wyzwolone po <span class="pill">8 min</span> nieaktywności'],
                    ['icon' => 'globe', 'title' => 'Strony', 'body' => 'Reguła działa na: <span class="pill">aureashop.pl</span>'],
                ]],
                ['n' => 2, 'kicker' => 'JEŚLI · warunki', 'title' => 'Wszystkie muszą się zgadzać', 'addLabel' => 'Dodaj warunek', 'blocks' => [
                    ['icon' => 'eye', 'title' => 'Częstotliwość', 'body' => 'Klient <strong>nie otrzymał</strong> tej automatyzacji w ciągu <span class="pill">7 dni</span>'],
                    ['icon' => 'mail', 'title' => 'Subskrypcja', 'body' => 'Zgoda marketingowa <span class="pill">tak</span>'],
                ]],
                ['n' => 3, 'kicker' => 'WTEDY · akcje', 'title' => 'Sekwencja krok po kroku', 'addLabel' => 'Dodaj krok', 'blocks' => [
                    ['icon' => 'mail', 'title' => 'Wyślij email — od razu', 'body' => 'Szablon <span class="pill">"Wróć po Twój koszyk"</span>'],
                    ['icon' => 'coupon', 'title' => 'Po 24h — wyślij kupon', 'body' => '<span class="pill">WROCMY-{id}</span> · <span class="pill">−10%</span>'],
                ]],
            ]],
        ],
    ],

    'flow-canvas' => [
        'title' => 'Rule flow canvas',
        'group' => 'Organizmy',
        'template' => 'components/organisms/flow-canvas.html.twig',
        'doc' => 'Node-graph view of the same rule model. Edges are dashed beziers in one SVG behind the nodes; node coordinates are px within a 900x540 canvas.',
        'variants' => [
            'cart-abandon' => [
                'nodes' => [
                    ['x' => 30, 'y' => 40, 'kind' => 'trigger', 'kicker' => 'KIEDY', 'title' => 'Porzucenie koszyka', 'sub' => 'cart_abandon · po 8 min', 'icon' => 'cart'],
                    ['x' => 320, 'y' => 40, 'kind' => 'cond', 'kicker' => 'JEŚLI', 'title' => 'Wartość ≥ 150 PLN', 'sub' => 'cart.value >= 150', 'icon' => 'filter'],
                    ['x' => 320, 'y' => 200, 'kind' => 'cond', 'kicker' => 'JEŚLI', 'title' => 'Nie jest w segmencie VIP', 'sub' => 'customer.segment != "vip"', 'icon' => 'filter'],
                    ['x' => 610, 'y' => 40, 'kind' => 'action', 'kicker' => 'WTEDY · 1', 'title' => 'Email — szablon A', 'sub' => '"Wróć po koszyk"', 'icon' => 'mail'],
                    ['x' => 610, 'y' => 200, 'kind' => 'action', 'kicker' => 'WTEDY · 2', 'title' => 'Czekaj 24h', 'sub' => 'delay 24h', 'icon' => 'pause'],
                    ['x' => 610, 'y' => 360, 'kind' => 'action', 'kicker' => 'WTEDY · 3', 'title' => 'Kupon −10%', 'sub' => 'coupon: WROCMY-{id}', 'icon' => 'coupon'],
                ],
                'edges' => [[0, 1], [1, 2], [1, 3], [3, 4], [4, 5]],
            ],
        ],
    ],

    'list-card' => [
        'title' => 'List card',
        'group' => 'Organizmy',
        'template' => 'components/organisms/list-card.html.twig',
        'doc' => 'Summary row for automations and popups: title plus meta chips, then right-aligned metrics.',
        'variants' => [
            'automation' => [
                'title' => 'Powrót do porzuconego koszyka',
                'action' => 'go-automation', 'payload' => 'a1',
                'chips' => [
                    ['label' => 'Aktywna', 'tone' => 'good'],
                    ['label' => 'Porzucenie koszyka', 'tone' => 'accent'],
                    ['label' => 'email', 'tone' => 'brown'],
                    ['label' => 'popup', 'tone' => 'brown'],
                ],
                'metrics' => [
                    ['value' => '1 287', 'label' => 'uruchomień (7d)'],
                    ['value' => '18.4%', 'label' => 'konwersja', 'color' => 'var(--good)', 'width' => 80],
                    ['value' => '24 800 zł', 'label' => 'przychód (7d)', 'width' => 110],
                ],
            ],
            'draft' => [
                'title' => 'Powiadomienie o powrocie produktu',
                'chips' => [['label' => 'Szkic'], ['label' => 'product_back_in_stock', 'tone' => 'accent', 'mono' => true]],
                'metrics' => [
                    ['value' => '—', 'label' => 'uruchomień (7d)'],
                    ['value' => '—', 'label' => 'konwersja', 'width' => 80],
                    ['value' => '—', 'label' => 'przychód (7d)', 'width' => 110],
                ],
            ],
        ],
    ],

    'feed-card' => [
        'title' => 'Product feed card',
        'group' => 'Organizmy',
        'template' => 'components/organisms/feed-card.html.twig',
        'doc' => 'Connected catalogue feed. The error state inserts a bad-tone strip above the stats — errors are never hidden behind a tooltip.',
        'variants' => [
            'synced' => ['name' => 'aureashop.pl — Google Merchant', 'url' => 'https://aureashop.pl/feeds/google.xml', 'source' => 'google', 'status' => 'synced', 'products' => 1284, 'mapped' => 1284, 'mismatched' => 0, 'lastSync' => '12 min temu', 'schedule' => 'co 6 godzin'],
            'syncing' => ['name' => 'aureashop.pl — Facebook Catalog', 'url' => 'https://aureashop.pl/feeds/facebook.xml', 'source' => 'facebook', 'status' => 'syncing', 'products' => 1280, 'mapped' => 1278, 'mismatched' => 2, 'lastSync' => 'teraz', 'schedule' => 'co 1 godzinę'],
            'error' => ['name' => 'mlot-narzedzia.pl — Google Merchant', 'url' => 'https://mlot-narzedzia.pl/feed/google', 'source' => 'google', 'status' => 'error', 'error' => 'HTTP 503 — Service Unavailable', 'products' => 0, 'mapped' => 0, 'mismatched' => 0, 'lastSync' => '3 godz. temu', 'schedule' => 'co 6 godzin'],
            'custom-xml' => ['name' => 'polna-bistro.pl — XML własny', 'url' => 'https://polna-bistro.pl/produkty.xml', 'source' => 'xml', 'status' => 'synced', 'products' => 84, 'mapped' => 84, 'mismatched' => 0, 'lastSync' => '28 min temu', 'schedule' => 'codziennie'],
        ],
    ],

    'popup-widget' => [
        'title' => 'Popup widget',
        'group' => 'Organizmy',
        'template' => 'components/organisms/popup-widget.html.twig',
        'doc' => 'Renders on the CUSTOMER\'S storefront, so it uses literal oklch() colours instead of our tokens — it must never inherit the app theme.',
        'variants' => [
            'modal' => ['type' => 'modal', 'kicker' => 'CZEKAJ —', 'title' => 'Zostań na 10% taniej', 'body' => 'Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie.', 'placeholder' => 'twoj@email.pl', 'cta' => 'Wyślij mi kupon →', 'fine' => 'Bez spamu. Wypisujesz się w 1 kliknięciu.'],
            'slide-in' => ['type' => 'slide-in', 'kicker' => 'CZEKAJ —', 'title' => 'Zostań na 10% taniej', 'placeholder' => 'twoj@email.pl', 'cta' => 'Wyślij mi kupon →'],
            'banner' => ['type' => 'banner', 'kicker' => 'DARMOWA DOSTAWA', 'title' => 'Od 199 zł wysyłamy na nasz koszt', 'cta' => 'Do zakupów →'],
            'fullscreen' => ['type' => 'fullscreen', 'kicker' => 'TYLKO DZIŚ', 'title' => 'Promo wakacyjne', 'body' => 'Wszystko taniej o 20% do północy.', 'cta' => 'Zobacz kolekcję →'],
            'toast' => ['type' => 'toast', 'title' => 'Ktoś właśnie kupił', 'body' => 'Zielona herbata Sencha 100g · Kraków, 4 min temu'],
        ],
    ],

    'popup-stage' => [
        'title' => 'Popup stage',
        'group' => 'Organizmy',
        'template' => 'components/organisms/popup-stage.html.twig',
        'doc' => 'Preview of a widget on a mock storefront. The min-height (480 desktop / 620 mobile) is load-bearing: without it the aspect-ratio box collapses under the widget and overflow:hidden clips it.',
        'variants' => [
            'modal-desktop' => ['type' => 'modal', 'device' => 'desktop', 'widget' => null],
            'banner-desktop' => ['type' => 'banner', 'device' => 'desktop', 'widget' => null],
            'slide-in-mobile' => ['type' => 'slide-in', 'device' => 'mobile', 'widget' => null],
        ],
    ],

    'modal' => [
        'title' => 'Modal',
        'group' => 'Organizmy',
        'template' => 'components/organisms/modal.html.twig',
        'doc' => 'Centred dialog on a 45% scrim. Escape closes, scrim click closes, focus is trapped. Renders fixed — the storybook canvas will show it over the whole frame.',
        'variants' => [
            'confirm' => ['title' => 'Podłącz feed produktów', 'body' => '<p class="muted" style="margin:0 0 12px;">Podaj adres URL feedu w formacie Google Merchant.</p>', 'foot' => '<button class="btn">Anuluj</button><button class="btn primary">Podłącz</button>'],
        ],
    ],

    'sidebar' => [
        'title' => 'Sidebar',
        'group' => 'Organizmy',
        'template' => 'components/organisms/sidebar.html.twig',
        'doc' => 'The left rail. Collapse is driven by [data-sidebar] on the .app ancestor, so in isolation it always renders expanded.',
        'variants' => [
            'default' => [
                'current' => 'dashboard',
                'workspace' => ['name' => 'aureashop.pl', 'meta' => 'Plan Pro · 3 strony', 'mark' => 'AS'],
                'user' => ['name' => 'Maciej Kowalczyk', 'email' => 'maciej@aureashop.pl'],
                'groups' => [
                    ['section' => 'main', 'label' => 'Główne', 'items' => [
                        ['label' => 'Pulpit', 'icon' => 'dashboard', 'route' => 'dashboard', 'href' => '/pl/dashboard'],
                        ['label' => 'Strumień zdarzeń', 'icon' => 'activity', 'route' => 'events', 'href' => '/pl/events', 'badge' => '·'],
                        ['label' => 'Klienci', 'icon' => 'users', 'route' => 'customers', 'href' => '/pl/customers'],
                    ]],
                    ['section' => 'automate', 'label' => 'Automatyzacja', 'items' => [
                        ['label' => 'Reguły', 'icon' => 'bolt', 'route' => 'automations', 'href' => '/pl/automations'],
                        ['label' => 'Kampanie email', 'icon' => 'mail', 'route' => 'campaigns', 'href' => '/pl/campaigns'],
                        ['label' => 'Popupy i widgety', 'icon' => 'layout', 'route' => 'popups', 'href' => '/pl/popups'],
                    ]],
                    ['section' => 'data', 'label' => 'Dane', 'items' => [
                        ['label' => 'Feedy produktów', 'icon' => 'cart', 'route' => 'feeds', 'href' => '/pl/feeds'],
                        ['label' => 'Import klientów', 'icon' => 'upload', 'route' => 'import', 'href' => '/pl/import'],
                    ]],
                    ['section' => 'config', 'label' => 'Konfiguracja', 'items' => [
                        ['label' => 'Ustawienia', 'icon' => 'settings', 'route' => 'settings', 'href' => '/pl/settings'],
                    ]],
                ],
            ],
        ],
    ],

    'topbar' => [
        'title' => 'Topbar',
        'group' => 'Organizmy',
        'template' => 'components/organisms/topbar.html.twig',
        'doc' => 'Sticky 56px header. Breadcrumbs and the command bar must never wrap — the bar shrinks to just the magnifier under 1100px.',
        'variants' => [
            'light' => ['siteName' => 'aureashop.pl', 'crumb' => 'Pulpit', 'locale' => 'pl', 'theme' => 'light'],
            'dark' => ['siteName' => 'aureashop.pl', 'crumb' => 'Ustawienia', 'locale' => 'en', 'theme' => 'dark'],
        ],
    ],

    'settings-nav' => [
        'title' => 'Settings nav',
        'group' => 'Organizmy',
        'template' => 'components/organisms/settings-nav.html.twig',
        'doc' => 'The eight configuration sections. Stacks above the panel under 1000px.',
        'variants' => [
            'sites-active' => [
                'current' => 'sites',
                'tabs' => [
                    ['id' => 'account', 'icon' => 'user', 'label' => 'Konto'],
                    ['id' => 'sites', 'icon' => 'globe', 'label' => 'Śledzone strony'],
                    ['id' => 'team', 'icon' => 'users', 'label' => 'Zespół'],
                    ['id' => 'providers', 'icon' => 'mail', 'label' => 'Dostawcy email'],
                    ['id' => 'api', 'icon' => 'code', 'label' => 'Webhooks i API'],
                    ['id' => 'notifications', 'icon' => 'bell', 'label' => 'Powiadomienia'],
                    ['id' => 'billing', 'icon' => 'money', 'label' => 'Plan i płatności'],
                    ['id' => 'gdpr', 'icon' => 'info', 'label' => 'RODO / DPA'],
                ],
            ],
        ],
    ],

    'editor-shell' => [
        'title' => 'Editor shell',
        'group' => 'Organizmy',
        'template' => 'components/organisms/editor-shell.html.twig',
        'doc' => 'Three-pane WYSIWYG frame shared by the email and popup editors: 280px library, fluid canvas, 280px inspector. The inspector hides under 1300px.',
        'variants' => [
            'skeleton' => [
                'leftTitle' => 'Bloki', 'leftIcon' => 'grid',
                'left' => '<p class="muted" style="font-size:12.5px;">Biblioteka bloków.</p>',
                'canvas' => '<div style="display:grid;place-items:center;min-height:320px;color:var(--fg-muted);font-size:13px;">Obszar kanwy</div>',
                'right' => '<p class="muted" style="font-size:12.5px;">Inspektor właściwości.</p>',
            ],
        ],
    ],
    /* ---------------- UZUPEŁNIENIA — parytet z storybook/stories.js ---------------- */

    'icon' => [
        'title' => 'Icon',
        'group' => 'Atomy',
        'template' => 'components/atoms/icon.html.twig',
        'doc' => 'Pełny zestaw ikon: 24x24 viewBox, stroke-width 1.7, zaokrąglone końce. To jedyne dozwolone wartości parametru name — nie wklejaj SVG w szablonie. W storybooku Twiga wyrenderuj katalog pętlą po globalu icons.',
        'variants' => [
            'bolt' => ['name' => 'bolt'],
            'activity' => ['name' => 'activity'],
            'cart' => ['name' => 'cart'],
            'mail' => ['name' => 'mail'],
            'users' => ['name' => 'users'],
            'w nawigacji (16px)' => ['name' => 'dashboard', 'class' => 'nav-icon'],
        ],
    ],

    'kbd' => [
        'title' => 'Kbd',
        'group' => 'Atomy',
        'template' => 'components/atoms/kbd.html.twig',
        'doc' => 'Token skrótu klawiaturowego. Występuje wyłącznie w pasku komend.',
        'variants' => [
            'command bar' => ['keys' => '⌘K'],
            'escape' => ['keys' => 'Esc'],
        ],
    ],

    'profile-fact' => [
        'title' => 'Profile fact',
        'group' => 'Molekuły',
        'template' => 'components/molecules/profile-fact.html.twig',
        'doc' => 'Wiersz etykieta/wartość w karcie profilu klienta. Wartość zawsze monospace — te dane porównuje się między klientami, więc muszą się pionowo zgadzać.',
        'variants' => [
            'liczba' => ['label' => 'Zamówienia', 'value' => '7'],
            'kwota' => ['label' => 'Wartość życiowa', 'value' => '4 992 zł'],
            'data' => ['label' => 'Pierwsze zdarzenie', 'value' => '14 sty 2024'],
            'identyfikator' => ['label' => 'customer_id', 'value' => 'c_1001', 'small' => true],
        ],
    ],

    'workspace-card' => [
        'title' => 'Workspace card',
        'group' => 'Molekuły',
        'template' => 'components/molecules/workspace-card.html.twig',
        'doc' => 'Przełącznik konta/strony na górze sidebara. Po zwinięciu panelu zostaje tylko znak 28px.',
        'variants' => [
            'default' => ['name' => 'aureashop.pl', 'meta' => 'Plan Pro · 3 strony', 'mark' => 'AS'],
            'inne konto' => ['name' => 'mlot-narzedzia.pl', 'meta' => 'Plan Free · 1 strona', 'mark' => 'MN'],
        ],
    ],

    'pagination' => [
        'title' => 'Pagination',
        'group' => 'Molekuły',
        'template' => 'components/molecules/pagination.html.twig',
        'doc' => 'Wyśrodkowane prev/next z monospace odczytem strony. Nawigacja parametrem ?page=N.',
        'variants' => [
            'pierwsza strona' => ['page' => 1, 'pages' => 192],
            'środek' => ['page' => 42, 'pages' => 192],
            'ostatnia' => ['page' => 192, 'pages' => 192],
        ],
    ],

    'add-slot' => [
        'title' => 'Add slot',
        'group' => 'Molekuły',
        'template' => 'components/molecules/add-slot.html.twig',
        'doc' => 'Przerywana afordancja „dodaj kolejny”. Na hover przechodzi na akcent. Wszędzie, gdzie lista jest rozszerzalna.',
        'variants' => [
            'krok reguły' => ['label' => 'Dodaj krok', 'action' => 'add-block'],
            'warunek' => ['label' => 'Dodaj warunek', 'action' => 'add-block'],
            'reguła warunkowa' => ['label' => 'Dodaj regułę warunkową', 'action' => 'add-rule'],
        ],
    ],

    'rb-block' => [
        'title' => 'Rule block',
        'group' => 'Molekuły',
        'template' => 'components/molecules/rb-block.html.twig',
        'doc' => 'Blok w kroku rule buildera. W treści używaj <span class="pill"> do wartości edytowalnych w miejscu.',
        'variants' => [
            'zdarzenie' => ['icon' => 'cart', 'title' => 'Zdarzenie', 'body' => '<span class="pill">cart_abandon</span> wyzwolone po <span class="pill">8 min</span> nieaktywności'],
            'warunek' => ['icon' => 'eye', 'title' => 'Częstotliwość', 'body' => 'Klient <strong>nie otrzymał</strong> tej automatyzacji w ciągu <span class="pill">7 dni</span>'],
            'akcja' => ['icon' => 'coupon', 'title' => 'Po 24h — wyślij kupon', 'body' => 'Kod jednorazowy <span class="pill">WROCMY-{id}</span> · <span class="pill">−10%</span> na cały koszyk'],
        ],
    ],

    'trigger-row' => [
        'title' => 'Trigger row',
        'group' => 'Molekuły',
        'template' => 'components/molecules/trigger-row.html.twig',
        'doc' => 'Linia wyzwalacza popupu z opcjonalnym progiem liczbowym w miejscu.',
        'variants' => [
            'exit intent' => ['label' => 'exit intent', 'tone' => 'accent', 'note' => 'kursor opuszcza okno'],
            'czas na stronie' => ['label' => 'czas na stronie', 'value' => '20', 'unit' => 'sek.'],
            'scroll' => ['label' => 'scroll', 'value' => '60', 'unit' => '% strony'],
        ],
    ],

    'swatch-grid' => [
        'title' => 'Accent swatches',
        'group' => 'Molekuły',
        'template' => 'components/molecules/swatch-grid.html.twig',
        'doc' => 'Wyselekcjonowane palety akcentu — nigdy dowolny color picker. Dowolny kolor zepsuje kontrast --accent-fg na przyciskach.',
        'variants' => [
            'cztery palety' => [
                'selected' => 'green',
                'palettes' => [
                    ['id' => 'green', 'name' => 'Mech', 'base' => 'oklch(0.42 0.06 150)', 'soft' => 'oklch(0.90 0.04 150)'],
                    ['id' => 'brown', 'name' => 'Brąz', 'base' => 'oklch(0.45 0.07 60)', 'soft' => 'oklch(0.90 0.04 65)'],
                    ['id' => 'rust', 'name' => 'Terakota', 'base' => 'oklch(0.52 0.12 32)', 'soft' => 'oklch(0.90 0.05 32)'],
                    ['id' => 'plum', 'name' => 'Śliwka', 'base' => 'oklch(0.40 0.08 320)', 'soft' => 'oklch(0.90 0.05 320)'],
                ],
            ],
        ],
    ],

    'coupon-code' => [
        'title' => 'Coupon code',
        'group' => 'Molekuły',
        'template' => 'components/molecules/coupon-code.html.twig',
        'doc' => 'Przerywany brązowy bilet z kodem rabatowym. Pojawia się w mailach i widgetach — dlatego kolory są literalne, nie z tokenów.',
        'variants' => [
            'z datą ważności' => ['code' => 'WROCMY-A8F2', 'note' => 'Ważny do 14 maja 2026, 23:59'],
            'bez daty' => ['code' => 'WITAJ10'],
        ],
    ],

    'block-library' => [
        'title' => 'Block library',
        'group' => 'Organizmy',
        'template' => 'components/organisms/block-library.html.twig',
        'doc' => 'Dwukolumnowa siatka źródeł bloków do przeciągania w edytorach. cursor: grab; obsługę HTML5 drag podłącza editor.ts.',
        'variants' => [
            'bloki maila' => ['blocks' => [
                ['icon' => 'layout', 'label' => 'Nagłówek'], ['icon' => 'list', 'label' => 'Tekst'],
                ['icon' => 'eye', 'label' => 'Obraz'], ['icon' => 'cart', 'label' => 'Produkty'],
                ['icon' => 'coupon', 'label' => 'Kupon'], ['icon' => 'play', 'label' => 'Przycisk CTA'],
                ['icon' => 'users', 'label' => 'Recenzje'], ['icon' => 'minus', 'label' => 'Separator'],
                ['icon' => 'mail', 'label' => 'Stopka'], ['icon' => 'code', 'label' => 'HTML własny'],
            ]],
            'bloki popupu' => ['blocks' => [
                ['icon' => 'list', 'label' => 'Nagłówek'], ['icon' => 'list', 'label' => 'Tekst'],
                ['icon' => 'mail', 'label' => 'Pole e-mail'], ['icon' => 'play', 'label' => 'Przycisk CTA'],
                ['icon' => 'coupon', 'label' => 'Kod kuponu'], ['icon' => 'check', 'label' => 'Checkbox zgody'],
            ]],
        ],
    ],

    'email-document' => [
        'title' => 'Email document',
        'group' => 'Organizmy',
        'template' => 'components/organisms/email-document.html.twig',
        'doc' => 'Kanwa maila 600px. Świadomie używa literalnych oklch(), nie tokenów — klienty pocztowe nie dziedziczą motywu aplikacji i mail nigdy nie może wyjść ciemny.',
        'variants' => [
            'powrót do koszyka' => ['sections' => [
                ['type' => 'hero', 'kicker' => 'AUREASHOP · ZIELONE HERBATY', 'title' => 'Hania, Twój koszyk czeka.', 'body' => 'Zostawiłaś u nas <strong>2 produkty</strong> warte <strong>88,90 zł</strong>. Wróć w 48h, dostajesz <strong>−10%</strong>.'],
                ['type' => 'coupon', 'code' => 'WROCMY-A8F2', 'note' => 'Ważny do 14 maja 2026, 23:59', 'cta' => 'Wróć do koszyka →'],
                ['type' => 'products', 'kicker' => 'W TWOIM KOSZYKU', 'items' => [
                    ['name' => 'Zielona herbata Sencha 100g', 'price' => '38,90 zł'],
                    ['name' => 'Filiżanka porcelanowa Nora', 'price' => '50,00 zł'],
                ]],
                ['type' => 'footer', 'body' => 'Dostajesz tę wiadomość, bo subskrybujesz aureashop.pl. <a href="#" style="color: oklch(0.42 0.06 150);">Zarządzaj subskrypcjami</a>'],
            ]],
        ],
    ],
    'cardiogram' => [
        'title' => 'Cardiogram',
        'group' => 'Organizmy',
        'template' => 'components/organisms/cardiogram.html.twig',
        'doc' => 'Jedyny <canvas> w systemie: zdarzenia na sekundę, bufor 60 kubełków, przerysowanie co 1 s. Kolory czyta z computed custom properties, więc wymaga przerysowania przy zmianie motywu, resize i po animacji kolumny gridu — obsługuje to controllers/cardiogram.ts. Patrz ARCHITECTURE.md par. 5.',
        'variants' => [
            'pulpit' => [
                'id' => 'cg-story',
                'title' => 'Pulsacja zdarzeń',
                'sub' => 'liczba zdarzeń / sekundę',
                'ranges' => [
                    ['label' => '5 min', 'active' => true],
                    ['label' => '1 godz.'],
                    ['label' => '24 godz.'],
                ],
                'legend' => [
                    ['label' => 'Średnia 5 min:', 'value' => '14.2 ev/s'],
                    ['label' => 'Pik:', 'value' => '28 ev/s · 12:42:18'],
                ],
            ],
            'bez przełącznika zakresu' => [
                'id' => 'cg-story-bare',
                'title' => 'Pulsacja zdarzeń',
                'sub' => 'liczba zdarzeń / sekundę',
            ],
        ],
    ],
];
