# CLAUDE.md — Remittance Platform

Outbound money-transfer platform from Mauritius (MUR). First corridor is Madagascar (MGA). India, Bangladesh, Nepal and South Africa follow phase by phase.
This repo is the backend: a Java modular monolith. The back-office console (React) and the customer mobile app come later, as separate repos (see "Back-office console" below).

Read before non-trivial work:
- `docs/HANDOFF.md`: **start here in a new session**. Decisions, current state, the owner's standing preferences and next steps
- `docs/architecture.md`: condensed technical design (modules, ledger, state machine, security, ports)
- `docs/backlog.md`: epics E01–E17 and user stories (R1 = Mauritius to Madagascar; R2 adds the wallet)
- `docs/*.docx` and `docs/*.xlsx`: full functional and technical design, and the backlog with status (source of truth for humans)
- `src/main/resources/db/migration/V1__core_schema.sql`: current schema

## Priorities (in this order)
1. **Security.** When in doubt, choose the safer option and say why.
2. **Speed.** Keep the customer path short. Anything slow or external is asynchronous.
3. **Correct money.** Double-entry, idempotent and reconcilable, always.

## Commands
```bash
./scripts/dev-up.sh          # start PostgreSQL 16, RabbitMQ, Valkey (Docker or Homebrew services on macOS); idempotent
./scripts/dev-status.sh      # check infra
./scripts/dev-down.sh        # stop infra
./scripts/db-reset.sh        # drop and recreate the dev schema (Flyway re-applies on next start)
./mvnw -B verify             # build plus all tests (includes the module-boundary test)
./mvnw test -Dtest=ClassName # single test class
./mvnw spring-boot:run       # run the app on :8080; health at /actuator/health
```
Dev DB: `remittance` on localhost:5432. Roles: `remit_migrator` (owns schema, used by Flyway) and `remit_app` (DML only, used by the app). Dev passwords are in `application.yml` defaults; override with env vars. Never commit real secrets.

## Stack
Java 21, Spring Boot 3.4, Spring Modulith, Spring Security, PostgreSQL 16 + Flyway, RabbitMQ (fed by a transactional outbox), Valkey (cache and rate limits only, never a source of truth), Resilience4j, Google Tink, springdoc-openapi, Micrometer/Prometheus. Tests: JUnit 5, Testcontainers, ArchUnit, Spring Modulith test.
Plain JDBC (`JdbcClient`) or jOOQ on the money path (ledger, state transitions). JPA is acceptable only for simple CRUD entities.
Do not add a dependency without saying why. Prefer what is already on the classpath.

