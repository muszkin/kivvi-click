# Release topology

- Default branch: `main` (GitHub `muszkin/kivvi-click`); no branch protection (GitHub Free reports the feature unavailable); no merge queue.
- Feature branch: `migration/spring-vue` from `main@8d3fc32`; journey branches `migration/<wave>/<journey>`; squash integration into the feature branch (repo policy: trunk-based, squash).
- Required PR checks: only `Docker Build` (`.github/workflows/docker-build.yml`, PHP image, push to main). No CI for the new stack yet — wave-0 introduces `.github/workflows/next-build.yml` (not exercised: no push in this run).
- Staging: none (NOT_APPLICABLE). RR-1 = second compose project on this host, separately authorized.
- Production: this host, `compose.prod` on 23456 behind the external TLS proxy for kivvi.click. CUT-1 separately authorized.
- Sonar: not configured. Dependency/secret scanning: none configured — reviewer performs diff-level checks.
