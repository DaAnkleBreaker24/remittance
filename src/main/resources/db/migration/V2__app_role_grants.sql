-- Least privilege: the application role gets DML only, never DDL.
-- Ledger and event tables are append-only for the app (triggers also forbid UPDATE/DELETE).
GRANT USAGE ON SCHEMA public TO remit_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO remit_app;
REVOKE UPDATE, DELETE ON ledger_entries, ledger_transactions, transaction_events FROM remit_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO remit_app;
-- Tables created by later migrations get the same baseline; restrict append-only tables explicitly in their migration.
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO remit_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO remit_app;
