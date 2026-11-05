# Handoff: from design conversation to Claude Code

Written 5 November 2026. This file carries the context of the design conversation with the project owner (Didier) into Claude Code. CLAUDE.md holds the coding rules. This file holds the decisions, the current state and what comes next. Update the "Current state" and "Next steps" sections as work progresses.

## 1. The project in one paragraph
A TapTap Send–style remittance platform: customers in Mauritius send money (MUR) abroad. Madagascar (MGA, mobile money payout) is first, then India, Bangladesh, Nepal and South Africa, corridor by corridor. Pay-in is by card, bank account or MauCAS. R2 adds a customer wallet (E17): top up, pay transfers from the wallet, withdraw to the customer's own bank account.

## 2. Owner and boundaries
- Didier owns the **technology** side only. Licensing, regulatory policy, AML/KYC rules and safeguarding are owned by others (compliance and legal). Implement their rules as parameters and workflows. Never invent policy values; ask, or leave them as `supplied` parameters.
- Didier works in English and French and is an experienced payments and core-banking professional (ISO 20022, Temenos T24). Use payments vocabulary freely and skip basic explanations.

## 3. Standing preferences (apply to all work)
1. **Security first, speed second**, then correctness and efficiency.
2. **Java** backend. **Open source first**. Paid products only where clearly better: HSM, eKYC, sanctions/PEP data, penetration test, support contracts.
3. **On-premise for development**, with cloud adapters behind ports for later. No cloud-only dependencies.
4. **Every fee, commission, FX margin, tax and limit is a parameter.** Nothing hard-coded.
5. **Tax on fees** is an on/off parameter (`TAX_ENABLED`, default off). The ledger tax account and tax line always exist.
6. **MauCAS participation is undecided.** Support both DIRECT and SPONSOR_BANK, selected by `MAUCAS_MODE`.
7. Cost summaries in **Mauritian rupees (MUR)**.
8. When adding content Didier didn't ask for, present it neutrally and just do the work. Don't frame it as "things you missed".
9. Don't commit or push without asking.

## 4. Deliverables and where they live
| File | What it is |
|---|---|
| `docs/Remittance_Platform_Functional_Design.docx` (v0.2) | Workflows, menus, statuses, reports, parameters, back-office, wallet (section 8) |
| `docs/Remittance_Platform_Technical_Design.docx` (v0.2) | Architecture, stack, components, ledger, security, deployment, delivery plan, decisions |
| `docs/Remittance_Backlog_Epics_UserStories.xlsx` | 17 epics, 176 stories, 819 points; Summary sheet has live formulas; status tracking |
| `docs/architecture.md`, `docs/functional-design.md`, `docs/backlog.md` | Markdown copies for Claude Code. Regenerate with `scripts/docs-sync.sh` after editing the Word or Excel files |
| `docs/d1..d4_*.png` | Logical, deployment, ports and adapters, and transfer-flow diagrams |
| `src/main/resources/db/migration/V1__core_schema.sql` | Core schema: state machine, immutable double-entry ledger, parameters with maker-checker |
| `V2__app_role_grants.sql` | Least-privilege grants for `remit_app` |

When a design decision changes, update the Word or Excel source, run `scripts/docs-sync.sh`, and keep CLAUDE.md consistent.

## 5. Decisions taken in the conversation
- Modular monolith (Spring Boot + Spring Modulith), ports and adapters, async workers. PostgreSQL is the system of record. RabbitMQ is fed by a transactional outbox; Kafka only if needed later. Valkey for cache only.
- Immutable double-entry ledger in minor units. A = P + F + T on pay-in. Cross-currency payouts post through FX position accounts.
- Payout timeout means UNKNOWN: query the partner before any retry.
- The wallet is a liability sub-ledger (`WALLET_CUSTOMER`) with row-locked debits and holds, behind `WALLET_ENABLED`, planned for **R2** (Didier may move it to R1).
- **Coding order:** see CLAUDE.md "Coding order", including the walking skeleton after Phase 0.
- **Back-office console:** React + TypeScript with a Spring Boot BFF and Keycloak. See CLAUDE.md "Back-office console".

## 6. Current state of the dev environment (Didier's MacBook Pro, Intel x64)
- Project at `~/Projects/remittance`, opened in VS Code with the Java, Spring, SonarLint, SQLTools and Claude Code extensions.
- Setup was done by `scripts/setup-mac.sh`:
  - Java: Temurin 21.
  - Maven 3.9.11: the Apache binary in `~/.local/share`, because Homebrew's `maven` needs an ARM-only openjdk on Intel.
  - Maven wrapper: `mvnw`, only-script type.
- Infra mode is **docker** (`.dev/infra-mode`). PostgreSQL 16, RabbitMQ and Valkey run from `docker-compose.yml`. Use `scripts/dev-up.sh`, `dev-status.sh`, `dev-down.sh` and `db-reset.sh`.
- `./mvnw -B verify` **passes**: `ModularityTests` is green and the jar builds. The app itself hasn't been started against the database yet.
- `git init` done; **nothing committed yet**.
- Modules exist as empty packages: identity, onboarding, profile, beneficiary, wallet, pricing, transfer, compliance, ledger, payout, notification, reporting, admin, platform (ports and adapters).

## 7. Known gaps to handle early
- Dependency versions (Spring Boot 3.4.5, Spring Modulith 1.3.4, springdoc 2.8.6, Resilience4j 2.2.0, Tink 1.15.0) were pinned without checking for newer releases. Check for current supported releases before Phase 0 work and upgrade if sensible.
- The schema needs the additions listed in architecture.md Appendix A. The first of them:
  - FX_POSITION_MUR, FX_POSITION_MGA and REFUND accounts
  - the outbox and audit_log tables
  - corridors and payout_methods
  - balance snapshots and monthly partitioning
- `customers.phone` is plain text. It must become encrypted plus a blind index before production.
- With docker infra, the Postgres container user `remit_migrator` is a superuser (dev only). Production uses separate, non-superuser roles.

## 8. Open decisions (ask Didier; don't assume)
| # | Decision | Status |
|---|---|---|
| 1 | Add **E18 Back-office Console Foundation** (BFF and session, Keycloak roles, layout, masking, generated API client), built right after E02 | Proposed, awaiting a yes |
| 2 | Wallet in R2 or R1 | R2 proposed |
| 3 | Mobile framework: Flutter or React Native (Flutter is already set up on Didier's Mac) | Open |
| 4 | MauCAS mode at go-live | Deferred (both built) |
| 5 | Vendors: eKYC, sanctions/PEP data, card PSP, payout aggregator for Madagascar | Open (business) |
| 6 | Wallet licensing and safeguarding; KYC limits; tax values; data residency | Open (compliance/legal) |

## 9. Next steps (suggested first session)
1. Ask Didier to confirm E18. If yes, add it to the workbook (copy the row styles of the existing epics and stories; keep the Summary formulas working), to both Word documents, and run `scripts/docs-sync.sh`.
2. Propose a first commit of the scaffold (ask before committing).
3. Check and upgrade dependency versions, then run `./mvnw -B verify`.
4. Start Phase 0 with E11 and E12:
   - a `Money` value type (long minor units plus currency) with tests
   - migration V3 with the ledger additions from section 7
   - a ledger posting service with balanced-per-currency checks and property tests
   - a cached parameter service with scope precedence
5. Then build the walking skeleton.

Suggested opening prompt for Claude Code:
> Read docs/HANDOFF.md and CLAUDE.md. Then do the "Next steps" in order, starting by asking me about E18.
