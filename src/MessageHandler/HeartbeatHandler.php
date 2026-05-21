<?php

declare(strict_types=1);

namespace App\MessageHandler;

use App\Message\Heartbeat;
use Psr\Log\LoggerInterface;
use Symfony\Component\Messenger\Attribute\AsMessageHandler;

#[AsMessageHandler]
final class HeartbeatHandler
{
    public function __construct(private readonly LoggerInterface $logger)
    {
    }

    public function __invoke(Heartbeat $message): void
    {
        $this->logger->info('Scheduler heartbeat tick.');
    }
}
