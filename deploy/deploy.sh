#!/bin/bash
# Образец для VPS: скопировать в /opt/bulba/deploy.sh и запускать там руками.
# Скрипт забирает ветку main с GitHub и пересобирает контейнеры.
# Сам себя и соседние файлы из /opt/bulba он не обновляет: после правки
# в репозитории копию на сервере заменяют отдельно.

set -euo pipefail
cd /opt/bulba

REPO=https://github.com/Bulbasaur-Team/Bulbasaur-Office-Server.git
BRANCH=main

if [ ! -d server/.git ]; then
  git clone --branch "$BRANCH" "$REPO" server
else
  git -C server fetch origin
  git -C server checkout "$BRANCH"
  git -C server pull --ff-only origin "$BRANCH"
fi

docker compose up -d --build
git -C server log -1 --oneline
curl -fsS http://127.0.0.1:8080/actuator/health
echo
