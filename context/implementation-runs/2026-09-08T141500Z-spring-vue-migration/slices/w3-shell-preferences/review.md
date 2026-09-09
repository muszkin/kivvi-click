# Independent review — w3-shell-preferences

PASS

Diff reviewed: `git diff c74e3b8e9ed6763204fa193bf66b891d35ef7fe4...60296f16250c6ebd25f7b4435c795268772fb6be`
(single commit `60296f1`, 3 files, 126 insertions, 0 deletions).

## Findings

### MEDIUM — wrong old-stack file cited in two permanent comments
`frontend/src/stores/shell.ts:78` and `frontend/test/unit/useIntents.spec.ts:11` both say the
DOM-update-before-fetch, never-awaited pattern matches "the old stack's own
`assets/controllers/shell.ts`". That file (`assets/controllers/shell.ts`) only wires a
`transitionend` listener for canvas resize — it has no `fetch`, no preferences, nothing relevant.
The actual old-stack behaviour being described (`void fetch("/preferences/theme"|"/preferences/sidebar", …)`
fired without `await`, DOM attribute set first) lives in `assets/app.ts:41-61`. Verified by reading
both files. The underlying technical claim is correct once checked against the right file, so this
does not change the verdict, but the citation should be fixed to `assets/app.ts` so a future reader
doesn't go looking for POST logic in a file that never had any.
Fix: `s/assets\/controllers\/shell.ts/assets\/app.ts/` in both locations.

No other issues found.

## Rubric

1. **Scope — PASS.** `git diff --stat` shows only `frontend/src/stores/shell.ts` (+16),
   `frontend/test/integration/shellStore.spec.ts` (+29), `frontend/test/unit/useIntents.spec.ts`
   (new, +81). No CSS, no `compare.mjs`, no other product file.

2. **Correctness — PASS.** `keepalive: true` added to both fetches (`shell.ts:96`, `:106`); the
   diff hunk shows only that one added line per call, method/headers/body untouched. DOM update is
   still synchronous before the POST: `this.theme = ...; document.documentElement.dataset.theme = ...;`
   (lines 90-91) then `await fetch(...)` (92); same order in `setSidebar` (101-102). Grepped
   `fetch(` across `frontend/src` (13 hits) and `method:\s*["'](POST|PUT|DELETE|PATCH)["']` (2
   hits) — the only two POST/PUT/DELETE/PATCH fetches in the whole frontend are these two
   preference calls; no logout, pause-stream (DOM-attribute only, no fetch) or copy-* (clipboard
   API, no fetch) POST exists that could race a navigation without keepalive.

3. **Tests — PASS.** `useIntents.spec.ts` mounts a harness with `@vue/test-utils` `attachTo:
   document.body` and calls `useIntents()`, then `.trigger("click")` on real `[data-action]`
   buttons — this is a genuine DOM click that bubbles to the `document.addEventListener("click",
   …)` delegate `useIntents` installs, not a direct store call. `shellStore.spec.ts` asserts
   `expect.objectContaining({ keepalive: true })` for both preference POSTs. Mutation-tested
   myself: removed both `keepalive: true` lines, ran
   `npx vitest run test/unit/useIntents.spec.ts test/integration/shellStore.spec.ts` → 4 failed
   (exactly the 4 new/changed assertions), 3 pre-existing cases stayed green; restored the file,
   re-ran → 7/7 pass, `git status --porcelain` clean again.

4. **Evidence — PASS.** Inspected all 10 `evidence/load-runs/run-{1..10}.txt` plus
   `nav-baseline.txt`: every run is 12 passed / 2 failed, and per-test breakdown confirms the 2
   failures are always `sidebar entry "popups"` and `sidebar entry "import"` (pre-existing,
   accepted — `EmptyPageView` placeholders), with `theme toggle survives a reload` and `sidebar
   collapse survives a reload` green in all 10+baseline runs. `compare-login-contract.txt` and
   `compare-shell-navigation-contract.txt`: `0 regression(s)`. `compare-login-visual.txt`:
   `0 regression(s)`. `evidence/e2e-public.txt`: `public.spec.ts` 5/5 passed.

5. **Commit hygiene — PASS.** One commit, message
   `fix: keep preference POSTs alive across an immediate reload (#shell)` — Conventional Commits,
   English, single line, no trailers, no AI/co-author mention.

6. **Report accuracy — PASS.** Re-ran in the worktree's `frontend/`: `npm ci` (259 packages, 0
   vuln) → `npm run test -- --run` (128/128) → `npm run test:integration -- --run` (91/91) →
   `npm run lint` (0 errors, 6 pre-existing `vue/multiline-html-element-content-newline` warnings
   in unrelated files) → `npm run typecheck` (clean) → `npm run format:check` (clean). All figures
   match `evidence/gates/*.log` and the worker report exactly.

`git status --porcelain` empty in the worktree at end of review; no throwaway files left.
