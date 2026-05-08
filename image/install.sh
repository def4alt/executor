#!/usr/bin/env bash
set -euo pipefail

export BUN_INSTALL="${BUN_INSTALL:-$HOME/.bun}"
export PATH="${BUN_INSTALL}/bin:${PATH}"

if command -v pi >/dev/null 2>&1; then
  echo "pi already installed: $(command -v pi)"
  exit 0
fi

if ! command -v bun >/dev/null 2>&1; then
  echo "bun is not installed or not in PATH" >&2
  exit 1
fi

echo "Installing Pi with Bun..."
bun add -g @earendil-works/pi-coding-agent

if ! command -v pi >/dev/null 2>&1; then
  echo "Pi installation failed: 'pi' not in PATH" >&2
  echo "Current PATH: ${PATH}" >&2
  exit 1
fi

echo "Pi installed at: $(command -v pi)"
pi --help >/dev/null 2>&1 || true
