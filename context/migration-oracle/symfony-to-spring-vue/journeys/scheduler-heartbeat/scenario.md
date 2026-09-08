# Journey: scheduler-heartbeat

The system emits a scheduler heartbeat every hour through the worker and logs it

Source tests: src/Schedule.php, src/MessageHandler/HeartbeatHandler.php (no browser surface)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

No browser steps: this journey is a background job. Its contract is recorded in `contract.md`.
