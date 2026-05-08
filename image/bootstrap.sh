#!/usr/bin/env bash
set -euo pipefail

SESSION_NAME="${SESSION_NAME:-main}"
WORKSPACE_ROOT="${WORKSPACE_ROOT:-/workspace}"
REPO_DIR="${REPO_DIR:-${WORKSPACE_ROOT}/repo}"
LOG_DIR="${LOG_DIR:-${WORKSPACE_ROOT}/logs}"
STATE_DIR="${STATE_DIR:-${WORKSPACE_ROOT}/state}"

REPO_URL="${REPO_URL:-}"
REPO_REF="${REPO_REF:-main}"

PI_CMD="${PI_CMD:-pi}"
PI_ARGS="${PI_ARGS:-}"
PI_LOG_FILE="${PI_LOG_FILE:-${LOG_DIR}/pi.log}"
BOOTSTRAP_LOG_FILE="${BOOTSTRAP_LOG_FILE:-${LOG_DIR}/bootstrap.log}"

timestamp() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

log() {
  echo "[$(timestamp)] $*" | tee -a "${BOOTSTRAP_LOG_FILE}"
}

mkdir -p "${WORKSPACE_ROOT}" "${LOG_DIR}" "${STATE_DIR}"

export BUN_INSTALL="${BUN_INSTALL:-$HOME/.bun}"
export PATH="${BUN_INSTALL}/bin:${HOME}/.nix-profile/bin:/nix/var/nix/profiles/default/bin:${PATH}"

clone_or_update_repo() {
  if [[ -d "${REPO_DIR}/.git" ]]; then
    log "Updating repo in ${REPO_DIR}"
    git -C "${REPO_DIR}" fetch --all --prune
    git -C "${REPO_DIR}" checkout "${REPO_REF}"
    git -C "${REPO_DIR}" pull --ff-only origin "${REPO_REF}" || true
    return
  fi

  if [[ -z "${REPO_URL}" ]]; then
    log "REPO_URL is required when ${REPO_DIR} does not already contain a repo"
    exit 1
  fi

  log "Cloning ${REPO_URL} into ${REPO_DIR}"
  rm -rf "${REPO_DIR}"
  git clone --branch "${REPO_REF}" --single-branch "${REPO_URL}" "${REPO_DIR}"
}

build_repo_command() {
  local pi_path="${BUN_INSTALL}/bin/pi"
  local inner
  inner="export PATH=\"${BUN_INSTALL}/bin:\${PATH}\" && cd '${REPO_DIR}' && exec ${PI_CMD} ${PI_ARGS} 2>&1 | tee -a '${PI_LOG_FILE}'"

  if [[ -f "${REPO_DIR}/flake.nix" ]]; then
    printf "cd '%s' && exec sudo -i nix develop --accept-flake-config --no-pure-eval -c bash -lc %q" \
      "${REPO_DIR}" "${inner}"
  else
    printf "cd '%s' && exec bash -lc %q" \
      "${REPO_DIR}" "${inner}"
  fi
}

ensure_tmux_session() {
  local cmd
  cmd="$(build_repo_command)"

  if tmux has-session -t "${SESSION_NAME}" 2>/dev/null; then
    log "tmux session ${SESSION_NAME} already exists"
    return
  fi

  log "Creating tmux session ${SESSION_NAME}"
  tmux new-session -d -s "${SESSION_NAME}" "${cmd}"
  tmux set-option -t "${SESSION_NAME}" -g remain-on-exit on
  tmux set-option -t "${SESSION_NAME}" -g mouse on
}

write_state() {
  cat > "${STATE_DIR}/session.env" <<EOF
SESSION_NAME=${SESSION_NAME}
REPO_DIR=${REPO_DIR}
REPO_REF=${REPO_REF}
PI_CMD=${PI_CMD}
PI_LOG_FILE=${PI_LOG_FILE}
STARTED_AT=$(timestamp)
EOF
}

main() {
  log "Bootstrapping workspace"
  clone_or_update_repo
  ensure_tmux_session
  write_state
  log "Bootstrap complete"
}

main "$@"
