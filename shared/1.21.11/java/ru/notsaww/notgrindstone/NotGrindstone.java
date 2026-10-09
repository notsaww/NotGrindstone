package ru.notsaww.notgrindstone;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.notsaww.notgrindstone.advancement.NotGrindstoneCriteria;

public class NotGrindstone implements ModInitializer {

    public static final String MOD_ID = "notgrindstone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        NotGrindstoneCriteria.register();
        LOGGER.info("NotGrindstone loaded! Overhaul active.");
    }
}
