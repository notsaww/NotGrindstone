package ru.notsaww.notgrindstone.screen;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.LinkedHashSet;

/**
 * Stained glass panes the fake GUI is drawn with. Their tooltip is suppressed so the filler
 * slots stay blank.
 *
 * <p>The panes are resolved through the registry on purpose: up to 26.1 Minecraft exposes one
 * {@code Items.<COLOR>_STAINED_GLASS_PANE} constant per colour, from 26.2 on it exposes a single
 * {@code Items.STAINED_GLASS_PANE} colour collection instead.
 */
final class GlassPanes {

    private static final TooltipDisplay NO_TOOLTIP = new TooltipDisplay(true,
            LinkedHashSet.<DataComponentType<?>>newLinkedHashSet(0));

    private GlassPanes() {
    }

    static ItemStack hidden(DyeColor color) {
        ItemStack pane = new ItemStack(BuiltInRegistries.ITEM.getValue(
                Identifier.withDefaultNamespace(color.getSerializedName() + "_stained_glass_pane")));
        pane.set(DataComponents.TOOLTIP_DISPLAY, NO_TOOLTIP);
        return pane;
    }
}