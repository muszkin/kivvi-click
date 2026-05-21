<?php

$finder = (new PhpCsFixer\Finder())
    ->in(__DIR__)
    ->exclude('var')
    ->notPath([
        'config/bundles.php',
        'config/reference.php',
    ])
;

return (new PhpCsFixer\Config())
    // Allow running on PHP 8.5 (newer than the fixer's officially supported runtime).
    ->setUnsupportedPhpVersionAllowed(true)
    ->setRules([
        '@Symfony' => true,
    ])
    ->setFinder($finder)
;
