# compare.mjs report — journey `feeds`

Base: https://localhost:19021

| Step | Dimension | Verdict | Detail |
| --- | --- | --- | --- |
| 1 | contract | accepted-deviation(DEV-4) |  |
| 1 | visual.url | parity |  |
| 1 | visual.texts | parity |  |
| 1 | visual.aria | parity |  |
| 1 | visual.screenshotDesktop | parity | 0.000% pixels differ (0/1296000) |
| 1 | visual.screenshotMobile | parity | 0.000% pixels differ (0/329160) |
| db | sessions | accepted-deviation(DEV-9) | oracle delta 0, candidate delta 0 (mapped to spring_session) |
| db | cache_items | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to event_dedup+shedlock) |

**Regressions: 0**
