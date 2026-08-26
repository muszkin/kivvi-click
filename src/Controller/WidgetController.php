<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\WidgetCatalog;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * On-site widgets: the index with a live preview, and the widget composer.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class WidgetController extends AbstractController
{
    private const DEVICE_DESKTOP = 'desktop';
    private const DEVICE_MOBILE = 'mobile';

    #[Route('/popups', name: 'popups', methods: ['GET'])]
    public function index(Request $request, WidgetCatalog $widgets): Response
    {
        $selectedId = (string) $request->query->get('preview', $widgets->firstId());

        return $this->render('pages/popups.html.twig', [
            'popups' => $widgets->cards(),
            'selected' => $widgets->selected($selectedId),
        ]);
    }

    #[Route('/popups/new', name: 'popup_new', methods: ['GET'])]
    public function create(Request $request, WidgetCatalog $widgets): Response
    {
        return $this->editor('new', $request, $widgets);
    }

    #[Route('/popups/{id}', name: 'popup_edit', requirements: ['id' => 'p\d+'], methods: ['GET'])]
    public function edit(string $id, Request $request, WidgetCatalog $widgets): Response
    {
        return $this->editor($id, $request, $widgets);
    }

    private function editor(string $id, Request $request, WidgetCatalog $widgets): Response
    {
        $widget = $widgets->selected($id);
        $type = (string) $request->query->get('type', $widget['type']);
        $device = self::DEVICE_MOBILE === $request->query->get('device') ? self::DEVICE_MOBILE : self::DEVICE_DESKTOP;

        return $this->render('pages/popup-editor.html.twig', [
            'widget' => [
                'id' => $widget['id'],
                'name' => $widget['name'],
                'meta' => $widget['meta'],
                'type' => $type,
                'content' => $widgets->content($type),
            ],
            'device' => $device,
            'types' => $widgets->types(),
            'blocks' => $widgets->blocks(),
            'variables' => $widgets->variables(),
            'triggers' => $widgets->triggers(),
            'audience' => $widgets->audience(),
            'accent_colors' => $widgets->accentColors(),
            'viewport' => self::DEVICE_DESKTOP === $device ? '1440 × 900' : '390 × 844',
        ]);
    }
}
