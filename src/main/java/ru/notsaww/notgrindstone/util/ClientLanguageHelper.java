package ru.notsaww.notgrindstone.util;

import net.minecraft.client.MinecraftClient;

public class ClientLanguageHelper {

    public static String getClientLanguage() {
        return MinecraftClient.getInstance().getLanguageManager().getLanguage();
    }
}