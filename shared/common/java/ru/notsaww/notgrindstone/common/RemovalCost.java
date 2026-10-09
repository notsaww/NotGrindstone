package ru.notsaww.notgrindstone.common;

import java.util.Random;

public final class RemovalCost {

    private RemovalCost() {
    }

    /**
     * @return the experience a removal is worth, or {@code 0} when no enchantment was removed.
     * Never returns {@code 0} for a non empty set of removed enchantments, so stripping an
     * enchantment always pays out at least one point.
     */
    public static int forLevels(int[] levels, Random random) {
        int total = 0;
        for (int level : levels) {
            for (int i = 0; i < level; i++) {
                total += random.nextInt(5) + 1;
            }
        }
        return levels.length == 0 ? 0 : Math.max(1, total);
    }
}
