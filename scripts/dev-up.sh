#!/usr/bin/env bash
# Starts local infra (PostgreSQL 16, RabbitMQ, Valkey) and dev roles. Idempotent.
# Mode from .dev/infra-mode: docker | brew (macOS) | native (Linux)
set -euo pipefail
source "$(dirname "$0")/_infra.sh"

if [[ "$MODE" == "docker" ]]; then
  compose up -d
  echo -n "waiting for postgres"; for i in $(seq 1 60); do compose exec -T postgres pg_isready -q -U remit_migrator -d remittance && break; echo -n "."; sleep 1; done; echo
  echo -n "waiting for rabbitmq"; for i in $(seq 1 90); do compose exec -T rabbitmq rabbitmq-diagnostics -q check_running >/dev/null 2>&1 && break; echo -n "."; sleep 1; done; echo
  echo "infra up (docker): postgres:5432 rabbitmq:5672 (UI http://localhost:15672) valkey:6379"
  exit 0
fi

if [[ "$MODE" == "brew" ]]; then
  for s in postgresql@16 rabbitmq valkey; do brew services start "$s" >/dev/null; done
  admin_sql() { psql -d postgres -v ON_ERROR_STOP=1 -q; }
else
  pg_ctlcluster 16 main start 2>/dev/null || true
  rabbitmqctl -q status >/dev/null 2>&1 || rabbitmq-server -detached
  valkey-cli ping >/dev/null 2>&1 || valkey-server --daemonize yes --save "" --appendonly no >/dev/null
  admin_sql() { su postgres -c "psql -v ON_ERROR_STOP=1 -q"; }
fi

echo -n "waiting for postgres"; for i in $(seq 1 60); do pg_isready -q -h localhost && break; echo -n "."; sleep 1; done; echo
admin_sql <<'SQL'
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='remit_migrator') THEN CREATE ROLE remit_migrator LOGIN PASSWORD 'remit_mig_dev_pw'; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='remit_app') THEN CREATE ROLE remit_app LOGIN PASSWORD 'remit_dev_pw'; END IF;
END $$;
SELECT 'CREATE DATABASE remittance OWNER remit_migrator' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname='remittance')\gexec
REVOKE ALL ON DATABASE remittance FROM PUBLIC;
GRANT CONNECT ON DATABASE remittance TO remit_app;
SQL

echo -n "waiting for rabbitmq"; for i in $(seq 1 90); do rabbitmqctl -q await_startup >/dev/null 2>&1 && break; echo -n "."; sleep 1; done; echo
rabbitmqctl -q list_vhosts | grep -qx remit || rabbitmqctl -q add_vhost remit
rabbitmqctl -q list_users | grep -q '^remit' || rabbitmqctl -q add_user remit remit_dev_pw
rabbitmqctl -q set_permissions -p remit remit '.*' '.*' '.*'
echo "infra up ($MODE): postgres:5432 rabbitmq:5672 valkey:6379"