## Architecture rules (enforced by tests; the build fails if broken)
- Package root `mu.remit`. Each top-level package is a module: `identity, onboarding, profile, beneficiary, wallet, pricing, transfer, compliance, ledger, payout, notification, reporting, admin, platform`.
- A module's public API is its top-level package. Internals go in sub-packages (`internal`, `web`, `persistence`). Other modules must not import another module's internals.
- A module never reads or writes another module's tables. Call its API or listen to its events.
- Business code depends on **ports** (`mu.remit.platform.ports`), never on infrastructure SDKs. Adapters live in `mu.remit.platform.adapters.<onprem|aws|azure>` and are selected by Spring profile (`onprem` now; cloud later).
- External business systems also sit behind ports: `KycProvider`, `ScreeningProvider`, `PayinConnector` (card, bank, MauCAS DIRECT, MauCAS SPONSOR_BANK, wallet), `PayoutPartner`, `DisbursementConnector` (wallet withdrawals to the customer's own bank account), `RateProvider`, `NotificationSender`.
- `ModularityTests` must stay green. Add ArchUnit rules when you add a new convention.

## Money rules (non-negotiable)
- Amounts are `long` in **minor units** plus an ISO currency code. No `double` or `float` anywhere near money. Use a `Money` value type.
- The ledger is **immutable double-entry**. Never UPDATE or DELETE ledger rows; corrections are reversing entries (`reverses_id`). DB triggers enforce this, so don't work around them.
- Each currency balances by itself. A cross-currency payout posts two legs through `FX_POSITION_<CCY>` accounts.
- Pay-in posting: `A = P + F + T` (amount = principal + fee + tax). The tax line and `TAX_PAYABLE` account always exist; `TAX_ENABLED` only decides whether T is non-zero.
- State change + ledger postings + outbox event = **one DB transaction**. Nothing is published before commit.
- All status changes go through `transition_tx()` / the `allowed_transitions` table. No ad-hoc `UPDATE transactions SET status`.
- Every state-changing API takes an `Idempotency-Key`. Every handler is idempotent (events are at-least-once).
- Payout timeout = UNKNOWN → **query partner status before any retry**. Never blind-retry a payout.
- Stored quotes are never recalculated. Parameter changes affect new quotes only.
- **Wallet (E17)**: each wallet is a `WALLET_CUSTOMER` liability sub-account; the ledger is the balance source of truth. Available = posted - active holds. Every debit locks the wallet row (`SELECT ... FOR UPDATE` plus version) and checks available balance in the same transaction that posts it. No negative balances, no overdraft. A transfer paid from the wallet reaches PAYIN_CONFIRMED in the same transaction as the debit. Withdrawals go only to verified own-name accounts and follow the payout rules (hold, send, UNKNOWN means query first, reverse on failure). Postings are in docs/architecture.md section 5.11.

## Parameters
- No fee, commission, FX margin, tax, limit, timeout or threshold is hard-coded. They come from the `parameters` table (typed, scoped, versioned, effective-dated, maker-checker: approver ≠ creator).
- Scope precedence: customer segment → payment type → channel → corridor → global.
- Seeded flags: `TAX_ENABLED=false`, `MAUCAS_ENABLED=false`, `MAUCAS_MODE=SPONSOR_BANK`. The MauCAS participation model is undecided: **both** DIRECT and SPONSOR_BANK connectors must work, selected by parameter. In-flight payments finish in the mode they started in.
- `WALLET_ENABLED` (default false, seeded by the wallet migration) gates every wallet endpoint and the Wallet payment type.
- Read parameters through a cached service, not a query per request.

## Security rules
- Target OWASP ASVS L2. Deny by default: every endpoint declares its role or scope. Sensitive actions need step-up auth (customer) or four-eyes approval (staff).
- PII (ID numbers, phone, document refs, beneficiary account numbers) is field-encrypted with Tink (envelope keys; master key in OpenBao/HSM later, a local dev keyset now). Searchable fields use a keyed hash (blind index). `customers.phone` is currently plain text and must be migrated.
- Never log PII, secrets, tokens, OTPs, card data or full account numbers. Use the masking logger helpers.
- Card numbers never touch this system (PSP-hosted fields; store token, brand, last4, expiry only).
- Validate all input (Bean Validation, reject unknown fields). Webhooks: verify signature and timestamp, dedupe via `webhook_inbox`, ack fast, process async.
- Passwords and PINs: Argon2id. No custom crypto.
- SQL: parameterised only. No string-concatenated SQL.
- New dependencies: must be maintained, licence-compatible (prefer Apache-2.0, MIT, BSD) and free of known critical CVEs.

## Database and migrations
- Flyway, SQL-first: `src/main/resources/db/migration/V<n>__<snake_case>.sql`. Never edit an applied migration; add a new one.
- Expand-and-contract for changes that break the running version.
- New append-only tables: revoke UPDATE/DELETE from `remit_app` in the same migration.
- Planned next migrations (see docs/architecture.md, Appendix A): FX_POSITION_MUR/MGA and REFUND accounts, outbox, audit_log (hash chain), corridors, payout_methods, beneficiary_field_defs, balance_snapshots, monthly partitioning of ledger_entries and transaction_events, KYC tables, payment_instruments, limit_usage, compliance_cases, reconciliation, notifications. R2: wallets, wallet_holds, wallet_operations, withdrawal_accounts, WITHDRAWAL_IN_TRANSIT and SETTLEMENT_BANK accounts.

## Testing
- Unit tests for pricing, limits, transitions and posting templates.
- Integration tests use Testcontainers (real PostgreSQL), not H2. Docker must be running for those. Tag them `@Tag("integration")`.
- Ledger property tests: any generated sequence of events keeps every currency balanced.
- Failure tests for every partner integration: timeout after send, duplicate webhook, out-of-order webhook, partner down.
- A story is done when its acceptance criteria in docs/backlog.md have tests.

## Conventions
- Java records for DTOs and value objects. Constructor injection only. No Lombok.
- REST under `/v1`. Errors as RFC 9457 problem details with a stable `code`. Amounts in JSON as integer minor units plus currency.
- Times are `Instant` in UTC; JVM runs with `-Duser.timezone=UTC`.
- Every log line carries the correlation ID (and transaction ID once it exists).
- Small, focused commits. Conventional Commit messages (`feat(ledger): ...`).

## Coding order (agreed)
Build what everything depends on first: parameters, ledger and authentication before customer features.
1. Phase 0 Foundations: E16 core (CI, secrets, encryption helpers, outbox, audit, correlation IDs) → E11 parameter registry (API with maker-checker; seed values via migrations until the UI exists) → E12 ledger core (accounts, balanced postings, immutability, integrity checks) → E15 corridor model + mock partner.
2. **Walking skeleton** right after Phase 0: one seeded test customer sends a transfer through quote → mock pay-in → ledger → mock payout. Every later epic replaces a piece of this path with the real thing.
3. Phase 1 Customer core: E02 auth → E18 back-office console foundation (if confirmed) → E01 onboarding/KYC → E03 profile → E04 instruments (card, bank, both MauCAS modes) → E05 beneficiaries and screening.
4. Phase 2 Money flow: E06 quote/fees/FX → E07 transfer execution → E15 Madagascar payout adapter → E08 history.
5. Phase 3 Operations: E09 compliance ops → E14 support/refunds → E13 notifications → E10 reports → E12 reconciliation and prefunding.
6. Phase 4 Hardening: load tests (k6), resilience tests, pen test fixes, DR drill.
7. R2 starts with E17 Customer Wallet. Keep the ledger and pay-in ports wallet-ready in R1, but don't build wallet features before R2 unless asked.
Each epic delivers its back-office screens in the same sprint, not at the end.

## Back-office console (separate repo, later)
- React + TypeScript (Vite), Ant Design (or Mantine), TanStack Query and TanStack Table (virtualised, server-side keyset pagination), React Hook Form + Zod, an API client generated from this repo's OpenAPI spec (orval or openapi-typescript), and Vitest + Playwright.
- Auth through a backend-for-frontend (BFF) in Spring Boot. Keycloak OIDC with Authorization Code + PKCE, and the tokens stay server-side. The browser only gets an HttpOnly, Secure, SameSite=Strict session cookie: no tokens in localStorage.
- Back-office API under `/admin/v1` on its own security filter chain and port/ingress. It's reachable only from the internal network or VPN with an IP allow-list, never through the customer edge.
- Keycloak MFA (passkeys or TOTP). Step-up for refunds, adjustments, parameter changes and data reveal. Four-eyes flows. Authorisation always enforced on the server.
- PII masked by default; reveal and export endpoints are logged.
- Strict CSP (no inline scripts, no third-party CDNs), HSTS, frame-ancestors none.
- Reports: Apache Superset on the read replica with Keycloak SSO. Don't rebuild charting in React.
- Developer tools (Swagger UI, Grafana, RabbitMQ and Keycloak consoles) stay in the management zone. Swagger UI is off in production.

## Working agreement for Claude Code
- Before coding a story, read its acceptance criteria in docs/backlog.md and state the plan briefly.
- Keep changes inside one module where possible. If a change crosses modules, use an event or a public API method.
- Run `./mvnw -B verify` before saying a task is done. Report failures honestly.
- When a design question touches money, security or compliance and isn't answered here or in docs/, ask rather than guess.
- Licensing and regulatory policy are owned by others. Implement rules as parameters and workflows; don't invent policy values.
