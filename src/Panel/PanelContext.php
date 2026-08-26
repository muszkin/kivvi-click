<?php

declare(strict_types=1);

namespace App\Panel;

use Symfony\Component\HttpFoundation\RequestStack;

/**
 * Everything the app shell needs, exposed to Twig as the global `panel`.
 *
 * Templates read it instead of reaching for services one by one, which keeps
 * layout/app.html.twig free of wiring and lets every page inherit the same chrome.
 */
final readonly class PanelContext
{
    public function __construct(
        private RequestStack $requestStack,
        private Navigation $navigation,
        private Workspace $workspace,
        private PanelPreferences $preferences,
        private PanelIdentity $identity,
    ) {
    }

    public function theme(): string
    {
        return $this->preferences->theme();
    }

    public function sidebarState(): string
    {
        return $this->preferences->sidebarState();
    }

    /**
     * @return list<array{label: string, items: list<array{label: string, icon: string, route: string, href: string, badge?: string}>}>
     */
    public function navGroups(): array
    {
        return $this->navigation->groups();
    }

    public function currentSection(): string
    {
        return $this->navigation->currentSection($this->route());
    }

    public function crumb(): string
    {
        return $this->navigation->crumb($this->route());
    }

    /**
     * @return array{name: string, meta: string, mark: string}
     */
    public function workspace(): array
    {
        return $this->workspace->card();
    }

    /**
     * @return array{name: string, email: string}
     */
    public function user(): array
    {
        return ['name' => $this->identity->name(), 'email' => $this->identity->email()];
    }

    private function route(): string
    {
        $request = $this->requestStack->getCurrentRequest();
        $route = $request?->attributes->get('_route');

        return \is_string($route) ? $route : '';
    }
}
