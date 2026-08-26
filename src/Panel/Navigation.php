<?php

declare(strict_types=1);

namespace App\Panel;

use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Contracts\Translation\TranslatorInterface;

/**
 * Sidebar navigation and breadcrumb labels.
 *
 * Detail routes highlight their index entry — the customer profile keeps "Klienci" lit —
 * so the sidebar never loses the sense of place while drilling down.
 */
final readonly class Navigation
{
    private const GROUPS = [
        'main' => [
            ['route' => 'dashboard', 'icon' => 'dashboard'],
            ['route' => 'events', 'icon' => 'activity', 'badge' => '·'],
            ['route' => 'customers', 'icon' => 'users'],
        ],
        'automate' => [
            ['route' => 'automations', 'icon' => 'bolt'],
            ['route' => 'campaigns', 'icon' => 'mail'],
            ['route' => 'popups', 'icon' => 'layout'],
        ],
        'data' => [
            ['route' => 'feeds', 'icon' => 'cart'],
            ['route' => 'import', 'icon' => 'upload'],
        ],
        'config' => [
            ['route' => 'settings', 'icon' => 'settings'],
        ],
    ];

    private const INDEX_OF_DETAIL = [
        'customer_show' => 'customers',
        'automation_edit' => 'automations',
        'automation_new' => 'automations',
        'email_edit' => 'campaigns',
        'email_new' => 'campaigns',
        'popup_edit' => 'popups',
        'popup_new' => 'popups',
    ];

    private const LABEL_KEY = [
        'dashboard' => 'nav.dashboard',
        'events' => 'nav.events',
        'customers' => 'nav.customers',
        'automations' => 'nav.automations',
        'campaigns' => 'nav.campaigns',
        'popups' => 'nav.popups',
        'feeds' => 'nav.feeds',
        'import' => 'nav.import',
        'settings' => 'nav.settings',
    ];

    public function __construct(
        private TranslatorInterface $translator,
        private UrlGeneratorInterface $urls,
    ) {
    }

    /**
     * @return list<array{label: string, items: list<array{label: string, icon: string, route: string, href: string, badge?: string}>}>
     */
    public function groups(): array
    {
        $groups = [];
        foreach (self::GROUPS as $section => $items) {
            $groups[] = [
                'label' => $this->translator->trans('nav.'.$section),
                'items' => array_map(fn (array $item): array => $this->item($item), $items),
            ];
        }

        return $groups;
    }

    public function currentSection(string $route): string
    {
        return self::INDEX_OF_DETAIL[$route] ?? $route;
    }

    public function crumb(string $route): string
    {
        $section = $this->currentSection($route);

        return isset(self::LABEL_KEY[$section]) ? $this->translator->trans(self::LABEL_KEY[$section]) : $section;
    }

    /**
     * @param array{route: string, icon: string, badge?: string} $item
     *
     * @return array{label: string, icon: string, route: string, href: string, badge?: string}
     */
    private function item(array $item): array
    {
        $entry = [
            'label' => $this->translator->trans(self::LABEL_KEY[$item['route']]),
            'icon' => $item['icon'],
            'route' => $item['route'],
            'href' => $this->urls->generate($item['route']),
        ];

        if (isset($item['badge'])) {
            $entry['badge'] = $item['badge'];
        }

        return $entry;
    }
}
