import os
import time
import logging
from datetime import datetime, timedelta, timezone

import requests
import schedule

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger(__name__)

TOKEN = os.environ["TELEGRAM_BOT_TOKEN"]
API_KEY = os.environ["SUMMARY_API_KEY"]
API_URL = os.environ.get("SUMMARY_API_URL", "https://3gus.ru/tgscrapper/api_summary.php")
MODEL = os.environ.get("SUMMARY_MODEL", "deepseek-chat")
SCHEDULE_TIME = os.environ.get("SCHEDULE_TIME", "08:00")

TG = f"https://api.telegram.org/bot{TOKEN}"

CHATS = [
    "DV_Business",
    "DV_IT",
    "DVFinance",
    "DVHomeowners",
    "DVNewLife",
    "DVOfftop",
    "DVPolitics",
    "DVTrucking",
    "RuAmericaGreenCard",
]

last_update_id = 0


def fetch_summary(chat: str, date: str) -> str | None:
    resp = requests.get(API_URL, params={"date": date, "chat": chat, "model": MODEL, "key": API_KEY}, timeout=120)
    resp.raise_for_status()
    text = resp.text.strip()
    if not text or "нет значимых событий" in text.lower():
        return None
    return text


def send_message(chat_id: int, text: str) -> None:
    try:
        resp = requests.post(f"{TG}/sendMessage", json={"chat_id": chat_id, "text": text, "parse_mode": "Markdown", "disable_web_page_preview": True}, timeout=30)
        resp.raise_for_status()
    except requests.RequestException:
        log.warning("Markdown send failed for %s, retrying as plain text", chat_id)
        resp = requests.post(f"{TG}/sendMessage", json={"chat_id": chat_id, "text": text, "disable_web_page_preview": True}, timeout=30)
        resp.raise_for_status()


def resolve_chat_name(chat_id: int) -> str | None:
    """Match a Telegram chat_id to one of the known API chat names."""
    for name in CHATS:
        try:
            r = requests.get(f"{TG}/getChat", params={"chat_id": f"@{name}"}, timeout=10)
            if r.status_code == 200 and r.json()["result"]["id"] == chat_id:
                return name
        except Exception:
            continue
    return None


def run_for_chat(chat_id: int, chat_name: str) -> None:
    """Fetch and post summary for a single chat."""
    yesterday = (datetime.now(timezone.utc) - timedelta(days=1)).strftime("%Y-%m-%d")
    log.info("Running summary for @%s date=%s", chat_name, yesterday)
    summary = fetch_summary(chat_name, yesterday)
    if summary is None:
        log.info("No meaningful summary for @%s", chat_name)
        send_message(chat_id, "No summary available for yesterday.")
        return
    send_message(chat_id, summary)
    log.info("Posted summary to @%s", chat_name)


def run_all() -> None:
    """Scheduled job: post summaries to all chats the bot is in."""
    yesterday = (datetime.now(timezone.utc) - timedelta(days=1)).strftime("%Y-%m-%d")
    log.info("Running scheduled summary job for date=%s", yesterday)

    for name in CHATS:
        try:
            r = requests.get(f"{TG}/getChat", params={"chat_id": f"@{name}"}, timeout=10)
            if r.status_code != 200:
                log.info("Bot not in @%s, skipping", name)
                continue
            chat_id = r.json()["result"]["id"]
        except Exception:
            log.info("Cannot resolve @%s, skipping", name)
            continue

        try:
            summary = fetch_summary(name, yesterday)
            if summary is None:
                log.info("No meaningful summary for @%s, skipping", name)
                continue
            send_message(chat_id, summary)
            log.info("Posted summary to @%s", name)
        except Exception:
            log.exception("Failed to process @%s", name)


def is_chat_admin(chat_id: int, user_id: int) -> bool:
    try:
        resp = requests.get(f"{TG}/getChatMember", params={"chat_id": chat_id, "user_id": user_id}, timeout=10)
        if resp.status_code != 200:
            return False
        status = resp.json().get("result", {}).get("status", "")
        return status in ("creator", "administrator")
    except Exception:
        return False


def poll_commands() -> None:
    global last_update_id
    try:
        resp = requests.get(f"{TG}/getUpdates", params={"offset": last_update_id + 1, "timeout": 0}, timeout=10)
        if resp.status_code != 200:
            return
        updates = resp.json().get("result", [])
    except Exception:
        return

    for update in updates:
        last_update_id = update["update_id"]
        msg = update.get("message")
        if not msg:
            continue
        text = msg.get("text", "")
        user_id = msg.get("from", {}).get("id")
        chat_id = msg["chat"]["id"]

        if text.startswith("/summary"):
            is_private = msg["chat"]["type"] == "private"
            if not is_private and not is_chat_admin(chat_id, user_id):
                send_message(chat_id, "Access denied. Admins only.")
                log.warning("Unauthorized /summary from user_id=%s in chat_id=%s", user_id, chat_id)
                continue
            chat_name = resolve_chat_name(chat_id)
            if not chat_name:
                send_message(chat_id, "This chat is not in the known list.")
                log.info("/summary in unknown chat_id=%s", chat_id)
                continue
            log.info("/summary triggered by user_id=%s in @%s", user_id, chat_name)
            run_for_chat(chat_id, chat_name)


if __name__ == "__main__":
    log.info("Scheduling summary job at %s UTC daily", SCHEDULE_TIME)
    schedule.every().day.at(SCHEDULE_TIME, "UTC").do(run_all)

    while True:
        schedule.run_pending()
        poll_commands()
        time.sleep(2)
