<?php

declare(strict_types=1);

namespace App\Twig;

use App\Panel\PanelContext;
use Twig\Extension\AbstractExtension;
use Twig\Extension\GlobalsInterface;

/**
 * Publishes the shell context as the Twig global `panel`.
 */
final class PanelExtension extends AbstractExtension implements GlobalsInterface
{
    public function __construct(private readonly PanelContext $panel)
    {
    }

    /** @return array<string, PanelContext> */
    public function getGlobals(): array
    {
        return ['panel' => $this->panel];
    }
}
