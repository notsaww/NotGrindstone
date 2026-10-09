package ru.notsaww.notgrindstone.plugin;

import java.util.Random;

final class RemovalCost {

    private static final Random RANDOM = new Random();

    private RemovalCost() {
    }

    static int forLevels(int[] levels) {
        int total = 0;
        for (int level : levels) {
            for (int i = 0; i < level; i++) {
                total += RANDOM.nextInt(5) + 1;
            }
        }
        return levels.length == 0 ? 0 : Math.max(1, total);
    }
}
