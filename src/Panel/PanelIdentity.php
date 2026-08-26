<?php

declare(strict_types=1);

namespace App\Panel;

use Symfony\Component\HttpFoundation\RequestStack;

/**
 * The person the panel is rendered for: name and e-mail shown in the sidebar footer.
 *
 * The identity is established by the login form and kept in the Postgres-backed session.
 * It is deliberately not a security identity — the User entity and the security firewall
 * arrive with the domain model; until then nothing in the panel makes authorisation
 * decisions based on this value.
 */
final readonly class PanelIdentity
{
    private const SESSION_KEY = 'panel.identity';
    private const DEFAULT_EMAIL = 'maciej@aureashop.pl';
    private const DEFAULT_NAME = 'Maciej Kowalczyk';

    public function __construct(private RequestStack $requestStack)
    {
    }

    public function signIn(string $email): void
    {
        $this->requestStack->getSession()->set(self::SESSION_KEY, $email);
    }

    public function signOut(): void
    {
        $this->requestStack->getSession()->remove(self::SESSION_KEY);
    }

    public function email(): string
    {
        $email = $this->requestStack->getSession()->get(self::SESSION_KEY, self::DEFAULT_EMAIL);

        return \is_string($email) && '' !== $email ? $email : self::DEFAULT_EMAIL;
    }

    public function name(): string
    {
        $email = $this->email();
        if (self::DEFAULT_EMAIL === $email) {
            return self::DEFAULT_NAME;
        }

        $local = substr($email, 0, (int) strpos($email, '@'));

        return implode(' ', array_map(
            static fn (string $part): string => ucfirst($part),
            preg_split('/[._-]+/', $local) ?: [$local],
        ));
    }
}
