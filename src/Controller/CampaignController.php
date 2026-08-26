<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\CampaignCatalog;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * E-mail campaign index and the WYSIWYG template editor.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class CampaignController extends AbstractController
{
    #[Route('/campaigns', name: 'campaigns', methods: ['GET'])]
    public function index(Request $request, CampaignCatalog $campaigns): Response
    {
        return $this->render('pages/campaigns.html.twig', [
            'kpis' => $campaigns->kpis(),
            'filters' => $campaigns->filters((string) $request->query->get('filter', 'all')),
            'columns' => $campaigns->columns(),
            'rows' => $campaigns->rows(),
        ]);
    }

    #[Route('/emails/new', name: 'email_new', methods: ['GET'])]
    public function create(CampaignCatalog $campaigns): Response
    {
        return $this->editor('new', $campaigns);
    }

    #[Route('/emails/{id}', name: 'email_edit', requirements: ['id' => 'k\d+'], methods: ['GET'])]
    public function edit(string $id, CampaignCatalog $campaigns): Response
    {
        return $this->editor($id, $campaigns);
    }

    private function editor(string $id, CampaignCatalog $campaigns): Response
    {
        return $this->render('pages/email-editor.html.twig', [
            'template' => $campaigns->template($id),
            'blocks' => $campaigns->blocks(),
            'variables' => $campaigns->variables(),
            'sections' => $campaigns->sections(),
            'selected_block' => $campaigns->selectedBlock(),
        ]);
    }
}
