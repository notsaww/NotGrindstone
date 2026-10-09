package ru.notsaww.notgrindstone.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GrindstoneHolder implements InventoryHolder {

    private static final String[] ROMAN = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    };

    private final List<Map.Entry<Enchantment, Integer>> enchantments = new ArrayList<>();
    private final Set<Enchantment> selected = new HashSet<>();
    private Inventory inventory;

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void render(Player player) {
        ItemStack input = input();

        for (int i = 0; i < GrindstoneLayout.CONTAINER_SIZE; i++) {
            inventory.setItem(i, null);
        }
        inventory.setItem(GrindstoneLayout.ITEM_SLOT, input);

        for (int i = 0; i < GrindstoneLayout.CONTAINER_SIZE; i++) {
            if (!GrindstoneLayout.isInteractiveSlot(i)) {
                inventory.setItem(i, filler(Material.GRAY_STAINED_GLASS_PANE));
            }
        }

        inventory.setItem(GrindstoneLayout.REMOVE_ALL_SLOT, named(Material.NETHER_STAR, Messages.removeAll(player)));
        renderEnchantments();

        boolean nothingSelected = selected.isEmpty();
        inventory.setItem(GrindstoneLayout.CONFIRM_SLOT,
                named(nothingSelected ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                        nothingSelected ? Messages.nothingSelected(player) : Messages.removeSelected(player)));
    }

    private void renderEnchantments() {
        readEnchantments();

        for (int i = 0; i < GrindstoneLayout.ENCHANT_SLOTS.length; i++) {
            int slotIndex = GrindstoneLayout.ENCHANT_SLOTS[i];

            if (i >= enchantments.size()) {
                inventory.setItem(slotIndex,
                        enchantments.isEmpty() ? filler(Material.ORANGE_STAINED_GLASS_PANE) : null);
                continue;
            }

            Enchantment enchantment = enchantments.get(i).getKey();
            inventory.setItem(slotIndex, preview(enchantment, enchantments.get(i).getValue(),
                    selected.contains(enchantment)));
        }
    }

    private void readEnchantments() {
        enchantments.clear();

        ItemMeta meta = inputMeta();
        if (meta == null) {
            selected.clear();
            return;
        }

        meta.getEnchants().forEach((enchantment, level) -> enchantments.add(Map.entry(enchantment, level)));
        selected.retainAll(meta.getEnchants().keySet());
    }

    private static ItemStack filler(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack named(Material material, Component name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack preview(Enchantment enchantment, int level, boolean isSelected) {
        ItemStack book = new ItemStack(isSelected ? Material.BOOK : Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) {
            return book;
        }

        Component name = Component.translatable(enchantment);
        if (level > 1) {
            name = name.append(Component.space()).append(Component.text(roman(level)));
        }

        meta.displayName(name.color(isSelected ? NamedTextColor.RED : NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.STRIKETHROUGH, isSelected));
        book.setItemMeta(meta);
        return book;
    }

    private static String roman(int level) {
        return level >= 0 && level < ROMAN.length ? ROMAN[level] : String.valueOf(level);
    }

    public void click(Player player, int rawSlot) {
        if (rawSlot == GrindstoneLayout.ITEM_SLOT) {
            handleItemSlot(player);
        } else if (rawSlot == GrindstoneLayout.REMOVE_ALL_SLOT) {
            handleRemoveAll(player);
        } else if (rawSlot == GrindstoneLayout.CONFIRM_SLOT) {
            handleConfirm(player);
        } else if (GrindstoneLayout.isEnchantSlot(rawSlot)) {
            handleEnchantSlot(player, rawSlot);
        }
    }

    private void handleItemSlot(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack inSlot = inventory.getItem(GrindstoneLayout.ITEM_SLOT);

        player.setItemOnCursor(inSlot);
        inventory.setItem(GrindstoneLayout.ITEM_SLOT, cursor);

        selected.clear();
        render(player);
    }

    private void handleEnchantSlot(Player player, int rawSlot) {
        int index = GrindstoneLayout.enchantIndexOfSlot(rawSlot);
        if (index < 0 || index >= enchantments.size() || input() == null) {
            return;
        }

        Enchantment enchantment = enchantments.get(index).getKey();
        if (!selected.remove(enchantment)) {
            selected.add(enchantment);
        }
        render(player);
    }

    private void handleRemoveAll(Player player) {
        ItemStack input = input();
        ItemMeta meta = inputMeta();
        if (meta == null || !meta.hasEnchants()) {
            return;
        }

        int[] levels = meta.getEnchants().values().stream().mapToInt(Integer::intValue).toArray();
        int experience = RemovalCost.forLevels(levels);

        finish(player, experience, levels.length);

        meta.removeEnchantments();
        input.setItemMeta(meta);
        applyResult(toBookIfSpent(input, meta));

        selected.clear();
        render(player);
    }

    private void handleConfirm(Player player) {
        ItemStack input = input();
        ItemMeta meta = inputMeta();
        if (meta == null || selected.isEmpty()) {
            return;
        }

        int[] levels = new int[selected.size()];
        int i = 0;
        for (Enchantment enchantment : selected) {
            levels[i++] = meta.getEnchantLevel(enchantment);
            meta.removeEnchant(enchantment);
        }
        if (levels.length == 0) {
            return;
        }

        finish(player, RemovalCost.forLevels(levels), levels.length);

        input.setItemMeta(meta);
        applyResult(toBookIfSpent(input, meta));

        selected.clear();
        render(player);
    }

    private void applyResult(ItemStack result) {
        if (result != null) {
            inventory.setItem(GrindstoneLayout.ITEM_SLOT, result);
        }
    }

    private static ItemStack toBookIfSpent(ItemStack input, ItemMeta meta) {
        if (input.getType() != Material.ENCHANTED_BOOK || meta.hasEnchants()) {
            return null;
        }

        ItemStack book = new ItemStack(Material.BOOK, input.getAmount());
        if (meta.hasDisplayName() || meta.hasLore()) {
            ItemMeta bookMeta = book.getItemMeta();
            bookMeta.displayName(meta.displayName());
            bookMeta.lore(meta.lore());
            book.setItemMeta(bookMeta);
        }
        return book;
    }

    private void finish(Player player, int experience, int removed) {
        if (removed <= 0) {
            return;
        }

        player.playSound(player.getLocation(), "block.grindstone.use", SoundCategory.BLOCKS, 0.8F, 1.0F);
        if (experience > 0) {
            player.giveExp(experience);
        }
    }

    public void giveBack(Player player) {
        ItemStack input = input();
        inventory.setItem(GrindstoneLayout.ITEM_SLOT, null);
        if (input == null) {
            return;
        }

        for (ItemStack leftover : player.getInventory().addItem(input).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private ItemStack input() {
        ItemStack input = inventory.getItem(GrindstoneLayout.ITEM_SLOT);
        return input == null || input.getType().isAir() ? null : input;
    }

    private ItemMeta inputMeta() {
        ItemStack input = input();
        return input == null ? null : input.getItemMeta();
    }
}
