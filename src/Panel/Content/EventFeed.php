<?php

declare(strict_types=1);

namespace App\Panel\Content;

use App\Panel\Workspace;
use Symfony\Contracts\Translation\TranslatorInterface;

/**
 * Rows for the live event stream.
 *
 * The tracker feeds real events through /collect; until a site starts sending, the
 * stream is filled from this sample so the page never renders an empty container.
 * Row shape matches components/molecules/event-row.html.twig exactly.
 */
final readonly class EventFeed
{
    public const TYPES = [
        ['id' => 'pageview', 'icon' => 'eye', 'tone' => ''],
        ['id' => 'add_to_cart', 'icon' => 'cart', 'tone' => 'accent'],
        ['id' => 'purchase', 'icon' => 'money', 'tone' => 'good'],
        ['id' => 'login', 'icon' => 'user', 'tone' => 'info'],
        ['id' => 'signup', 'icon' => 'user', 'tone' => 'brown'],
        ['id' => 'search', 'icon' => 'search', 'tone' => ''],
        ['id' => 'cart_abandon', 'icon' => 'cart', 'tone' => 'warn'],
        ['id' => 'wishlist', 'icon' => 'heart', 'tone' => ''],
    ];

    private const PRODUCT_PATHS = ['zielona-herbata-sencha', 'filizanka-porcelana', 'swieca-soja-figa', 'pasta-pomidorowa', 'plecak-canvas'];
    private const PRODUCT_NAMES = ['Zielona herbata Sencha 100g', 'Filiżanka porcelanowa Nora', 'Świeca sojowa „Figa”', 'Plecak canvas Olive', 'Pasta z pomidorów'];
    private const SEARCH_PHRASES = ['herbata', 'prezent', 'kubek', 'plecak', 'świeca', 'pasta', 'olej kokosowy'];
    private const ABANDON_DELAYS = ['Po 3:12 minut', 'Po 8:42 minut', 'Po 14:09 minut'];
    private const BASKET_SIZES = ['1 produkt', '2 produkty', '3 produkty', '4 produkty'];
    private const SECONDS_BETWEEN_EVENTS = 12;

    public function __construct(
        private CustomerDirectory $customers,
        private Workspace $workspace,
        private TranslatorInterface $translator,
    ) {
    }

    /**
     * @return list<array{time: string, typeIcon: string, tone: string, type: string, detail: string, customerName: string, customerId: string, siteName: string, siteColor: string}>
     */
    public function rows(int $count, ?\DateTimeImmutable $now = null): array
    {
        $now ??= new \DateTimeImmutable();
        $customers = $this->customers->all();

        $rows = [];
        for ($i = 0; $i < $count; ++$i) {
            $type = self::TYPES[($i * 3 + 1) % \count(self::TYPES)];
            $customer = $customers[($i * 5) % \count($customers)];
            $site = $this->workspace->site($i * 2);
            $moment = $now->modify(sprintf('-%d seconds', $i * self::SECONDS_BETWEEN_EVENTS));

            $rows[] = [
                'time' => $moment->format('H:i:s'),
                'typeIcon' => $type['icon'],
                'tone' => $type['tone'],
                'type' => $this->translator->trans('events.'.$type['id']),
                'detail' => $this->detail($type['id'], $i, $customer['email']),
                'customerName' => $customer['name'],
                'customerId' => $customer['id'],
                'siteName' => $site['name'],
                'siteColor' => $site['color'],
            ];
        }

        return $rows;
    }

    /**
     * @return list<array{label: string, icon?: string, dotColor?: string, active: bool, action: string, payload: string}>
     */
    public function typeFilters(string $active = 'all'): array
    {
        $filters = [[
            'label' => $this->translator->trans('common.all'),
            'active' => 'all' === $active,
            'action' => 'set-event-type',
            'payload' => 'all',
        ]];

        foreach (self::TYPES as $type) {
            $filters[] = [
                'label' => $this->translator->trans('events.'.$type['id']),
                'icon' => $type['icon'],
                'active' => $type['id'] === $active,
                'action' => 'set-event-type',
                'payload' => $type['id'],
            ];
        }

        return $filters;
    }

    /**
     * @return list<array{label: string, icon?: string, dotColor?: string, active: bool, action: string, payload: string}>
     */
    public function siteFilters(string $active = 'all'): array
    {
        $filters = [[
            'label' => $this->translator->trans('common.all'),
            'icon' => 'globe',
            'active' => 'all' === $active,
            'action' => 'set-event-site',
            'payload' => 'all',
        ]];

        foreach ($this->workspace->sites() as $site) {
            $filters[] = [
                'label' => $site['name'],
                'dotColor' => $site['color'],
                'active' => $site['id'] === $active,
                'action' => 'set-event-site',
                'payload' => $site['id'],
            ];
        }

        return $filters;
    }

    /**
     * @return list<array{value: string, label: string, active: bool}>
     */
    public function ranges(string $active = '1h'): array
    {
        $ranges = ['5m' => '5 min', '1h' => '1 godz.', '24h' => '24 godz.', '7d' => '7 dni'];

        $options = [];
        foreach ($ranges as $value => $label) {
            $options[] = ['value' => $value, 'label' => $label, 'active' => $value === $active];
        }

        return $options;
    }

    private function detail(string $type, int $seed, string $email): string
    {
        return match ($type) {
            'pageview' => '/produkt/'.self::PRODUCT_PATHS[$seed % \count(self::PRODUCT_PATHS)],
            'add_to_cart', 'wishlist' => self::PRODUCT_NAMES[$seed % \count(self::PRODUCT_NAMES)],
            'purchase' => number_format((($seed * 47) % 350) + 89, 2, ',', ' ').' PLN · '.self::BASKET_SIZES[$seed % \count(self::BASKET_SIZES)],
            'search' => '„'.self::SEARCH_PHRASES[$seed % \count(self::SEARCH_PHRASES)].'”',
            'cart_abandon' => self::ABANDON_DELAYS[$seed % \count(self::ABANDON_DELAYS)],
            default => $email,
        };
    }
}
