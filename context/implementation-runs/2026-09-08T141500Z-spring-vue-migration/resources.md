# Resource leases

| Lease | Owner | Compose project | Edge port | Other | State |
| --- | --- | --- | --- | --- | --- |
| w0-login | worker w0-login | kivvi-w-login | 19000 (http) / 19001 (https, if used) | Postgres random published port; Mercure JWT `w-login`; Playwright context per run; `~/.m2` shared (writes serialized by cap 1 in wave-0); `node_modules` in worktree | ACQUIRED |
| integration-verification | orchestrator | kivvi-int | 19100 / 19101 | released after cohort on 3b17c07 (down -v); verification checkouts removed to reclaim disk | RELEASED |
| final-verification | orchestrator | kivvi-final | 19150 / 19151 | as above | REQUESTED |

Never used: 23456/23457 (prod), 18080/18443 (oracle capture), 8080/8443/5432 (other projects on host).
