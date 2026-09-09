# compare.mjs report — journey `login`

Base: http://localhost:23458

| Step | Dimension | Verdict | Detail |
| --- | --- | --- | --- |
| 1 | contract | accepted-deviation(DEV-4,DEV-5,DEV-9) |  |
| 4 | contract | accepted-deviation(DEV-4,DEV-5,DEV-9) |  |
| 5 | contract | accepted-deviation(DEV-4,DEV-5,DEV-9) |  |
| 6 | contract | accepted-deviation(DEV-4,DEV-5,DEV-9) |  |
| 7 | contract | accepted-deviation(DEV-4,DEV-5,DEV-9) |  |
| 8 | contract | accepted-deviation(DEV-4,DEV-5,DEV-9) |  |
| db | sessions | accepted-deviation(DEV-9) | oracle delta 0, candidate delta 1 (mapped to spring_session) |
| db | cache_items | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to event_dedup+shedlock) |
| db | var/import files | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to var/import files) |

**Regressions: 0**
