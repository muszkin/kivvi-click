<?php

declare(strict_types=1);

namespace App;

use App\Message\Heartbeat;
use Symfony\Component\Scheduler\Attribute\AsSchedule;
use Symfony\Component\Scheduler\RecurringMessage;
use Symfony\Component\Scheduler\Schedule as SymfonySchedule;
use Symfony\Component\Scheduler\ScheduleProviderInterface;
use Symfony\Contracts\Cache\CacheInterface;

/**
 * Application schedule. Run with `php bin/console messenger:consume scheduler_default`.
 * State is persisted in the Postgres-backed cache, so missed runs survive restarts.
 */
#[AsSchedule]
final class Schedule implements ScheduleProviderInterface
{
    public function __construct(
        private CacheInterface $cache,
    ) {
    }

    public function getSchedule(): SymfonySchedule
    {
        return (new SymfonySchedule())
            ->stateful($this->cache) // ensure missed tasks are executed
            ->processOnlyLastMissedRun(true) // ensure only last missed task is run
            ->add(RecurringMessage::every('1 hour', new Heartbeat()));
    }
}
