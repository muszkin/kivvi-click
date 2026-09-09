# compare.mjs report — journey `import-wizard`

Base: https://localhost:19101

| Step | Dimension | Verdict | Detail |
| --- | --- | --- | --- |
| 1 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 2 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 3 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 4 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 5 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 6 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 7 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 8 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 9 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| 10 | contract | accepted-deviation(DEV-4,DEV-5) |  |
| db | sessions | accepted-deviation(DEV-9) | oracle delta 1, candidate delta 1 (mapped to spring_session) |
| db | cache_items | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to event_dedup+shedlock) |
| db | var/import files | accepted-deviation(DEV-5) | oracle delta 1, candidate delta 1 (mapped to var/import files) |

**Regressions: 0**
