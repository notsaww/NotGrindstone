package ru.notsaww.notgrindstone.screen;

import ru.notsaww.notgrindstone.util.LanguageHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
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

import java.util.*;

public class CustomGrindstoneScreenHandler extends GenericContainerScreenHandler {

    private static final int CONTAINER_SIZE = 45;
    private static final int[] ENCHANT_SLOTS = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34};
    private static final int REMOVE_ALL_SLOT = 4;
    private static final int ITEM_SLOT = 19;
    private static final int CONFIRM_SLOT = 26;
    private static final Set<Integer> INTERACTIVE_SLOTS = new HashSet<>();

    static {
        INTERACTIVE_SLOTS.add(REMOVE_ALL_SLOT);
        INTERACTIVE_SLOTS.add(ITEM_SLOT);
        INTERACTIVE_SLOTS.add(CONFIRM_SLOT);
        for (int s : ENCHANT_SLOTS) {
            INTERACTIVE_SLOTS.add(s);
        }
    }

    private final SimpleInventory containerInventory;
    private final PlayerEntity player;
    private final String playerLanguage;
    private final List<EnchantmentEntry> enchantmentEntries = new ArrayList<>();
    private final Set<Integer> selectedEnchantments = new HashSet<>();

    public CustomGrindstoneScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(CONTAINER_SIZE));
    }

    public CustomGrindstoneScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ScreenHandlerType.GENERIC_9X5, syncId, playerInventory, inventory, 5);
        this.player = playerInventory.player;
        this.containerInventory = (SimpleInventory) inventory;
        this.playerLanguage = LanguageHelper.getLanguage(player);

        fillBackground();
        updateDisplay();
    }

    private void fillBackground() {
        ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));

        for (int i = 0; i < CONTAINER_SIZE; i++) {
            if (!INTERACTIVE_SLOTS.contains(i)) {
                containerInventory.setStack(i, glass.copy());
            }
        }
    }

    private void updateDisplay() {
        updateRemoveAllButton();
        updateEnchantmentSlots();
        updateConfirmButton();
        sendContentUpdates();
    }

    private void updateRemoveAllButton() {
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        boolean isCis = LanguageHelper.isCisLanguage(playerLanguage);
        Text name = Text.literal(isCis ? "Снять все зачарования" : "Remove all enchantments")
                .setStyle(Style.EMPTY.withColor(Formatting.BLUE).withItalic(false));
        star.set(DataComponentTypes.CUSTOM_NAME, name);
        containerInventory.setStack(REMOVE_ALL_SLOT, star);
    }

    private void updateEnchantmentSlots() {
        enchantmentEntries.clear();
        ItemStack inputItem = containerInventory.getStack(ITEM_SLOT);

        if (!inputItem.isEmpty()) {
            ItemEnchantmentsComponent enchantments = inputItem.getOrDefault(
                    DataComponentTypes.ENCHANTMENTS,
                    ItemEnchantmentsComponent.DEFAULT
            );
            for (var entry : enchantments.getEnchantmentEntries()) {
                enchantmentEntries.add(new EnchantmentEntry(entry.getKey(), entry.getIntValue(), false));
            }

            ItemEnchantmentsComponent storedEnchantments = inputItem.getOrDefault(
                    DataComponentTypes.STORED_ENCHANTMENTS,
                    ItemEnchantmentsComponent.DEFAULT
            );
            for (var entry : storedEnchantments.getEnchantmentEntries()) {
                enchantmentEntries.add(new EnchantmentEntry(entry.getKey(), entry.getIntValue(), true));
            }
        }

        boolean hasEnchants = !enchantmentEntries.isEmpty();

        for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
            int slotIndex = ENCHANT_SLOTS[i];

            if (i < enchantmentEntries.size()) {
                EnchantmentEntry entry = enchantmentEntries.get(i);

                if (selectedEnchantments.contains(i)) {
                    ItemStack book = new ItemStack(Items.BOOK);
                    Text enchantName = Enchantment.getName(entry.enchantment, entry.level);
                    Text displayName = Text.literal("")
                            .append(enchantName)
                            .setStyle(Style.EMPTY.withStrikethrough(true).withItalic(false).withColor(Formatting.RED));
                    book.set(DataComponentTypes.CUSTOM_NAME, displayName);
                    containerInventory.setStack(slotIndex, book);
                } else {
                    ItemStack enchBook = new ItemStack(Items.ENCHANTED_BOOK);
                    Text enchantName = Enchantment.getName(entry.enchantment, entry.level);
                    Text displayName = Text.literal("")
                            .append(enchantName)
                            .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.AQUA));
                    enchBook.set(DataComponentTypes.CUSTOM_NAME, displayName);
                    containerInventory.setStack(slotIndex, enchBook);
                }
            } else {
                if (hasEnchants) {
                    containerInventory.setStack(slotIndex, ItemStack.EMPTY);
                } else {
                    ItemStack orangeGlass = new ItemStack(Items.ORANGE_STAINED_GLASS_PANE);
                    orangeGlass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
                    containerInventory.setStack(slotIndex, orangeGlass);
                }
            }
        }
    }

    private void updateConfirmButton() {
        boolean isCis = LanguageHelper.isCisLanguage(playerLanguage);

        if (selectedEnchantments.isEmpty()) {
            ItemStack redGlass = new ItemStack(Items.RED_STAINED_GLASS_PANE);
            Text name = Text.literal(isCis ? "Сначала выбери что убрать!" : "Enchantments not selected!")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(false));
            redGlass.set(DataComponentTypes.CUSTOM_NAME, name);
            containerInventory.setStack(CONFIRM_SLOT, redGlass);
        } else {
            ItemStack limeGlass = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
            Text name = Text.literal(isCis ? "Снять выбранные чары" : "Remove selected")
                    .setStyle(Style.EMPTY.withColor(Formatting.GREEN).withItalic(false));
            limeGlass.set(DataComponentTypes.CUSTOM_NAME, name);
            containerInventory.setStack(CONFIRM_SLOT, limeGlass);
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex < 0) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }

        if (slotIndex >= CONTAINER_SIZE) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }

        if (!INTERACTIVE_SLOTS.contains(slotIndex)) {
            sendContentUpdates();
            return;
        }

        if (slotIndex == ITEM_SLOT) {
            handleItemSlotClick();
        } else if (slotIndex == REMOVE_ALL_SLOT) {
            handleRemoveAll(player);
        } else if (slotIndex == CONFIRM_SLOT) {
            handleConfirm(player);
        } else if (isEnchantSlot(slotIndex)) {
            handleEnchantSlotClick(slotIndex);
        }
    }

    private void handleItemSlotClick() {
        ItemStack cursor = getCursorStack();
        ItemStack inSlot = containerInventory.getStack(ITEM_SLOT);

        if (inSlot.isEmpty()) {
            if (!cursor.isEmpty()) {
                containerInventory.setStack(ITEM_SLOT, cursor.copy());
                setCursorStack(ItemStack.EMPTY);
                selectedEnchantments.clear();
                updateDisplay();
            }
        } else {
            if (cursor.isEmpty()) {
                setCursorStack(inSlot.copy());
                containerInventory.setStack(ITEM_SLOT, ItemStack.EMPTY);
                selectedEnchantments.clear();
                updateDisplay();
            } else {
                ItemStack old = inSlot.copy();
                containerInventory.setStack(ITEM_SLOT, cursor.copy());
                setCursorStack(old);
                selectedEnchantments.clear();
                updateDisplay();
            }
        }
    }

    private void handleRemoveAll(PlayerEntity player) {
        ItemStack inputItem = containerInventory.getStack(ITEM_SLOT);
        if (inputItem.isEmpty()) return;

        ItemEnchantmentsComponent enchants = inputItem.getOrDefault(
                DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent storedEnchants = inputItem.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        if (enchants.isEmpty() && storedEnchants.isEmpty()) return;

        int totalExp = calculateExperience(enchantmentEntries);

        inputItem.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        inputItem.set(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        containerInventory.setStack(ITEM_SLOT, inputItem.copy());
        selectedEnchantments.clear();
        enchantmentEntries.clear();
        updateDisplay();

        player.playSound(SoundEvents.BLOCK_GRINDSTONE_USE, 1.0F, 1.0F);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            dropExperience(serverPlayer, totalExp);
        }
    }

    private void handleEnchantSlotClick(int slotIndex) {
        ItemStack inputItem = containerInventory.getStack(ITEM_SLOT);
        if (inputItem.isEmpty()) return;

        int enchIndex = getEnchantIndexFromSlot(slotIndex);
        if (enchIndex < 0 || enchIndex >= enchantmentEntries.size()) return;

        if (selectedEnchantments.contains(enchIndex)) {
            selectedEnchantments.remove(enchIndex);
        } else {
            selectedEnchantments.add(enchIndex);
        }

        updateDisplay();
    }

    private void handleConfirm(PlayerEntity player) {
        ItemStack inputItem = containerInventory.getStack(ITEM_SLOT);
        if (selectedEnchantments.isEmpty() || inputItem.isEmpty()) return;

        List<EnchantmentEntry> toRemove = new ArrayList<>();
        for (int idx : selectedEnchantments) {
            if (idx < enchantmentEntries.size()) {
                toRemove.add(enchantmentEntries.get(idx));
            }
        }

        int totalExp = calculateExperience(toRemove);

        ItemEnchantmentsComponent currentEnchants = inputItem.getOrDefault(
                DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent currentStoredEnchants = inputItem.getOrDefault(
                DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        ItemEnchantmentsComponent.Builder regularBuilder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent.Builder storedBuilder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

        for (var entry : currentEnchants.getEnchantmentEntries()) {
            boolean shouldRemove = false;
            for (int idx : selectedEnchantments) {
                if (idx < enchantmentEntries.size()) {
                    EnchantmentEntry selected = enchantmentEntries.get(idx);
                    if (!selected.isStored && entry.getKey().equals(selected.enchantment) && entry.getIntValue() == selected.level) {
                        shouldRemove = true;
                        break;
                    }
                }
            }
            if (!shouldRemove) {
                regularBuilder.add(entry.getKey(), entry.getIntValue());
            }
        }

        for (var entry : currentStoredEnchants.getEnchantmentEntries()) {
            boolean shouldRemove = false;
            for (int idx : selectedEnchantments) {
                if (idx < enchantmentEntries.size()) {
                    EnchantmentEntry selected = enchantmentEntries.get(idx);
                    if (selected.isStored && entry.getKey().equals(selected.enchantment) && entry.getIntValue() == selected.level) {
                        shouldRemove = true;
                        break;
                    }
                }
            }
            if (!shouldRemove) {
                storedBuilder.add(entry.getKey(), entry.getIntValue());
            }
        }

        inputItem.set(DataComponentTypes.ENCHANTMENTS, regularBuilder.build());
        inputItem.set(DataComponentTypes.STORED_ENCHANTMENTS, storedBuilder.build());
        containerInventory.setStack(ITEM_SLOT, inputItem.copy());
        selectedEnchantments.clear();
        updateDisplay();

        player.playSound(SoundEvents.BLOCK_GRINDSTONE_USE, 1.0F, 1.0F);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            dropExperience(serverPlayer, totalExp);
        }
    }

    private int calculateExperience(List<EnchantmentEntry> entries) {
        int totalExp = 0;
        Random random = new Random();

        for (EnchantmentEntry entry : entries) {
            int level = entry.level;
            for (int i = 0; i < level; i++) {
                totalExp += random.nextInt(5) + 1;
            }
        }

        return totalExp;
    }

    private void dropExperience(ServerPlayerEntity player, int amount) {
        if (amount <= 0) return;
        player.addExperience(amount);
    }

    private int getEnchantIndexFromSlot(int slotIndex) {
        for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
            if (ENCHANT_SLOTS[i] == slotIndex) {
                return i;
            }
        }
        return -1;
    }

    private boolean isEnchantSlot(int slotIndex) {
        for (int s : ENCHANT_SLOTS) {
            if (s == slotIndex) return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (slotIndex == ITEM_SLOT) {
            if (!this.insertItem(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            containerInventory.setStack(ITEM_SLOT, ItemStack.EMPTY);
            selectedEnchantments.clear();
            updateDisplay();
            return copy;
        } else if (slotIndex >= CONTAINER_SIZE) {
            ItemStack currentInSlot = containerInventory.getStack(ITEM_SLOT);
            if (currentInSlot.isEmpty()) {
                containerInventory.setStack(ITEM_SLOT, stack.copy());
                slot.setStack(ItemStack.EMPTY);
                selectedEnchantments.clear();
                updateDisplay();
                return copy;
            }
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
        ItemStack stack = containerInventory.getStack(ITEM_SLOT);
        if (!stack.isEmpty()) {
            containerInventory.setStack(ITEM_SLOT, ItemStack.EMPTY);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
        }
    }

    private record EnchantmentEntry(RegistryEntry<Enchantment> enchantment, int level, boolean isStored) {
    }
}