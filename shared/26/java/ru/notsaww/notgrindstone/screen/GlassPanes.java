package ru.notsaww.notgrindstone.screen;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.LinkedHashSet;

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
