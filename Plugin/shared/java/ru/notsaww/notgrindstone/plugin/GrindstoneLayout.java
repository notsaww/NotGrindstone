package ru.notsaww.notgrindstone.plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class GrindstoneLayout {

    static final int COLUMNS = 9;
    static final int ROWS = 5;
    static final int CONTAINER_SIZE = COLUMNS * ROWS;

    static final int REMOVE_ALL_SLOT = 4;
    static final int ITEM_SLOT = 19;
    static final int CONFIRM_SLOT = 26;

    static final int[] ENCHANT_SLOTS = {
            12, 13, 14, 15, 16,
            21, 22, 23, 24, 25,
            30, 31, 32, 33, 34
    };

    private static final Set<Integer> INTERACTIVE_SLOTS;

    static {
        Set<Integer> slots = new HashSet<>();
        slots.add(REMOVE_ALL_SLOT);
        slots.add(ITEM_SLOT);
        slots.add(CONFIRM_SLOT);
        for (int slot : ENCHANT_SLOTS) {
            slots.add(slot);
        }
        INTERACTIVE_SLOTS = Collections.unmodifiableSet(slots);
    }

    private GrindstoneLayout() {
    }

    static boolean isInteractiveSlot(int slotIndex) {
        return INTERACTIVE_SLOTS.contains(slotIndex);
    }

    static boolean isEnchantSlot(int slotIndex) {
        return enchantIndexOfSlot(slotIndex) >= 0;
    }

    static int enchantIndexOfSlot(int slotIndex) {
        for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
            if (ENCHANT_SLOTS[i] == slotIndex) {
                return i;
            }
        }
        return -1;
    }
}
