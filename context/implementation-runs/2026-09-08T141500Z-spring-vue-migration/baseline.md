# Baseline

- Base SHA: `8d3fc320354604b641b44a3043a070f279e6d491` on `main` (clean tracked tree; untracked `.ai/` is excluded tool state).
- Product code SHA: `91f8f85` (commits since are docs/context/compose-restart-policy only).
- Old-stack gates: not rerun in this run (unchanged since the last verified state recorded by the maintainer; `composer test` documented, not executed here). Pre-existing failures: none known.
- Toolchain on host: OpenJDK 21.0.12 (backend compiles need JDK 25 → Temurin 25 tarball in `~/.cache/kivvi-toolchains/`), Node 26.8.1, npm 11.19.0, Chrome 152.0.7977.82, Docker 29.8.0, no Maven/Gradle (Maven wrapper per worktree). Network to Maven Central, npm registry and Docker Hub: 200.
- Resources: 16 CPU, 104 GB RAM free, 11 GB disk free (K7 threshold 3 GB), ports 19000–19199 free.
- Production: restored and healthy on 23456 (R17 repaired 2026-09-08 14:00Z); untouched by this run.

## Oracle

- Path: `/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue`
- Manifest SHA-256: `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f` (equals plan)
- `source_sha`: `5b806ac74b75d1d521de97f7d3c70b8c431697fb` (equals plan pre-migration SHA)
- 700 files, all sha256 match; 13/13 journeys `complete`; status `PASS`.
