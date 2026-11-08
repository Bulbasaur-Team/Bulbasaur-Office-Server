#!/bin/bash
# Это команда для локальной машины: забирает дампы с сервера в ~/Backups/bulba.
# Нужен SSH-хост bulba из ~/.ssh/config. Запускать руками, расписания нет.

set -euo pipefail
mkdir -p "$HOME/Backups/bulba"
rsync -a 'bulba:/opt/bulba/backups/bulba-*.dump' "$HOME/Backups/bulba/"
ls -lh "$HOME/Backups/bulba"
