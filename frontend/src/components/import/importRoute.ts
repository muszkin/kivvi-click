/**
 * Builds a step URL for the import wizard, dropping the step segment for step 1 — mirrors
 * Symfony's own URL generator, which omits a route parameter equal to its route's declared
 * default (`ImportController`'s `defaults: ['step' => ImportWizard::FIRST_STEP]`): the oracle's
 * own a11y snapshots show every "back to step 1"/"change file" link targeting `/pl/import`, never
 * `/pl/import/1` (see journeys/import-wizard/steps/2/a11y.json's "Zmień"/"← Wstecz" links).
 *
 * Every step-to-step transition in this wizard is a real document navigation (see the oracle's own
 * http.jsonl for steps 5-8: each is `"kind":"document"`), so this only ever feeds a plain `<a href>`
 * or `window.location.href` — never `router.push`.
 */
export function importStepPath(locale: string, step: 1 | 2 | 3 | 4): string {
    return step === 1 ? `/${locale}/import` : `/${locale}/import/${step}`;
}
