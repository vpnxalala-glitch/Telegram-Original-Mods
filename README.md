# Telegram Original Mods

Минимальные добавления **только функций** в оригинальный Telegram Android (DrKLO/Telegram).

## Что добавлено

1. **Анимация падающего снега** в чатах, списках диалогов и т.д. (постоянно, не только на Новый год)
2. **Удалённые сообщения** — сохранение и показ удалённых сообщений
3. **Изменённые сообщения** — история правок

Всё сделано максимально минимально: новые классы + небольшие хуки.

## Как использовать

1. Клонируй оригинальный репозиторий:
   ```bash
   git clone --recursive https://github.com/DrKLO/Telegram.git
   cd Telegram
   ```

2. Скопируй файлы из папки `src/` этого репозитория в соответствующие места:
   - `src/org/telegram/ui/Components/SnowflakesEffect.java` → `TMessagesProj/src/main/java/org/telegram/ui/Components/`
   - `src/org/telegram/messenger/Mods/` → `TMessagesProj/src/main/java/org/telegram/messenger/Mods/`

3. Добавь минимальные хуки (смотри `HOOKS.md`)

4. Собери как обычно.

## Структура

```
src/
├── org/telegram/ui/Components/
│   └── SnowflakesEffect.java          # анимация снега
└── org/telegram/messenger/Mods/
    ├── DeletedMessagesManager.java    # удалённые сообщения
    ├── EditedMessagesManager.java     # история правок
    └── ModsConfig.java                # настройки
```

## Настройки

В `ModsConfig` можно включать/выключать функции.

---
Только добавление функций. Оригинальный код почти не трогается.
