# Telegram Original Mods

Рабочие дополнения к **оригинальному** Telegram Android (https://github.com/DrKLO/Telegram).

## Реализовано

### 1. Падающий снег в чатах
- Класс `SnowflakesEffect`
- Работает постоянно (не только 31 декабря)
- `viewType = 1` — плотный снег для чатов

### 2. Удалённые сообщения
- `DeletedMessagesManager` — сохраняет сообщения при удалении
- Хранение в памяти + простое сохранение
- Методы: `onMessagesDeleted`, `isDeleted`, `getDeleted`, `getDeletedForDialog`

### 3. История правок
- `EditedMessagesManager` — сохраняет предыдущие версии текста
- `onMessageEdited`, `getHistory`, `hasHistory`

## Как поставить (пошагово)

1. Клонируй оригинал:
```bash
git clone --recursive https://github.com/DrKLO/Telegram.git
cd Telegram
```

2. Скопируй файлы:
```bash
cp -r /path/to/Telegram-Original-Mods/src/org/telegram/ui/Components/SnowflakesEffect.java TMessagesProj/src/main/java/org/telegram/ui/Components/
mkdir -p TMessagesProj/src/main/java/org/telegram/messenger/Mods
cp -r /path/to/Telegram-Original-Mods/src/org/telegram/messenger/Mods/* TMessagesProj/src/main/java/org/telegram/messenger/Mods/
```

3. Добавь хуки из файла **INTEGRATION.md** (конкретные места и код).

4. Собери проект как обычно.

## Важно

Это **не** готовый APK и не полный форк.  
Это готовые классы + точные места, куда вставить 5–15 строк в оригинальный код.  
Без этих хуков менеджеры сами ничего не увидят.
