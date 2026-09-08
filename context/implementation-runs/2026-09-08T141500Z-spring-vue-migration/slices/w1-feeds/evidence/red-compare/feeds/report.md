# compare.mjs report — journey `feeds`

Base: https://localhost:19021

| Step | Dimension | Verdict | Detail |
| --- | --- | --- | --- |
| 1 | contract | accepted-deviation(DEV-4) |  |
| 1 | visual.url | parity |  |
| 1 | visual.texts | regression | text arrays differ |
| 1 | visual.aria | regression | a11y trees differ |
| 1 | visual.screenshotDesktop | regression | 3.627% pixels differ (47001/1296000) |
| 1 | visual.screenshotMobile | regression | 2.210% pixels differ (7274/329160) |
| db | sessions | accepted-deviation(DEV-9) | oracle delta 0, candidate delta 0 (mapped to spring_session) |
| db | cache_items | accepted-deviation(DEV-5) | oracle delta 0, candidate delta 0 (mapped to event_dedup+shedlock) |

**Regressions: 4**
