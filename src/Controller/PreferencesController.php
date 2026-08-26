<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\PanelPreferences;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Endpoints behind the theme and sidebar toggles.
 *
 * The browser flips the attribute immediately for a snappy toggle; this stores the
 * choice so the next full page load renders with it and nothing flashes.
 */
#[Route('/preferences')]
final class PreferencesController extends AbstractController
{
    #[Route('/theme', name: 'preferences_theme', methods: ['POST'])]
    public function theme(Request $request, PanelPreferences $preferences): JsonResponse
    {
        return new JsonResponse(['theme' => $preferences->storeTheme($this->readValue($request, 'theme'))]);
    }

    #[Route('/sidebar', name: 'preferences_sidebar', methods: ['POST'])]
    public function sidebar(Request $request, PanelPreferences $preferences): JsonResponse
    {
        return new JsonResponse(['state' => $preferences->storeSidebarState($this->readValue($request, 'state'))]);
    }

    private function readValue(Request $request, string $key): string
    {
        $payload = json_decode($request->getContent(), true);
        $value = \is_array($payload) ? ($payload[$key] ?? null) : null;

        return \is_string($value) ? $value : (string) $request->request->get($key, '');
    }
}
