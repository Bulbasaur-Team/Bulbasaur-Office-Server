# Bulbasaur Office — Server

Бэкенд для игры Bulbasaur Office: авторизация по логину/паролю и лидерборды мини-игр.

## Стек

- Java 21, Spring Boot 4.0, Gradle (version catalog)
- PostgreSQL, Spring Data JPA, Liquibase
- Spring Security + JWT
- MapStruct для маппинга между слоями

## Архитектура (чистая, как в сервисах WDM)

- **domain** — доменные DTO и enum (`Player`, `GameId`, `Direction`, `LeaderboardRow`). Доступны из любого слоя.
- **usecase** — бизнес-логика (`AuthService`, `LeaderboardService`) и порты (`port/out/*`). Не знает про Spring и infra, только про порты.
- **infra** — технические детали: REST-контроллеры, JPA, security, конфигурация. Реализует порты адаптерами.

Каждый слой имеет свои request/response DTO, маппинг — через MapStruct.

## API

Все ответы — JSON. Токен передаётся в заголовке `Authorization: Bearer <token>`.

| Метод | Путь | Доступ | Тело | Ответ |
|-------|------|--------|------|-------|
| POST | `/api/auth/register` | публичный | `{login, password}` | `{token, login}` |
| POST | `/api/auth/login` | публичный | `{login, password}` | `{token, login}` |
| POST | `/api/leaderboard/{game}` | по токену | `{value}` | лидерборд |
| GET | `/api/leaderboard/{game}?limit=20` | по токену | — | лидерборд |
| GET | `/actuator/health` | публичный | — | статус |

`{game}` — код игры: `bulbajump`, `bulbapacker`, `bulbaparking`, `bulbaracing`, `bulbaguess`, `bulbawordle`.

Ответ лидерборда:
```json
{
  "entries": [{ "rank": 1, "login": "ash", "value": 42, "you": false }],
  "you":     { "rank": 7, "login": "misty", "value": 12, "you": true }
}
```
`entries` — топ по правилу игры (очки/слова — по убыванию, время/попытки — по возрастанию). `you` — строка текущего игрока (даже если не в топе); null, если результата ещё нет. Хранится один — лучший — результат на игрока.

## Переменные окружения

| Переменная | Назначение | Дефолт                                   |
|------------|-----------|------------------------------------------|
| `SPRING_DATASOURCE_URL` | JDBC URL Postgres | `jdbc:postgresql://localhost:5499/bulba` |
| `SPRING_DATASOURCE_USERNAME` | пользователь БД | `bulba`                                  |
| `SPRING_DATASOURCE_PASSWORD` | пароль БД | `bulba`                                  |
| `APP_JWT_SECRET` | секрет подписи JWT (мин. 32 символа) | dev-заглушка                             |
| `APP_JWT_TTL` | срок жизни токена (ISO-8601) | `P7D`                                    |
| `APP_CORS_ORIGIN` | origin фронта для CORS | `http://localhost:5173`                  |

## Запуск локально

Весь стек в Docker:
```bash
cp .env.example .env   # при желании поправь
docker compose up --build
```
API поднимется на `http://localhost:8080`.

Только приложение (Postgres — свой): в IntelliJ импортируй как Gradle-проект и запусти `OfficeApplication`, либо `./gradlew bootRun` (сгенерируй wrapper командой `gradle wrapper`, если его ещё нет).

## Деплой на VPS

Образ собирается `Dockerfile` (multi-stage, Gradle → JRE). Пуш в GHCR:
```bash
docker build -t ghcr.io/bulbasaur-team/bulba-backend:latest .
docker push ghcr.io/bulbasaur-team/bulba-backend:latest
```
На сервере — compose из инфра-гайда (Caddy + этот образ + Postgres), Caddyfile проксирует `api.bulba-office.online` → `app:8080`.
