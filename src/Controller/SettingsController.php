<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\SettingsCatalog;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Account configuration, one tab per URL.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class SettingsController extends AbstractController
{
    #[Route('/settings/{tab}', name: 'settings', defaults: ['tab' => SettingsCatalog::DEFAULT_TAB], methods: ['GET'])]
    public function index(string $tab, SettingsCatalog $settings): Response
    {
        if (!$settings->isKnownTab($tab)) {
            throw $this->createNotFoundException(sprintf('Nie ma zakładki ustawień „%s”.', $tab));
        }

        return $this->render('pages/settings.html.twig', [
            'tab' => $tab,
            'tabs' => $settings->tabs(),
            'tab_subtitle' => $settings->subtitle($tab),
            'settings' => $settings,
            'tracker_snippet' => SettingsCatalog::TRACKER_SNIPPET,
        ]);
    }
}
