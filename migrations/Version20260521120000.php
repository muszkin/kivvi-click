<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

/**
 * Infrastructure tables backed by Postgres:
 * - sessions: PdoSessionHandler store (LOGGED — sessions must survive crash recovery).
 * - cache_items: Doctrine DBAL cache adapter store, UNLOGGED for write throughput
 *   (cache is disposable, so crash-recovery truncation is acceptable).
 */
final class Version20260521120000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Create Postgres-backed sessions and UNLOGGED cache_items tables';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('CREATE TABLE IF NOT EXISTS sessions (
            sess_id VARCHAR(128) NOT NULL PRIMARY KEY,
            sess_data BYTEA NOT NULL,
            sess_lifetime INTEGER NOT NULL,
            sess_time INTEGER NOT NULL
        )');

        $this->addSql('CREATE UNLOGGED TABLE IF NOT EXISTS cache_items (
            item_id VARCHAR(255) NOT NULL,
            item_data BYTEA NOT NULL,
            item_lifetime INTEGER DEFAULT NULL,
            item_time INTEGER NOT NULL,
            PRIMARY KEY(item_id)
        )');
        // Ensure UNLOGGED even if the cache adapter already auto-created the table as LOGGED.
        $this->addSql('ALTER TABLE cache_items SET UNLOGGED');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP TABLE IF EXISTS cache_items');
        $this->addSql('DROP TABLE IF EXISTS sessions');
    }
}
