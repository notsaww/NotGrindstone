package ru.notsaww.notgrindstone;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotGrindstone implements ModInitializer {

    public static final String MOD_ID = "notgrindstone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("NotGrindstone loaded! Overhaul active.");
    }
}