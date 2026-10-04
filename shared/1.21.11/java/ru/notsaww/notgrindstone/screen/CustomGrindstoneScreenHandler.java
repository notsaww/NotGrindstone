package ru.notsaww.notgrindstone.screen;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.notsaww.notgrindstone.common.GrindstoneLayout;
import ru.notsaww.notgrindstone.common.RemovalCost;
import ru.notsaww.notgrindstone.common.UiText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Replaces the vanilla grindstone menu with a 9x5 grid: one slot for the item, a list of
 * its enchantments that can be toggled individually, a "remove all" shortcut and a confirm
 * button that strips the selected enchantments for experience.
 */
public class CustomGrindstoneScreenHandler extends GenericContainerScreenHandler {

    private static final Random RANDOM = new Random();
    private static final TooltipDisplayComponent NO_TOOLTIP = new TooltipDisplayComponent(true,
            LinkedHashSet.<ComponentType<?>>newLinkedHashSet(0));

    private final SimpleInventory containerInventory;
    private final List<EnchantmentEntry> enchantmentEntries = new ArrayList<>();
    private final Set<Integer> selectedEnchantments = new HashSet<>();

    public CustomGrindstoneScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(GrindstoneLayout.CONTAINER_SIZE));
    }

    public CustomGrindstoneScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ScreenHandlerType.GENERIC_9X5, syncId, playerInventory, inventory, GrindstoneLayout.ROWS);
        this.containerInventory = (SimpleInventory) inventory;

        fillBackground();
        refresh();
    }

    // --- fake GUI content ----------------------------------------------------

    private void fillBackground() {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponentTypes.TOOLTIP_DISPLAY, NO_TOOLTIP);

        for (int i = 0; i < GrindstoneLayout.CONTAINER_SIZE; i++) {
            if (!GrindstoneLayout.isInteractiveSlot(i)) {
                containerInventory.setStack(i, filler.copy());
            }
        }
    }

    private void refresh() {
        updateRemoveAllButton();
        updateEnchantmentSlots();
        updateConfirmButton();
        sendContentUpdates();
    }

    private void updateRemoveAllButton() {
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        star.set(DataComponentTypes.CUSTOM_NAME, Text.translatable(UiText.REMOVE_ALL)
                .setStyle(Style.EMPTY.withColor(Formatting.BLUE).withItalic(false)));
        containerInventory.setStack(GrindstoneLayout.REMOVE_ALL_SLOT, star);
    }

    private void updateEnchantmentSlots() {
        enchantmentEntries.clear();

        ItemStack input = containerInventory.getStack(GrindstoneLayout.ITEM_SLOT);
        if (!input.isEmpty()) {
            collect(input.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), false);
            collect(input.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT), true);
        }

        for (int i = 0; i < GrindstoneLayout.ENCHANT_SLOTS.length; i++) {
            int slotIndex = GrindstoneLayout.ENCHANT_SLOTS[i];
            EnchantmentEntry entry = i < enchantmentEntries.size() ? enchantmentEntries.get(i) : null;

            if (entry != null) {
                boolean selected = selectedEnchantments.contains(i);
                ItemStack preview = new ItemStack(selected ? Items.BOOK : Items.ENCHANTED_BOOK);
                preview.set(DataComponentTypes.CUSTOM_NAME, Text.empty()
                        .append(Enchantment.getName(entry.enchantment(), entry.level()))
                        .setStyle(selected
                                ? Style.EMPTY.withColor(Formatting.RED).withItalic(false).withStrikethrough(true)
                                : Style.EMPTY.withColor(Formatting.AQUA).withItalic(false)));
                containerInventory.setStack(slotIndex, preview);
            } else if (enchantmentEntries.isEmpty()) {
                ItemStack placeholder = new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);
                placeholder.set(DataComponentTypes.TOOLTIP_DISPLAY, NO_TOOLTIP);
                containerInventory.setStack(slotIndex, placeholder);
            } else {
                containerInventory.setStack(slotIndex, ItemStack.EMPTY);
            }
        }
    }

    private void collect(ItemEnchantmentsComponent enchantments, boolean stored) {
        for (var entry : enchantments.getEnchantmentEntries()) {
            enchantmentEntries.add(new EnchantmentEntry(entry.getKey(), entry.getIntValue(), stored));
        }
    }

    private void updateConfirmButton() {
        boolean nothingSelected = selectedEnchantments.isEmpty();

        ItemStack pane = new ItemStack(nothingSelected ? Items.RED_STAINED_GLASS_PANE : Items.LIME_STAINED_GLASS_PANE);
        pane.set(DataComponentTypes.CUSTOM_NAME, Text
                .translatable(nothingSelected ? UiText.NOTHING_SELECTED : UiText.REMOVE_SELECTED)
                .setStyle(Style.EMPTY
                        .withColor(nothingSelected ? Formatting.RED : Formatting.GREEN)
                        .withItalic(false)));
        containerInventory.setStack(GrindstoneLayout.CONFIRM_SLOT, pane);
    }

    // --- input handling ------------------------------------------------------

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        boolean insideContainer = slotIndex >= 0 && slotIndex < GrindstoneLayout.CONTAINER_SIZE;
        if (insideContainer && !GrindstoneLayout.isInteractiveSlot(slotIndex)) {
            sendContentUpdates();
            return;
        }

        if (slotIndex == GrindstoneLayout.ITEM_SLOT) {
            handleItemSlotClick();
        } else if (slotIndex == GrindstoneLayout.REMOVE_ALL_SLOT) {
            handleRemoveAll(player);
        } else if (slotIndex == GrindstoneLayout.CONFIRM_SLOT) {
            handleConfirm(player);
        } else if (insideContainer) {
            handleEnchantSlotClick(GrindstoneLayout.enchantIndexOfSlot(slotIndex));
        } else {
            super.onSlotClick(slotIndex, button, actionType, player);
        }
    }

    private void handleItemSlotClick() {
        ItemStack cursor = getCursorStack();
        ItemStack inSlot = containerInventory.getStack(GrindstoneLayout.ITEM_SLOT);

        if (inSlot.isEmpty()) {
            if (cursor.isEmpty()) {
                return;
            }
            containerInventory.setStack(GrindstoneLayout.ITEM_SLOT, cursor.copy());
            setCursorStack(ItemStack.EMPTY);
        } else if (cursor.isEmpty()) {
            setCursorStack(inSlot.copy());
            containerInventory.setStack(GrindstoneLayout.ITEM_SLOT, ItemStack.EMPTY);
        } else {
            containerInventory.setStack(GrindstoneLayout.ITEM_SLOT, cursor.copy());
            setCursorStack(inSlot.copy());
        }

        selectedEnchantments.clear();
        refresh();
    }

    private void handleEnchantSlotClick(int enchantIndex) {
        if (enchantIndex < 0 || enchantIndex >= enchantmentEntries.size()) {
            return;
        }
        if (containerInventory.getStack(GrindstoneLayout.ITEM_SLOT).isEmpty()) {
            return;
        }

        if (!selectedEnchantments.remove(enchantIndex)) {
            selectedEnchantments.add(enchantIndex);
        }
        refresh();
    }

    private void handleRemoveAll(PlayerEntity player) {
        ItemStack input = containerInventory.getStack(GrindstoneLayout.ITEM_SLOT);
        if (input.isEmpty()) {
            return;
        }

        ItemEnchantmentsComponent enchantments = input.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent storedEnchantments = input.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        if (enchantments.isEmpty() && storedEnchantments.isEmpty()) {
            return;
        }

        int[] levels = enchantmentEntries.stream().mapToInt(EnchantmentEntry::level).toArray();
        input.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        input.set(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        selectedEnchantments.clear();
        refresh();
        finishRemoval(player, RemovalCost.forLevels(levels, RANDOM));
    }

    private void handleConfirm(PlayerEntity player) {
        ItemStack input = containerInventory.getStack(GrindstoneLayout.ITEM_SLOT);
        if (input.isEmpty() || selectedEnchantments.isEmpty()) {
            return;
        }

        ItemEnchantmentsComponent.Builder enchantments = new ItemEnchantmentsComponent.Builder(
                input.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
        ItemEnchantmentsComponent.Builder storedEnchantments = new ItemEnchantmentsComponent.Builder(
                input.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));

        List<Integer> levels = new ArrayList<>();
        for (int index : selectedEnchantments) {
            if (index >= enchantmentEntries.size()) {
                continue;
            }
            EnchantmentEntry entry = enchantmentEntries.get(index);
            levels.add(entry.level());
            RegistryEntry<Enchantment> target = entry.enchantment();
            if (entry.stored()) {
                storedEnchantments.remove(enchantment -> enchantment.equals(target));
            } else {
                enchantments.remove(enchantment -> enchantment.equals(target));
            }
        }

        input.set(DataComponentTypes.ENCHANTMENTS, enchantments.build());
        input.set(DataComponentTypes.STORED_ENCHANTMENTS, storedEnchantments.build());

        selectedEnchantments.clear();
        refresh();
        finishRemoval(player, RemovalCost.forLevels(toIntArray(levels), RANDOM));
    }

    private void finishRemoval(PlayerEntity player, int experience) {
        player.playSound(SoundEvents.BLOCK_GRINDSTONE_USE, 1.0F, 1.0F);
        if (player instanceof ServerPlayerEntity serverPlayer && experience > 0) {
            serverPlayer.addExperience(experience);
        }
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    // --- vanilla contract ----------------------------------------------------

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);

        if (slotIndex == GrindstoneLayout.ITEM_SLOT) {
            if (slot == null || !slot.hasStack() || !insertItem(slot.getStack(),
                    GrindstoneLayout.CONTAINER_SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            ItemStack copy = slot.getStack().copy();
            containerInventory.setStack(GrindstoneLayout.ITEM_SLOT, ItemStack.EMPTY);
            selectedEnchantments.clear();
            refresh();
            return copy;
        }

        if (slotIndex >= GrindstoneLayout.CONTAINER_SIZE && slot != null && slot.hasStack()
                && containerInventory.getStack(GrindstoneLayout.ITEM_SLOT).isEmpty()) {
            ItemStack copy = slot.getStack().copy();
            containerInventory.setStack(GrindstoneLayout.ITEM_SLOT, copy.copy());
            slot.setStack(ItemStack.EMPTY);
            selectedEnchantments.clear();
            refresh();
            return copy;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        ItemStack stack = containerInventory.getStack(GrindstoneLayout.ITEM_SLOT);
        if (stack.isEmpty()) {
            return;
        }

        containerInventory.setStack(GrindstoneLayout.ITEM_SLOT, ItemStack.EMPTY);
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    private record EnchantmentEntry(RegistryEntry<Enchantment> enchantment, int level, boolean stored) {
    }
}