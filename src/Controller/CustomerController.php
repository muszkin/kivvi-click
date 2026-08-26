<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\AutomationCatalog;
use App\Panel\Content\CustomerDirectory;
use App\Panel\Format;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Customer index and the 360 profile.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class CustomerController extends AbstractController
{
    private const PAGES = 192;

    #[Route('/customers', name: 'customers', methods: ['GET'])]
    public function index(Request $request, CustomerDirectory $customers): Response
    {
        $now = new \DateTimeImmutable();
        $page = max(1, $request->query->getInt('page', 1));

        return $this->render('pages/customers.html.twig', [
            'subtitle' => sprintf(
                '%s zidentyfikowanych klientów · %s anonimowych sesji',
                Format::number($customers->total()),
                Format::number(7632),
            ),
            'segments' => $this->segments($customers->total()),
            'customers_rows' => array_map(
                static fn (array $customer): array => [
                    'id' => $customer['id'],
                    'name' => $customer['name'],
                    'initials' => $customer['initials'],
                    'email' => $customer['email'],
                    'segment' => $customer['segment'],
                    'orders' => (string) $customer['orders'],
                    'revenue' => Format::money($customer['revenue']),
                    'lastSeen' => Format::timeAgo($now->modify(sprintf('-%d minutes', $customer['lastSeenMinutes'])), $now),
                ],
                $customers->all(),
            ),
            'page' => $page,
            'pages' => self::PAGES,
        ]);
    }

    #[Route('/customers/{id}', name: 'customer_show', requirements: ['id' => 'c_\d+'], methods: ['GET'])]
    public function show(string $id, CustomerDirectory $customers, AutomationCatalog $automations): Response
    {
        $now = new \DateTimeImmutable();

        try {
            $customer = $customers->byId($id);
        } catch (\InvalidArgumentException) {
            throw $this->createNotFoundException(sprintf('Nie ma klienta „%s”.', $id));
        }

        $lastSeen = Format::timeAgo($now->modify(sprintf('-%d minutes', $customer['lastSeenMinutes'])), $now);

        return $this->render('pages/customer.html.twig', [
            'customer' => $customer + ['tags' => [
                ['label' => 'VIP', 'tone' => 'accent'],
                ['label' => 'subskrybent', 'tone' => 'brown'],
                ['label' => 'PL'],
            ]],
            'profile_sub' => sprintf('%s · klient od 14 stycznia 2024 · ostatnia aktywność %s', $customer['email'], $lastSeen),
            'facts' => [
                ['label' => 'Zamówienia', 'value' => (string) $customer['orders']],
                ['label' => 'Wartość życiowa', 'value' => Format::money($customer['revenue'] * 4)],
                ['label' => 'Średnia wartość koszyka', 'value' => Format::money($customer['revenue'])],
                ['label' => 'Pierwsze zdarzenie', 'value' => '14 sty 2024'],
                ['label' => 'Liczba sesji', 'value' => '28'],
                ['label' => 'Liczba zdarzeń', 'value' => '412'],
                ['label' => 'customer_id', 'value' => $customer['id'], 'small' => true],
            ],
            'automations' => $automations->activeForCustomer(),
            'tabs' => [
                ['label' => 'Aktywność', 'active' => true],
                ['label' => sprintf('Zamówienia (%d)', $customer['orders'])],
                ['label' => 'Wysłane maile (12)'],
                ['label' => 'Otrzymane kupony (3)'],
                ['label' => 'Atrybuty'],
            ],
            'scores' => [
                ['label' => 'Wskaźnik zaangażowania', 'value' => '82', 'unit' => '/100', 'delta' => '+12 vs miesiąc temu', 'dir' => 'up'],
                ['label' => 'Prawd. zakupu (30d)', 'value' => '68', 'unit' => '%', 'delta' => 'silny sygnał', 'dir' => 'up', 'deltaIcon' => 'spark'],
                ['label' => 'Open rate (90d)', 'value' => '74', 'unit' => '%', 'delta' => '+6,2pp', 'dir' => 'up'],
            ],
            'timeline' => $this->timeline($customer['email']),
        ]);
    }

    /**
     * @return list<array{label: string, icon?: string, count?: int, active: bool, action: string, payload: string}>
     */
    private function segments(int $total): array
    {
        return [
            ['label' => 'Wszyscy', 'icon' => 'users', 'count' => $total, 'active' => true, 'action' => 'set-segment', 'payload' => 'all'],
            ['label' => 'VIP', 'count' => 142, 'active' => false, 'action' => 'set-segment', 'payload' => 'vip'],
            ['label' => 'Nowi (7 dni)', 'count' => 412, 'active' => false, 'action' => 'set-segment', 'payload' => 'new'],
            ['label' => 'Porzucone koszyki', 'count' => 287, 'active' => false, 'action' => 'set-segment', 'payload' => 'abandoned'],
            ['label' => 'Subskrybenci', 'count' => 1829, 'active' => false, 'action' => 'set-segment', 'payload' => 'subscribers'],
            ['label' => 'Reaktywować', 'count' => 612, 'active' => false, 'action' => 'set-segment', 'payload' => 'winback'],
        ];
    }

    /**
     * @return list<array{time: string, title: string, detail: string, icon: string}>
     */
    private function timeline(string $email): array
    {
        return [
            ['time' => 'Dziś · 14:42', 'title' => 'Wyświetlenie produktu', 'detail' => '/produkt/zielona-herbata-sencha', 'icon' => 'eye'],
            ['time' => 'Dziś · 14:39', 'title' => 'Dodanie do koszyka', 'detail' => 'Zielona herbata Sencha 100g · 38,90 PLN', 'icon' => 'cart'],
            ['time' => 'Dziś · 14:38', 'title' => 'Wyświetlenie produktu', 'detail' => '/produkt/swieca-soja-figa', 'icon' => 'eye'],
            ['time' => 'Dziś · 14:32', 'title' => 'Wyszukiwanie', 'detail' => '„herbata zielona organiczna”', 'icon' => 'search'],
            ['time' => 'Dziś · 14:30', 'title' => 'Zalogowanie', 'detail' => $email, 'icon' => 'user'],
            ['time' => 'Wczoraj · 18:14', 'title' => 'Otrzymanie emaila', 'detail' => 'Newsletter „Tydzień smaków #18” — otwarty po 12 min', 'icon' => 'mail'],
            ['time' => 'Wczoraj · 17:09', 'title' => 'Porzucenie koszyka', 'detail' => '2 produkty · 88,90 PLN · uruchomiona reguła „Powrót do koszyka”', 'icon' => 'cart'],
            ['time' => 'Wczoraj · 16:42', 'title' => 'Wyświetlenie strony', 'detail' => '/kolekcja/zima-2025', 'icon' => 'eye'],
            ['time' => '14 mar · 11:08', 'title' => 'Zakup', 'detail' => '4 produkty · 412,00 PLN · zamówienie #AR-1287', 'icon' => 'money'],
        ];
    }
}
