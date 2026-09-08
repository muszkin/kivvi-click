PASS

Reviewed `git diff 22d7fcb..7ba8f08` (13 files, +616/-22) in
`/home/muszkin/work/kivvi-click-wt/w2-event-stream-r2`. All ten rubric items checked with direct
evidence; two independent mutation checks and full local re-runs of the reported gates.

## Findings

1. **Scope — PASS.** All 13 changed files are under `backend/`, `frontend/`, `tools/migration-verify/`.
   No old-stack path, oracle dir, or other-journey file touched.

2. **Zero-change rule — PASS.** `EventStreamTopic.forCurrentAccount()` returns the same
   `"/accounts/%s/events".formatted("1")` = `/accounts/1/events` previously inlined as
   `MERCURE_TOPIC`/`ACCOUNT_TOPIC` (`EventStreamTopic.java:13-14`, `EventsViewService.java:70`,
   `EventIngestionService.java:63-64`). `EventDedupStore implements EventDedupLedger`
   (`EventDedupStore.java:31`) delegates the same `claim()` body unchanged
   (`EventDedupStore.java:56`). `HttpMercurePublisher.buildRequest` is the exact `publish()` body
   moved verbatim into a package-private method (`HttpMercurePublisher.java:81-90`), `publish()`
   now just calls it — same headers, same form body, same timeout. Confirmed with `git diff`: only
   additions, no logic edited.

3. **R2-A unit tests — PASS.** `EventIngestionServiceTest` (`backend/src/test/java/click/kivvi/application/tracking/EventIngestionServiceTest.java`)
   uses an in-memory `FakeEventDedupLedger` and `RecordingMercurePublisher`, no Spring, no I/O;
   covers B17 (topic, translated type, icon/tone, blank-field omission) and B18 (duplicate →
   publisher called once). Mutation-tested live: removing the dedup gate in
   `EventIngestionService.ingest()` (`if (!dedupStore.claim(...)) return false;` → unconditional
   `dedupStore.claim(...)`) makes `sameIdempotencyIdIsNeverPublishedTwice` fail
   ("Expecting value to be false but was true") — reverted, `git status` clean afterward.
   `HttpMercurePublisherTest`'s two new `buildRequest*` tests (lines 122-165) call
   `publisher.buildRequest(TOPIC, DATA)` directly and drain the `BodyPublisher` via a
   `Flow.Subscriber` — genuinely no `HttpClient.send()`, no socket. Confirmed by diff: purely
   additive to the file, the 3 pre-existing network-stub tests are untouched.

4. **R2-B compare.mjs — PASS.** `compareHttpEntry` (`tools/migration-verify/compare.mjs:471-491`)
   moved the `status === 200` gate down into the login/preferences branches (unchanged behaviour
   there) and added a status-independent `oracleEntry.path === "/collect"` branch reusing the
   renamed `compareJsonBodies`. Positive proof: `evidence/repair-2-compare/positive/` and
   `.../final/` both show **0 regressions** for journey `event-stream`. Negative proof: worker
   changed `TrackedEvent`'s unknown-type message to `"R2-B DELIBERATE BREAK „…”."`, rebuilt, reran
   → `evidence/repair-2-compare/negative/event-stream/report.md` shows exactly 1 regression at
   step 5 with both bodies printed, then reverted (confirmed clean revert, 0 regressions again in
   `final/`). `/collect` always returns a JSON body in every status (202/200/400), so the new
   branch cannot spuriously flag other journeys; `/logout` keeps only its pre-existing
   status/Location check, matching the packet's narrower ask for that path.

