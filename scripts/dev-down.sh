#!/usr/bin/env bash
source "$(dirname "$0")/_infra.sh"
case "$MODE" in
  docker) compose stop ;;
  brew)   for s in valkey rabbitmq postgresql@16; do brew services stop "$s" >/dev/null 2>&1 || true; done ;;
  *)      valkey-cli shutdown nosave 2>/dev/null || true; rabbitmqctl -q stop 2>/dev/null || true; pg_ctlcluster 16 main stop 2>/dev/null || true ;;
esac
echo "infra stopped ($MODE)"
