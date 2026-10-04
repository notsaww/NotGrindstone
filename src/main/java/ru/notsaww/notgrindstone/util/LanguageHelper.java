package ru.notsaww.notgrindstone.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

public class LanguageHelper {

    private static final Set<String> CIS_LANGUAGES = Set.of("ru_ru", "uk_ua", "be_by", "kk_kz");

    public static String getLanguage(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            try {
                if (serverPlayer.getClientOptions() != null) {
                    String lang = serverPlayer.getClientOptions().language();
                    if (lang != null) {
                        return lang.toLowerCase();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return "en_us";
    }

    public static boolean isCisLanguage(PlayerEntity player) {
        return CIS_LANGUAGES.contains(getLanguage(player));
    }

    public static boolean isCisLanguage(String lang) {
        if (lang == null) return false;
        return CIS_LANGUAGES.contains(lang.toLowerCase());
    }
}