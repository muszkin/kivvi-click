<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;

/**
 * The login form: validation, and the identity it puts into the shell.
 */
final class SecurityControllerTest extends WebTestCase
{
    public function testSuccessfulSignInLandsOnTheDashboard(): void
    {
        $client = static::createClient();
        $client->request('POST', '/pl/login', ['_username' => 'anna@aureashop.pl', '_password' => 'secret']);

        self::assertResponseRedirects('/pl/dashboard');

        $crawler = $client->followRedirect();
        self::assertStringContainsString('anna@aureashop.pl', $crawler->filter('.sb-foot')->text());
    }

    public function testEmptyEmailIsRejected(): void
    {
        $client = static::createClient();
        $client->request('POST', '/pl/login', ['_username' => '', '_password' => '']);

        self::assertResponseIsSuccessful();
        self::assertSelectorTextContains('.callout, .feed-card__err', 'Podaj adres e-mail.');
    }

    public function testMalformedEmailIsRejected(): void
    {
        $client = static::createClient();
        $client->request('POST', '/pl/login', ['_username' => 'not-an-email']);

        self::assertResponseIsSuccessful();
        self::assertSelectorTextContains('.callout, .feed-card__err', 'poprawny adres e-mail');
    }

    public function testSignOutRestoresTheDefaultIdentity(): void
    {
        $client = static::createClient();
        $client->request('POST', '/pl/login', ['_username' => 'anna@aureashop.pl']);
        $client->request('POST', '/pl/logout');

        self::assertResponseRedirects('/pl/login');

        $crawler = $client->request('GET', '/pl/dashboard');
        self::assertStringContainsString('maciej@aureashop.pl', $crawler->filter('.sb-foot')->text());
    }
}
