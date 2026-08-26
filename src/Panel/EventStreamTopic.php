<?php

declare(strict_types=1);

namespace App\Panel;

/**
 * Mercure topic for the live event stream.
 *
 * One topic per account keeps a tenant's events off every other tenant's dashboard,
 * which is why the topic is built here and never assembled in a template.
 */
final readonly class EventStreamTopic
{
    private const PATTERN = '/accounts/%s/events';
    private const CURRENT_ACCOUNT = '1';

    public function forAccount(string $accountId): string
    {
        return sprintf(self::PATTERN, $accountId);
    }

    public function forCurrentAccount(): string
    {
        return $this->forAccount(self::CURRENT_ACCOUNT);
    }
}
