<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

final class HomeControllerTest extends WebTestCase
{
    public function testHomepageDefaultsToPolish(): void
    {
        $client = static::createClient();
        $client->request('GET', '/');

        self::assertResponseIsSuccessful();
        self::assertSelectorTextContains('.brand', 'Kivvi-click');
        self::assertSelectorTextContains('.tagline', 'Widzisz');
    }

    public function testEnglishLocaleIsAvailable(): void
    {
        $client = static::createClient();
        $client->request('GET', '/en');

        self::assertResponseIsSuccessful();
        self::assertSelectorTextContains('.tagline', 'See. Decide. Act');
    }

    public function testUnsupportedLocaleIsNotFound(): void
    {
        $client = static::createClient();
        $client->request('GET', '/de');

        self::assertResponseStatusCodeSame(404);
    }
}
