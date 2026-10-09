package ru.notsaww.notgrindstone.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.notsaww.notgrindstone.advancement.NotGrindstoneCriteria;
import ru.notsaww.notgrindstone.common.GrindstoneLayout;
import ru.notsaww.notgrindstone.common.RemovalCost;
import ru.notsaww.notgrindstone.common.UiText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class CustomGrindstoneScreenHandler extends ChestMenu {

    private static final Random RANDOM = new Random();

    /**
     * {@code LevelEvent.GRINDSTONE_USE}, the very same level event vanilla fires so the client
     * plays {@code block.grindstone.use} itself.
     */
    private static final int GRINDSTONE_USE_EVENT = 1042;

    private final SimpleContainer containerInventory;
    private final List<EnchantmentEntry> enchantmentEntries = new ArrayList<>();
    private final Set<Integer> selectedEnchantments = new HashSet<>();
    private final Level blockLevel;
    private final BlockPos blockPos;

    public CustomGrindstoneScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(GrindstoneLayout.CONTAINER_SIZE), null, null);
    }

    public CustomGrindstoneScreenHandler(int syncId, Inventory playerInventory, Level blockLevel, BlockPos blockPos) {
        this(syncId, playerInventory, new SimpleContainer(GrindstoneLayout.CONTAINER_SIZE), blockLevel, blockPos);
    }

    public CustomGrindstoneScreenHandler(int syncId, Inventory playerInventory, Container inventory) {
        this(syncId, playerInventory, inventory, null, null);
    }

    public CustomGrindstoneScreenHandler(int syncId, Inventory playerInventory, Container inventory,
                                         Level blockLevel, BlockPos blockPos) {
        super(MenuType.GENERIC_9x5, syncId, playerInventory, inventory, GrindstoneLayout.ROWS);
        this.containerInventory = (SimpleContainer) inventory;
        this.blockLevel = blockLevel;
        this.blockPos = blockPos;

        fillBackground();
        refresh();
    }

    private void fillBackground() {
        ItemStack filler = GlassPanes.hidden(DyeColor.GRAY);

        for (int i = 0; i < GrindstoneLayout.CONTAINER_SIZE; i++) {
            if (!GrindstoneLayout.isInteractiveSlot(i)) {
                containerInventory.setItem(i, filler.copy());
            }
        }
    }

    private void refresh() {
        updateRemoveAllButton();
        updateEnchantmentSlots();
        updateConfirmButton();
        broadcastChanges();
    }

    private void updateRemoveAllButton() {
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        star.set(DataComponents.CUSTOM_NAME, Component.translatable(UiText.REMOVE_ALL)
                .withStyle(style -> style.withColor(ChatFormatting.BLUE).withItalic(false)));
        containerInventory.setItem(GrindstoneLayout.REMOVE_ALL_SLOT, star);
    }

    private void updateEnchantmentSlots() {
        enchantmentEntries.clear();

        ItemStack input = containerInventory.getItem(GrindstoneLayout.ITEM_SLOT);
        if (!input.isEmpty()) {
            collect(input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), false);
            collect(input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), true);
        }

        for (int i = 0; i < GrindstoneLayout.ENCHANT_SLOTS.length; i++) {
            int slotIndex = GrindstoneLayout.ENCHANT_SLOTS[i];
            EnchantmentEntry entry = i < enchantmentEntries.size() ? enchantmentEntries.get(i) : null;

            if (entry != null) {
                boolean selected = selectedEnchantments.contains(i);
                ItemStack preview = new ItemStack(selected ? Items.BOOK : Items.ENCHANTED_BOOK);
                preview.set(DataComponents.CUSTOM_NAME, Component.empty()
                        .append(Enchantment.getFullname(entry.enchantment(), entry.level()))
                        .withStyle(style -> selected
                                ? style.withColor(ChatFormatting.RED).withItalic(false).withStrikethrough(true)
                                : style.withColor(ChatFormatting.AQUA).withItalic(false)));
                containerInventory.setItem(slotIndex, preview);
            } else if (enchantmentEntries.isEmpty()) {
                containerInventory.setItem(slotIndex, GlassPanes.hidden(DyeColor.ORANGE));
            } else {
                containerInventory.setItem(slotIndex, ItemStack.EMPTY);
            }
        }
    }

    private void collect(ItemEnchantments enchantments, boolean stored) {
        for (var entry : enchantments.entrySet()) {
            enchantmentEntries.add(new EnchantmentEntry(entry.getKey(), entry.getIntValue(), stored));
        }
    }

    private void updateConfirmButton() {
        boolean nothingSelected = selectedEnchantments.isEmpty();

        ItemStack pane = GlassPanes.hidden(nothingSelected ? DyeColor.RED : DyeColor.LIME);
        pane.set(DataComponents.CUSTOM_NAME, Component
                .translatable(nothingSelected ? UiText.NOTHING_SELECTED : UiText.REMOVE_SELECTED)
                .withStyle(style -> style
                        .withColor(nothingSelected ? ChatFormatting.RED : ChatFormatting.GREEN)
                        .withItalic(false)));
        containerInventory.setItem(GrindstoneLayout.CONFIRM_SLOT, pane);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        boolean insideContainer = slotIndex >= 0 && slotIndex < GrindstoneLayout.CONTAINER_SIZE;
        if (insideContainer && !GrindstoneLayout.isInteractiveSlot(slotIndex)) {
            broadcastChanges();
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
            super.clicked(slotIndex, button, actionType, player);
        }
    }

    private void handleItemSlotClick() {
        ItemStack cursor = getCarried();
        ItemStack inSlot = containerInventory.getItem(GrindstoneLayout.ITEM_SLOT);

        if (inSlot.isEmpty()) {
            if (cursor.isEmpty()) {
                return;
            }
            containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, cursor.copy());
            setCarried(ItemStack.EMPTY);
        } else if (cursor.isEmpty()) {
            setCarried(inSlot.copy());
            containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, ItemStack.EMPTY);
        } else {
            containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, cursor.copy());
            setCarried(inSlot.copy());
        }

        selectedEnchantments.clear();
        refresh();
    }

    private void handleEnchantSlotClick(int enchantIndex) {
        if (enchantIndex < 0 || enchantIndex >= enchantmentEntries.size()) {
            return;
        }
        if (containerInventory.getItem(GrindstoneLayout.ITEM_SLOT).isEmpty()) {
            return;
        }

        if (!selectedEnchantments.remove(enchantIndex)) {
            selectedEnchantments.add(enchantIndex);
        }
        refresh();
    }

    private void handleRemoveAll(Player player) {
        ItemStack input = containerInventory.getItem(GrindstoneLayout.ITEM_SLOT);
        if (input.isEmpty()) {
            return;
        }

        ItemEnchantments enchantments = input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments storedEnchantments = input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty() && storedEnchantments.isEmpty()) {
            return;
        }

        int removed = enchantments.size() + storedEnchantments.size();
        int experience = RemovalCost.forLevels(levelsOf(enchantments, storedEnchantments), RANDOM);

        finishRemoval(player, experience, removed);

        input.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        input.set(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, toBookIfSpent(input));

        selectedEnchantments.clear();
        refresh();
    }

    private void handleConfirm(Player player) {
        ItemStack input = containerInventory.getItem(GrindstoneLayout.ITEM_SLOT);
        if (input.isEmpty() || selectedEnchantments.isEmpty()) {
            return;
        }

        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(
                input.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        ItemEnchantments.Mutable storedEnchantments = new ItemEnchantments.Mutable(
                input.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));

        List<Integer> levels = new ArrayList<>();
        for (int index : selectedEnchantments) {
            if (index >= enchantmentEntries.size()) {
                continue;
            }
            EnchantmentEntry entry = enchantmentEntries.get(index);
            levels.add(entry.level());
            Enchantment target = entry.enchantment().value();
            if (entry.stored()) {
                storedEnchantments.removeIf(enchantment -> enchantment.value().equals(target));
            } else {
                enchantments.removeIf(enchantment -> enchantment.value().equals(target));
            }
        }

        if (levels.isEmpty()) {
            return;
        }

        finishRemoval(player, RemovalCost.forLevels(toIntArray(levels), RANDOM), levels.size());

        input.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        input.set(DataComponents.STORED_ENCHANTMENTS, storedEnchantments.toImmutable());
        containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, toBookIfSpent(input));

        selectedEnchantments.clear();
        refresh();
    }

    /**
     * Reads the levels straight off the item so the payout can never depend on a stale view of the
     * enchantment slots.
     */
    private static int[] levelsOf(ItemEnchantments enchantments, ItemEnchantments storedEnchantments) {
        List<Integer> levels = new ArrayList<>();
        collectLevels(enchantments, levels);
        collectLevels(storedEnchantments, levels);
        return toIntArray(levels);
    }

    private static void collectLevels(ItemEnchantments enchantments, List<Integer> levels) {
        for (var entry : enchantments.entrySet()) {
            levels.add(entry.getIntValue());
        }
    }

    /**
     * Vanilla turns an enchanted book back into a plain book once its last stored enchantment is
     * gone, the overhauled grindstone has to do the same.
     */
    private static ItemStack toBookIfSpent(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return stack;
        }
        if (!stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
            return stack;
        }
        if (!stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
            return stack;
        }
        return stack.transmuteCopy(Items.BOOK);
    }

    private void finishRemoval(Player player, int experience, int removed) {
        if (removed <= 0) {
            return;
        }

        playUseSound(player);

        if (player instanceof ServerPlayer serverPlayer) {
            awardExperience(serverPlayer, experience);
            NotGrindstoneCriteria.triggerDisenchanted(serverPlayer, removed);
        }
    }

    private void playUseSound(Player player) {
        // Vanilla fires level event 1042 here, which is the only path on which the client actually
        // plays block.grindstone.use itself. ServerLevel#globalLevelEvent must not be used: the
        // client only handles 1023/1028/1038 there and stays silent for everything else.
        if (blockPos != null && blockLevel instanceof ServerLevel blockServer) {
            blockServer.levelEvent(null, GRINDSTONE_USE_EVENT, blockPos, 0);
            return;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private void awardExperience(ServerPlayer player, int experience) {
        if (experience <= 0) {
            return;
        }

        ServerLevel level = player.level();
        ExperienceOrb.award(level,
                blockPos != null && blockLevel instanceof ServerLevel blockServer
                        ? Vec3.atCenterOf(blockPos)
                        : player.position(),
                experience);
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);

        if (slotIndex == GrindstoneLayout.ITEM_SLOT) {
            if (slot == null || !slot.hasItem() || !moveItemStackTo(slot.getItem(),
                    GrindstoneLayout.CONTAINER_SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            ItemStack copy = slot.getItem().copy();
            containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, ItemStack.EMPTY);
            selectedEnchantments.clear();
            refresh();
            return copy;
        }

        if (slotIndex >= GrindstoneLayout.CONTAINER_SIZE && slot != null && slot.hasItem()
                && containerInventory.getItem(GrindstoneLayout.ITEM_SLOT).isEmpty()) {
            ItemStack copy = slot.getItem().copy();
            containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, copy.copy());
            slot.set(ItemStack.EMPTY);
            selectedEnchantments.clear();
            refresh();
            return copy;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        ItemStack stack = containerInventory.getItem(GrindstoneLayout.ITEM_SLOT);
        if (stack.isEmpty()) {
            return;
        }

        containerInventory.setItem(GrindstoneLayout.ITEM_SLOT, ItemStack.EMPTY);
        if (!player.getInventory().add(stack) && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.spawnAtLocation(serverPlayer.level(), stack);
        }
    }

    private record EnchantmentEntry(Holder<Enchantment> enchantment, int level, boolean stored) {
    }
}
