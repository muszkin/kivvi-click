# compare.mjs report — journey `event-stream`

Base: https://localhost:19101

| Step | Dimension | Verdict | Detail |
| --- | --- | --- | --- |
| 1 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 2 | contract | accepted-deviation(DEV-3,DEV-4,DEV-5) |  |
| 3 | contract | accepted-deviation(DEV-3,DEV-4,DEV-5) |  |
| 4 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 5 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 7 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| db | sessions | accepted-deviation(DEV-9) | oracle delta 0, candidate delta 0 (mapped to spring_session) |
| db | cache_items | accepted-deviation(DEV-5) | oracle delta 1, candidate delta 1 (mapped to event_dedup+shedlock) |

**Regressions: 0**
