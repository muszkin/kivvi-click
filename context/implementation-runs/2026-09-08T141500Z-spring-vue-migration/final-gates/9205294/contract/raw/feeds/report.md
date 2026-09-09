# compare.mjs report — journey `feeds`

Base: https://localhost:19101

| Step | Dimension | Verdict | Detail |
| --- | --- | --- | --- |
| 1 | contract | accepted-deviation(DEV-4) |  |
| db | sessions | accepted-deviation(DEV-9) | oracle delta 0, candidate delta 0 (mapped to spring_session) |
| db | cache_items | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to event_dedup+shedlock) |
| db | var/import files | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to var/import files) |

**Regressions: 0**
