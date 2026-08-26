<?php

declare(strict_types=1);

namespace App\Tests\Tracking;

use App\Tracking\EventIngestion;
use App\Tracking\InvalidEventPayload;
use App\Tracking\TrackedEvent;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;
use Symfony\Component\Mercure\HubInterface;
use Symfony\Component\Mercure\MockHub;
use Symfony\Component\Mercure\Update;

/**
 * Ingestion publishes a server-rendered row and refuses to publish it twice.
 */
final class EventIngestionTest extends WebTestCase
{
    public function testPayloadWithoutIdempotencyIdIsRejected(): void
    {
        $this->expectException(InvalidEventPayload::class);

        TrackedEvent::fromPayload(['type' => 'purchase']);
    }

    public function testUnknownEventTypeIsRejected(): void
    {
        $this->expectException(InvalidEventPayload::class);

        TrackedEvent::fromPayload(['idempotency_id' => 'evt-1', 'type' => 'teleport']);
    }

    public function testEventIsPublishedAsRenderedRow(): void
    {
        $published = [];
        $ingestion = $this->ingestionWith($published);

        $accepted = $ingestion->ingest($this->event('evt-'.uniqid()));

        self::assertTrue($accepted);
        self::assertCount(1, $published);

        $payload = json_decode($published[0]->getData(), true, flags: \JSON_THROW_ON_ERROR);
        self::assertArrayHasKey('html', $payload);
        self::assertStringContainsString('event-row', $payload['html']);
        self::assertStringContainsString('Dodanie do koszyka', $payload['html']);
        self::assertStringContainsString('Zielona herbata Sencha 100g', $payload['html']);
        self::assertSame(['/accounts/1/events'], $published[0]->getTopics());
    }

    public function testDuplicateIdempotencyIdIsNotPublishedTwice(): void
    {
        $published = [];
        $ingestion = $this->ingestionWith($published);
        $event = $this->event('evt-duplicate-'.uniqid());

        self::assertTrue($ingestion->ingest($event));
        self::assertFalse($ingestion->ingest($event));
        self::assertCount(1, $published);
    }

    public function testEndpointAnswersAcceptedThenDuplicate(): void
    {
        $client = static::createClient();
        $published = [];
        static::getContainer()->set(HubInterface::class, new MockHub(
            'https://localhost/.well-known/mercure',
            static::getContainer()->get('mercure.hub.default.jwt.provider'),
            static function (Update $update) use (&$published): string {
                $published[] = $update;

                return 'id';
            },
        ));

        $payload = [
            'idempotency_id' => 'evt-http-'.uniqid(),
            'type' => 'purchase',
            'detail' => '412,00 PLN · 4 produkty',
            'customer_id' => 'c_1001',
            'customer_name' => 'Hania Kowalska',
            'site' => 'aureashop.pl',
        ];

        $client->jsonRequest('POST', '/collect', $payload);
        self::assertResponseStatusCodeSame(202);

        $client->jsonRequest('POST', '/collect', $payload);
        self::assertResponseStatusCodeSame(200);

        $client->jsonRequest('POST', '/collect', ['type' => 'purchase']);
        self::assertResponseStatusCodeSame(400);
    }

    /**
     * @param list<Update> $published
     */
    private function ingestionWith(array &$published): EventIngestion
    {
        self::bootKernel();
        $container = static::getContainer();

        $hub = new MockHub(
            'https://localhost/.well-known/mercure',
            $container->get('mercure.hub.default.jwt.provider'),
            static function (Update $update) use (&$published): string {
                $published[] = $update;

                return 'id';
            },
        );

        return new EventIngestion(
            $container->get('cache.app'),
            $hub,
            $container->get('twig'),
            $container->get(\App\Panel\EventStreamTopic::class),
            $container->get(\App\Panel\Workspace::class),
            $container->get('translator'),
        );
    }

    private function event(string $idempotencyId): TrackedEvent
    {
        return new TrackedEvent(
            idempotencyId: $idempotencyId,
            type: 'add_to_cart',
            occurredAt: new \DateTimeImmutable('2026-08-26 14:42:08'),
            detail: 'Zielona herbata Sencha 100g',
            customerId: 'c_1001',
            customerName: 'Hania Kowalska',
            siteName: 'aureashop.pl',
        );
    }
}
