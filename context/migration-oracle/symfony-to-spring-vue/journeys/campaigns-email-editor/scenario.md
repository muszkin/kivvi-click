# Journey: campaigns-email-editor

The store owner can review e-mail campaigns and open the template editor, whose document keeps literal colours in dark mode

Source tests: tests/e2e/specs/lists.spec.ts (campaigns), tests/e2e/specs/editors.spec.ts (email editor)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/campaigns"}`
2. `{"goto":"/pl/campaigns?filter=triggered"}`
3. `{"goto":"/pl/campaigns"}`
4. `{"click":".table tbody tr","waitUrl":"/pl/emails/k1"}`
5. `{"click":"[data-action=\"set-theme\"]","waitAttr":["html","data-theme","dark"],"evalStyle":[".ee-doc","backgroundColor"]}`
6. `{"reload":true,"waitAttr":["html","data-theme","dark"],"note":"data-payload of the theme button is server-rendered; a reload flips it to 'light'"}`
7. `{"click":"[data-action=\"set-theme\"]","waitAttr":["html","data-theme","light"]}`
8. `{"goto":"/pl/emails/new"}`
