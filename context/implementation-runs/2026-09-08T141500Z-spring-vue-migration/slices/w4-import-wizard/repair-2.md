# Repair packet w4-import-wizard / repair-2 (post-integration feature-head RED: lint)

Fresh worktree /home/muszkin/work/kivvi-click-wt/w4-import-r2 · branch `migration/wave-4/import-wizard-repair2` · base e7a16e0898b1f23459c685ab241dca0e46b32c64 (feature HEAD = popups + import integrated). HIGH reasoning effort. New commit only.

## Finding
`npm run lint` on the merged head: `frontend/src/router/routes.ts:12:8 error 'EmptyPageView' is defined but never used` — after both wave-4 slices replaced their placeholder routes, the import is dead. Neither slice saw it alone.

## Required change
Remove the unused import; delete `frontend/src/views/EmptyPageView.vue` and any test that only exists for it IF nothing else references it (grep `frontend/src`, `frontend/test`, `config/storybook`-like registries in the new stack); otherwise keep the file and remove only the import. Gates: `npm run lint`, `npm run typecheck`, `npm run test -- --run`, `npm run test:integration -- --run`, `npm run format:check`, `npm run build` (logs under evidence/repair-2-gates/). Commit `chore: drop the unused EmptyPageView placeholder after wave-4 routes landed (#import-wizard)` (Conventional Commits, English, NO trailers). Append "## Repair-2" to worker-report.md with the new SHA and `git status --porcelain` empty; return it.
