#!/usr/bin/env bash
set -euo pipefail

export USER="${USER:-workspace}"
export HOME="${HOME:-/home/workspace}"

export WORKSPACE_ROOT="${WORKSPACE_ROOT:-/workspace}"
export REPO_DIR="${REPO_DIR:-${WORKSPACE_ROOT}/repo}"
export LOG_DIR="${LOG_DIR:-${WORKSPACE_ROOT}/logs}"
export STATE_DIR="${STATE_DIR:-${WORKSPACE_ROOT}/state}"

export SESSION_NAME="${SESSION_NAME:-main}"
export TTYD_PORT="${TTYD_PORT:-7681}"
export TTYD_INTERFACE="${TTYD_INTERFACE:-0.0.0.0}"

export BUN_INSTALL="${BUN_INSTALL:-$HOME/.bun}"
export PATH="${BUN_INSTALL}/bin:${HOME}/.nix-profile/bin:/nix/var/nix/profiles/default/bin:${PATH}"

mkdir -p "${HOME}/.config/nix" "${WORKSPACE_ROOT}" "${LOG_DIR}" "${STATE_DIR}"

cat > "${HOME}/.config/nix/nix.conf" <<'EOF'
experimental-features = nix-command flakes
accept-flake-config = true
EOF

if [[ -x /usr/local/bin/bootstrap-workspace.sh ]]; then
  /usr/local/bin/bootstrap-workspace.sh
else
  /home/workspace/bin/bootstrap-workspace.sh
fi

exec ttyd \
  --port "${TTYD_PORT}" \
  --interface "${TTYD_INTERFACE}" \
  --writable \
  tmux attach -t "${SESSION_NAME}"
