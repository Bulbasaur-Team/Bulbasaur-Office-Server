#!/bin/bash
# Образец для VPS: скопировать в /opt/bulba/backup.sh.
# Раз в запуск снимает дамп Postgres и оставляет только 5 последних файлов.
# На сервере его вызывает cron. На ноутбук дампы сами не уезжают.

set -euo pipefail
export PATH=/usr/bin:/bin
cd /opt/bulba
mkdir -p backups
tmp=$(mktemp backups/bulba.XXXXXX)
trap 'rm -f "$tmp"' EXIT
docker compose exec -T postgres pg_dump -U bulba -Fc bulba > "$tmp"
mv "$tmp" "backups/bulba-$(date +%F).dump"
trap - EXIT
ls -1t backups/bulba-*.dump | tail -n +6 | xargs -r rm --
