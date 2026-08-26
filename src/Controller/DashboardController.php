<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\AutomationCatalog;
use App\Panel\Content\CustomerDirectory;
use App\Panel\Content\DashboardMetrics;
use App\Panel\Content\EventFeed;
use App\Panel\EventStreamTopic;
use App\Panel\Format;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * The operational overview: is traffic flowing, are automations earning.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class DashboardController extends AbstractController
{
    private const LIVE_ROWS = 10;
    private const RECENT_CUSTOMERS = 6;
    private const TOP_AUTOMATIONS = 4;

    #[Route('/dashboard', name: 'dashboard', methods: ['GET'])]
    public function index(
        DashboardMetrics $metrics,
        EventFeed $events,
        CustomerDirectory $customers,
        AutomationCatalog $automations,
        EventStreamTopic $topic,
    ): Response {
        $now = new \DateTimeImmutable();

        return $this->render('pages/dashboard.html.twig', [
            'kpis' => $metrics->kpis(),
            'legend' => $metrics->cardiogramLegend(),
            'events' => $events->rows(self::LIVE_ROWS, $now),
            'mercure_topic' => $topic->forCurrentAccount(),
            'recent_customers' => array_map(
                static fn (array $customer): array => $customer + [
                    'lastSeen' => Format::timeAgo($now->modify(sprintf('-%d minutes', $customer['lastSeenMinutes'])), $now),
                ],
                $customers->recent(self::RECENT_CUSTOMERS),
            ),
            'top_automations' => array_map(
                static fn (array $automation): array => [
                    'id' => $automation['id'],
                    'name' => $automation['name'],
                    'channels' => $automation['channels'],
                    'runs' => Format::number($automation['runs']),
                    'conversion' => Format::percent($automation['conversion']),
                    'revenue' => Format::money($automation['revenue']),
                ],
                $automations->topEarning(self::TOP_AUTOMATIONS),
            ),
        ]);
    }
}
