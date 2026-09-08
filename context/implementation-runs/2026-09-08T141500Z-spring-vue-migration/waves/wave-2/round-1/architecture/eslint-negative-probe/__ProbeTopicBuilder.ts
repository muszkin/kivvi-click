// Probe: composes a Mercure topic string outside the tracking/stream package.
// If the wave-2 ArchUnit/ESLint rule existed, this would be flagged.
export function buildTopic(accountId: number): string {
  return `/accounts/${accountId}/events`;
}
