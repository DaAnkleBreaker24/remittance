-- Remittance Maurice -> Madagascar : schema PostgreSQL
-- Montants en entiers (unite mineure : centimes MUR / ariary MGA sans decimale)

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============ ENUMS ============
CREATE TYPE tx_status AS ENUM (
  'CREATED','QUOTED','EXPIRED','PAYIN_PENDING','PAYIN_CONFIRMED',
  'COMPLIANCE_CHECK','MANUAL_REVIEW','PAYOUT_SENT','PAYOUT_RETRY',
  'PAID','FAILED','REFUND_PENDING','REFUNDED'
);
CREATE TYPE entry_direction AS ENUM ('DEBIT','CREDIT');
CREATE TYPE account_type AS ENUM ('ASSET','LIABILITY','REVENUE','EXPENSE');
CREATE TYPE kyc_level AS ENUM ('NONE','BASIC','FULL');
CREATE TYPE payout_channel AS ENUM ('ORANGE_MONEY','MVOLA','AIRTEL_MONEY','BANK');

-- ============ CLIENTS ============
CREATE TABLE customers (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  phone         text UNIQUE NOT NULL,
  full_name     text NOT NULL,
  kyc_level     kyc_level NOT NULL DEFAULT 'NONE',
  kyc_ref       text,                          -- reference chez le fournisseur eKYC
  monthly_limit_mur bigint NOT NULL DEFAULT 5000000, -- en centimes
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE beneficiaries (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id   uuid NOT NULL REFERENCES customers(id),
  full_name     text NOT NULL,
  channel       payout_channel NOT NULL,
  account_ref   text NOT NULL,                 -- numero mobile money ou IBAN
  created_at    timestamptz NOT NULL DEFAULT now(),
  UNIQUE (customer_id, channel, account_ref)
);

-- ============ DEVIS FX ============
CREATE TABLE quotes (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id    uuid NOT NULL REFERENCES customers(id),
  send_amount    bigint NOT NULL CHECK (send_amount > 0),   -- MUR centimes
  fee_amount     bigint NOT NULL CHECK (fee_amount >= 0),
  tax_amount     bigint NOT NULL DEFAULT 0 CHECK (tax_amount >= 0), -- 0 when tax is disabled
  receive_amount bigint NOT NULL CHECK (receive_amount > 0),-- MGA
  mid_rate       numeric(18,8) NOT NULL,       -- taux de marche
  customer_rate  numeric(18,8) NOT NULL,       -- taux client (marge incluse)
  expires_at     timestamptz NOT NULL,
  created_at     timestamptz NOT NULL DEFAULT now()
);

-- ============ TRANSACTIONS ============
CREATE TABLE transactions (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  idempotency_key text NOT NULL UNIQUE,
  customer_id     uuid NOT NULL REFERENCES customers(id),
  beneficiary_id  uuid NOT NULL REFERENCES beneficiaries(id),
  quote_id        uuid NOT NULL REFERENCES quotes(id),
  status          tx_status NOT NULL DEFAULT 'CREATED',
  send_amount     bigint NOT NULL,
  fee_amount      bigint NOT NULL,
  tax_amount      bigint NOT NULL DEFAULT 0,
  receive_amount  bigint NOT NULL,
  payin_ref       text,                        -- reference PSP
  version         integer NOT NULL DEFAULT 0,  -- verrouillage optimiste
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_customer ON transactions(customer_id, created_at DESC);
CREATE UNIQUE INDEX idx_tx_payin_ref ON transactions(payin_ref) WHERE payin_ref IS NOT NULL;

-- Historique des transitions (append-only)
CREATE TABLE transaction_events (
  id             bigserial PRIMARY KEY,
  transaction_id uuid NOT NULL REFERENCES transactions(id),
  from_status    tx_status,
  to_status      tx_status NOT NULL,
  reason         text,
  actor          text NOT NULL,                -- system / webhook:psp / agent:<id>
  created_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_tx_events ON transaction_events(transaction_id, id);

-- Transitions autorisees
CREATE TABLE allowed_transitions (
  from_status tx_status NOT NULL,
  to_status   tx_status NOT NULL,
  PRIMARY KEY (from_status, to_status)
);
INSERT INTO allowed_transitions VALUES
 ('CREATED','QUOTED'),
 ('QUOTED','PAYIN_PENDING'),('QUOTED','EXPIRED'),
 ('PAYIN_PENDING','PAYIN_CONFIRMED'),('PAYIN_PENDING','FAILED'),
 ('PAYIN_CONFIRMED','COMPLIANCE_CHECK'),
 ('COMPLIANCE_CHECK','PAYOUT_SENT'),('COMPLIANCE_CHECK','MANUAL_REVIEW'),
 ('MANUAL_REVIEW','PAYOUT_SENT'),('MANUAL_REVIEW','REFUND_PENDING'),
 ('PAYOUT_SENT','PAID'),('PAYOUT_SENT','PAYOUT_RETRY'),('PAYOUT_SENT','REFUND_PENDING'),
 ('PAYOUT_RETRY','PAYOUT_SENT'),('PAYOUT_RETRY','REFUND_PENDING'),
 ('REFUND_PENDING','REFUNDED');

-- Fonction de transition : valide, verrouille, journalise
CREATE OR REPLACE FUNCTION transition_tx(
  p_tx uuid, p_to tx_status, p_actor text, p_reason text DEFAULT NULL
) RETURNS void LANGUAGE plpgsql AS $$
DECLARE v_from tx_status;
BEGIN
  SELECT status INTO v_from FROM transactions WHERE id = p_tx FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'transaction % introuvable', p_tx; END IF;
  IF NOT EXISTS (SELECT 1 FROM allowed_transitions
                 WHERE from_status = v_from AND to_status = p_to) THEN
    RAISE EXCEPTION 'transition interdite % -> %', v_from, p_to;
  END IF;
  UPDATE transactions
     SET status = p_to, version = version + 1, updated_at = now()
   WHERE id = p_tx;
  INSERT INTO transaction_events(transaction_id, from_status, to_status, reason, actor)
  VALUES (p_tx, v_from, p_to, p_reason, p_actor);
END $$;

-- ============ PAYOUTS PARTENAIRES ============
CREATE TABLE partner_payouts (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  transaction_id uuid NOT NULL REFERENCES transactions(id),
  partner        text NOT NULL,                -- THUNES, TERRAPAY...
  partner_ref    text,                         -- reference cote partenaire
  attempt        integer NOT NULL DEFAULT 1,
  status         text NOT NULL,                -- SENT / SUCCESS / REJECTED / UNKNOWN
  raw_response   jsonb,
  created_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (transaction_id, attempt),
  UNIQUE (partner, partner_ref)
);

-- Webhooks entrants dedupliques
CREATE TABLE webhook_inbox (
  id          bigserial PRIMARY KEY,
  source      text NOT NULL,
  external_id text NOT NULL,
  payload     jsonb NOT NULL,
  processed   boolean NOT NULL DEFAULT false,
  received_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (source, external_id)
);

-- ============ LEDGER ============
CREATE TABLE accounts (
  id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code      text UNIQUE NOT NULL,              -- ex: COLLECTION_MU
  name      text NOT NULL,
  type      account_type NOT NULL,
  currency  char(3) NOT NULL,                  -- MUR / MGA
  customer_id uuid REFERENCES customers(id),   -- pour les wallets clients
  created_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO accounts(code, name, type, currency) VALUES
 ('COLLECTION_MU','Fonds collectes Maurice','ASSET','MUR'),
 ('PAYOUT_IN_TRANSIT','Payouts en cours','LIABILITY','MUR'),
 ('PREFUNDING_MG','Prefinancement agregateur','ASSET','MGA'),
 ('FEE_REVENUE','Revenus frais','REVENUE','MUR'),
 ('FX_REVENUE','Revenus marge FX','REVENUE','MUR'),
 -- Tax account always exists, whether tax is enabled or not
 ('TAX_PAYABLE','Taxe a reverser','LIABILITY','MUR'),
 -- MauCAS collection accounts, one per mode (both supported)
 ('COLLECTION_MAUCAS_DIRECT','Collecte MauCAS mode direct','ASSET','MUR'),
 ('COLLECTION_MAUCAS_SPONSOR','Collecte MauCAS via banque sponsor','ASSET','MUR');

-- Une ecriture comptable = un groupe de lignes equilibre
CREATE TABLE ledger_transactions (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  transaction_id  uuid REFERENCES transactions(id),
  idempotency_key text NOT NULL UNIQUE,
  description     text NOT NULL,
  reverses_id     uuid REFERENCES ledger_transactions(id), -- contre-passation
  created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries (
  id                    bigserial PRIMARY KEY,
  ledger_transaction_id uuid NOT NULL REFERENCES ledger_transactions(id),
  account_id            uuid NOT NULL REFERENCES accounts(id),
  direction             entry_direction NOT NULL,
  amount                bigint NOT NULL CHECK (amount > 0),
  currency              char(3) NOT NULL,
  created_at            timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_entries_account ON ledger_entries(account_id, id);
CREATE INDEX idx_entries_ltx ON ledger_entries(ledger_transaction_id);

-- Immutabilite : interdit UPDATE et DELETE
CREATE OR REPLACE FUNCTION forbid_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'table % immuable : utiliser une contre-passation', TG_TABLE_NAME;
END $$;

CREATE TRIGGER trg_entries_immutable
  BEFORE UPDATE OR DELETE ON ledger_entries
  FOR EACH ROW EXECUTE FUNCTION forbid_mutation();
CREATE TRIGGER trg_ltx_immutable
  BEFORE UPDATE OR DELETE ON ledger_transactions
  FOR EACH ROW EXECUTE FUNCTION forbid_mutation();
CREATE TRIGGER trg_events_immutable
  BEFORE UPDATE OR DELETE ON transaction_events
  FOR EACH ROW EXECUTE FUNCTION forbid_mutation();

-- Equilibre debit = credit par devise, verifie en fin de transaction SQL
CREATE OR REPLACE FUNCTION check_ledger_balanced() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE bad record;
BEGIN
  SELECT currency,
         SUM(CASE direction WHEN 'DEBIT' THEN amount ELSE -amount END) AS diff
    INTO bad
    FROM ledger_entries
   WHERE ledger_transaction_id = NEW.ledger_transaction_id
   GROUP BY currency
  HAVING SUM(CASE direction WHEN 'DEBIT' THEN amount ELSE -amount END) <> 0
   LIMIT 1;
  IF FOUND THEN
    RAISE EXCEPTION 'ecriture % desequilibree en % (ecart %)',
      NEW.ledger_transaction_id, bad.currency, bad.diff;
  END IF;
  RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER trg_ledger_balanced
  AFTER INSERT ON ledger_entries
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION check_ledger_balanced();

-- Soldes calcules (vue)
CREATE VIEW account_balances AS
SELECT a.id, a.code, a.currency,
       COALESCE(SUM(CASE e.direction WHEN 'DEBIT' THEN e.amount ELSE -e.amount END), 0) AS balance_debit_positive
  FROM accounts a
  LEFT JOIN ledger_entries e ON e.account_id = a.id
 GROUP BY a.id, a.code, a.currency;

-- ============ PARAMETERS (fees, limits, tax and MauCAS settings) ============
CREATE TABLE parameters (
  id             bigserial PRIMARY KEY,
  key            text NOT NULL,                  -- e.g. TAX_ENABLED, MAUCAS_MODE, FEE_TABLE
  scope          text NOT NULL DEFAULT 'GLOBAL', -- GLOBAL / CORRIDOR:MU-MG / CHANNEL:...
  value          jsonb NOT NULL,
  effective_from timestamptz NOT NULL DEFAULT now(),
  version        integer NOT NULL,
  status         text NOT NULL DEFAULT 'PENDING',-- PENDING / ACTIVE / SUPERSEDED (maker-checker)
  created_by     text NOT NULL,
  approved_by    text,
  created_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (key, scope, version),
  CHECK (approved_by IS NULL OR approved_by <> created_by)  -- maker cannot approve own change
);
INSERT INTO parameters(key, scope, value, version, status, created_by, approved_by) VALUES
 ('TAX_ENABLED','GLOBAL','false',1,'ACTIVE','seed','seed-approver'),
 ('MAUCAS_ENABLED','GLOBAL','false',1,'ACTIVE','seed','seed-approver'),
 ('MAUCAS_MODE','GLOBAL','"SPONSOR_BANK"',1,'ACTIVE','seed','seed-approver');

-- ============ EXEMPLE : pay-in confirme (10 000 MUR, frais 100) ============
-- BEGIN;
--   SELECT transition_tx(:tx, 'PAYIN_CONFIRMED', 'webhook:psp');
--   WITH l AS (
--     INSERT INTO ledger_transactions(transaction_id, idempotency_key, description)
--     VALUES (:tx, 'payin:' || :tx, 'Pay-in confirme') RETURNING id)
--   INSERT INTO ledger_entries(ledger_transaction_id, account_id, direction, amount, currency)
--   SELECT l.id, (SELECT id FROM accounts WHERE code='COLLECTION_MU'),   'DEBIT',  1000000, 'MUR' FROM l
--   UNION ALL
--   SELECT l.id, (SELECT id FROM accounts WHERE code='PAYOUT_IN_TRANSIT'),'CREDIT',  990000, 'MUR' FROM l
--   UNION ALL
--   SELECT l.id, (SELECT id FROM accounts WHERE code='FEE_REVENUE'),      'CREDIT',   10000, 'MUR' FROM l
--   UNION ALL  -- tax line: amount is 0 when tax is disabled, so skip the row in that case
--   SELECT l.id, (SELECT id FROM accounts WHERE code='TAX_PAYABLE'),      'CREDIT',    :tax, 'MUR' FROM l WHERE :tax > 0;
-- COMMIT;
