#!/usr/bin/env bash
# Sync this repo to the production VPS and rebuild/restart the bot.
#
# IMPORTANT: uses `rsync --delete` to mirror the local tree onto the server.
# Production's live data (data/foods.json, data/languages.json) is NOT part
# of the local repo (it only exists bind-mounted on the server), so it MUST
# stay excluded below - dropping these excludes wiped production data once
# already (2026-08-17). Do not remove them without reading CLAUDE.md first.
#
# Usage:
#   VPS_ROOT_PASSWORD=... bash scripts/deploy.sh          # dry run (default)
#   VPS_ROOT_PASSWORD=... bash scripts/deploy.sh --apply   # actually deploy
set -euo pipefail

HOST="${DEPLOY_HOST:-178.105.231.152}"
REMOTE_DIR="${DEPLOY_DIR:-/opt/foodbot}"
APPLY=false
[[ "${1:-}" == "--apply" ]] && APPLY=true

if [[ -z "${VPS_ROOT_PASSWORD:-}" ]]; then
  echo "VPS_ROOT_PASSWORD must be set (e.g. \`source .env\` first)." >&2
  exit 1
fi

RSYNC_OPTS=(-az --delete
  --exclude='.git' --exclude='target' --exclude='.env' --exclude='.env.example'
  --exclude='*.patch' --exclude='data/' --exclude='backups/'
  --exclude='/foods.json' --exclude='/languages.json'
)
$APPLY || RSYNC_OPTS+=(--dry-run -v)

echo "== rsync $( $APPLY && echo '(APPLYING)' || echo '(DRY RUN - nothing will change, pass --apply to deploy for real)') =="
sshpass -p "$VPS_ROOT_PASSWORD" rsync "${RSYNC_OPTS[@]}" \
  -e "ssh -o StrictHostKeyChecking=accept-new" \
  ./ "root@${HOST}:${REMOTE_DIR}/"

if $APPLY; then
  echo "== rebuilding and restarting on server =="
  sshpass -p "$VPS_ROOT_PASSWORD" ssh "root@${HOST}" \
    "cd '${REMOTE_DIR}' && docker compose up -d --build && docker compose ps"
else
  echo "Dry run only - re-run with --apply to actually deploy."
fi
