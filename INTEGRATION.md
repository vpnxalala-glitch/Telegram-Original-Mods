# Точная интеграция (что куда вставлять)

## 1. Снег в чате (ChatActivity)

Файл: `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`

### Добавить поле:
```java
private org.telegram.ui.Components.SnowflakesEffect snowflakesEffect;
```

### В onCreateView (после создания contentView):
```java
if (org.telegram.messenger.Mods.ModsConfig.snowEnabled) {
    snowflakesEffect = new org.telegram.ui.Components.SnowflakesEffect(1);
}
```

### В методе, где рисуется контент чата (обычно в `drawChild` или кастомном View чата):
```java
if (snowflakesEffect != null) {
    snowflakesEffect.onDraw(this, canvas);
}
```

Лучшее место — поверх `chatListView` (в `onDraw` родительского View или через `setWillNotDraw(false)` + override).

---

## 2. Удалённые сообщения

### Место 1: обработка удаления

Файл: `MessagesController.java`  
Ищи обработку `TLRPC.TL_updateDeleteMessages` и `TLRPC.TL_updateDeleteChannelMessages`.

Перед реальным удалением из памяти добавь:

```java
// Пример (подстрой под реальные переменные)
java.util.HashMap<Integer, MessageObject> existing = new java.util.HashMap<>();
for (Integer id : messages) {
    MessageObject msg = dialogMessage.get(dialogId); // или как у тебя хранятся
    // лучше взять из messagesDict
    if (msg != null) existing.put(id, msg);
}
org.telegram.messenger.Mods.DeletedMessagesManager.getInstance(currentAccount)
    .onMessagesDeleted(dialogId, messages, existing);
```

### Место 2: показ в чате

В `ChatMessageCell` или там, где решается, рисовать ли сообщение:

```java
if (org.telegram.messenger.Mods.DeletedMessagesManager.getInstance(currentAccount).isDeleted(messageObject)) {
    // сделать текст серым, добавить иконку корзины, показать "Сообщение удалено" + оригинальный текст
}
```

---

## 3. История правок

Файл: `MessagesController.java`  
Ищи `TLRPC.TL_updateEditMessage` / `TLRPC.TL_updateEditChannelMessage`.

Перед заменой сообщения:

```java
MessageObject oldMsg = ...; // текущее сообщение
org.telegram.messenger.Mods.EditedMessagesManager.getInstance(currentAccount)
    .onMessageEdited(oldMsg, newMessage);
```

При долгом нажатии на сообщение (в `ChatActivity` → `createMenu` или `processSelectedOption`):

```java
if (org.telegram.messenger.Mods.EditedMessagesManager.getInstance(currentAccount).hasHistory(dialog_id, messageObject.getId())) {
    // показать AlertDialog со списком старых версий текста
    java.util.List<org.telegram.messenger.Mods.EditedMessagesManager.EditEntry> history =
        org.telegram.messenger.Mods.EditedMessagesManager.getInstance(currentAccount)
            .getHistory(dialog_id, messageObject.getId());
    // вывести history.get(i).text + дата
}
```

---

## 4. Инициализация (опционально)

В `ApplicationLoader` или `MessagesController` constructor:

```java
org.telegram.messenger.Mods.ModsConfig.load(ApplicationLoader.applicationContext);
```

---

После этих вставок функции начинают работать.
