<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use PHPUnit\Framework\Attributes\DataProvider;
use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

/**
 * Every panel screen renders, carries the shell and shows its own headline.
 */
final class PanelPagesTest extends WebTestCase
{
    /**
     * @return iterable<string, array{0: string, 1: string, 2: string}>
     */
    public static function pages(): iterable
    {
        yield 'landing' => ['/pl', 'h1', 'Widzisz'];
        yield 'login' => ['/pl/login', '.auth-form h1', 'Wróć do Kivvi'];
        yield 'dashboard' => ['/pl/dashboard', '.page-title', 'Co dzieje się teraz'];
        yield 'events' => ['/pl/events', '.page-title', 'Strumień zdarzeń'];
        yield 'customers' => ['/pl/customers', '.page-title', 'Klienci'];
        yield 'customer profile' => ['/pl/customers/c_1001', '.profile-name', 'Kasia'];
        yield 'automations' => ['/pl/automations', '.page-title', 'Reguły i automatyzacje'];
        yield 'automation editor' => ['/pl/automations/a1', '.page-title', 'Powrót do porzuconego koszyka'];
        yield 'campaigns' => ['/pl/campaigns', '.page-title', 'Kampanie email'];
        yield 'email editor' => ['/pl/emails/k1', '.page-title', 'Powrót do koszyka — wariant A'];
        yield 'popups' => ['/pl/popups', '.page-title', 'Popupy i widgety'];
        yield 'popup editor' => ['/pl/popups/p1', '.page-title', 'Exit intent — 10% rabatu'];
        yield 'feeds' => ['/pl/feeds', '.page-title', 'Feedy produktów'];
        yield 'import' => ['/pl/import/1', '.wiz-title', 'Wgraj plik z klientami'];
        yield 'import mapping' => ['/pl/import/2', '.wiz-title', 'Mapowanie kolumn'];
        yield 'import rules' => ['/pl/import/3', '.wiz-title', 'Reguły deduplikacji i segmenty'];
        yield 'import run' => ['/pl/import/4', '.wiz-title', 'Podgląd i uruchomienie'];
        yield 'settings account' => ['/pl/settings/account', '.card-title', 'Dane konta'];
        yield 'settings sites' => ['/pl/settings/sites', '.card-title', 'Śledzone strony'];
        yield 'settings team' => ['/pl/settings/team', '.card-title', 'Członkowie zespołu'];
        yield 'settings providers' => ['/pl/settings/providers', '.card-title', 'Dostawcy wysyłki'];
        yield 'settings api' => ['/pl/settings/api', '.card-title', 'Klucze API'];
        yield 'settings notifications' => ['/pl/settings/notifications', '.card-title', 'Kanały powiadomień'];
        yield 'settings billing' => ['/pl/settings/billing', '.card-title', 'Wykorzystanie limitów'];
        yield 'settings gdpr' => ['/pl/settings/gdpr', '.card-title', 'Umowa powierzenia (DPA)'];
    }

    #[DataProvider('pages')]
    public function testPageRenders(string $url, string $selector, string $expectedText): void
    {
        $client = static::createClient();
        $client->request('GET', $url);

        self::assertResponseIsSuccessful();
        self::assertSelectorTextContains($selector, $expectedText);
    }

    public function testPanelPagesCarryTheShell(): void
    {
        $client = static::createClient();
        $crawler = $client->request('GET', '/pl/dashboard');

        self::assertResponseIsSuccessful();
        self::assertCount(1, $crawler->filter('.app[data-sidebar] .sidebar'));
        self::assertCount(1, $crawler->filter('.main .topbar'));
        self::assertCount(1, $crawler->filter('.main-scroll'));
        self::assertSame('page', $crawler->filter('.nav-item[data-route="dashboard"]')->attr('aria-current'));
    }

    public function testDetailRoutesKeepTheirSectionActive(): void
    {
        $client = static::createClient();
        $crawler = $client->request('GET', '/pl/customers/c_1001');

        self::assertResponseIsSuccessful();
        self::assertSame('true', $crawler->filter('.nav-item[data-route="customers"]')->attr('data-active'));
    }

    public function testEnglishLocaleTranslatesTheNavigation(): void
    {
        $client = static::createClient();
        $crawler = $client->request('GET', '/en/dashboard');

        self::assertResponseIsSuccessful();
        self::assertSelectorTextContains('.page-title', "What's happening now");
        self::assertStringContainsString('Event stream', $crawler->filter('.nav-item[data-route="events"]')->text());
    }

    public function testUnknownCustomerIsNotFound(): void
    {
        $client = self::browser();
        $client->request('GET', '/pl/customers/c_9999');

        self::assertResponseStatusCodeSame(404);
    }

    public function testUnknownSettingsTabIsNotFound(): void
    {
        $client = self::browser();
        $client->request('GET', '/pl/settings/nonexistent');

        self::assertResponseStatusCodeSame(404);
    }

    public function testUnsupportedLocaleIsNotFound(): void
    {
        $client = self::browser();
        $client->request('GET', '/de/dashboard');

        self::assertResponseStatusCodeSame(404);
    }

    private static function browser(): KernelBrowser
    {
        $client = static::createClient();
        $client->catchExceptions(true);

        return $client;
    }
}
