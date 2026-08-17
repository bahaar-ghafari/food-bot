#!/usr/bin/env bash
# Back up FoodBot's production data (data/foods.json, data/languages.json)
# to timestamped, gzipped tarballs, keeping the last N days.
#
# Run on the VPS (manually or via cron):
#   bash /opt/foodbot/scripts/backup.sh
#
# Tunables (env vars, with defaults):
#   DATA_DIR        directory holding the JSON files (default: /opt/foodbot/data)
#   BACKUP_DIR      where snapshots are written        (default: /opt/foodbot/backups)
#   RETENTION_DAYS  delete snapshots older than this    (default: 14)
set -euo pipefail

DATA_DIR="${DATA_DIR:-/opt/foodbot/data}"
BACKUP_DIR="${BACKUP_DIR:-/opt/foodbot/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="foodbot-data-${STAMP}.tar.gz"

mkdir -p "$BACKUP_DIR"

tar -czf "${BACKUP_DIR}/${OUT}" -C "$(dirname "$DATA_DIR")" "$(basename "$DATA_DIR")"

echo "$(date -u +%FT%TZ) backup written: ${BACKUP_DIR}/${OUT} ($(du -h "${BACKUP_DIR}/${OUT}" | cut -f1))"

find "$BACKUP_DIR" -maxdepth 1 -name 'foodbot-data-*.tar.gz' -type f -mtime +"$RETENTION_DAYS" -print -delete \
  | sed 's/^/pruned: /' || true
