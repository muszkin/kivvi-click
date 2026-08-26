<?php

declare(strict_types=1);

namespace App\Tracking;

/**
 * Raised when the tracking script sends something the ingestion cannot accept.
 */
final class InvalidEventPayload extends \InvalidArgumentException
{
}
