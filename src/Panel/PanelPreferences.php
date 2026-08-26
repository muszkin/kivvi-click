<?php

declare(strict_types=1);

namespace App\Panel;

use Symfony\Component\HttpFoundation\RequestStack;

/**
 * View preferences that survive a full page load: colour theme and sidebar state.
 *
 * They are rendered server-side into <html data-theme> and .app[data-sidebar] so the
 * page never flashes the wrong theme; app.ts POSTs the new value after toggling.
 */
final readonly class PanelPreferences
{
    public const THEME_LIGHT = 'light';
    public const THEME_DARK = 'dark';
    public const SIDEBAR_EXPANDED = 'expanded';
    public const SIDEBAR_COLLAPSED = 'collapsed';

    private const SESSION_THEME = 'panel.theme';
    private const SESSION_SIDEBAR = 'panel.sidebar';

    public function __construct(private RequestStack $requestStack)
    {
    }

    public function theme(): string
    {
        return $this->read(self::SESSION_THEME, self::THEME_LIGHT);
    }

    public function sidebarState(): string
    {
        return $this->read(self::SESSION_SIDEBAR, self::SIDEBAR_EXPANDED);
    }

    public function storeTheme(string $theme): string
    {
        $value = self::THEME_DARK === $theme ? self::THEME_DARK : self::THEME_LIGHT;
        $this->write(self::SESSION_THEME, $value);

        return $value;
    }

    public function storeSidebarState(string $state): string
    {
        $value = self::SIDEBAR_COLLAPSED === $state ? self::SIDEBAR_COLLAPSED : self::SIDEBAR_EXPANDED;
        $this->write(self::SESSION_SIDEBAR, $value);

        return $value;
    }

    private function read(string $key, string $fallback): string
    {
        $session = $this->requestStack->getSession();
        $value = $session->get($key, $fallback);

        return \is_string($value) ? $value : $fallback;
    }

    private function write(string $key, string $value): void
    {
        $this->requestStack->getSession()->set($key, $value);
    }
}
