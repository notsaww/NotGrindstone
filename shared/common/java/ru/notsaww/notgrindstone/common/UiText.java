package ru.notsaww.notgrindstone.common;

/**
 * Translation keys for every user visible string. The Minecraft client resolves them
 * against assets/notgrindstone/lang/*.json, so no server side language sniffing is needed.
 */
public final class UiText {

    public static final String SCREEN_TITLE = "container.notgrindstone.grindstone";
    public static final String REMOVE_ALL = "notgrindstone.button.remove_all";
    public static final String REMOVE_SELECTED = "notgrindstone.button.remove_selected";
    public static final String NOTHING_SELECTED = "notgrindstone.button.nothing_selected";

    private UiText() {
    }
}