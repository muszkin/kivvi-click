# Scheduler

DAG: w0-login → {w1-landing, w1-feeds, w1-scheduler-heartbeat} → {w2-event-stream, w2-customers} → {w3-automations, w3-settings, w3-campaigns-email-editor} → {w4-popups-widget-editor, w4-import-wizard, w4-dashboard} → w5-shell-navigation → final cohort.

Concurrency cap: 2 (disk 11 GB). Integration order inside a wave: alphabetical.

| Node | State | Parent SHA | Worktree |
| --- | --- | --- | --- |
| w0-login | PLANNED → WORKTREE_READY | 8d3fc32 | /home/muszkin/work/kivvi-click-wt/w0-login |
