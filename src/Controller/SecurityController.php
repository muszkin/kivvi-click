<?php

declare(strict_types=1);

namespace App\Controller;

use App\Panel\PanelIdentity;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

/**
 * The login screen.
 *
 * The form establishes the panel identity used by the shell. Credential
 * verification arrives with the User entity and the security firewall; the form
 * therefore validates its input and refuses empty or malformed addresses rather
 * than pretending to check a password it has nowhere to check against.
 */
#[Route('/{_locale}', requirements: ['_locale' => 'pl|en'], defaults: ['_locale' => 'pl'])]
final class SecurityController extends AbstractController
{
    #[Route('/login', name: 'login', methods: ['GET', 'POST'])]
    public function login(Request $request, PanelIdentity $identity): Response
    {
        $email = (string) $request->request->get('_username', '');
        $error = null;

        if ($request->isMethod('POST')) {
            $error = $this->validate($email);

            if (null === $error) {
                $identity->signIn($email);

                return $this->redirectToRoute('dashboard');
            }
        }

        return $this->render('pages/login.html.twig', [
            'last_username' => $email,
            'error_message' => $error,
        ]);
    }

    #[Route('/logout', name: 'logout', methods: ['POST'])]
    public function logout(PanelIdentity $identity): Response
    {
        $identity->signOut();

        return $this->redirectToRoute('login');
    }

    private function validate(string $email): ?string
    {
        if ('' === trim($email)) {
            return 'Podaj adres e-mail.';
        }

        if (!filter_var($email, \FILTER_VALIDATE_EMAIL)) {
            return 'To nie wygląda na poprawny adres e-mail.';
        }

        return null;
    }
}
