<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

/**
 * Theme and sidebar choices survive the next full page load.
 */
final class PreferencesControllerTest extends WebTestCase
{
    public function testThemeChoiceIsRenderedIntoTheDocument(): void
    {
        $client = static::createClient();

        $client->jsonRequest('POST', '/preferences/theme', ['theme' => 'dark']);
        self::assertResponseIsSuccessful();
        self::assertJsonStringEqualsJsonString('{"theme":"dark"}', (string) $client->getResponse()->getContent());

        $crawler = $client->request('GET', '/pl/dashboard');
        self::assertSame('dark', $crawler->filter('html')->attr('data-theme'));
    }

    public function testUnknownThemeFallsBackToLight(): void
    {
        $client = static::createClient();

        $client->jsonRequest('POST', '/preferences/theme', ['theme' => 'neon']);

        self::assertJsonStringEqualsJsonString('{"theme":"light"}', (string) $client->getResponse()->getContent());
    }

    public function testCollapsedSidebarIsRenderedIntoTheShell(): void
    {
        $client = static::createClient();

        $client->jsonRequest('POST', '/preferences/sidebar', ['state' => 'collapsed']);
        self::assertResponseIsSuccessful();

        $crawler = $client->request('GET', '/pl/dashboard');
        self::assertSame('collapsed', $crawler->filter('.app')->attr('data-sidebar'));
    }
}
