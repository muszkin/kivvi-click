<?php

declare(strict_types=1);

namespace App\Panel;

use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\RequestStack;

/**
 * Keeps an uploaded import file until the wizard is finished.
 *
 * The file lands under var/import with a generated name — the original name is only
 * shown back to the user — so a crafted filename can never escape the directory.
 */
final readonly class ImportUploadStorage
{
    private const SESSION_NAME = 'import.file_name';
    private const SESSION_PATH = 'import.file_path';

    public function __construct(
        private RequestStack $requestStack,
        private string $uploadDirectory,
    ) {
    }

    private const DEFAULT_EXTENSION = 'csv';

    public function store(UploadedFile $file): string
    {
        $storedName = bin2hex(random_bytes(16)).'.'.$this->extensionOf($file);
        $file->move($this->uploadDirectory, $storedName);

        $session = $this->requestStack->getSession();
        $session->set(self::SESSION_NAME, $file->getClientOriginalName());
        $session->set(self::SESSION_PATH, $this->uploadDirectory.'/'.$storedName);

        return $storedName;
    }

    /**
     * The extension comes from the client-supplied name, so it is whitelisted rather
     * than trusted — the stored name must stay a plain "<random>.<ext>".
     */
    private function extensionOf(UploadedFile $file): string
    {
        $extension = strtolower($file->getClientOriginalExtension());

        return preg_match('/^[a-z0-9]{1,8}$/', $extension) ? $extension : self::DEFAULT_EXTENSION;
    }

    public function currentFileName(): ?string
    {
        $name = $this->requestStack->getSession()->get(self::SESSION_NAME);

        return \is_string($name) && '' !== $name ? $name : null;
    }

    public function currentFilePath(): ?string
    {
        $path = $this->requestStack->getSession()->get(self::SESSION_PATH);

        return \is_string($path) && is_file($path) ? $path : null;
    }
}
