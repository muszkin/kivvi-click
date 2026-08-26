<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\ProductFeedCatalog;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Product feeds: catalogue sources, sync state and the matching diagnostic that
 * explains why an event did or did not get a price.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class ProductFeedController extends AbstractController
{
    #[Route('/feeds', name: 'feeds', methods: ['GET'])]
    public function index(ProductFeedCatalog $catalog): Response
    {
        return $this->render('pages/feeds.html.twig', [
            'kpis' => $catalog->kpis(),
            'sources' => $catalog->sources(),
            'feeds' => $catalog->feeds(),
            'coverage' => $catalog->matchingCoverage(),
            'fallback_rules' => $catalog->fallbackRules(),
            'mismatched' => $catalog->mismatchedCount(),
        ]);
    }
}
