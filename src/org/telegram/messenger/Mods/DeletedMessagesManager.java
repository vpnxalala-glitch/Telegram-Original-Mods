package org.telegram.messenger.Mods;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Полноценный менеджер удалённых сообщений.
 * Хранит MessageObject в памяти + пытается сериализовать базовую информацию.
 */
public class DeletedMessagesManager {

    private static final Map<Integer, DeletedMessagesManager> instances = new ConcurrentHashMap<>();

    public static DeletedMessagesManager getInstance(int account) {
        return instances.computeIfAbsent(account, DeletedMessagesManager::new);
    }

    private final int currentAccount;

    // dialogId -> list (новые сверху)
    private final Map<Long, List<SavedDeleted>> deletedByDialog = new ConcurrentHashMap<>();

    // dialogId_msgId -> SavedDeleted
    private final Map<String, SavedDeleted> index = new ConcurrentHashMap<>();

    public static class SavedDeleted {
        public final long dialogId;
        public final int messageId;
        public final int fromId;
        public final String text;
        public final long date;
        public final boolean isOut;
        public final String mediaType; // "photo", "video", "document", null

        public SavedDeleted(long dialogId, int messageId, int fromId, String text, long date, boolean isOut, String mediaType) {
            this.dialogId = dialogId;
            this.messageId = messageId;
            this.fromId = fromId;
            this.text = text;
            this.date = date;
            this.isOut = isOut;
            this.mediaType = mediaType;
        }
    }

    private DeletedMessagesManager(int account) {
        this.currentAccount = account;
        loadFromPrefs();
    }

    private String key(long dialogId, int messageId) {
        return dialogId + "_" + messageId;
    }

    /**
     * Главный метод — вызывать при удалении сообщений.
     */
    public void onMessagesDeleted(long dialogId, ArrayList<Integer> messageIds, Map<Integer, MessageObject> existingMessages) {
        if (!ModsConfig.deletedMessagesEnabled || messageIds == null || messageIds.isEmpty()) {
            return;
        }

        List<SavedDeleted> list = deletedByDialog.computeIfAbsent(dialogId, k -> new ArrayList<>());

        for (Integer id : messageIds) {
            if (id == null) continue;

            MessageObject msg = existingMessages != null ? existingMessages.get(id) : null;
            if (msg == null || msg.messageOwner == null) continue;

            TLRPC.Message m = msg.messageOwner;
            String text = m.message;
            int fromId = 0;
            if (m.from_id != null) {
                if (m.from_id instanceof TLRPC.TL_peerUser) {
                    fromId = (int) ((TLRPC.TL_peerUser) m.from_id).user_id;
                } else if (m.from_id instanceof TLRPC.TL_peerChannel) {
                    fromId = (int) ((TLRPC.TL_peerChannel) m.from_id).channel_id;
                }
            }

            String mediaType = null;
            if (m.media != null) {
                if (m.media instanceof TLRPC.TL_messageMediaPhoto) mediaType = "photo";
                else if (m.media instanceof TLRPC.TL_messageMediaDocument) mediaType = "document";
                else if (m.media instanceof TLRPC.TL_messageMediaGeo) mediaType = "geo";
                else mediaType = "media";
            }

            SavedDeleted saved = new SavedDeleted(
                    dialogId,
                    id,
                    fromId,
                    text,
                    m.date,
                    msg.isOut(),
                    mediaType
            );

            // не дублируем
            if (index.containsKey(key(dialogId, id))) continue;

            list.add(0, saved);
            index.put(key(dialogId, id), saved);

            while (list.size() > ModsConfig.maxDeletedPerDialog) {
                SavedDeleted removed = list.remove(list.size() - 1);
                index.remove(key(dialogId, removed.messageId));
            }
        }

        saveToPrefs();
    }

    public boolean isDeleted(long dialogId, int messageId) {
        return index.containsKey(key(dialogId, messageId));
    }

    public boolean isDeleted(MessageObject messageObject) {
        if (messageObject == null) return false;
        return isDeleted(messageObject.getDialogId(), messageObject.getId());
    }

    public SavedDeleted getDeleted(long dialogId, int messageId) {
        return index.get(key(dialogId, messageId));
    }

    public List<SavedDeleted> getDeletedForDialog(long dialogId) {
        List<SavedDeleted> list = deletedByDialog.get(dialogId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public void clearDialog(long dialogId) {
        List<SavedDeleted> list = deletedByDialog.remove(dialogId);
        if (list != null) {
            for (SavedDeleted s : list) {
                index.remove(key(dialogId, s.messageId));
            }
        }
        saveToPrefs();
    }

    // ---------- простое хранение (текст + мета) ----------

    private void saveToPrefs() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;

            SharedPreferences.Editor editor = ctx.getSharedPreferences("mods_deleted_" + currentAccount, Context.MODE_PRIVATE).edit();
            editor.clear();

            int total = 0;
            for (Map.Entry<Long, List<SavedDeleted>> e : deletedByDialog.entrySet()) {
                long dialogId = e.getKey();
                List<SavedDeleted> list = e.getValue();
                for (int i = 0; i < list.size() && total < 2000; i++) {
                    SavedDeleted s = list.get(i);
                    String k = "d_" + dialogId + "_" + s.messageId;
                    String v = s.fromId + "|" + s.date + "|" + (s.isOut ? "1" : "0") + "|" +
                            (s.mediaType != null ? s.mediaType : "") + "|" +
                            (s.text != null ? s.text.replace("|", " ") : "");
                    editor.putString(k, v);
                    total++;
                }
            }
            editor.apply();
        } catch (Throwable ignore) {}
    }

    private void loadFromPrefs() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;

            SharedPreferences prefs = ctx.getSharedPreferences("mods_deleted_" + currentAccount, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            if (all == null) return;

            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String k = entry.getKey();
                if (!k.startsWith("d_")) continue;
                String[] parts = k.substring(2).split("_");
                if (parts.length < 2) continue;

                long dialogId = Long.parseLong(parts[0]);
                int messageId = Integer.parseInt(parts[1]);
                String val = String.valueOf(entry.getValue());
                String[] v = val.split("\\|", 5);
                if (v.length < 5) continue;

                int fromId = Integer.parseInt(v[0]);
                long date = Long.parseLong(v[1]);
                boolean isOut = "1".equals(v[2]);
                String mediaType = v[3].isEmpty() ? null : v[3];
                String text = v[4];

                SavedDeleted saved = new SavedDeleted(dialogId, messageId, fromId, text, date, isOut, mediaType);
                List<SavedDeleted> list = deletedByDialog.computeIfAbsent(dialogId, x -> new ArrayList<>());
                list.add(saved);
                index.put(key(dialogId, messageId), saved);
            }
        } catch (Throwable ignore) {}
    }
}
