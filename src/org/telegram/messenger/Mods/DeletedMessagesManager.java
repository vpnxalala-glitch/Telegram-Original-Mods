package org.telegram.messenger.Mods;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер удалённых сообщений.
 * Хранит копии сообщений, которые были удалены.
 * Только добавление функций — подключай через хуки.
 */
public class DeletedMessagesManager {

    private static final Map<Integer, DeletedMessagesManager> instances = new ConcurrentHashMap<>();

    public static DeletedMessagesManager getInstance(int account) {
        return instances.computeIfAbsent(account, DeletedMessagesManager::new);
    }

    private final int currentAccount;

    // dialogId -> list of deleted MessageObject
    private final Map<Long, List<MessageObject>> deletedByDialog = new ConcurrentHashMap<>();

    // messageId -> MessageObject (быстрый поиск)
    private final Map<String, MessageObject> deletedIndex = new ConcurrentHashMap<>();

    private DeletedMessagesManager(int account) {
        this.currentAccount = account;
    }

    private String key(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    /**
     * Вызывать при получении UpdateDeleteMessages / UpdateDeleteChannelMessages
     */
    public void onMessagesDeleted(long dialogId, ArrayList<Integer> messageIds, Map<Integer, MessageObject> existingMessages) {
        if (!ModsConfig.deletedMessagesEnabled || messageIds == null || messageIds.isEmpty()) {
            return;
        }

        List<MessageObject> list = deletedByDialog.computeIfAbsent(dialogId, k -> new ArrayList<>());

        for (Integer id : messageIds) {
            if (id == null) continue;
            MessageObject msg = existingMessages != null ? existingMessages.get(id) : null;
            if (msg == null) continue;

            // Сохраняем копию
            MessageObject copy = msg; // в реальном коде лучше клонировать
            list.add(0, copy);
            deletedIndex.put(key(dialogId, id), copy);

            // Ограничение
            while (list.size() > ModsConfig.maxDeletedPerDialog) {
                MessageObject removed = list.remove(list.size() - 1);
                if (removed != null) {
                    deletedIndex.remove(key(dialogId, removed.getId()));
                }
            }
        }
    }

    public boolean isDeleted(long dialogId, int messageId) {
        return deletedIndex.containsKey(key(dialogId, messageId));
    }

    public boolean isDeleted(MessageObject messageObject) {
        if (messageObject == null) return false;
        return isDeleted(messageObject.getDialogId(), messageObject.getId());
    }

    public MessageObject getDeleted(long dialogId, int messageId) {
        return deletedIndex.get(key(dialogId, messageId));
    }

    public List<MessageObject> getDeletedForDialog(long dialogId) {
        List<MessageObject> list = deletedByDialog.get(dialogId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public void clearDialog(long dialogId) {
        List<MessageObject> list = deletedByDialog.remove(dialogId);
        if (list != null) {
            for (MessageObject msg : list) {
                deletedIndex.remove(key(dialogId, msg.getId()));
            }
        }
    }
}
