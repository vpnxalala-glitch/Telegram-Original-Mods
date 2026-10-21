package org.telegram.messenger.Mods;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Настройки модов. Сохраняются в SharedPreferences.
 */
public class ModsConfig {

    public static boolean snowEnabled = true;
    public static boolean deletedMessagesEnabled = true;
    public static boolean editedMessagesEnabled = true;

    public static int maxDeletedPerDialog = 300;
    public static int maxEditHistory = 30;

    private static final String PREF = "telegram_mods_config";

    public static void load(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        snowEnabled = prefs.getBoolean("snowEnabled", true);
        deletedMessagesEnabled = prefs.getBoolean("deletedMessagesEnabled", true);
        editedMessagesEnabled = prefs.getBoolean("editedMessagesEnabled", true);
        maxDeletedPerDialog = prefs.getInt("maxDeletedPerDialog", 300);
        maxEditHistory = prefs.getInt("maxEditHistory", 30);
    }

    public static void save(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("snowEnabled", snowEnabled)
                .putBoolean("deletedMessagesEnabled", deletedMessagesEnabled)
                .putBoolean("editedMessagesEnabled", editedMessagesEnabled)
                .putInt("maxDeletedPerDialog", maxDeletedPerDialog)
                .putInt("maxEditHistory", maxEditHistory)
                .apply();
    }
}
