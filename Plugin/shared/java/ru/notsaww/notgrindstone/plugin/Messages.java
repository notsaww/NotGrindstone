package ru.notsaww.notgrindstone.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Locale;

final class Messages {

    private Messages() {
    }

    static Component title(Player player) {
        return Component.text(cis(player) ? "Точило" : "Grindstone", NamedTextColor.DARK_GRAY);
    }

    static Component removeAll(Player player) {
        return button(player, "Снять все зачарования", "Remove all enchantments", NamedTextColor.BLUE);
    }

    static Component removeSelected(Player player) {
        return button(player, "Снять выбранные чары", "Remove selected enchantments", NamedTextColor.GREEN);
    }

    static Component nothingSelected(Player player) {
        return button(player, "Сначала выбери что убрать!", "Select enchantments to remove first!", NamedTextColor.RED);
    }

    private static Component button(Player player, String ru, String en, NamedTextColor color) {
        return Component.text(cis(player) ? ru : en, color)
                .decoration(TextDecoration.ITALIC, false);
    }

    private static boolean cis(Player player) {
        return cis(player.locale());
    }

    static boolean cis(Locale locale) {
        return switch (locale.getLanguage()) {
            case "ru", "uk", "be", "kk" -> true;
            default -> false;
        };
    }
}
