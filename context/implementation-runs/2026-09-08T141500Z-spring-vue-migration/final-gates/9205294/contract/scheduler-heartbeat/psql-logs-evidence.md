# scheduler-heartbeat — contract verification via read-only psql/logs

Command: docker compose -p kivvi-int -f compose.next.yaml exec -T database psql -U app -d app -c "select name, lock_until, locked_at, locked_by from shedlock;"

```
        name         |       lock_until        |        locked_at        |  locked_by   
---------------------+-------------------------+-------------------------+--------------
 heartbeat           | 2026-09-09 13:20:15.185 | 2026-09-09 13:19:15.196 | a6cb99deceb9
 event-dedup-cleanup | 2026-09-09 13:20:15.215 | 2026-09-09 13:19:15.216 | a6cb99deceb9
(2 rows)

```

Container start time (api): 2026-09-09T13:19:11.082300552Z
Captured at (UTC): 2026-09-09T13:45:12Z

## API log lines matching the heartbeat tick message

Command: docker compose -p kivvi-int -f compose.next.yaml logs api | grep 'Scheduler heartbeat tick.'

```
api-1  | 2026-09-09T13:19:15.206Z  INFO 1 --- [kivvi-click] [eat-scheduler-1] c.k.i.scheduling.HeartbeatJob            : Scheduler heartbeat tick.
```

Tick line count: 1
