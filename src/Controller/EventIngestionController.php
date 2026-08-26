<?php

declare(strict_types=1);

namespace App\Controller;

use App\Tracking\EventIngestion;
use App\Tracking\InvalidEventPayload;
use App\Tracking\TrackedEvent;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * Ingestion endpoint for the tracking script.
 *
 * Answers 202 for a stored event and 200 for a duplicate, so a retrying tracker can
 * tell "we already have it" from "we took it" without treating either as an error.
 */
final class EventIngestionController extends AbstractController
{
    #[Route('/collect', name: 'collect', methods: ['POST'])]
    public function collect(Request $request, EventIngestion $ingestion): JsonResponse
    {
        $payload = json_decode($request->getContent(), true);
        if (!\is_array($payload)) {
            return new JsonResponse(['error' => 'Oczekiwano obiektu JSON.'], Response::HTTP_BAD_REQUEST);
        }

        try {
            $event = TrackedEvent::fromPayload($payload);
        } catch (InvalidEventPayload $exception) {
            return new JsonResponse(['error' => $exception->getMessage()], Response::HTTP_BAD_REQUEST);
        }

        $published = $ingestion->ingest($event);

        return new JsonResponse(
            ['status' => $published ? 'accepted' : 'duplicate'],
            $published ? Response::HTTP_ACCEPTED : Response::HTTP_OK,
        );
    }
}
