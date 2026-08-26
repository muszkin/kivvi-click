<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\LandingContent;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Public marketing page and the shortcut into the panel demo.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class LandingController extends AbstractController
{
    #[Route('', name: 'home', methods: ['GET'])]
    public function index(LandingContent $content): Response
    {
        return $this->render('pages/landing.html.twig', [
            'features' => $content->features(),
            'steps' => $content->steps(),
            'plans' => $content->plans(),
            'trust_points' => $content->trustPoints(),
            'preview_tiles' => $content->previewTiles(),
            'preview_series' => $content->previewSeries(),
        ]);
    }

    #[Route('/demo', name: 'demo', methods: ['GET'])]
    public function demo(): RedirectResponse
    {
        return $this->redirectToRoute('dashboard');
    }
}
