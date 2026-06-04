# Summary Bot

Telegram bot that posts daily chat summaries. It fetches a summary for each
configured chat from the summary API and posts it back to the Telegram chat,
either on a daily schedule or on demand via the `/summary` command.

## How it works

- **Scheduled** (`run_all`): once per day at `SCHEDULE_TIME`, posts a summary of
  *yesterday* to every chat with `"scheduled": true`.
- **On demand** (`/summary`): posts yesterday's summary to the current chat.
  In a group this is restricted to chat admins (or operators in
  `SUMMARY_ADMIN_IDS`); in a 1-on-1 DM with the bot anyone may run it.

## `chats.json`

A JSON object keyed by chat name. The **key** is used both to look up the
Telegram chat (`@key`) and as the `chat` value sent to the summary API —
unless overridden by the optional fields below.

| Field      | Type    | Required | Description |
|------------|---------|----------|-------------|
| `scheduled`| boolean | yes      | Include this chat in the daily scheduled job. |
| `chat_id`  | integer | no       | Telegram numeric chat id. Use for **private** groups/channels that have no public `@username`. When set, `@key` lookup is skipped and this id is used directly. |
| `api_name` | string  | no       | The `chat` value sent to the summary API. Defaults to the JSON key. Use when the config key should differ from the name the summary site knows. |

The two lookups are independent:

- **Telegram target** → `chat_id` if present, else `@key` via `getChat`.
- **Summary API name** → `api_name` if present, else the JSON key.

### Examples

Public chat — key matches both the Telegram username and the API name:

```json
"DVFinance": { "scheduled": true }
```

Private supergroup — identified by numeric id (derive from a `t.me/c/<id>/...`
message link as `-100<id>`), API name = key:

```json
"DVNorthCarolina": { "scheduled": true, "chat_id": -1001151243206 }
```

Friendly key decoupled from both the Telegram id and the API name:

```json
"NorthCarolina": { "scheduled": true, "chat_id": -1001151243206, "api_name": "DVNorthCarolina" }
```

## Environment variables

| Variable             | Required | Default                                       | Description |
|----------------------|----------|-----------------------------------------------|-------------|
| `TELEGRAM_BOT_TOKEN` | yes      | —                                             | Telegram Bot API token. |
| `SUMMARY_API_KEY`    | yes      | —                                             | API key for the summary endpoint. |
| `SUMMARY_API_URL`    | no       | `https://3gus.ru/tgscrapper/api_summary.php`  | Summary API base URL. |
| `SUMMARY_MODEL`      | no       | `deepseek-chat`                               | Model passed to the summary API. |
| `SCHEDULE_TIME`      | no       | `18:00`                                       | Daily run time (`HH:MM`). |
| `SCHEDULE_TZ`        | no       | `US/Eastern`                                  | Timezone for the schedule and the "yesterday" date. |
| `SUMMARY_ADMIN_IDS`  | no       | *(empty)*                                     | Comma/space-separated Telegram user ids allowed to run `/summary` in any chat, even when not a chat admin. |

## Run

```bash
docker compose up -d summary-bot
```

Secrets (`TELEGRAM_BOT_TOKEN`, `SUMMARY_API_KEY`) are provided via the host
environment / `.env` file referenced by `docker-compose.yml`.
