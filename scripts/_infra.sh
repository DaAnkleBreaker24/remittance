# shared helpers: sourced by dev-*.sh
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="$(cat "$PROJECT_DIR/.dev/infra-mode" 2>/dev/null || echo native)"
if [[ "$(uname)" == "Darwin" && "$MODE" != "docker" ]]; then
  MODE=brew; B="$(brew --prefix)"; export PATH="$B/opt/postgresql@16/bin:$B/sbin:$PATH"
fi
compose() { (cd "$PROJECT_DIR" && docker compose "$@"); }
