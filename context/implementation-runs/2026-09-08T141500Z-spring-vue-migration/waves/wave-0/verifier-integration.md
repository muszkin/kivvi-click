# Verifier packet — wave wave-0, dimension integration

Read-only verifier. You receive no worker claims. Inspect the assembled wave SHA from your own detached checkout and return one verdict per journey with evidence paths.

- Wave SHA:  · Journeys: login · Checkout: /home/muszkin/work/kivvi-click-wt/verify-wave-0-int (detached at the wave SHA; do not edit).
- Oracle: /home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue (manifest sha256 4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f). Deviation table: plan section "Accepted deviations" (DEV-1..DEV-12) and tools/migration-verify/deviations.json.
- Verifier contract (plan section "Verifier contract"): thresholds and commands for your dimension.
- Running stack for this SHA (if your dimension needs one): https://localhost:19101 (compose project kivvi-int, edge http 19100), compose project kivvi-int, started by the orchestrator; do not start or stop stacks yourself.
- Evidence output dir: /home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration/ (write only here) and the summary file /home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/waves/wave-0/integration.md.

Dimension duties (migration-waves.md): unit → run the new stack's unit tests and map every behaviour in behaviours.json for these journeys to a named test (FAIL if one has no test or any test is red); integration → component/store/router + Spring integration tests; architecture → ArchUnit + ESLint boundary rules against rules-translated.md (FAIL if a rule is violated or a rule has no equivalent and no deviation); contract → replay each journey with compare.mjs --dimension contract and compare HTTP recordings and db deltas with the oracle (FAIL on a missing/extra/changed call without a deviation id); visual → compare.mjs --dimension visual: screenshots (≤0.5% after masks), a11y tree and texts exact (FAIL otherwise); e2e → run the journeys' Playwright specs from tests/e2e (unchanged, E2E_BASE_URL to the stack) and performance.mjs budgets (FAIL if any step fails or a budget is exceeded without a deviation).

Return: dimension status PASS | FAIL | NOT_APPLICABLE bound to the wave SHA; per journey: verdict parity | accepted-deviation (with deviation_id) | regression; commands run with cwd and exit codes; evidence paths.

## Environment

Java 25: `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH` before any `./mvnw`. Node 26 on host; run `npm ci` inside `frontend/` (and `tools/migration-verify/` if needed) of YOUR checkout only. Never touch other checkouts, the main repo, or the oracle. Build outputs stay in your checkout.
