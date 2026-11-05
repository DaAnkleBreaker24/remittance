#!/usr/bin/env bash
# One-shot developer setup for macOS (Intel and Apple Silicon). Safe to re-run.
# - Java 21: Temurin cask (has Intel and ARM builds)
# - Maven: official Apache binary (checksum verified); avoids Homebrew's ARM-only openjdk dependency
# - Infra (PostgreSQL 16, RabbitMQ, Valkey): Docker if Docker Desktop is running, otherwise Homebrew services
# - CLIs and Claude Code: best effort, failures are listed at the end instead of stopping the script
set -uo pipefail
cd "$(dirname "$0")/.."
PROJECT_DIR="$(pwd)"
MVN_VER=3.9.11
FAILED=()
step() { printf "\n\033[1;34m==> %s\033[0m\n" "$*"; }
warn() { printf "\033[1;33m  ! %s\033[0m\n" "$*"; }
add_rc() { grep -qF "$1" ~/.zshrc 2>/dev/null || echo "$1" >> ~/.zshrc; }
brew_try() { for f in "$@"; do brew list "$f" >/dev/null 2>&1 && { echo "  ok  $f (installed)"; continue; }
  if brew install "$f"; then echo "  ok  $f"; else warn "could not install $f"; FAILED+=("$f"); fi; done; }

echo "Mac architecture: $(uname -m)"

step "Homebrew"
if ! command -v brew >/dev/null 2>&1 && [[ ! -x /usr/local/bin/brew && ! -x /opt/homebrew/bin/brew ]]; then
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" || { echo "Homebrew install failed"; exit 1; }
fi
eval "$(/opt/homebrew/bin/brew shellenv 2>/dev/null || /usr/local/bin/brew shellenv)"
export HOMEBREW_NO_AUTO_UPDATE=1
brew update >/dev/null || true

step "Java 21 (Temurin)"
brew list --cask temurin@21 >/dev/null 2>&1 || brew install --cask temurin@21 || { echo "Temurin install failed"; exit 1; }
export JAVA_HOME="$(/usr/libexec/java_home -v 21)" || { echo "JDK 21 not found after install"; exit 1; }
export PATH="$JAVA_HOME/bin:$PATH"
add_rc 'export JAVA_HOME="$(/usr/libexec/java_home -v 21)"'
java -version 2>&1 | head -1

step "Maven $MVN_VER (Apache binary)"
MVN_HOME="$HOME/.local/share/apache-maven-$MVN_VER"
if [[ ! -x "$MVN_HOME/bin/mvn" ]]; then
  tmp="$(mktemp -d)"; base="https://archive.apache.org/dist/maven/maven-3/$MVN_VER/binaries"
  curl -fsSL "$base/apache-maven-$MVN_VER-bin.tar.gz" -o "$tmp/m.tgz" \
   && curl -fsSL "$base/apache-maven-$MVN_VER-bin.tar.gz.sha512" -o "$tmp/m.sha512" \
   && echo "$(awk '{print $1}' "$tmp/m.sha512")  $tmp/m.tgz" | shasum -a 512 -c - \
   && mkdir -p "$HOME/.local/share" && tar xzf "$tmp/m.tgz" -C "$HOME/.local/share" \
   || { echo "Maven download or checksum failed"; exit 1; }
fi
export PATH="$MVN_HOME/bin:$PATH"
add_rc "export PATH=\"$MVN_HOME/bin:\$PATH\""
mvn -v | head -1

step "Infrastructure (PostgreSQL 16, RabbitMQ, Valkey)"
mkdir -p .dev
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  echo "Docker is running: infra will run in containers (docker-compose.yml)."
  echo docker > .dev/infra-mode
else
  echo "Docker not running: installing infra with Homebrew (on Intel Macs this may compile from source and take a while)."
  echo brew > .dev/infra-mode
  brew_try postgresql@16 rabbitmq valkey
  BREW="$(brew --prefix)"
  add_rc "export PATH=\"$BREW/opt/postgresql@16/bin:$BREW/sbin:\$PATH\""
fi

step "Developer CLIs (best effort)"
brew_try git jq gitleaks trivy k6

step "Claude Code CLI"
if command -v claude >/dev/null 2>&1; then echo "  ok  claude (installed)"
elif command -v npm >/dev/null 2>&1; then npm install -g @anthropic-ai/claude-code || FAILED+=("claude-code")
else curl -fsSL https://claude.ai/install.sh | bash || FAILED+=("claude-code"); fi

step "VS Code extensions"
CODE="$(command -v code || echo "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code")"
if [[ -x "$CODE" ]]; then
  for ext in vscjava.vscode-java-pack vmware.vscode-boot-dev-pack redhat.vscode-yaml redhat.vscode-xml \
             sonarsource.sonarlint-vscode ms-azuretools.vscode-docker mtxr.sqltools mtxr.sqltools-driver-pg \
             editorconfig.editorconfig humao.rest-client anthropic.claude-code; do
    "$CODE" --install-extension "$ext" --force >/dev/null 2>&1 && echo "  ok  $ext" || { warn "skip $ext"; FAILED+=("vscode:$ext"); }
  done
else warn "VS Code CLI not found (Cmd+Shift+P > 'Shell Command: Install code command in PATH')"; fi

step "Start infrastructure"
"$PROJECT_DIR/scripts/dev-up.sh" || FAILED+=("infra-start")

step "Maven wrapper and first build"
[[ -f mvnw ]] || mvn -q -B wrapper:wrapper -Dmaven="$MVN_VER"
chmod +x mvnw 2>/dev/null
if ./mvnw -B verify; then BUILD=ok; else BUILD=failed; FAILED+=("build"); fi

step "git"
[[ -d .git ]] || { git init -q -b main && echo "git repo initialised (no commit yet)"; }

step "Summary"
echo "Java:  $(java -version 2>&1 | head -1)"
echo "Maven: $(mvn -v 2>/dev/null | head -1)"
echo "Infra mode: $(cat .dev/infra-mode)"
"$PROJECT_DIR/scripts/dev-status.sh" || true
echo "Build: $BUILD"
if ((${#FAILED[@]})); then warn "Needs attention: ${FAILED[*]}"; else echo "Everything installed. Open a NEW terminal so PATH changes apply."; fi
"$CODE" "$PROJECT_DIR" >/dev/null 2>&1 || true
