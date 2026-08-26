<?php

declare(strict_types=1);

namespace App\Controller;

use App\Storybook\StoryRegistry;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Storybook — renders every component from its real Twig template, so the
 * catalogue can never drift from production markup.
 *
 * Development tooling: the routes are only matched outside production.
 */
#[Route('/_storybook', name: 'storybook_', condition: "env('APP_ENV') in ['dev', 'test']")]
final class StorybookController extends AbstractController
{
    public function __construct(private readonly StoryRegistry $stories)
    {
    }

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(): Response
    {
        return $this->render('storybook/index.html.twig', [
            'stories' => $this->stories->all(),
            'groups' => $this->stories->groups(),
        ]);
    }

    #[Route('/{id}', name: 'story', methods: ['GET'])]
    public function story(string $id): Response
    {
        $story = $this->stories->get($id) ?? throw $this->createNotFoundException(sprintf('No story "%s".', $id));

        return $this->render('storybook/story.html.twig', [
            'id' => $id,
            'story' => $story,
            'groups' => $this->stories->groups(),
        ]);
    }

    /**
     * Isolated frame for one variant. The storybook iframes this so a
     * component is always rendered without the storybook's own chrome.
     */
    #[Route('/{id}/{variant}/frame', name: 'frame', methods: ['GET'])]
    public function frame(string $id, string $variant): Response
    {
        $story = $this->stories->get($id) ?? throw $this->createNotFoundException(sprintf('No story "%s".', $id));
        $params = $story['variants'][$variant] ?? throw $this->createNotFoundException(sprintf('No variant "%s".', $variant));

        return $this->render('storybook/frame.html.twig', [
            'template' => $story['template'],
            'params' => $params,
        ]);
    }
}
