package org.telegram.messenger.Mods;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * История правок сообщений.
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
        public final String mediaType;

        public EditEntry(String text, long date, String mediaType) {
            this.text = text;
            this.date = date;
            this.mediaType = mediaType;
        }
    }

    // key = dialogId_messageId
    private final Map<String, List<EditEntry>> history = new ConcurrentHashMap<>();

    private EditedMessagesManager(int account) {
        this.currentAccount = account;
        loadFromPrefs();
    }

    private String key(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    /**
     * Вызывать ПЕРЕД применением новой версии сообщения.
     */
    public void onMessageEdited(MessageObject oldMessage, TLRPC.Message newMessage) {
        if (!ModsConfig.editedMessagesEnabled || oldMessage == null || oldMessage.messageOwner == null) {
            return;
        }

        long dialogId = oldMessage.getDialogId();
        int messageId = oldMessage.getId();

        String oldText = oldMessage.messageOwner.message;
        String mediaType = null;
        if (oldMessage.messageOwner.media != null) {
            if (oldMessage.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto) mediaType = "photo";
            else if (oldMessage.messageOwner.media instanceof TLRPC.TL_messageMediaDocument) mediaType = "document";
            else mediaType = "media";
        }

        // если текст не изменился и медиа то же — пропускаем
        if (newMessage != null && oldText != null && oldText.equals(newMessage.message) && mediaType == null) {
            return;
        }

        String k = key(dialogId, messageId);
        List<EditEntry> list = history.computeIfAbsent(k, x -> new ArrayList<>());

        list.add(new EditEntry(oldText, System.currentTimeMillis() / 1000, mediaType));

        while (list.size() > ModsConfig.maxEditHistory) {
            list.remove(0);
        }

        saveToPrefs();
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
        saveToPrefs();
    }

    private void saveToPrefs() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;

            SharedPreferences.Editor editor = ctx.getSharedPreferences("mods_edited_" + currentAccount, Context.MODE_PRIVATE).edit();
            editor.clear();

            int total = 0;
            for (Map.Entry<String, List<EditEntry>> e : history.entrySet()) {
                String k = e.getKey();
                List<EditEntry> list = e.getValue();
                StringBuilder sb = new StringBuilder();
                for (EditEntry entry : list) {
                    if (sb.length() > 0) sb.append(";;");
                    sb.append(entry.date).append("|").append(entry.mediaType != null ? entry.mediaType : "")
                      .append("|").append(entry.text != null ? entry.text.replace(";;", " ").replace("|", " ") : "");
                }
                editor.putString("e_" + k, sb.toString());
                total++;
                if (total > 1500) break;
            }
            editor.apply();
        } catch (Throwable ignore) {}
    }

    private void loadFromPrefs() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;

            SharedPreferences prefs = ctx.getSharedPreferences("mods_edited_" + currentAccount, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            if (all == null) return;

            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String k = entry.getKey();
                if (!k.startsWith("e_")) continue;
                String realKey = k.substring(2);
                String val = String.valueOf(entry.getValue());
                String[] parts = val.split(";;");

                List<EditEntry> list = new ArrayList<>();
                for (String p : parts) {
                    String[] v = p.split("\\|", 3);
                    if (v.length < 3) continue;
                    long date = Long.parseLong(v[0]);
                    String mediaType = v[1].isEmpty() ? null : v[1];
                    String text = v[2];
                    list.add(new EditEntry(text, date, mediaType));
                }
                if (!list.isEmpty()) {
                    history.put(realKey, list);
                }
            }
        } catch (Throwable ignore) {}
    }
}
