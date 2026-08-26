<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\EventFeed;
use App\Panel\EventStreamTopic;
use App\Panel\Format;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * The raw event log: proof that tracking works, with type, site and range filters.
 *
 * Filters live in the query string so every filtered view is a link someone can send.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class EventStreamController extends AbstractController
{
    private const ROWS_PER_PAGE = 30;
    private const EVENTS_IN_WINDOW = 9360;

    #[Route('/events', name: 'events', methods: ['GET'])]
    public function index(Request $request, EventFeed $events, EventStreamTopic $topic): Response
    {
        return $this->render('pages/events.html.twig', [
            'type_filters' => $events->typeFilters((string) $request->query->get('type', 'all')),
            'site_filters' => $events->siteFilters((string) $request->query->get('site', 'all')),
            'ranges' => $events->ranges((string) $request->query->get('range', '1h')),
            'events' => $events->rows(self::ROWS_PER_PAGE),
            'total' => Format::number(self::EVENTS_IN_WINDOW),
            'shown' => self::ROWS_PER_PAGE,
            'mercure_topic' => $topic->forCurrentAccount(),
        ]);
    }
}
