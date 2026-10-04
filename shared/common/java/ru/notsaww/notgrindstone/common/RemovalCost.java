package ru.notsaww.notgrindstone.common;

import java.util.Random;

/**
 * Experience cost of stripping enchantments, same formula vanilla grindstone uses:
 * every enchantment level contributes a random amount of 1 to 5 points.
 */
public final class RemovalCost {

    private RemovalCost() {
    }

    public static int forLevels(int[] levels, Random random) {
        int total = 0;
        for (int level : levels) {
            for (int i = 0; i < level; i++) {
                total += random.nextInt(5) + 1;
            }
        }
        return total;
    }
}