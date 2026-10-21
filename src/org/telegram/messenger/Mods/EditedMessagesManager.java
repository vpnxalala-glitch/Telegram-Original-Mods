package org.telegram.messenger.Mods;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер истории правок сообщений.
 * Сохраняет предыдущие версии текста/медиа.
 */
public class EditedMessagesManager {

    private static final Map<Integer, EditedMessagesManager> instances = new ConcurrentHashMap<>();

    public static EditedMessagesManager getInstance(int account) {
        return instances.computeIfAbsent(account, EditedMessagesManager::new);
    }

    private final int currentAccount;

    public static class EditEntry {
        public final String text;
        public final long date;
        public final TLRPC.MessageMedia media; // может быть null

        public EditEntry(String text, long date, TLRPC.MessageMedia media) {
            this.text = text;
            this.date = date;
            this.media = media;
        }
    }

    // key = dialogId_messageId -> список правок (от старых к новым)
    private final Map<String, List<EditEntry>> history = new ConcurrentHashMap<>();

    private EditedMessagesManager(int account) {
        this.currentAccount = account;
    }

    private String key(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    /**
     * Вызывать перед применением правки (когда пришёл UpdateEditMessage)
     * @param oldMessage старое сообщение (до правки)
     * @param newMessage новое сообщение
     */
    public void onMessageEdited(MessageObject oldMessage, TLRPC.Message newMessage) {
        if (!ModsConfig.editedMessagesEnabled || oldMessage == null || newMessage == null) {
            return;
        }

        long dialogId = oldMessage.getDialogId();
        int messageId = oldMessage.getId();

        String k = key(dialogId, messageId);
        List<EditEntry> list = history.computeIfAbsent(k, x -> new ArrayList<>());

        // Сохраняем старую версию
        String oldText = oldMessage.messageOwner != null ? oldMessage.messageOwner.message : null;
        TLRPC.MessageMedia oldMedia = oldMessage.messageOwner != null ? oldMessage.messageOwner.media : null;

        list.add(new EditEntry(oldText, System.currentTimeMillis() / 1000, oldMedia));

        // Ограничение глубины
        while (list.size() > ModsConfig.maxEditHistory) {
            list.remove(0);
        }
    }

    public List<EditEntry> getHistory(long dialogId, int messageId) {
        List<EditEntry> list = history.get(key(dialogId, messageId));
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public boolean hasHistory(long dialogId, int messageId) {
        List<EditEntry> list = history.get(key(dialogId, messageId));
        return list != null && !list.isEmpty();
    }

    public void clear(long dialogId, int messageId) {
        history.remove(key(dialogId, messageId));
    }
}
