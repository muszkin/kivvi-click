# Journey: scheduler-heartbeat — contract (static capture)

Source: `src/Schedule.php`, `src/Message/Heartbeat.php`, `src/MessageHandler/HeartbeatHandler.php`, `compose.yaml` (`worker` service), captured from `91f8f85`.

- Schedule name: `default` (`#[AsSchedule]`), consumed by `php bin/console messenger:consume async scheduler_default --time-limit=3600 --memory-limit=128M` in the `worker` container.
- Recurrence: `RecurringMessage::every('1 hour', new Heartbeat())`.
- State: `stateful($cache)` on `cache.app` (Postgres `cache_items`), `processOnlyLastMissedRun(true)` — after a worker outage exactly one missed run executes.
- Effect: `HeartbeatHandler` logs `Scheduler heartbeat tick.` at INFO. No database write besides the scheduler state key in `cache_items`.
- Messenger routing: none — `Heartbeat` is handled synchronously by the consumer.

Parity on the new stack: a scheduled job every 60 minutes, with distributed-lock state in Postgres (ShedLock `shedlock` table), that logs the same message at INFO and executes at most once after a missed window. Verified by `unit`/`integration` (job fires under a simulated clock, lock held) and `contract` (only the lock/state table changes).
