package ru.notsaww.notgrindstone.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public final class GrindstoneListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.GRINDSTONE) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof GrindstoneHolder) {
            return;
        }

        event.setCancelled(true);

        GrindstoneHolder holder = new GrindstoneHolder();
        Inventory inventory = Bukkit.createInventory(holder, GrindstoneLayout.CONTAINER_SIZE, Messages.title(player));
        holder.attach(inventory);
        holder.render(player);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GrindstoneHolder holder)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            event.setCancelled(true);
            return;
        }

        if (clicked.equals(event.getInventory()) && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            holder.click(player, event.getRawSlot());
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GrindstoneHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < GrindstoneLayout.CONTAINER_SIZE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GrindstoneHolder holder
                && event.getPlayer() instanceof Player player) {
            holder.giveBack(player);
        }
    }
}
