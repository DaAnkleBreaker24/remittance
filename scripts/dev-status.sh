#!/usr/bin/env bash
source "$(dirname "$0")/_infra.sh"
echo "mode: $MODE"
if [[ "$MODE" == "docker" ]]; then compose ps; exit 0; fi
printf "postgres: "; pg_isready -h localhost || true
printf "rabbitmq: "; rabbitmqctl -q await_startup >/dev/null 2>&1 && echo ok || echo down
printf "valkey:   "; valkey-cli ping 2>/dev/null || echo down
