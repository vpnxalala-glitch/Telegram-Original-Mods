# Минимальные хуки (что добавить в оригинал)

## 1. Снег

В `ChatActivity.java` / `DialogsActivity.java` / любом View, где нужен снег:

```java
// поле
private SnowflakesEffect snowflakesEffect;

// в onCreateView / конструкторе
snowflakesEffect = new SnowflakesEffect(1); // 1 = плотный снег в чате

// в onDraw или drawChild
if (snowflakesEffect != null) {
    snowflakesEffect.onDraw(this, canvas);
}
```

Также можно добавить в `Theme` или `ActionBarLayout` для глобального эффекта.

## 2. Удалённые сообщения

В `MessagesController.java` или там, где обрабатывается `TLRPC.UpdateDeleteMessages`:

```java
// при получении удаления
DeletedMessagesManager.getInstance(currentAccount).onMessagesDeleted(dialogId, messages);
```

В `ChatActivity` при отрисовке сообщения проверяй:
```java
if (DeletedMessagesManager.getInstance(currentAccount).isDeleted(messageObject)) {
    // рисуй как удалённое (серый текст, иконка корзины и т.д.)
}
```

## 3. Изменённые сообщения

В обработке `TLRPC.UpdateEditMessage` / `UpdateEditChannelMessage`:

```java
EditedMessagesManager.getInstance(currentAccount).onMessageEdited(oldMessage, newMessage);
```

При долгом нажатии на сообщение — показывать историю правок из `EditedMessagesManager`.
