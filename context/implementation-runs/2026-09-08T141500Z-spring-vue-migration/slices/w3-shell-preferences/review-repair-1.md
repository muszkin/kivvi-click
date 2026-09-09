PASS

Independent review of `60296f1..11d3cc4` (7 new files, 813 insertions, backend-only). Verified by reading
every changed line, decompiling the actual Spring Boot 4.1.1 jars on the classpath (not memory), running
`./mvnw -q verify` myself (JDK 25 toolchain), and injecting two live mutations (reverted, tree clean).

## Rubric

**1. SessionLockRegistry correctness — PASS.**
`acquire`/`release` (SessionLockRegistry.java:30-44) use `ConcurrentHashMap.compute`/`computeIfPresent`,
whose remapping function runs atomically per key, so a concurrent acquire and the release that would evict
the same key can never interleave — no lost-lock race, no leak (entry removed at line 44 the instant
`holders` hits 0; unbounded growth is impossible since every acquire is paired with a release). Verified
live: mutated `SessionRequestSerializationFilter.java`'s outer `finally` (line 130) to drop
`lockRegistry.release(sessionId)` → `SessionRequestSerializationFilterTest.successfulRequestReleasesTheLockAndEvictsTheRegistryEntry`
failed immediately (expected 0, was 1); reverted. `tryLock(lockTimeoutMillis, ...)` (line 109) with the
named `DEFAULT_LOCK_TIMEOUT_MS = 30_000L` (line 55) → 503 via `response.sendError` (line 115) with no lock
held (early `return` at line 118 skips the inner try, so `lock.unlock()` is never called when `tryLock`
never succeeded — correct, calling `unlock()` on a lock this thread doesn't hold would throw). Chain-throw
case: `chain.doFilter` (inside try at line ~124) is wrapped by `finally { lock.unlock(); }` (line 127)
nested inside `finally { lockRegistry.release(...) }` (line 130) — release always runs.
Reentrancy/async: filter extends `OncePerRequestFilter` (line 52); no `setDispatcherTypes()` call in
`SessionRequestSerializationConfig`, so Spring Boot's `AbstractFilterRegistrationBean.determineDispatcherTypes()`
auto-detects `OncePerRequestFilter` and registers **all** dispatcher types (confirmed by decompiling
`spring-boot-4.1.1.jar`: `EnumSet.allOf(DispatcherType.class)`), including ASYNC — but `OncePerRequestFilter`'s
own "already-filtered" guard means a FORWARD/INCLUDE never re-enters `doFilterInternal` (safe), and there is
no async/SSE/streaming controller in this app (`grep -rl "Callable<\|DeferredResult\|SseEmitter\|@Async" src/main/java` → empty; all 12 controllers are plain synchronous `@RestController`s) so the
lock-released-before-completion async gap this pattern would otherwise open never triggers today. **Low
note:** this is an implicit invariant, not an enforced one — nothing stops a future async controller from
silently breaking the lock's coverage; worth a comment or ArchUnit guard, not a blocker.

**2. Session id resolution — PASS.** `resolveSessionId` (SessionRequestSerializationFilter.java:134-145)
reads `request.getCookies()` directly for `sessionCookieName`, injected as
`${server.servlet.session.cookie.name:SESSION}` (SessionRequestSerializationConfig.java:41), matching
`application.yml`'s `server.servlet.session.cookie.name: SESSION`. A bogus/expired cookie value is used
verbatim as a lock key — harmless (creates a one-off entry, evicted after that single request). No cookie
→ `resolveSessionId` returns null → immediate unlocked pass-through (line 91-94): `/collect`, static
assets, and the first document GET are unaffected. Class Javadoc (lines 34-46) correctly documents why
`getRequestedSessionId()`/`getSession(false)` don't work here (Tomcat only knows `JSESSIONID`) — this
matches the worker report's claimed root-cause finding, and I independently confirm it's necessary: this
filter runs *before* Spring Session wraps the request, so there is no session-aware API to call yet.

**3. Filter order — PASS.** `FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10`
(SessionRequestSerializationConfig.java:27). Decompiled `SessionProperties$Servlet` from
`spring-boot-session-4.1.1.jar`: default `filterOrder` field initializer is the constant
`-2147483598` = `Integer.MIN_VALUE + 50`, confirming `SessionRepositoryFilter` really does default to
`HIGHEST_PRECEDENCE + 50` as the code comments claim. `SessionRequestSerializationIT#sessionLockFilterWrapsOutsideSessionRepositoryFilter`
asserts this live from the running context, not two hard-coded constants. Mutation-tested: changed
`FILTER_ORDER` to `HIGHEST_PRECEDENCE + 60` (wrapping *inside* `SessionRepositoryFilter` instead of
outside) → both `SessionRequestSerializationIT` tests failed (order assertion, and the concurrent-reload
test now saw the stale "light" theme instead of "dark") → reverted. No Spring Security on the classpath
(`pom.xml:106` explicitly calls out avoiding it), so there is no security-chain ordering to reconcile.

**4. `kivvi.session-lock.enabled` toggle — PASS.** Not set in `src/main/resources/application.yml` or any
other `application*.{yml,properties}` in the repo (`grep -rn session-lock` finds only the two Java sites and
the RED-proof IT). `@Value("${kivvi.session-lock.enabled:true}")` (SessionRequestSerializationConfig.java:38)
defaults `true` when absent — a typo'd property name in prod config silently keeps the lock on (fails safe),
it does not silently disable it, since a wrong key just falls back to the `:true` default rather than
matching nothing and defaulting `false`.

**5. Tests — PASS.** `SessionLockRegistryTest` (6/6): same-id reuse, different-id independence, serialization
(max observed concurrency = 1), full concurrency across ids, eviction on last release, survival while a
holder remains. `SessionRequestSerializationFilterTest` (5/5): pass-through, release+evict, disabled-toggle
bypass, 503-on-timeout, delay scoped to the theme POST only. `SessionRequestSerializationIT` (2/2, real
Testcontainers Postgres + HTTP): filter-order proof, GREEN concurrent-reload proof with a 400 ms
artificial delay on `POST /preferences/theme`. `SessionRequestSerializationRedProofIT` (1, `@Disabled`):
identical scenario with `kivvi.session-lock.enabled=false`, captured RED in `evidence/repair-1/red-proof.log`
(`expected: "dark", but was: "light"`) — genuinely proves the regression test detects the bug it claims to.
Both my own mutations (item 1, item 3) independently reproduce this GREEN→RED behavior on demand.

**6. Behavioural parity — PASS.** `git diff --stat` confirms zero frontend files touched — DOM-then-POST
order and `keepalive: true` untouched, as the worker report claims. Contract compares
(`evidence/repair-1/compare-login-contract.txt`, `compare-shell-navigation-contract.txt`): **0 regressions**
each. Performance (`evidence/repair-1/performance.txt`): all 4 budgets pass, `collectP95Millis` 7.18 ms /
500 ms (lock adds no cost to `/collect`, which never carries a session cookie). **20/20 load runs verified
by reading every log myself** (`evidence/repair-1/load-runs/run-1.txt` .. `run-20.txt` + `baseline.txt`):
every single run is exactly "2 failed (popups, import) / 12 passed", and `theme toggle survives a reload` /
`sidebar collapse survives a reload` show `✓` in all 21 files with zero exceptions — the round-3 flake (1/5
under load) is gone, not just reduced.

**7. Architecture/hygiene — PASS.** `ArchitectureTest` 5/5 (including "infrastructure only used from
application" — vacuously satisfied here since no `web`/`domain` class imports anything from
`infrastructure.session`; the servlet container invokes the filter directly, not through a compile-time
reference). Class Javadoc (SessionRequestSerializationFilter.java:13-50) explains PHP-session-lock parity in
detail. No magic numbers in production code (`DEFAULT_LOCK_TIMEOUT_MS`, `FILTER_ORDER`,
`TEST_DELAY_TARGET_METHOD/PATH` are all named). Commit `11d3cc4`: `fix: serialize requests per session like
PHP's session lock (#shell)` — single line, Conventional Commits, English, no trailers, no AI/co-author
mention. **Minor nit:** `SessionRequestSerializationFilter` is `public` (line 52) though only referenced
from its own package's `@Bean` methods — could be package-private; purely cosmetic, not gating.

**8. Report accuracy — PASS.** Re-ran `./mvnw -q verify` myself against this exact SHA: surefire
236/0/0/0 + failsafe 69/0/0/1 = **305 run, 0 failures, 0 errors, 1 skipped** (the RED-proof IT) — exact
match to the worker report's claim. Skipped test confirmed to be `SessionRequestSerializationRedProofIT`
via `target/failsafe-reports/`. All evidence-table paths in the worker report resolve correctly once
read relative to the worktree root (`evidence/repair-1/...`, `evidence/repair-1-gates/...`) — I initially
searched the wrong tree (the slice's `context/implementation-runs/.../evidence/` from an earlier verifier
round) and want to flag that for whoever reads this next: the repair's own evidence lives in the
**worktree**, not in `context/implementation-runs/.../slices/w3-shell-preferences/evidence/`.

## Verdict

PASS. The concurrency mechanism is correct (proven both analytically and by two independent live
mutations), the filter wraps outside `SessionRepositoryFilter` as claimed (bytecode-verified), the
session-id-resolution bug the packet anticipated was real and is correctly fixed, and the 20-run load
proof genuinely closes the round-3 race rather than just narrowing it. One low-severity, non-blocking
observation: the filter's dispatcher-type registration implicitly depends on this codebase never adding
Spring MVC async/streaming — true today, worth a guarding comment or ArchUnit rule if that ever changes.
