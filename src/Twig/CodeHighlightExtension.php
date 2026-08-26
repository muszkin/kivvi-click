<?php

declare(strict_types=1);

namespace App\Twig;

use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;

/**
 * Server-side syntax highlighting for the code-block atom.
 *
 * Highlighting happens here rather than in the browser so a snippet is one
 * escaped string of markup, identical in the panel, in the storybook and in
 * any snapshot test.
 */
final class CodeHighlightExtension extends AbstractExtension
{
    private const COMMENT = '/(\/\/[^\n]*)/';
    private const KEYWORD = '/(&lt;\/?[a-z]+|\bsrc\b|\basync\b|data-[a-z-]+|\bwindow\b|\bconst\b|\bfunction\b)/';
    private const STRING = '/(&quot;[^&]*?&quot;|\'[^\']*\')/';

    public function getFilters(): array
    {
        return [new TwigFilter('kivvi_highlight', $this->highlight(...), ['is_safe' => ['html']])];
    }

    public function highlight(string $code): string
    {
        $escaped = htmlspecialchars($code, \ENT_QUOTES | \ENT_SUBSTITUTE, 'UTF-8');

        $highlighted = preg_replace(self::STRING, '<span class="s">$1</span>', $escaped) ?? $escaped;
        $highlighted = preg_replace(self::KEYWORD, '<span class="k">$1</span>', $highlighted) ?? $highlighted;

        return preg_replace(self::COMMENT, '<span class="c">$1</span>', $highlighted) ?? $highlighted;
    }
}
