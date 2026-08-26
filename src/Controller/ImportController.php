<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\Content\ImportWizard;
use App\Panel\ImportUploadStorage;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * The customer import wizard.
 *
 * Each step is its own URL so going back never loses work, and the upload endpoint
 * redirects into step 2 — the same path a drag-and-drop and a file picker take.
 */
final class ImportController extends AbstractController
{
    #[Route(
        '/{_locale}/import/{step}',
        name: 'import',
        requirements: ['_locale' => 'pl|en', 'step' => '[1-4]'],
        defaults: ['_locale' => 'pl', 'step' => ImportWizard::FIRST_STEP],
        methods: ['GET'],
    )]
    public function wizard(int $step, ImportWizard $wizard, ImportUploadStorage $uploads): Response
    {
        return $this->render('pages/import.html.twig', [
            'steps' => $wizard->steps(),
            'step' => $step,
            'file' => $wizard->file($uploads->currentFileName()),
            'columns' => $wizard->columns(),
            'targets' => $wizard->targets(),
            'detection' => $wizard->detection(),
            'validations' => $wizard->validations(),
            'dedup_strategies' => $wizard->dedupStrategies(),
            'summary' => $wizard->summary(),
            'preview' => $wizard->preview(),
            'row_count' => $wizard->rowCount(),
            'error_count' => $wizard->errorCount(),
            'recent' => $wizard->recentImports(),
        ]);
    }

    #[Route('/import/upload', name: 'import_upload', methods: ['POST'])]
    public function upload(Request $request, ImportUploadStorage $uploads): RedirectResponse
    {
        $file = $request->files->get('file');
        if (!$file instanceof UploadedFile) {
            throw $this->createNotFoundException('Brak pliku w żądaniu.');
        }

        try {
            $uploads->store($file);
        } catch (FileException $exception) {
            throw new \RuntimeException('Nie udało się zapisać wgranego pliku.', previous: $exception);
        }

        return $this->redirectToRoute('import', ['step' => 2]);
    }
}
