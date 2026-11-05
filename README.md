# Remittance Platform (backend)

Outbound transfers from Mauritius (MUR), starting with Madagascar (MGA). Java 21 / Spring Boot modular monolith.

## First-time setup (macOS)
```bash
cd ~/Projects/remittance
./scripts/setup-mac.sh
```
This installs Temurin 21, Maven (official Apache binary), the infrastructure (Docker if Docker Desktop is running, otherwise Homebrew), developer CLIs, the Claude Code CLI and the VS Code extensions. Works on Intel and Apple Silicon Macs. It then starts the infrastructure, runs the first build and opens VS Code. It is safe to re-run.

## Daily use
| Task | Command (or VS Code: Terminal > Run Task) |
|---|---|
| Start infra | `./scripts/dev-up.sh` |
| Build and test | `./mvnw -B verify` |
| Run app | `./mvnw spring-boot:run` then open http://localhost:8080/actuator/health |
| Stop infra | `./scripts/dev-down.sh` |
| Claude Code | `claude` in this folder, or the Claude Code panel in VS Code |

Docker is optional. `docker-compose.yml` is an alternative to the Homebrew services. Use one or the other, because they use the same ports. Integration tests that use Testcontainers need Docker running.

## Docs
- `CLAUDE.md`: working rules for Claude Code and for developers
- `docs/architecture.md` and `docs/functional-design.md`: design (Markdown copies of the Word files)
- `docs/backlog.md`: epics and user stories (the Excel workbook remains the tracker)
