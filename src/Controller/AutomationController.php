<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\AutomationCatalog;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Automation index and the rule editor.
 *
 * The editor's list/diagram switch is a URL parameter, not client state: the same
 * rule model rendered two ways, and both views are linkable.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class AutomationController extends AbstractController
{
    private const VIEW_LIST = 'list';
    private const VIEW_FLOW = 'flow';

    #[Route('/automations', name: 'automations', methods: ['GET'])]
    public function index(Request $request, AutomationCatalog $automations): Response
    {
        return $this->render('pages/automations.html.twig', [
            'filters' => $automations->statusFilters((string) $request->query->get('status', 'all')),
            'automations' => $automations->cards(),
        ]);
    }

    #[Route('/automations/new', name: 'automation_new', methods: ['GET'])]
    public function create(Request $request, AutomationCatalog $automations): Response
    {
        return $this->editor('new', $request, $automations);
    }

    #[Route('/automations/{id}', name: 'automation_edit', requirements: ['id' => 'a\d+'], methods: ['GET'])]
    public function edit(string $id, Request $request, AutomationCatalog $automations): Response
    {
        return $this->editor($id, $request, $automations);
    }

    private function editor(string $id, Request $request, AutomationCatalog $automations): Response
    {
        $view = self::VIEW_FLOW === $request->query->get('view') ? self::VIEW_FLOW : self::VIEW_LIST;

        return $this->render('pages/automation-editor.html.twig', [
            'automation' => $automations->header($id),
            'view' => $view,
            'tabs' => $automations->editorTabs(),
            'steps' => $automations->pipelineSteps(),
            'nodes' => $automations->flowNodes(),
            'edges' => $automations->flowEdges(),
            'simulation' => $automations->simulation(),
        ]);
    }
}
