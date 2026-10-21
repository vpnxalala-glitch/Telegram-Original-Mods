# Release v1.0.0

## Что внутри

- Падающий снег в чатах (постоянно)
- Удалённые сообщения (сохранение + показ)
- История изменённых сообщений

## Как сделать официальный Release (1 клик)

1. Открой: https://github.com/vpnxalala-glitch/Telegram-Original-Mods/releases/new
2. Tag: `v1.0.0`
3. Title: `v1.0.0 — Snow + Deleted + Edited`
4. Описание скопируй из этого файла
5. Create release

(Инструменты не позволяют создать Release автоматически — только так.)

## Установка

Скопируй файлы из `src/` в оригинальный Telegram Android и добавь хуки из `INTEGRATION.md`.

## Файлы релиза

- `src/org/telegram/ui/Components/SnowflakesEffect.java`
- `src/org/telegram/messenger/Mods/ModsConfig.java`
- `src/org/telegram/messenger/Mods/DeletedMessagesManager.java`
- `src/org/telegram/messenger/Mods/EditedMessagesManager.java`
- `INTEGRATION.md` — куда вставлять код