5. **R2-C architecture enforcement — PASS, with one accepted limitation.**
   `ArchitectureTest.onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`
   (`ArchitectureTest.java:76-90`) is a real ArchUnit `onlyBeAccessed().byAnyPackage(...)` rule.
   `accountsTopicLiteralExistsOnlyInEventStreamTopic` (`ArchitectureTest.java:96-116`) is a
   quote-anchored source scan of `src/main/java` only, explicitly not covering `src/test/java` —
   this is defensible: test fixtures (`HttpMercurePublisherTest.TOPIC`, `CollectApiIT` payloads)
   legitimately hard-code the topic/messages as test data, and the wave-2 rule targets production
   code composing topics, not fixtures. Mutation-tested live: added a throwaway
   `application/ReviewMutationProbe.java` with a `"/accounts/9/events"` literal → test failed
   listing the extra file; deleted, `git status` clean. ESLint (`frontend/eslint.config.js:96-153`)
   scopes `no-restricted-syntax` on `/accounts/` and `vue/no-restricted-class` on `event-row` to
   `src/**` only (excluding `test/**`), for the same reason. `npm run lint` reproduced clean (0
   errors, the 2 pre-existing FeedCard.vue warnings), and an independent negative mutation (a new
   `src/review-negative-topic.ts` literal + a second `.vue` component with `class="event-row"`)
   was caught by name by both rules, then removed — `git status` clean. Config confirmed to apply
   to `.vue` via `eslint-plugin-vue` 10.11.0's `no-restricted-class` rule (present in
   `node_modules`) and the `files: ["src/**/*.vue"]` glob.

6. **R2-D integration tests — PASS.** `CollectApiIT.unknownEventTypeAnswers400` (B16) asserts 400
   + `"Nieznany typ zdarzenia „teleport”."`, byte-identical to the oracle
   (`context/migration-oracle/.../event-stream/steps/5/http.jsonl`, matches
   `src/Tracking/TrackedEvent.php:45`'s message format). `duplicateIdempotencyIdIsNeverPublishedTwice`
   (B18) asserts the stub hub's `PUBLISHED_BODIES` count is unchanged after the second (200
   duplicate) POST — a real IT-level dedup-publish-count proof. `EventsView.spec.ts`'s second
   `describe("B24 ...")` block (lines 232-331) mounts the real `EventsView`
   (`import EventsView from "@/views/EventsView.vue"`) with the router, and drives a `FakeEventSource`
   via new `emitOpen`/`emitMessage` methods. Traced `useEventStream.ts`: `data-paused` is read live
   from the DOM (`el.dataset.paused === "true"`) at message time, not from a Vue ref — so the
   test's `element.setAttribute("data-paused", "true")` genuinely exercises the production pause
   gate, not a hand-rolled harness. Confirmed the cap/prepend/pause assertions map 1:1 onto
   `useEventStream`'s `[row, ...rows.value].slice(0, MAX_ROWS)` and `onmessage` pause check.

7. **Test hygiene — PASS.** Deterministic (no sleeps; `BlockingQueue.poll(5, SECONDS)` /
   `CountDownLatch.await(5, SECONDS)` are bounded waits on real async I/O, not flaky timing), names
   carry B-ids (`B16`, `B17`, `B18`, `B24`, plus `B15` promoted-in-spirit though the original B19
   assertion at `CollectApiIT.java:91` was left in place rather than removed — a minor report
   wording nit ("promoted... to its own case" reads as a move; it is a duplication), not a
   functional or hygiene problem).

8. **Commit hygiene — PASS.** `git show -s 7ba8f08`: Conventional Commits (`test: repair-2 for
   w2-event-stream — ...`), English, descriptive body, no `Co-Authored-By`, no `Claude-Session`, no
   AI mention.

9. **Report accuracy — PASS, independently reproduced.**
   - `cd backend && ./mvnw -q test` → exit 0; surefire: `EventIngestionServiceTest` 4/4,
     `HttpMercurePublisherTest` 5/5, `ArchitectureTest` 5/5 — matches the report exactly.
   - `cd backend && ./mvnw -q verify` → exit 0; failsafe: `CollectApiIT` 6/6 — matches.
   - `cd frontend && npm run test -- --run` → 14 files / 74 tests passed — matches.
   - `cd frontend && npm run test:integration -- --run` → 8 files / 40 tests passed — matches.
   - `cd frontend && npm run lint` → 0 errors, 2 pre-existing `FeedCard.vue` warnings — matches.
   - No Testcontainers/docker state left running after `verify` (Ryuk reaped automatically);
     `git status --porcelain` clean throughout and at the end.

## No FAILs found

No fix items — the three round-1 findings (unit-dimension B17/B18 coverage, architecture-rule
enforcement, integration coverage for B16/B24/B18-at-IT-level) and the contract note (compare.mjs
`/collect` body diff) are each closed with credible positive and negative proof, and the
zero-change refactor (`EventDedupLedger`, `EventStreamTopic`, `buildRequest` extraction) does not
alter runtime behaviour.
