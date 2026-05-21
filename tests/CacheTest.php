<?php

declare(strict_types=1);

namespace App\Tests;

use Psr\Cache\CacheItemPoolInterface;
use Symfony\Bundle\FrameworkBundle\Test\KernelTestCase;

final class CacheTest extends KernelTestCase
{
    public function testApplicationCacheRoundTripsThroughPostgres(): void
    {
        self::bootKernel();

        /** @var CacheItemPoolInterface $cache */
        $cache = self::getContainer()->get('cache.app');

        $item = $cache->getItem('smoke_test');
        $item->set('ok');
        $cache->save($item);

        self::assertSame('ok', $cache->getItem('smoke_test')->get());

        $cache->deleteItem('smoke_test');
    }
}
