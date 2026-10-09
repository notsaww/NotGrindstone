package ru.notsaww.notgrindstone.advancement;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the custom criterion triggers used by the overhauled grindstone advancements and
 * fires them whenever an item loses enchantments.
 */
public final class NotGrindstoneCriteria {

    private static final DisenchantCriterion DISENCHANTED = new DisenchantCriterion();
    private static final DisenchantCriterion DISENCHANTED_AT_ONCE = new DisenchantCriterion();

    private NotGrindstoneCriteria() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Identifier.fromNamespaceAndPath("notgrindstone", "disenchanted"), DISENCHANTED);
        Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                Identifier.fromNamespaceAndPath("notgrindstone", "disenchanted_at_once"), DISENCHANTED_AT_ONCE);
    }

    /**
     * @param removed how many enchantments were stripped from a single item in one go
     */
    public static void triggerDisenchanted(ServerPlayer player, int removed) {
        DISENCHANTED.trigger(player, removed);
        DISENCHANTED_AT_ONCE.trigger(player, removed);
    }
}
