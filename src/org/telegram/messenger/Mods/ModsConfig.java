package org.telegram.messenger.Mods;

/**
 * Простые настройки модов.
 * Можно потом привязать к SharedPreferences.
 */
public class ModsConfig {

    public static boolean snowEnabled = true;
    public static boolean deletedMessagesEnabled = true;
    public static boolean editedMessagesEnabled = true;

    // Максимальное количество сохраняемых удалённых сообщений на диалог
    public static int maxDeletedPerDialog = 200;

    // Максимальная глубина истории правок
    public static int maxEditHistory = 20;
}
