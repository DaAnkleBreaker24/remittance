#!/usr/bin/env bash
# Drops and recreates the dev schema; Flyway re-applies migrations on next app start.
set -euo pipefail
source "$(dirname "$0")/_infra.sh"
SQL="SET client_min_messages=warning; DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
if [[ "$MODE" == "docker" ]]; then compose exec -T postgres psql -q -U remit_migrator -d remittance -c "$SQL"
else PGPASSWORD=remit_mig_dev_pw psql -h localhost -U remit_migrator -d remittance -q -c "$SQL"; fi
echo "schema reset"
