package ru.notsaww.notgrindstone.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class NotGrindstonePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new GrindstoneListener(), this);
        getLogger().info("NotGrindstone enabled!");
    }
}
