<?php

declare(strict_types=1);

namespace App\Tracking;

/**
 * One visitor event as it arrives from the tracking script.
 *
 * The idempotency id is mandatory: k.js retries on a flaky connection, and a retried
 * purchase must not show up twice in the stream or count twice in revenue.
 */
final readonly class TrackedEvent
{
    private const SUPPORTED_TYPES = [
        'pageview', 'add_to_cart', 'remove_from_cart', 'purchase', 'login', 'signup',
        'search', 'wishlist', 'cart_abandon', 'email_open', 'email_click', 'popup_shown', 'coupon_used',
    ];

    public function __construct(
        public string $idempotencyId,
        public string $type,
        public \DateTimeImmutable $occurredAt,
        public string $detail,
        public string $customerId,
        public string $customerName,
        public string $siteName,
    ) {
    }

    /**
     * @param array<string, mixed> $payload
     *
     * @throws InvalidEventPayload
     */
    public static function fromPayload(array $payload): self
    {
        $idempotencyId = self::readString($payload, 'idempotency_id');
        if ('' === $idempotencyId) {
            throw new InvalidEventPayload('Pole „idempotency_id” jest wymagane.');
        }

        $type = self::readString($payload, 'type');
        if (!\in_array($type, self::SUPPORTED_TYPES, true)) {
            throw new InvalidEventPayload(sprintf('Nieznany typ zdarzenia „%s”.', $type));
        }

        return new self(
            idempotencyId: $idempotencyId,
            type: $type,
            occurredAt: self::readMoment($payload),
            detail: self::readString($payload, 'detail'),
            customerId: self::readString($payload, 'customer_id'),
            customerName: self::readString($payload, 'customer_name'),
            siteName: self::readString($payload, 'site'),
        );
    }

    /**
     * @param array<string, mixed> $payload
     */
    private static function readString(array $payload, string $key): string
    {
        $value = $payload[$key] ?? '';

        return \is_string($value) ? trim($value) : '';
    }

    /**
     * @param array<string, mixed> $payload
     *
     * @throws InvalidEventPayload
     */
    private static function readMoment(array $payload): \DateTimeImmutable
    {
        $occurredAt = self::readString($payload, 'occurred_at');
        if ('' === $occurredAt) {
            return new \DateTimeImmutable();
        }

        try {
            return new \DateTimeImmutable($occurredAt);
        } catch (\DateMalformedStringException $exception) {
            throw new InvalidEventPayload('Pole „occurred_at” nie jest poprawną datą.', previous: $exception);
        }
    }
}
