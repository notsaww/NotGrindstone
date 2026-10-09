package ru.notsaww.notgrindstone.advancement;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Registers the custom criterion used by the overhauled grindstone advancements and fires it
 * whenever an item loses enchantments.
 */
public final class NotGrindstoneCriteria {

    private static final DisenchantCriterion DISENCHANTED = new DisenchantCriterion();
    private static final DisenchantCriterion DISENCHANTED_AT_ONCE = new DisenchantCriterion();

    private NotGrindstoneCriteria() {
    }

    public static void register() {
        Registry.register(Registries.CRITERION,
                Identifier.of("notgrindstone", "disenchanted"), DISENCHANTED);
        Registry.register(Registries.CRITERION,
                Identifier.of("notgrindstone", "disenchanted_at_once"), DISENCHANTED_AT_ONCE);
    }

    /**
     * @param removed how many enchantments were stripped from a single item in one go
     */
    public static void triggerDisenchanted(ServerPlayerEntity player, int removed) {
        DISENCHANTED.trigger(player, removed);
        DISENCHANTED_AT_ONCE.trigger(player, removed);
    }
}
