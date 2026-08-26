<?php

declare(strict_types=1);

namespace App\Tracking;

use App\Panel\Content\EventFeed;
use App\Panel\EventStreamTopic;
use App\Panel\Workspace;
use Psr\Cache\CacheItemPoolInterface;
use Symfony\Component\Mercure\HubInterface;
use Symfony\Component\Mercure\Update;
use Symfony\Contracts\Translation\TranslatorInterface;
use Twig\Environment;

/**
 * Accepts a tracked event, drops duplicates and pushes the rendered row to Mercure.
 *
 * What travels over Mercure is server-rendered HTML, not the event as JSON: the row's
 * markup then exists in exactly one place (event-row.html.twig) instead of drifting
 * between a Twig template and a TypeScript builder.
 */
final readonly class EventIngestion
{
    private const DEDUP_TTL_SECONDS = 86_400;
    private const DEDUP_PREFIX = 'event.seen.';

    public function __construct(
        private CacheItemPoolInterface $cache,
        private HubInterface $hub,
        private Environment $twig,
        private EventStreamTopic $topic,
        private Workspace $workspace,
        private TranslatorInterface $translator,
    ) {
    }

    /**
     * @return bool true when the event was published, false when it was a duplicate
     */
    public function ingest(TrackedEvent $event, string $accountId = '1'): bool
    {
        $seen = $this->cache->getItem(self::DEDUP_PREFIX.hash('xxh128', $event->idempotencyId));
        if ($seen->isHit()) {
            return false;
        }

        $seen->set(true);
        $seen->expiresAfter(self::DEDUP_TTL_SECONDS);
        $this->cache->save($seen);

        $this->hub->publish(new Update(
            $this->topic->forAccount($accountId),
            json_encode(['html' => $this->renderRow($event)], \JSON_THROW_ON_ERROR),
        ));

        return true;
    }

    private function renderRow(TrackedEvent $event): string
    {
        return trim($this->twig->render('components/molecules/event-row.html.twig', $this->rowParameters($event)));
    }

    /**
     * @return array<string, string|bool>
     */
    private function rowParameters(TrackedEvent $event): array
    {
        $type = $this->typeDescriptor($event->type);
        $site = $this->siteColor($event->siteName);

        $parameters = [
            'time' => $event->occurredAt->format('H:i:s'),
            'typeIcon' => $type['icon'],
            'tone' => $type['tone'],
            'type' => $this->translator->trans('events.'.$event->type),
            'detail' => $event->detail,
            'isNew' => true,
        ];

        if ('' !== $event->customerName) {
            $parameters['customerName'] = $event->customerName;
        }
        if ('' !== $event->customerId) {
            $parameters['customerId'] = $event->customerId;
        }
        if ('' !== $event->siteName) {
            $parameters['siteName'] = $event->siteName;
            $parameters['siteColor'] = $site;
        }

        return $parameters;
    }

    /**
     * @return array{icon: string, tone: string}
     */
    private function typeDescriptor(string $type): array
    {
        foreach (EventFeed::TYPES as $descriptor) {
            if ($descriptor['id'] === $type) {
                return ['icon' => $descriptor['icon'], 'tone' => $descriptor['tone']];
            }
        }

        return ['icon' => 'activity', 'tone' => ''];
    }

    private function siteColor(string $siteName): string
    {
        foreach ($this->workspace->sites() as $site) {
            if ($site['name'] === $siteName) {
                return $site['color'];
            }
        }

        return 'var(--fg-muted)';
    }
}
