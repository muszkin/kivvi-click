Negative probe evidence: two files copied into a scratch checkout of frontend/ (not the
verifier checkout) and linted with the wave-2 SHA's real eslint.config.js.

Command: cd <scratch>/eslint-probe && npx eslint src/components/molecules/__ProbeTopicBuilder.ts src/components/molecules/__ProbeSecondEventRow.vue
Result: exit 0, zero output — neither probe is flagged.

__ProbeTopicBuilder.ts composes a Mercure topic string (`/accounts/${id}/events`) outside
any tracking/stream module — this is exactly the pattern the rules-translated.md wave-2 row
"Mercure topic built only server-side" (ESLint no-restricted-syntax on `/accounts/` literals)
is supposed to catch. It is not caught because that rule is absent from eslint.config.js.

__ProbeSecondEventRow.vue renders class="event-row" from a component other than EventRow.vue
— this is exactly the pattern the wave-2 row "Row markup in one place" (ESLint
vue/no-restricted-class rule) is supposed to catch. It is not caught because that rule is
absent from eslint.config.js.
