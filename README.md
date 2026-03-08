# Executor

Small Spring Boot control plane for one-shot shell execution in k3s.

Default executor flavors are intentionally small for low-power homelabs:

- `small-linux`: 1 vCPU, 512 MiB
- `medium-linux`: 1 vCPU, 1024 MiB
- `large-linux`: 2 vCPU, 2048 MiB

## What exists

- `POST /jobs` stores jobs as `QUEUED` and picks the smallest matching flavor.
- `GET /jobs/{id}` returns current job state and output fields.
- `POST /internal/executors/register` registers a warm executor and marks it `READY`.
- `POST /internal/executors/{id}/result` marks the assigned job `FINISHED` and the executor `TERMINATED`.
- Scheduler and pool policy primitives exist in code for the next dispatch loop.

## Local development

Run tests:

```sh
nix shell nixpkgs#gradle nixpkgs#jdk -c gradle test
```

Run the app with local Postgres:

```sh
docker compose up --build
```

## Image publishing

GitHub Actions publishes `ghcr.io/def4alt/executor` on pushes to `main` and version tags.
Pull requests run the test job without pushing an image.

## Layout

- `src/main/kotlin/com/def4alt/executor/api` - HTTP controllers and DTOs
- `src/main/kotlin/com/def4alt/executor/application` - orchestration services
- `src/main/kotlin/com/def4alt/executor/domain` - core models
- `src/main/kotlin/com/def4alt/executor/persistence` - JDBC repositories
- `src/main/kotlin/com/def4alt/executor/pool` - warm-pool math
- `src/main/resources/db/migration` - Flyway schema
- `.github/workflows` - CI and image publishing
