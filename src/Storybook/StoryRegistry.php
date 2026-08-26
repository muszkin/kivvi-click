<?php

declare(strict_types=1);

namespace App\Storybook;

/**
 * Loads the story catalogue from config/storybook.php.
 *
 * Stories are plain data — title, group, template and named variants — so adding a
 * documented state never means writing a one-off demo template.
 */
final class StoryRegistry
{
    /** @var array<string, array{title: string, group: string, template: string, doc?: string, variants: array<string, array<string, mixed>>}>|null */
    private ?array $stories = null;

    public function __construct(private readonly string $storyFile)
    {
    }

    /**
     * @return array<string, array{title: string, group: string, template: string, doc?: string, variants: array<string, array<string, mixed>>}>
     */
    public function all(): array
    {
        return $this->stories ??= require $this->storyFile;
    }

    /**
     * @return array{title: string, group: string, template: string, doc?: string, variants: array<string, array<string, mixed>>}|null
     */
    public function get(string $id): ?array
    {
        return $this->all()[$id] ?? null;
    }

    /**
     * @return array<string, list<string>>
     */
    public function groups(): array
    {
        $groups = [];
        foreach ($this->all() as $id => $story) {
            $groups[$story['group']][] = $id;
        }

        return $groups;
    }
}
